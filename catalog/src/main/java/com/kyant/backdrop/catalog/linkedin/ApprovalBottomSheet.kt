package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.network.models.AgentPendingAction

@Composable
fun ApprovalBottomSheetContent(
    action: AgentPendingAction,
    contentColor: Color,
    accentColor: Color,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = action.title.ifBlank { "Pending approval" },
                style = TextStyle(
                    color = contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentDisplayFontFamily
                )
            )
            BasicText(
                text = "Review what the agent wants to do before it runs.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = contentColor.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaLine(label = "Action", value = action.actionType.ifBlank { action.toolName }, color = contentColor)
                MetaLine(label = "Tool", value = action.toolName, color = contentColor)
                MetaLine(label = "Status", value = action.status, color = contentColor)
                BasicText(
                    text = action.summary,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = contentColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = !isSubmitting, onClick = onReject)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (isSubmitting) "Working…" else "Reject",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = !isSubmitting, onClick = onApprove)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (isSubmitting) "Working…" else "Approve",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }
    }
}

@Composable
private fun MetaLine(
    label: String,
    value: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicText(
            text = label,
            style = TextStyle(
                color = color.copy(alpha = 0.58f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
        BasicText(
            text = value,
            style = TextStyle(
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}
