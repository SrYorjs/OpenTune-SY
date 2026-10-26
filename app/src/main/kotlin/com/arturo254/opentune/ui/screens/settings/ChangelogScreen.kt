package com.arturo254.opentune.ui.screens.settings

import com.arturo254.opentune.ui.screens.settings.AsterSettingsScaffold as Scaffold
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.arturo254.opentune.ui.screens.settings.AsterSettingsTopBar as TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arturo254.opentune.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.constants.UpdateChannel
import com.arturo254.opentune.constants.UpdateChannelKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.MarkdownText
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.NightlyInfo
import com.arturo254.opentune.utils.ReleaseInfo
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) = AsterSettingsEnvironment(AsterSettingsSection.CHANGELOG) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var updateChannel by remember { mutableStateOf(UpdateChannel.STABLE) }

    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }

    var nightlyInfo by remember { mutableStateOf<NightlyInfo?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        updateChannel = context.dataStore.data.map {
            it[UpdateChannelKey]?.let { value ->
                try {
                    UpdateChannel.valueOf(value)
                } catch (e: Exception) {
                    UpdateChannel.STABLE
                }
            } ?: UpdateChannel.STABLE
        }.first()
    }

    suspend fun loadContent() {
        if (updateChannel == UpdateChannel.NIGHTLY) {

            Updater.getLatestReleaseInfo().onSuccess { releaseInfo ->
                nightlyInfo = NightlyInfo(
                    versionName = releaseInfo.tagName,
                    apkUrl = "",
                    changelog = releaseInfo.body,
                    publishedAt = releaseInfo.publishedAt
                )
                error = null
            }.onFailure { e ->
                if (nightlyInfo == null) {
                    error = e.message ?: "Error loading nightly info"
                }
            }
            isLoading = false
        } else {

            Updater.getAllReleases(forceRefresh = true).onSuccess { result ->
                releases = result
                error = null
                isLoading = false
            }.onFailure { e ->
                if (releases.isEmpty()) {
                    error = e.message ?: "Error loading releases"
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(updateChannel) {
        isLoading = true
        error = null

        if (updateChannel == UpdateChannel.STABLE) {

            val cachedReleases = Updater.getCachedReleases()
            if (cachedReleases.isNotEmpty()) {
                releases = cachedReleases
                isLoading = false
            }
        }

        loadContent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (updateChannel == UpdateChannel.NIGHTLY)
                            "Nightly Changelog"
                        else
                            stringResource(R.string.changelog)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null &&
                        ((updateChannel == UpdateChannel.STABLE && releases.isEmpty()) ||
                                (updateChannel == UpdateChannel.NIGHTLY && nightlyInfo == null)) -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading changelog: ${error ?: "Unknown error"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            isLoading = true
                            error = null
                            coroutineScope.launch {
                                loadContent()
                            }
                        }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                updateChannel == UpdateChannel.NIGHTLY && nightlyInfo != null -> {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "galax_changelog_nightly_hero") {
                            GalaxChangelogHero(
                                label = "NIGHTLY",
                                title = stringResource(R.string.changelog),
                                subtitle = nightlyInfo?.versionName.orEmpty(),
                            )
                        }

                        item {
                            NightlyChangelogCard(nightlyInfo = nightlyInfo!!)
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }

                updateChannel == UpdateChannel.STABLE && releases.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_releases),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                updateChannel == UpdateChannel.STABLE -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "galax_changelog_stable_hero") {
                            GalaxChangelogHero(
                                label = "STABLE",
                                title = stringResource(R.string.changelog),
                                subtitle = "${releases.size} releases",
                            )
                        }

                        items(releases, key = { it.tagName }) { release ->
                            ReleaseCard(release = release)
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}



@Composable
private fun GalaxChangelogHero(label: String, title: String, subtitle: String) {
    Column {
        AsterSettingsHero(title = title)
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GalaxReleaseEntry(
    eyebrow: String,
    title: String,
    date: String,
    markdown: String?,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surfaceContainerLow.copy(alpha = .94f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = .26f),
                                colors.tertiary.copy(alpha = .16f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = .30f))
        if (!markdown.isNullOrBlank()) {
            MarkdownText(
                markdown = markdown,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
        } else {
            Text(
                text = stringResource(R.string.no_releases),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NightlyChangelogCard(nightlyInfo: NightlyInfo) {
    val displayDate = remember(nightlyInfo.publishedAt) {
        galaxReleaseDate(nightlyInfo.publishedAt)
    }
    GalaxReleaseEntry(
        eyebrow = "NIGHTLY",
        title = nightlyInfo.versionName,
        date = displayDate,
        markdown = nightlyInfo.changelog,
    )
}

@Composable
private fun ReleaseCard(release: ReleaseInfo) {
    val displayDate = remember(release.publishedAt) {
        galaxReleaseDate(release.publishedAt)
    }
    GalaxReleaseEntry(
        eyebrow = "STABLE",
        title = release.name.ifBlank { release.tagName },
        date = displayDate,
        markdown = release.body,
    )
}

private fun galaxReleaseDate(raw: String): String {
    val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy")
    for (pattern in formats) {
        val result = runCatching {
            val input = SimpleDateFormat(pattern, Locale.getDefault())
            input.isLenient = false
            val parsed = input.parse(raw.take(10))
            parsed?.let {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
            }
        }.getOrNull()
        if (result != null) return result
    }
    return raw
}
