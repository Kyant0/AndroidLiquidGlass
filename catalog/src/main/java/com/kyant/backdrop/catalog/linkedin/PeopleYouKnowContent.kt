package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.PeopleYouKnowInvite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleYouKnowContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    uiState: FindPeopleUiState,
    viewModel: FindPeopleViewModel,
    onNavigateToProfile: (String) -> Unit,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.discoverPeopleYouKnowFromDeviceContacts()
        } else {
            Toast.makeText(
                context,
                "You can use a file instead whenever you want.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.discoverPeopleYouKnowFromFile(uri)
        }
    }

    fun startDiscovery() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.discoverPeopleYouKnowFromDeviceContacts()
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    fun openFileFallback() {
        filePickerLauncher.launch(
            arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel")
        )
    }

    fun shareInvite(entry: PeopleYouKnowInvite) {
        val shareLink = uiState.peopleYouKnowShareLink
            ?: "https://vormex.in/login?mode=signup"
        val shareText = buildString {
            append("Join me on Vormex")
            entry.contactName?.takeIf { it.isNotBlank() }?.let {
                append(", $it")
            }
            append(". ")
            append(shareLink)
        }

        if (launchShareSheet(context, shareText)) {
            viewModel.markPeopleYouKnowInviteSent(entry.id)
        } else {
            copyText(context, "Invite link", shareText)
            Toast.makeText(context, "Invite copied", Toast.LENGTH_SHORT).show()
            viewModel.markPeopleYouKnowInviteSent(entry.id)
        }
    }

    val hasResults = uiState.peopleYouKnowStats.totalContacts > 0 ||
        uiState.peopleYouKnowLastSyncedAt != null ||
        uiState.peopleYouKnowMatches.isNotEmpty() ||
        uiState.peopleYouKnowInvites.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                when {
                    uiState.isLoadingPeopleYouKnow && !hasResults -> {
                        PeopleYouKnowLoadingCard(contentColor = contentColor, accentColor = accentColor)
                    }

                    !hasResults -> {
                        PeopleYouKnowEmptyState(
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onFindThem = { viewModel.showPeopleYouKnowGate() }
                        )
                    }

                    else -> {
                        PeopleYouKnowSummaryCard(
                            contentColor = contentColor,
                            accentColor = accentColor,
                            matched = uiState.peopleYouKnowMatches,
                            stats = uiState.peopleYouKnowStats,
                            onRefresh = { viewModel.showPeopleYouKnowGate() },
                            onClear = { viewModel.clearPeopleYouKnow() },
                            isClearing = uiState.isClearingPeopleYouKnow
                        )
                    }
                }
            }

            uiState.peopleYouKnowError?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    InlineInfoCard(
                        title = message,
                        body = "You can try again or use a file instead.",
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }
            }

            if (uiState.peopleYouKnowMatches.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "On Vormex",
                        subtitle = "People already here from your list",
                        contentColor = contentColor
                    )
                }

                items(uiState.peopleYouKnowMatches, key = { it.id }) { person ->
                    PersonCard(
                        person = person,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        isLightTheme = isLightTheme,
                        isActionInProgress = uiState.connectionActionInProgress.contains(person.id),
                        onConnect = { viewModel.sendConnectionRequest(person.id) },
                        onCardClick = { onNavigateToProfile(person.id) },
                        reduceAnimations = reduceAnimations
                    )
                }
            }

            if (uiState.peopleYouKnowInvites.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Invite to Vormex",
                        subtitle = "People who can join you next",
                        contentColor = contentColor
                    )
                }

                items(uiState.peopleYouKnowInvites, key = { it.id }) { invite ->
                    PeopleYouKnowInviteRow(
                        invite = invite,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isSending = uiState.peopleYouKnowInviteInProgress.contains(invite.id),
                        onInvite = { shareInvite(invite) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (uiState.isPeopleYouKnowGateVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hidePeopleYouKnowGate() },
            sheetState = sheetState,
            containerColor = if (isLightTheme) Color.White else Color(0xFF101114),
            contentColor = contentColor
        ) {
            PeopleYouKnowGateSheet(
                contentColor = contentColor,
                accentColor = accentColor,
                isDiscovering = uiState.isDiscoveringPeopleYouKnow,
                onContinue = { startDiscovery() },
                onUseFileInstead = { openFileFallback() },
                onSkip = { viewModel.hidePeopleYouKnowGate() }
            )
        }
    }
}

@Composable
private fun PeopleYouKnowEmptyState(
    contentColor: Color,
    accentColor: Color,
    onFindThem: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.PeopleAlt,
                    contentDescription = null,
                    tint = accentColor
                )
            }

            BasicText(
                "You might already know people on Vormex",
                style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
            )
            BasicText(
                "See who’s already here and connect instantly",
                style = TextStyle(contentColor.copy(alpha = 0.72f), 14.sp)
            )
            BasicText(
                "Start with people you already trust",
                style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
            )

            PrimaryActionButton(
                label = "Find them",
                accentColor = accentColor,
                onClick = onFindThem
            )
        }
    }
}

