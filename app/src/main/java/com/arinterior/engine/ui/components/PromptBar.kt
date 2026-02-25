package com.arinterior.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom floating prompt bar with camera, text input, and mic buttons.
 */
@Composable
fun PromptBar(
    onPromptSubmit: (String) -> Unit,
    onImageSelected: () -> Unit,
    onVoiceInput: (String) -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xCC1A1A2E))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera button (Pipeline B trigger)
            IconButton(
                onClick = onImageSelected,
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Upload image",
                    tint = Color(0xFF00D4FF)
                )
            }

            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Describe your dream room...",
                        style = TextStyle(
                            color = Color(0x80FFFFFF),
                            fontSize = 15.sp
                        )
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    enabled = isEnabled,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF00D4FF)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Send or Mic button
            if (text.isNotBlank()) {
                IconButton(
                    onClick = {
                        onPromptSubmit(text)
                        text = ""
                    },
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Submit prompt",
                        tint = Color(0xFF00D4FF)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        isListening = !isListening
                        if (isListening) {
                            // SpeechRecognizer will be started externally
                            onVoiceInput("")
                        }
                    },
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice input",
                        tint = if (isListening) Color(0xFFFF9F43) else Color(0xFF00D4FF)
                    )
                }
            }
        }
    }
}
