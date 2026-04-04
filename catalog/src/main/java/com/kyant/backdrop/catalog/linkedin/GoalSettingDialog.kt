package com.kyant.backdrop.catalog.linkedin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText

@Composable
fun GoalSettingDialog(
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (goal: String, category: String?) -> Unit
) {
    var goal by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = {
            BasicText(
                text = "Add Agent Goal",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentDisplayFontFamily
                )
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Goal") },
                    textStyle = TextStyle(color = contentColor)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Category (optional)") },
                    textStyle = TextStyle(color = contentColor)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(goal.trim(), category.trim().ifBlank { null })
                },
                enabled = goal.isNotBlank()
            ) {
                androidx.compose.material3.Text("Save", color = accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Cancel", color = contentColor.copy(alpha = 0.72f))
            }
        }
    )
}