@Composable
private fun PeopleYouKnowSummaryCard(
    contentColor: Color,
    accentColor: Color,
    matched: List<com.kyant.backdrop.catalog.network.models.PersonInfo>,
    stats: PeopleYouKnowStatsUi,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    isClearing: Boolean
) {
    val count = stats.matchedCount
    val title = if (count > 0) {
        "You already know $count people on Vormex"
    } else {
        "No one from this list is on Vormex yet"
    }

    val subtitle = if (count > 0) {
        "Start with people you already trust"
    } else {
        "You can still invite people to join you"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (count > 0) {
                AvatarProofRow(
                    matched = matched.take(2),
                    accentColor = accentColor,
                    contentColor = contentColor
                )
            }

            BasicText(
                title,
                style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
            )
            BasicText(
                subtitle,
                style = TextStyle(contentColor.copy(alpha = 0.72f), 14.sp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryIconAction(
                    label = "Refresh",
                    icon = Icons.Outlined.Refresh,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = onRefresh
                )
                SecondaryIconAction(
                    label = if (isClearing) "Clearing..." else "Clear list",
                    icon = Icons.Outlined.DeleteOutline,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = onClear,
                    enabled = !isClearing
                )
            }
        }
    }
}

@Composable
private fun AvatarProofRow(
    matched: List<com.kyant.backdrop.catalog.network.models.PersonInfo>,
    accentColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row {
            matched.forEachIndexed { index, person ->
                Box(
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else (-10).dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                ) {
                    MiniAvatar(
                        imageUrl = person.profileImage,
                        fallback = person.name ?: person.username ?: "V",
                        accentColor = accentColor
                    )
                }
            }
        }

        BasicText(
            "People you already know",
            style = TextStyle(contentColor.copy(alpha = 0.78f), 13.sp, FontWeight.Medium)
        )
    }
}

@Composable
private fun MiniAvatar(
    imageUrl: String?,
    fallback: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        } else {
            BasicText(
                fallback
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString(""),
                style = TextStyle(accentColor, 11.sp, FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun PeopleYouKnowInviteRow(
    invite: PeopleYouKnowInvite,
    contentColor: Color,
    accentColor: Color,
    isSending: Boolean,
    onInvite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    (invite.contactName ?: "V")
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString(""),
                    style = TextStyle(accentColor, 12.sp, FontWeight.Bold)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    invite.contactName ?: "Someone you know",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    if (invite.invitedAt == null) "Invite them personally" else "Invite sent",
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (invite.invitedAt == null) accentColor.copy(alpha = 0.16f)
                        else contentColor.copy(alpha = 0.08f)
                    )
                    .clickable(enabled = !isSending, onClick = onInvite)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    BasicText(
                        if (invite.invitedAt == null) "Invite to join you" else "Sent",
                        style = TextStyle(
                            if (invite.invitedAt == null) accentColor else contentColor.copy(alpha = 0.6f),
                            12.sp,
                            FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            title,
            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
        )
        BasicText(
            subtitle,
            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
        )
    }
}

@Composable
private fun InlineInfoCard(
    title: String,
    body: String,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(title, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
            BasicText(body, style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
        }
    }
}

@Composable
private fun PeopleYouKnowLoadingCard(
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
            BasicText(
                "Looking for people you already know",
                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun PeopleYouKnowGateSheet(
    contentColor: Color,
    accentColor: Color,
    isDiscovering: Boolean,
    onContinue: () -> Unit,
    onUseFileInstead: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BasicText(
            "Find people you already know",
            style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
        )

        listOf(
            "See friends already on Vormex",
            "Discover mutual connections",
            "You’re always in control"
        ).forEach { line ->
            BasicText(
                "• $line",
                style = TextStyle(contentColor.copy(alpha = 0.76f), 14.sp)
            )
        }

        BasicText(
            "No spam",
            style = TextStyle(accentColor, 13.sp, FontWeight.Medium)
        )

        PrimaryActionButton(
            label = if (isDiscovering) "Finding..." else "Continue",
            accentColor = accentColor,
            onClick = onContinue,
            enabled = !isDiscovering
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryIconAction(
                label = "Use a file instead",
                icon = Icons.Outlined.UploadFile,
                accentColor = accentColor,
                contentColor = contentColor,
                onClick = onUseFileInstead,
                enabled = !isDiscovering
            )
            SecondaryTextButton(
                label = "Skip",
                contentColor = contentColor,
                onClick = onSkip,
                enabled = !isDiscovering
            )
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accentColor else accentColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
        )
    }
}

@Composable
private fun SecondaryIconAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) accentColor else accentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        BasicText(
            label,
            style = TextStyle(
                if (enabled) contentColor else contentColor.copy(alpha = 0.45f),
                12.sp,
                FontWeight.Medium
            )
        )
    }
}

@Composable
private fun SecondaryTextButton(
    label: String,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                if (enabled) contentColor else contentColor.copy(alpha = 0.45f),
                12.sp,
                FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

private fun launchShareSheet(context: Context, text: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Invite to join you"))
        true
    }.getOrElse { false }
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
