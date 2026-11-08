package com.example.util

import com.example.data.model.CarrierInfo

object CarrierLookupEngine {

    fun lookup(rawNumber: String): CarrierInfo {
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.isEmpty()) {
            return CarrierInfo(name = "Unknown Carrier", country = "Local", countryCode = "")
        }

        // Normalize BD numbers
        val bdNumber = when {
            cleanNumber.startsWith("+880") -> cleanNumber.removePrefix("+880")
            cleanNumber.startsWith("880") -> cleanNumber.removePrefix("880")
            cleanNumber.startsWith("0") -> cleanNumber.removePrefix("0")
            else -> cleanNumber
        }

        // Check if it's likely a Bangladeshi mobile/landline number
        if (cleanNumber.startsWith("+880") || cleanNumber.startsWith("880") || cleanNumber.startsWith("01") || cleanNumber.startsWith("02")) {
            val prefix2 = if (bdNumber.length >= 2) bdNumber.substring(0, 2) else ""
            when {
                prefix2 == "17" || prefix2 == "13" -> return CarrierInfo(
                    name = "Grameenphone",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "National (GP 4G/5G)",
                    primaryColorHex = 0xFF0288D1
                )
                prefix2 == "19" || prefix2 == "14" -> return CarrierInfo(
                    name = "Banglalink",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "National (BL 4G)",
                    primaryColorHex = 0xFFF57C00
                )
                prefix2 == "18" -> return CarrierInfo(
                    name = "Robi Axiata",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "National (Robi 4.5G)",
                    primaryColorHex = 0xFFD32F2F
                )
                prefix2 == "16" -> return CarrierInfo(
                    name = "Airtel Bangladesh",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "National (Airtel)",
                    primaryColorHex = 0xFFE53935
                )
                prefix2 == "15" -> return CarrierInfo(
                    name = "Teletalk",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "State Telecom (Teletalk 4G)",
                    primaryColorHex = 0xFF388E3C
                )
                bdNumber.startsWith("2") -> return CarrierInfo(
                    name = "BTCL Landline",
                    country = "Bangladesh",
                    countryCode = "+880",
                    circle = "Dhaka Division",
                    primaryColorHex = 0xFF455A64
                )
            }
        }

        // Check International prefixes
        if (cleanNumber.startsWith("+1")) {
            return CarrierInfo("North America Telecom", "USA / Canada", "+1", "US/CA", primaryColorHex = 0xFF1976D2)
        } else if (cleanNumber.startsWith("+44")) {
            return CarrierInfo("UK Mobile / Landline", "United Kingdom", "+44", "UK", primaryColorHex = 0xFF7B1FA2)
        } else if (cleanNumber.startsWith("+91")) {
            return CarrierInfo("Indian Telecom", "India", "+91", "IN (Jio/Airtel)", primaryColorHex = 0xFFFF6F00)
        } else if (cleanNumber.startsWith("+971")) {
            return CarrierInfo("Etisalat / du", "United Arab Emirates", "+971", "UAE", primaryColorHex = 0xFF00796B)
        } else if (cleanNumber.startsWith("+966")) {
            return CarrierInfo("STC / Mobily", "Saudi Arabia", "+966", "KSA", primaryColorHex = 0xFF2E7D32)
        } else if (cleanNumber.startsWith("+60")) {
            return CarrierInfo("Celcom / Maxis", "Malaysia", "+60", "MY", primaryColorHex = 0xFF0288D1)
        } else if (cleanNumber.startsWith("+65")) {
            return CarrierInfo("Singtel / StarHub", "Singapore", "+65", "SG", primaryColorHex = 0xFFC2185B)
        } else if (cleanNumber.startsWith("+81")) {
            return CarrierInfo("NTT Docomo / SoftBank", "Japan", "+81", "JP", primaryColorHex = 0xFF512DA8)
        }

        return CarrierInfo(
            name = "Cellular Network",
            country = "Mobile",
            countryCode = "",
            primaryColorHex = 0xFF475569
        )
    }

    fun formatDisplayNumber(raw: String): String {
        val digits = raw.replace("[^0-9+]".toRegex(), "")
        if (digits.startsWith("+880") && digits.length >= 13) {
            val prefix = digits.substring(0, 4) // +880
            val operator = digits.substring(4, 7) // 171
            val mid = digits.substring(7, 10)
            val rest = digits.substring(10)
            return "$prefix $operator-$mid-$rest"
        } else if (digits.startsWith("01") && digits.length == 11) {
            val op = digits.substring(0, 5) // 01712
            val rest = digits.substring(5)
            return "$op-$rest"
        }
        return raw
    }
}
