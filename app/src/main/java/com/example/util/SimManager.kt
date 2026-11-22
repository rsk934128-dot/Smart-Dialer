package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimSlotInfo(
    val slotNumber: Int, // 1 for SIM 1, 2 for SIM 2
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String = "",
    val accountId: String = "",
    val phoneAccountHandle: PhoneAccountHandle? = null
)

object SimManager {

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads active dual-SIM information from SubscriptionManager and pairs it with
     * TelecomManager's call-capable PhoneAccountHandles so outgoing calls & logs
     * can accurately recognize SIM 1 and SIM 2.
     */
    @SuppressLint("MissingPermission")
    fun getAvailableSims(context: Context): List<SimSlotInfo> {
        val result = mutableListOf<SimSlotInfo>()
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

            val phoneAccounts: List<PhoneAccountHandle> = try {
                if (hasPhoneStatePermission(context)) {
                    telecomManager?.callCapablePhoneAccounts ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            val subscriptions: List<SubscriptionInfo> = try {
                if (hasPhoneStatePermission(context)) {
                    subscriptionManager?.activeSubscriptionInfoList
                        ?.sortedBy { it.simSlotIndex }
                        ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            if (subscriptions.isNotEmpty()) {
                val usedHandles = mutableSetOf<Int>()
                subscriptions.forEachIndexed { index, sub ->
                    val slotNum = (sub.simSlotIndex + 1).coerceAtLeast(1)
                    val matchedHandleIdx = findMatchingAccountIndex(sub, index, phoneAccounts, usedHandles, telecomManager)
                    val matchedHandle = if (matchedHandleIdx >= 0) {
                        usedHandles.add(matchedHandleIdx)
                        phoneAccounts[matchedHandleIdx]
                    } else {
                        null
                    }

                    val rawCarrier = sub.carrierName?.toString()?.trim().orEmpty()
                    val rawDisplay = sub.displayName?.toString()?.trim().orEmpty()
                    val carrierLabel = when {
                        rawCarrier.isNotEmpty() -> rawCarrier
                        rawDisplay.isNotEmpty() -> rawDisplay
                        else -> "SIM $slotNum"
                    }

                    @Suppress("DEPRECATION")
                    val simNumber = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            subscriptionManager?.getPhoneNumber(sub.subscriptionId).orEmpty()
                        } else {
                            sub.number.orEmpty()
                        }
                    } catch (_: Exception) {
                        ""
                    }

                    result.add(
                        SimSlotInfo(
                            slotNumber = slotNum,
                            subscriptionId = sub.subscriptionId,
                            displayName = if (rawDisplay.isNotEmpty()) rawDisplay else "SIM $slotNum",
                            carrierName = carrierLabel,
                            number = simNumber,
                            accountId = matchedHandle?.id ?: sub.subscriptionId.toString(),
                            phoneAccountHandle = matchedHandle
                        )
                    )
                }
            } else if (phoneAccounts.isNotEmpty()) {
                phoneAccounts.forEachIndexed { index, handle ->
                    val slotNum = index + 1
                    val accountLabel = try {
                        telecomManager?.getPhoneAccount(handle)?.label?.toString()?.trim().orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    result.add(
                        SimSlotInfo(
                            slotNumber = slotNum,
                            subscriptionId = slotNum,
                            displayName = "SIM $slotNum",
                            carrierName = accountLabel.ifEmpty { "SIM $slotNum" },
                            number = "",
                            accountId = handle.id,
                            phoneAccountHandle = handle
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        } catch (_: Exception) {
            // OEM telephony fallback
        }
        return result.sortedBy { it.slotNumber }
    }

    private fun findMatchingAccountIndex(
        sub: SubscriptionInfo,
        subPosition: Int,
        phoneAccounts: List<PhoneAccountHandle>,
        usedIndices: Set<Int>,
        telecomManager: TelecomManager?
    ): Int {
        if (phoneAccounts.isEmpty()) return -1

        val subIdStr = sub.subscriptionId.toString()
        val iccId = sub.iccId.orEmpty()
        val carrier = sub.carrierName?.toString().orEmpty()

        // 1. Direct ID / ICCID match on PhoneAccountHandle.id
        phoneAccounts.forEachIndexed { idx, handle ->
            if (idx !in usedIndices) {
                val id = handle.id
                if (id == subIdStr ||
                    (iccId.length >= 6 && id.contains(iccId, ignoreCase = true)) ||
                    id.contains("sub$subIdStr", ignoreCase = true)
                ) {
                    return idx
                }
            }
        }

        // 2. Label match against carrier/display name
        if (carrier.isNotBlank()) {
            phoneAccounts.forEachIndexed { idx, handle ->
                if (idx !in usedIndices) {
                    val label = try {
                        telecomManager?.getPhoneAccount(handle)?.label?.toString().orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    if (label.isNotBlank() && label.contains(carrier, ignoreCase = true)) {
                        return idx
                    }
                }
            }
        }

        // 3. Positional fallback if unused
        if (subPosition < phoneAccounts.size && subPosition !in usedIndices) {
            return subPosition
        }

        return phoneAccounts.indices.firstOrNull { it !in usedIndices } ?: -1
    }

    /**
     * Resolves a PhoneAccountHandle or CallLog PHONE_ACCOUNT_ID string to 1 (SIM 1) or 2 (SIM 2).
     */
    fun resolveSimSlot(
        context: Context,
        handle: PhoneAccountHandle? = null,
        phoneAccountId: String? = null
    ): Int {
        val targetId = handle?.id ?: phoneAccountId
        val sims = getAvailableSims(context)
        return resolveSimSlotFromList(targetId, sims)
    }

    fun resolveSimSlotFromList(targetAccountId: String?, sims: List<SimSlotInfo>): Int {
        if (targetAccountId.isNullOrBlank()) return 1

        // Exact or substring match against known SIMs
        val matched = sims.firstOrNull { sim ->
            sim.accountId == targetAccountId ||
                sim.subscriptionId.toString() == targetAccountId ||
                (sim.accountId.length >= 4 && targetAccountId.contains(sim.accountId, ignoreCase = true)) ||
                (targetAccountId.length >= 4 && sim.accountId.contains(targetAccountId, ignoreCase = true))
        }
        if (matched != null) {
            return matched.slotNumber.coerceIn(1, 2)
        }

        // Heuristic fallback for common Android Telecom IDs
        val lower = targetAccountId.lowercase()
        return when {
            lower.contains("sim2") || lower.contains("slot2") || lower.contains("slot_1") -> 2
            lower.contains("sim1") || lower.contains("slot1") || lower.contains("slot_0") -> 1
            lower == "2" -> 2
            else -> 1
        }
    }

    /**
     * Places an outgoing call using TelecomManager (with explicit SIM PhoneAccountHandle when selected)
     * and gracefully falls back to ACTION_CALL intent.
     *
     * @return true if a real Android telephony dial request was dispatched to the OS, false otherwise.
     */
    @SuppressLint("MissingPermission")
    fun placeOutgoingCall(
        context: Context,
        rawNumber: String,
        selectedSim: SimSlotInfo?
    ): Boolean {
        if (!hasCallPhonePermission(context)) return false
        val uri = Uri.fromParts("tel", rawNumber, null)

        // Primary path: TelecomManager.placeCall with selected SIM PhoneAccountHandle
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && selectedSim?.phoneAccountHandle != null) {
                val extras = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim.phoneAccountHandle)
                }
                telecomManager.placeCall(uri, extras)
                return true
            }
        } catch (_: SecurityException) {
            // Fall through to Intent
        } catch (_: Exception) {
            // Fall through to Intent
        }

        // Fallback path: ACTION_CALL Intent with slot + handle extras
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(rawNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                selectedSim?.let { sim ->
                    sim.phoneAccountHandle?.let { handle ->
                        putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    }
                    putExtra("com.android.phone.extra.slot", (sim.slotNumber - 1).coerceAtLeast(0))
                    putExtra("simSlot", (sim.slotNumber - 1).coerceAtLeast(0))
                    putExtra("subscription_id", sim.subscriptionId)
                }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
