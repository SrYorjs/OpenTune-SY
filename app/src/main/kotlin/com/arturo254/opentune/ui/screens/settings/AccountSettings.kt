package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.arturo254.opentune.App.Companion.forgetAccount
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AccountChannelHandleKey
import com.arturo254.opentune.constants.AccountEmailKey
import com.arturo254.opentune.constants.AccountNameKey
import com.arturo254.opentune.constants.DataSyncIdKey
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.constants.PoTokenKey
import com.arturo254.opentune.constants.SelectedYtmPlaylistsKey
import com.arturo254.opentune.constants.UseLoginForBrowse
import com.arturo254.opentune.constants.VisitorDataKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.utils.completed
import com.arturo254.opentune.innertube.utils.parseCookieString
import com.arturo254.opentune.ui.component.InfoLabel
import com.arturo254.opentune.ui.component.TextFieldDialog
import com.arturo254.opentune.ui.screens.buildLoginRoute
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel

@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) = AsterSettingsEnvironment(AsterSettingsSection.ACCOUNT, modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (poToken, onPoTokenChange) = rememberPreference(PoTokenKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val viewModel: HomeViewModel = hiltViewModel()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val hasUpdate = !Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)

    Column(
        modifier = Modifier

            .verticalScroll(rememberScrollState())
    ) {

        AccountSettingsHeader(onClose = onClose)
        AsterSettingsPageIntro()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AccountCard(
                isLoggedIn = isLoggedIn,
                accountName = accountName,
                accountEmail = accountEmail,
                accountImageUrl = accountImageUrl,
                onAccountClick = {
                    onClose()
                    if (isLoggedIn) {
                        navController.navigate("account")
                    } else {
                        navController.navigate(buildLoginRoute())
                    }
                },
                onLogout = {
                    onInnerTubeCookieChange("")
                    forgetAccount(context)
                }
            )

            if (showTokenEditor) {
                TokenEditorDialog(
                    innerTubeCookie = innerTubeCookie,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    accountNamePref = accountNamePref,
                    accountEmail = accountEmail,
                    accountChannelHandle = accountChannelHandle,
                    onInnerTubeCookieChange = onInnerTubeCookieChange,
                    onPoTokenChange = onPoTokenChange,
                    onVisitorDataChange = onVisitorDataChange,
                    onDataSyncIdChange = onDataSyncIdChange,
                    onAccountNameChange = onAccountNameChange,
                    onAccountEmailChange = onAccountEmailChange,
                    onAccountChannelHandleChange = onAccountChannelHandleChange,
                    onDismiss = { showTokenEditor = false }
                )
            }

            AnimatedVisibility(
                visible = isLoggedIn,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SettingsSection(title = stringResource(R.string.account)) {
                    SettingsToggleItem(
                        icon = painterResource(R.drawable.add_circle),
                        title = stringResource(R.string.more_content),
                        subtitle = stringResource(R.string.use_login_for_browse_desc),
                        checked = useLoginForBrowse,
                        onCheckedChange = {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    SettingsToggleItem(
                        icon = painterResource(R.drawable.cached),
                        title = stringResource(R.string.yt_sync),
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.integration)) {
                SettingsClickableItem(
                    icon = painterResource(R.drawable.playlist_add),
                    title = stringResource(R.string.select_playlist_to_sync),
                    onClick = { showPlaylistDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                SettingsClickableItem(
                    icon = painterResource(R.drawable.integration),
                    title = stringResource(R.string.integration),
                    subtitle = "Discord, Last.fm, ListenBrainz",
                    onClick = {
                        onClose()
                        navController.navigate("settings/integration")
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                SettingsClickableItem(
                    icon = painterResource(R.drawable.fire),
                    title = stringResource(R.string.music_together),
                    onClick = {
                        onClose()
                        navController.navigate("settings/music_together")
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.misc)) {
                SettingsClickableItem(
                    icon = painterResource(R.drawable.token),
                    title = when {
                        !isLoggedIn -> stringResource(R.string.advanced_login)
                        showToken -> stringResource(R.string.token_shown)
                        else -> stringResource(R.string.token_hidden)
                    },
                    onClick = {
                        if (!isLoggedIn) showTokenEditor = true
                        else if (!showToken) showToken = true
                        else showTokenEditor = true
                    }
                )
            }

            SettingsSection {
                SettingsClickableItem(
                    icon = painterResource(R.drawable.settings),
                    title = stringResource(R.string.settings),
                    showBadge = hasUpdate,
                    onClick = {
                        onClose()
                        navController.navigate("settings")
                    }
                )

                if (hasUpdate) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    UpdateAvailableItem(
                        latestVersion = latestVersionName,
                        onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) }
                    )
                }
            }

            AppVersionFooter()

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            onDismiss = { showPlaylistDialog = false }
        )
    }
}


@Composable
private fun AccountSettingsHeader(onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.opentune), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(painterResource(R.drawable.close), stringResource(R.string.close)) }
    }
}

@Composable
private fun AccountCard(
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountImageUrl: String?,
    onAccountClick: () -> Unit,
    onLogout: () -> Unit
) {
    AsterSettingsPanel {
        com.arturo254.opentune.ui.component.NewMenuItem(
            headlineContent = { Text(if (isLoggedIn) accountName else stringResource(R.string.login),
                style = MaterialTheme.typography.titleLarge) },
            supportingContent = { Text(if (isLoggedIn) accountEmail else stringResource(R.string.not_logged_in)) },
            leadingContent = {
                com.arturo254.opentune.core.designsystem.component.Artwork(accountImageUrl,
                    Modifier.size(52.dp), shape = CircleShape, placeholderIcon = R.drawable.account)
            },
            trailingContent = { Icon(painterResource(R.drawable.chevron_right), null) },
            onClick = onAccountClick,
        )
        if (isLoggedIn) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLogout) { Text(stringResource(R.string.action_logout)) }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        title?.let { AsterSettingsHeading(it) }
        AsterSettingsPanel { content() }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    AsterPreferenceRow(
        title = { Text(title) },
        supporting = subtitle?.let { { Text(it) } },
        icon = { Icon(icon, null) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showBadge) Badge()
                Icon(painterResource(R.drawable.chevron_right), null)
            }
        },
        grouped = true,
        onClick = onClick,
    )
}

