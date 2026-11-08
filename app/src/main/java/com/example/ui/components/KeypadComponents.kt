package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen

data class KeyData(
    val digit: Char,
    val subtext: String
)

val KEYPAD_KEYS = listOf(
    listOf(KeyData('1', " "), KeyData('2', "A B C"), KeyData('3', "D E F")),
    listOf(KeyData('4', "G H I"), KeyData('5', "J K L"), KeyData('6', "M N O")),
    listOf(KeyData('7', "P Q R S"), KeyData('8', "T U V"), KeyData('9', "W X Y Z")),
    listOf(KeyData('*', " "), KeyData('0', "+"), KeyData('#', " "))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadKey(
    keyData: KeyData,
    onDigitClick: (Char) -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = { onDigitClick(keyData.digit) },
                onLongClick = onLongClick
            )
            .testTag("keypad_key_${keyData.digit}"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = keyData.digit.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (keyData.subtext.isNotBlank()) {
                Text(
                    text = keyData.subtext,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(11.dp))
            }
        }
    }
}

@Composable
fun KeypadMatrix(
    onDigitClick: (Char) -> Unit,
    onLongDigitClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in KEYPAD_KEYS) {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    KeypadKey(
                        keyData = key,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadBottomBar(
    hasInput: Boolean,
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
            .padding(vertical = 12.dp),
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
                    modifier = Modifier.size(28.dp)
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
                    contentDescription = "Keypad view",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Center: Big Green Call Button
        FilledIconButton(
            onClick = onCallClick,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CallGreen,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(72.dp)
                .testTag("keypad_call_button")
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                modifier = Modifier.size(34.dp)
            )
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
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Delete Digit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
