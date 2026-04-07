package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AgentInlineResultsSurface(
    panel: AgentInlineResultsPanel,
    dismissedIds: Set<String>,
    actionInProgress: Set<String>,
    contentColor: Color,
    accentColor: Color,
    isDarkTheme: Boolean,
    enableNavigationActions: Boolean = true,
    modifier: Modifier = Modifier,
    onViewProfile: (String) -> Unit,
    onMessage: (String) -> Unit,
    onConnect: (String) -> Unit,
    onCloseItem: (String) -> Unit,
    onClosePanel: () -> Unit,
    onSeeMore: (() -> Unit)? = null
) {
    val visiblePeople = panel.visiblePeople(dismissedIds)
    if (visiblePeople.isEmpty()) {
        return
    }

    val surfaceColor =
        if (isDarkTheme) Color(0xFF0F1418).copy(alpha = 0.94f)
        else Color(0xFFFCFBF8).copy(alpha = 0.96f)
    val borderColor =
        if (isDarkTheme) Color.White.copy(alpha = 0.08f)
        else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(30.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AgentInlineMetaChip(
                        text = "${visiblePeople.size}/${panel.totalCount}",
                        accentColor = accentColor
                    )
                    AgentInlineHeading(
                        text = panel.title.ifBlank { "People to check out" },
                        color = contentColor
                    )
                    panel.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        AgentInlineBody(
                            text = subtitle,
                            color = contentColor.copy(alpha = 0.66f)
                        )
                    }
                    if (visiblePeople.size > 1) {
                        AgentInlineBody(
                            text = "Swipe to browse profiles",
                            color = contentColor.copy(alpha = 0.56f)
                        )
                    }
                }

                AgentInlineSmallButton(
                    text = "Dismiss",
                    background = contentColor.copy(alpha = 0.08f),
                    textColor = contentColor.copy(alpha = 0.82f),
                    onClick = onClosePanel
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visiblePeople, key = { person -> person.id }) { person ->
                    AgentInlinePeopleCard(
                        modifier = Modifier.widthIn(min = 284.dp, max = 320.dp),
                        person = person,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        isActionInProgress = actionInProgress.contains(person.id),
                        enableNavigationActions = enableNavigationActions,
                        onViewProfile = { onViewProfile(person.id) },
                        onMessage = { onMessage(person.id) },
                        onConnect = { onConnect(person.id) },
                        onClose = { onCloseItem(person.id) }
                    )
                }
            }

            if (panel.totalCount > panel.shownCount && onSeeMore != null) {
                AgentInlinePrimaryButton(
                    text = "Open full results in Find",
                    accentColor = accentColor,
                    onClick = onSeeMore
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentInlinePeopleCard(
    modifier: Modifier = Modifier,
    person: AgentInlinePerson,
    contentColor: Color,
    accentColor: Color,
    isDarkTheme: Boolean,
    isActionInProgress: Boolean,
    enableNavigationActions: Boolean,
    onViewProfile: () -> Unit,
    onMessage: () -> Unit,
    onConnect: () -> Unit,
    onClose: () -> Unit
) {
    val cardColor = if (isDarkTheme) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f)
    val supportTextColor = contentColor.copy(alpha = 0.62f)
    val summaryText = person.bio?.takeIf { it.isNotBlank() }
    val highlightTags =
        (person.matchReasons + person.sharedInterests + person.skills.take(3) + person.interests.take(2))
            .distinct()
            .take(6)
    val connectLabel = when (person.connectionStatus) {
        "connected" -> "Connected"
        "pending_sent" -> "Pending"
        "pending_received" -> "Accept in Find"
        else -> "Connect"
    }
    val connectEnabled =
        !isActionInProgress &&
            (person.connectionStatus == "none")
    val statusChip = when (person.connectionStatus) {
        "connected" -> "Connected"
        "pending_sent" -> "Request sent"
        "pending_received" -> "Accept request"
        else -> null
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(cardColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (!person.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(person.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile image",
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AgentInlineBody(
                        text = (person.name ?: person.username ?: "U")
                            .split(" ")
                            .mapNotNull { token -> token.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString(""),
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AgentInlineHeading(
                        text = person.name ?: person.username ?: "Unknown",
                        color = contentColor,
                        fontSize = 15.sp
                    )
                    if (person.isOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF31C76A))
                        )
                    }
                }

                person.username?.takeIf { it.isNotBlank() }?.let { username ->
                    AgentInlineBody(
                        text = "@$username",
                        color = supportTextColor
                    )
                }

                person.headline?.takeIf { it.isNotBlank() }?.let { headline ->
                    AgentInlineBody(
                        text = headline,
                        color = contentColor.copy(alpha = 0.74f)
                    )
                }
            }

            AgentInlineSmallButton(
                text = "Hide",
                background = contentColor.copy(alpha = 0.08f),
                textColor = contentColor.copy(alpha = 0.72f),
                onClick = onClose
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            person.college?.takeIf { it.isNotBlank() }?.let {
                AgentInlineMetaChip(text = it, accentColor = accentColor, emphasize = false)
            }
            person.branch?.takeIf { it.isNotBlank() }?.let {
                AgentInlineMetaChip(text = it, accentColor = accentColor, emphasize = false)
            }
            person.mutualConnections.takeIf { it > 0 }?.let {
                AgentInlineMetaChip(text = "$it mutual", accentColor = accentColor, emphasize = false)
            }
            statusChip?.let {
                AgentInlineMetaChip(text = it, accentColor = accentColor, emphasize = true)
            }
        }

        summaryText?.takeIf { it.isNotBlank() }?.let { text ->
            AgentInlineBody(
                text = text,
                color = contentColor.copy(alpha = 0.78f)
            )
        }

        if (highlightTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                highlightTags.forEach { tag ->
                    AgentInlineMetaChip(text = tag, accentColor = accentColor, emphasize = false)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (enableNavigationActions) {
                AgentInlineActionButton(
                    modifier = Modifier.weight(1f),
                    text = "Profile",
                    background = contentColor.copy(alpha = 0.07f),
                    textColor = contentColor,
                    onClick = onViewProfile
                )
                AgentInlineActionButton(
                    modifier = Modifier.weight(1f),
                    text = "Message",
                    background = contentColor.copy(alpha = 0.07f),
                    textColor = contentColor,
                    onClick = onMessage
                )
            }
            AgentInlineActionButton(
                modifier = Modifier.weight(1f),
                text = connectLabel,
                background = accentColor.copy(alpha = if (connectEnabled) 0.18f else 0.10f),
                textColor = if (connectEnabled) accentColor else accentColor.copy(alpha = 0.58f),
                enabled = connectEnabled
            ) {
                onConnect()
            }
        }

        if (isActionInProgress) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.8.dp,
                    color = accentColor
                )
                AgentInlineBody(
                    text = "Updating connection…",
                    color = supportTextColor
                )
            }
        }
    }
}

@Composable
private fun AgentInlineHeading(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = AgentBodyFontFamily
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AgentInlineBody(
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            fontFamily = AgentBodyFontFamily
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AgentInlineMetaChip(
    text: String,
    accentColor: Color,
    emphasize: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (emphasize) accentColor.copy(alpha = 0.14f)
                else accentColor.copy(alpha = 0.09f)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = TextStyle(
                color = accentColor.copy(alpha = if (emphasize) 0.96f else 0.84f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun AgentInlinePrimaryButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = TextStyle(
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun AgentInlineSmallButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun AgentInlineActionButton(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}