@Composable
private fun SettingsToggleItem(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AsterSettingsSwitch(
        title = { Text(title) },
        description = subtitle,
        icon = { Icon(icon, null) },
        checked = checked,
        onCheckedChange = onCheckedChange,
        grouped = true,
    )
}

@Composable
private fun UpdateAvailableItem(
    latestVersion: String,
    onClick: () -> Unit
) {
    SettingsClickableItem(painterResource(R.drawable.update),
        stringResource(R.string.new_version_available), latestVersion, showBadge = true, onClick = onClick)
}

@Composable
private fun AppVersionFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TokenEditorDialog(
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    accountNamePref: String,
    accountEmail: String,
    accountChannelHandle: String,
    onInnerTubeCookieChange: (String) -> Unit,
    onPoTokenChange: (String) -> Unit,
    onVisitorDataChange: (String) -> Unit,
    onDataSyncIdChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onAccountChannelHandleChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val text = """
        ***INNERTUBE COOKIE*** =$innerTubeCookie
        ***VISITOR DATA*** =$visitorData
        ***DATASYNC ID*** =$dataSyncId
        ***PO TOKEN*** =${YouTube.poToken.orEmpty()}
        ***ACCOUNT NAME*** =$accountNamePref
        ***ACCOUNT EMAIL*** =$accountEmail
        ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
    """.trimIndent()

    TextFieldDialog(
        initialTextFieldValue = TextFieldValue(text),
        onDone = { data ->
            data.split("\n").forEach {
                when {
                    it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                    it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                    it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                    it.startsWith("***PO TOKEN*** =") -> onPoTokenChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                }
            }
        },
        onDismiss = onDismiss,
        singleLine = false,
        maxLines = 20,
        isInputValid = {
            it.isNotEmpty() && "SAPISID" in parseCookieString(it)
        },
        extraContent = {
            InfoLabel(text = stringResource(R.string.token_adv_login_description))
        }
    )
}

@Composable
private fun PlaylistSelectionDialog(onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val (initialSelected, _) = rememberPreference(SelectedYtmPlaylistsKey, "")
    val selectedList = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialSelected) {
        selectedList.clear()
        if (initialSelected.isNotEmpty()) {
            selectedList.addAll(
                initialSelected.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            )
        }
    }

    var loading by remember { mutableStateOf(true) }
    val playlists = remember { mutableStateListOf<com.arturo254.opentune.innertube.models.PlaylistItem>() }

    LaunchedEffect(Unit) {
        loading = true
        com.arturo254.opentune.innertube.YouTube
            .library("FEmusic_liked_playlists")
            .completed()
            .onSuccess { page ->
                playlists.clear()
                playlists.addAll(
                    page.items
                        .filterIsInstance<com.arturo254.opentune.innertube.models.PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .reversed()
                )
            }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(
                onClick = {
                    com.arturo254.opentune.utils.PreferenceStore.launchEdit(context.dataStore) {
                        this[SelectedYtmPlaylistsKey] = selectedList.joinToString(",")
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel_button))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.select_playlist_to_sync),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {

                    val density = LocalDensity.current
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        stroke = Stroke(
                            width = with(density) { 2.dp.toPx() }
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(playlists) { pl ->
                        val isSelected = selectedList.contains(pl.id)
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                Color.Transparent,
                            label = "playlistItemColor"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .clickable {
                                    if (isSelected) selectedList.remove(pl.id)
                                    else selectedList.add(pl.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedList.add(pl.id)
                                    else selectedList.remove(pl.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(Modifier.width(8.dp))

                            AsyncImage(
                                model = pl.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    )
}
