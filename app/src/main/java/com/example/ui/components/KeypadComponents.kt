package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen
import com.example.ui.viewmodel.AppLanguage
import com.example.util.T9SearchEngine

data class KeyData(
    val digit: Char,
    val subtext: String
)

private fun spacedAlphabet(digit: Char): String {
    val letters = T9SearchEngine.getLettersForDigit(digit)
    return if (letters.isNotEmpty()) letters.toCharArray().joinToString(" ") else " "
}

val KEYPAD_KEYS = listOf(
    listOf(KeyData('1', " "), KeyData('2', spacedAlphabet('2')), KeyData('3', spacedAlphabet('3'))),
    listOf(KeyData('4', spacedAlphabet('4')), KeyData('5', spacedAlphabet('5')), KeyData('6', spacedAlphabet('6'))),
    listOf(KeyData('7', spacedAlphabet('7')), KeyData('8', spacedAlphabet('8')), KeyData('9', spacedAlphabet('9'))),
    listOf(KeyData('*', " "), KeyData('0', "+"), KeyData('#', " "))
)

@Composable
fun T9KeypadInterface(
    inputQuery: String,
    matchCount: Int,
    language: AppLanguage = AppLanguage.ENGLISH,
    availableSims: List<com.example.util.SimSlotInfo> = emptyList(),
    selectedSimSlot: Int = 1,
    hasPhonePermission: Boolean = true,
    onSelectSimSlot: (Int) -> Unit = {},
    onRequestPhonePermission: () -> Unit = {},
    onDigitClick: (Char) -> Unit,
    onLongDigitClick: (Char) -> Unit,
    onCallClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearAllClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onToggleKeypad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeT9Digits = remember(inputQuery) {
        inputQuery.filter { it in '2'..'9' }.toSet()
    }
    val queryPairs = remember(inputQuery) {
        T9SearchEngine.describeQueryT9Letters(inputQuery)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("t9_keypad_interface"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dual-SIM Selector Bar (SIM 1 / SIM 2)
            SimSelectorRow(
                availableSims = availableSims,
                selectedSimSlot = selectedSimSlot,
                hasPhonePermission = hasPhonePermission,
                language = language,
                onSelectSimSlot = onSelectSimSlot,
                onRequestPhonePermission = onRequestPhonePermission,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // Subtle handle & T9 alphabet mapping strip
            AnimatedVisibility(
                visible = queryPairs.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                T9MappingStrip(
                    queryPairs = queryPairs,
                    matchCount = matchCount,
                    language = language,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (queryPairs.isEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("t9_idle_hint"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI)
                            "T9: নাম খুঁজতে ২–৯ চাপুন"
                        else
                            "T9: Tap 2–9 to search contacts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            KeypadMatrix(
                onDigitClick = onDigitClick,
                onLongDigitClick = onLongDigitClick,
                highlightedDigits = activeT9Digits
            )

            KeypadBottomBar(
                hasInput = inputQuery.isNotEmpty(),
                selectedSimSlot = selectedSimSlot,
                hasDetectedSim = availableSims.isNotEmpty(),
                onCallClick = onCallClick,
                onBackspaceClick = onBackspaceClick,
                onClearAllClick = onClearAllClick,
                onAddContactClick = onAddContactClick,
                onToggleKeypad = onToggleKeypad
            )
        }
    }
}

@Composable
private fun SimSelectorRow(
    availableSims: List<com.example.util.SimSlotInfo>,
    selectedSimSlot: Int,
    hasPhonePermission: Boolean,
    language: AppLanguage,
    onSelectSimSlot: (Int) -> Unit,
    onRequestPhonePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("sim_selector_row"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hasPhonePermission) {
            Surface(
                onClick = onRequestPhonePermission,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                modifier = Modifier.testTag("btn_grant_sim_permission")
            ) {
                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "সিম ১ / সিম ২ চিনতে Phone পারমিশন দিন"
                    else
                        "Grant Phone permission to detect SIM 1 & SIM 2",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        } else if (availableSims.isNotEmpty()) {
            availableSims.forEach { sim ->
                val isSelected = selectedSimSlot == sim.slotNumber
                val chipAccent = if (sim.slotNumber == 1) CallGreen else MaterialTheme.colorScheme.primary
                Surface(
                    onClick = { onSelectSimSlot(sim.slotNumber) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) chipAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        width = if (isSelected) 1.2.dp else 0.5.dp,
                        color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("sim_chip_${sim.slotNumber}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SIM ${sim.slotNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (sim.carrierName.isNotBlank() && sim.carrierName != "SIM ${sim.slotNumber}") {
                            Text(
                                text = "• ${sim.carrierName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Device reports 0 active hardware SIMs; still let user pick SIM 1 / SIM 2 tag
            listOf(1, 2).forEach { slot ->
                val isSelected = selectedSimSlot == slot
                val chipAccent = if (slot == 1) CallGreen else MaterialTheme.colorScheme.primary
                Surface(
                    onClick = { onSelectSimSlot(slot) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) chipAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("sim_chip_$slot")
                ) {
                    Text(
                        text = "SIM $slot",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun T9MappingStrip(
    queryPairs: List<Pair<Char, String>>,
    matchCount: Int,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("t9_mapping_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            queryPairs.takeLast(7).forEach { (digit, letters) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = digit.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = letters,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (matchCount > 0) CallGreen.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (language == AppLanguage.BENGALI) {
                    if (matchCount == 0) "কোনো মিল নেই" else "$matchCount টি মিল"
                } else {
                    when (matchCount) {
                        0 -> "No match"
                        1 -> "1 match"
                        else -> "$matchCount matches"
                    }
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (matchCount > 0) CallGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadKey(
    keyData: KeyData,
    onDigitClick: (Char) -> Unit,
    onLongClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    keyWidth: Dp = 84.dp,
    keyHeight: Dp = 62.dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "keyScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        label = "keyColor"
    )

    val spokenDescription = buildString {
        append("Key ${keyData.digit}")
        if (keyData.subtext.isNotBlank()) {
            append(", letters ${keyData.subtext}")
        }
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(width = keyWidth, height = keyHeight)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = { onDigitClick(keyData.digit) },
                onLongClick = onLongClick
            )
            .semantics { contentDescription = spokenDescription }
            .testTag("keypad_key_${keyData.digit}"),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = if (isHighlighted) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        },
        tonalElevation = if (isHighlighted) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = keyData.digit.toString(),
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            when {
                keyData.digit == '1' -> {
                    Icon(
                        imageVector = Icons.Default.Voicemail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
                keyData.subtext.isNotBlank() -> {
                    Text(
                        text = keyData.subtext,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun KeypadMatrix(
    onDigitClick: (Char) -> Unit,
    onLongDigitClick: (Char) -> Unit,
    highlightedDigits: Set<Char> = emptySet(),
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val horizontalGap = 10.dp
        val maxGridWidth = (maxWidth - 32.dp).coerceAtMost(380.dp)
        val computedKeyWidth = ((maxGridWidth - horizontalGap * 2) / 3).coerceIn(68.dp, 112.dp)
        val computedKeyHeight = if (maxWidth < 340.dp) 54.dp else 62.dp

        Column(
            modifier = Modifier.width(maxGridWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in KEYPAD_KEYS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        KeypadKey(
                            keyData = key,
                            isHighlighted = key.digit in highlightedDigits,
                            keyWidth = computedKeyWidth,
                            keyHeight = computedKeyHeight,
                            onDigitClick = onDigitClick,
                            onLongClick = {
                                if (key.digit == '0') {
                                    onLongDigitClick('+')
                                } else {
                                    onLongDigitClick(key.digit)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadBottomBar(
    hasInput: Boolean,
    selectedSimSlot: Int = 1,
    hasDetectedSim: Boolean = false,
    onCallClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearAllClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onToggleKeypad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left action: Add Contact or Hide Keypad
        if (hasInput) {
            IconButton(
                onClick = onAddContactClick,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("keypad_add_contact")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            IconButton(
                onClick = onToggleKeypad,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("keypad_toggle_view")
            ) {
                Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = "Hide Keypad",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Center: Big Green Call Button with SIM indicator
        FilledIconButton(
            onClick = onCallClick,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CallGreen,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(68.dp)
                .testTag("keypad_call_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call using SIM $selectedSimSlot",
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (hasDetectedSim) "SIM $selectedSimSlot" else "SIM $selectedSimSlot",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // Right action: Backspace (only visible when there's input)
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasInput) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onBackspaceClick,
                            onLongClick = onClearAllClick
                        )
                        .testTag("keypad_backspace"),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete Digit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
