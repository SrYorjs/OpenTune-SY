package com.arturo254.opentune.playback

import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.TogetherClientIdKey
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.mediaItems
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.utils.getAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal class TogetherController(
    private val context: Context,
    private val player: ExoPlayer,
    private val queueController: QueueController,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val ioScopeProvider: () -> CoroutineScope,
    private val ensureScopesActive: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val scope: CoroutineScope
        get() = scopeProvider()

    private val ioScope: CoroutineScope
        get() = ioScopeProvider()

    val sessionState = MutableStateFlow<com.arturo254.opentune.together.TogetherSessionState>(
        com.arturo254.opentune.together.TogetherSessionState.Idle,
    )
    private var togetherServer: com.arturo254.opentune.together.TogetherServer? = null
    private var togetherOnlineHost: com.arturo254.opentune.together.TogetherOnlineHost? = null
    private var togetherClient: com.arturo254.opentune.together.TogetherClient? = null
    private var togetherBroadcastJob: Job? = null
    private var togetherOnlineConnectJob: Job? = null
    private var togetherClientEventsJob: Job? = null
    private var togetherHeartbeatJob: Job? = null
    private var togetherClock: com.arturo254.opentune.together.TogetherClock? = null
    private var togetherSelfParticipantId: String? = null
    private var togetherLastAppliedQueueHash: String? = null
    private var togetherIsOnlineSession: Boolean = false
    @Volatile
    private var togetherApplyingRemote: Boolean = false
    @Volatile
    private var togetherSuppressEchoUntilElapsedMs: Long = 0L
    @Volatile
    private var togetherLastAppliedRoomStateSentAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastRemoteAppliedPlayWhenReady: Boolean? = null
    @Volatile
    private var togetherLastRemoteAppliedIndex: Int = -1
    @Volatile
    private var togetherLastSentControlAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastSentControlAction: com.arturo254.opentune.together.ControlAction? = null
    @Volatile
    private var togetherPendingGuestControl: TogetherPendingGuestControl? = null

    fun isApplyingRemote(): Boolean = togetherApplyingRemote
    private val togetherHostId: String = "host"
    private var lastTogetherNoticeAtElapsedMs: Long = 0L
    private var lastTogetherNoticeKey: String? = null

    private data class TogetherPendingGuestControl(
        val desiredIsPlaying: Boolean? = null,
        val desiredIndex: Int? = null,
        val desiredTrackId: String? = null,
        val requestedAtElapsedMs: Long,
        val expiresAtElapsedMs: Long,
    )

    fun showNotice(message: String, key: String? = null) {
        val now = android.os.SystemClock.elapsedRealtime()
        val normalizedKey = key ?: message
        if (normalizedKey == lastTogetherNoticeKey && now - lastTogetherNoticeAtElapsedMs < 1200L) return
        lastTogetherNoticeKey = normalizedKey
        lastTogetherNoticeAtElapsedMs = now
        scope.launch(SilentHandler) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getOrCreateTogetherClientId(): String {
        val existing = dataStore.getAsync(TogetherClientIdKey)?.trim().orEmpty()
        if (existing.isNotBlank()) return existing
        val generated = java.util.UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[TogetherClientIdKey] = generated }
        return generated
    }

    fun startTogetherHost(
        port: Int,
        displayName: String,
        settings: com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            sessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = false

            val localIp = getLocalIpv4Address()
            val sessionId = java.util.UUID.randomUUID().toString()
            val sessionKey = java.util.UUID.randomUUID().toString()
            val joinInfo =
                com.arturo254.opentune.together.TogetherJoinInfo(
                    host = localIp ?: "127.0.0.1",
                    port = port,
                    sessionId = sessionId,
                    sessionKey = sessionKey,
                )
            val joinLink = com.arturo254.opentune.together.TogetherLink.encode(joinInfo)

            val server =
                com.arturo254.opentune.together.TogetherServer(
                    scope = ioScope,
                    sessionId = sessionId,
                    sessionKey = sessionKey,
                    hostDisplayName = displayName.trim().ifBlank { context.getString(R.string.app_name) },
                    initialSettings = settings,
                )

            server.onEvent = { event ->
                ioScope.launch(SilentHandler) {
                    handleTogetherHostEvent(event) { server.currentSettings() }
                }
            }

            server.start(port)
            togetherServer = server

            scope.launch(SilentHandler) {
                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Hosting(
                        sessionId = sessionId,
                        joinLink = joinLink,
                        localAddressHint = localIp,
                        port = port,
                        settings = settings,
                        roomState = null,
                    )
            }

            togetherBroadcastJob =
                ioScope.launch(SilentHandler) {
                    while (togetherServer === server) {
                        val state = buildTogetherRoomState(sessionId = sessionId, hostId = togetherHostId)
                        server.broadcastRoomState(state)
                        scope.launch(SilentHandler) {
                            val hosting = sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Hosting
                            if (hosting?.sessionId == sessionId) {
                                sessionState.value =
                                    hosting.copy(
                                        settings = server.currentSettings(),
                                        roomState = state.copy(
                                            participants = server.currentParticipants(),
                                            settings = server.currentSettings(),
                                        ),
                                    )
                            }
                        }
                        kotlinx.coroutines.delay(750)
                    }
                }
        }
    }

    private fun togetherOnlineErrorMessage(t: Throwable): String {
        if (t is com.arturo254.opentune.together.TogetherOnlineApiException) {
            val code = t.statusCode
            return when {
                code == 404 -> context.getString(R.string.together_session_not_found)
                code != null && code in 500..599 -> context.getString(R.string.together_server_error)
                else -> t.message ?: context.getString(R.string.network_unavailable)
            }
        }
        val root = generateSequence(t) { it.cause }.lastOrNull() ?: t
        return when (root) {
            is UnknownHostException -> context.getString(R.string.together_server_unreachable)
            is ConnectException -> context.getString(R.string.together_server_unreachable)
            is SocketTimeoutException -> context.getString(R.string.together_connection_timed_out)
            is javax.net.ssl.SSLHandshakeException -> context.getString(R.string.together_server_unreachable)
            else -> context.getString(R.string.network_unavailable)
        }
    }

    fun startTogetherOnlineHost(
        displayName: String,
        settings: com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            sessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = true

            val baseUrl = com.arturo254.opentune.together.TogetherOnlineEndpoint.baseUrlOrNull(dataStore)
            if (baseUrl == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = context.getString(R.string.together_online_not_configured),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val togetherToken = com.arturo254.opentune.BuildConfig.TOGETHER_BEARER_TOKEN.trim().takeIf { it.isNotBlank() }
            if (togetherToken == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = context.getString(R.string.together_token_missing),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val api = com.arturo254.opentune.together.TogetherOnlineApi(baseUrl = baseUrl, bearerToken = togetherToken)
            val hostName = displayName.trim().ifBlank { context.getString(R.string.app_name) }

            val created =
                runCatching {
                    api.createSession(
                        hostDisplayName = hostName,
                        settings = settings,
                    )
                }.getOrElse { t ->
                    scope.launch(SilentHandler) {
                        sessionState.value =
                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                message = togetherOnlineErrorMessage(t),
                                recoverable = true,
                            )
                    }
                    onError(t)
                    return@launch
                }

            val onlineHost =
                com.arturo254.opentune.together.TogetherOnlineHost(
                    externalScope = ioScope,
                    sessionId = created.sessionId,
                    sessionKey = created.hostKey,
                    hostId = togetherHostId,
                    hostDisplayName = hostName,
                    initialSettings = created.settings,
                    clientId = getOrCreateTogetherClientId(),
                    bearerToken = togetherToken,
                )

            onlineHost.onEvent = { event ->
                ioScope.launch(SilentHandler) {
                    handleTogetherHostEvent(event) { onlineHost.currentSettings() }
                }
            }

            togetherOnlineHost = onlineHost

            scope.launch(SilentHandler) {
                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.HostingOnline(
                        sessionId = created.sessionId,
                        code = created.code,
                        settings = created.settings,
                        roomState = null,
                    )
            }

            val wsUrl =
                com.arturo254.opentune.together.TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(
                    rawWsUrl = created.wsUrl,
                    baseUrl = baseUrl,
                )
            if (wsUrl == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = "Connection failed: Invalid server websocket URL",
                            recoverable = true,
                        )
                }
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                return@launch
            }

            togetherOnlineConnectJob?.cancel()
            togetherOnlineConnectJob =
                ioScope.launch(SilentHandler) {
                    onlineHost.connect(wsUrl)
                }

            togetherBroadcastJob =
                ioScope.launch(SilentHandler) {
                    while (togetherOnlineHost === onlineHost) {
                        val state =
                            buildTogetherRoomState(
                                sessionId = created.sessionId,
                                hostId = togetherHostId,
                            )
                        onlineHost.broadcastRoomState(state)
                        scope.launch(SilentHandler) {
                            val hosting =
                                sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.HostingOnline
                            if (hosting?.sessionId == created.sessionId) {
                                val currentSettings = onlineHost.currentSettings()
                                sessionState.value =
                                    hosting.copy(
                                        settings = currentSettings,
                                        roomState =
                                            state.copy(
                                                participants = onlineHost.currentParticipants(),
                                                settings = currentSettings,
                                            ),
                                    )
                            }
                        }
                        kotlinx.coroutines.delay(750)
                    }
                }
        }
    }

    fun joinTogether(
        rawLink: String,
        displayName: String,
    ) {
        ensureScopesActive()
        val joinInfo = com.arturo254.opentune.together.TogetherLink.decode(rawLink)
        if (joinInfo == null) {
            scope.launch(SilentHandler) {
                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = context.getString(R.string.invalid_link),
                        recoverable = true,
                    )
            }
            return
        }

        scope.launch(SilentHandler) {
            sessionState.value = com.arturo254.opentune.together.TogetherSessionState.Joining(joinInfo.toDeepLink())
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = false
            val client =
                com.arturo254.opentune.together.TogetherClient(
                    ioScope,
                    clientId = getOrCreateTogetherClientId(),
                )
            togetherClient = client
            togetherClock = com.arturo254.opentune.together.TogetherClock()
            togetherSelfParticipantId = null
            togetherLastAppliedQueueHash = null

            togetherClientEventsJob?.cancel()
            togetherClientEventsJob =
                ioScope.launch(SilentHandler) {
                    client.events.collect { event ->
                        when (event) {
                            is com.arturo254.opentune.together.TogetherClientEvent.Welcome -> {
                                togetherSelfParticipantId = event.welcome.participantId
                                scope.launch(SilentHandler) {
                                    val state = sessionState.value
                                    if (state is com.arturo254.opentune.together.TogetherSessionState.Joining) {
                                        val selfName = displayName.trim().ifBlank { context.getString(R.string.together_role_guest) }
                                        val initial =
                                            com.arturo254.opentune.together.TogetherRoomState(
                                                sessionId = joinInfo.sessionId,
                                                hostId = togetherHostId,
                                                participants =
                                                    listOf(
                                                        com.arturo254.opentune.together.TogetherParticipant(
                                                            id = event.welcome.participantId,
                                                            name = selfName,
                                                            isHost = false,
                                                            isPending = event.welcome.isPending,
                                                            isConnected = true,
                                                        ),
                                                    ),
                                                settings = event.welcome.settings,
                                                queue = emptyList(),
                                                queueHash = "",
                                                currentIndex = 0,
                                                isPlaying = false,
                                                positionMs = 0L,
                                                repeatMode = 0,
                                                shuffleEnabled = false,
                                                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                                            )
                                        sessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Joined(
                                                role = com.arturo254.opentune.together.TogetherRole.Guest,
                                                sessionId = joinInfo.sessionId,
                                                selfParticipantId = event.welcome.participantId,
                                                roomState = initial,
                                            )
                                    }
                                }
                                startTogetherHeartbeat(joinInfo.sessionId, client)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.RoomState -> {
                                applyRemoteRoomState(event.state)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.JoinDecision -> {
                                if (!event.decision.approved) {
                                    scope.launch(SilentHandler) {
                                        sessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                                message = context.getString(R.string.not_allowed),
                                                recoverable = true,
                                            )
                                    }
                                    ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.ServerIssue -> {
                                Timber.tag("Together").w("server issue (lan) code=${event.code.orEmpty()} message=${event.message}")
                                when (event.code) {
                                    "GUEST_CONTROL_DISABLED" -> {
                                        showNotice(event.message, key = "GUEST_CONTROL_DISABLED")
                                        val joined =
                                            sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
                                        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
                                            togetherPendingGuestControl = null
                                            togetherLastSentControlAction = null
                                            scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                                        }
                                    }

                                    "GUEST_ADD_DISABLED" -> {
                                        showNotice(event.message, key = "GUEST_ADD_DISABLED")
                                    }

                                    "HOST_OFFLINE" -> {
                                        showNotice(event.message, key = "HOST_OFFLINE")
                                    }

                                    else -> {
                                        scope.launch(SilentHandler) {
                                            sessionState.value =
                                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                                    message = event.message,
                                                    recoverable = true,
                                                )
                                        }
                                        ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                    }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.HeartbeatPong -> {
                                val clock = togetherClock ?: return@collect
                                clock.onPong(
                                    sentAtElapsedMs = event.pong.clientElapsedRealtimeMs,
                                    receivedAtElapsedMs = event.receivedAtElapsedRealtimeMs,
                                    serverElapsedMs = event.pong.serverElapsedRealtimeMs,
                                )
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.Error -> {
                                scope.launch(SilentHandler) {
                                    sessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message = event.message,
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }

                            com.arturo254.opentune.together.TogetherClientEvent.Disconnected -> {
                                val current = sessionState.value
                                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return@collect
                                scope.launch(SilentHandler) {
                                    val currentState = sessionState.value
                                    sessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message =
                                                if (currentState is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                                                    currentState.role is com.arturo254.opentune.together.TogetherRole.Guest
                                                ) {
                                                    context.getString(R.string.together_host_left_session)
                                                } else {
                                                    context.getString(R.string.network_unavailable)
                                                },
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }
                        }
                    }
                }

            client.connect(joinInfo, displayName.trim().ifBlank { context.getString(R.string.together_role_guest) })
        }
    }

    fun joinTogetherOnline(
        code: String,
        displayName: String,
    ) {
        ensureScopesActive()
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            scope.launch(SilentHandler) {
                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = context.getString(R.string.invalid_code),
                        recoverable = true,
                    )
            }
            return
        }

        scope.launch(SilentHandler) {
            sessionState.value = com.arturo254.opentune.together.TogetherSessionState.JoiningOnline(trimmedCode)
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = true

            val baseUrl = com.arturo254.opentune.together.TogetherOnlineEndpoint.baseUrlOrNull(dataStore)
            if (baseUrl == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = context.getString(R.string.together_online_not_configured),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val togetherToken = com.arturo254.opentune.BuildConfig.TOGETHER_BEARER_TOKEN.trim().takeIf { it.isNotBlank() }
            if (togetherToken == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = context.getString(R.string.together_token_missing),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val api = com.arturo254.opentune.together.TogetherOnlineApi(baseUrl = baseUrl, bearerToken = togetherToken)
            val resolved =
                runCatching { api.resolveCode(trimmedCode) }
                    .getOrElse { t ->
                        scope.launch(SilentHandler) {
                            sessionState.value =
                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                    message = togetherOnlineErrorMessage(t),
                                    recoverable = true,
                                )
                        }
                        onError(t)
                        return@launch
                    }

            val client =
                com.arturo254.opentune.together.TogetherClient(
                    ioScope,
                    clientId = getOrCreateTogetherClientId(),
                    bearerToken = togetherToken,
                )
            togetherClient = client
            togetherClock = com.arturo254.opentune.together.TogetherClock()
            togetherSelfParticipantId = null
            togetherLastAppliedQueueHash = null

            togetherClientEventsJob?.cancel()
            togetherClientEventsJob =
                ioScope.launch(SilentHandler) {
                    client.events.collect { event ->
                        when (event) {
                            is com.arturo254.opentune.together.TogetherClientEvent.Welcome -> {
                                togetherSelfParticipantId = event.welcome.participantId
                                scope.launch(SilentHandler) {
                                    val state = sessionState.value
                                    if (state is com.arturo254.opentune.together.TogetherSessionState.JoiningOnline) {
                                        val selfName = displayName.trim().ifBlank { context.getString(R.string.together_role_guest) }
                                        val initial =
                                            com.arturo254.opentune.together.TogetherRoomState(
                                                sessionId = resolved.sessionId,
                                                hostId = togetherHostId,
                                                participants =
                                                    listOf(
                                                        com.arturo254.opentune.together.TogetherParticipant(
                                                            id = event.welcome.participantId,
                                                            name = selfName,
                                                            isHost = false,
                                                            isPending = event.welcome.isPending,
                                                            isConnected = true,
                                                        ),
                                                    ),
                                                settings = event.welcome.settings,
                                                queue = emptyList(),
                                                queueHash = "",
                                                currentIndex = 0,
                                                isPlaying = false,
                                                positionMs = 0L,
                                                repeatMode = 0,
                                                shuffleEnabled = false,
                                                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                                            )
                                        sessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Joined(
                                                role = com.arturo254.opentune.together.TogetherRole.Guest,
                                                sessionId = resolved.sessionId,
                                                selfParticipantId = event.welcome.participantId,
                                                roomState = initial,
                                            )
                                    }
                                }
                                startTogetherHeartbeat(resolved.sessionId, client)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.RoomState -> {
                                applyRemoteRoomState(event.state)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.JoinDecision -> {
                                if (!event.decision.approved) {
                                    scope.launch(SilentHandler) {
                                        sessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                                message = context.getString(R.string.not_allowed),
                                                recoverable = true,
                                            )
                                    }
                                    ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.ServerIssue -> {
                                Timber.tag("Together").w("server issue (online) code=${event.code.orEmpty()} message=${event.message}")
                                when (event.code) {
                                    "GUEST_CONTROL_DISABLED" -> {
                                        showNotice(event.message, key = "GUEST_CONTROL_DISABLED")
                                        val joined =
                                            sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
                                        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
                                            togetherPendingGuestControl = null
                                            togetherLastSentControlAction = null
                                            scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                                        }
                                    }

                                    "GUEST_ADD_DISABLED" -> {
                                        showNotice(event.message, key = "GUEST_ADD_DISABLED")
                                    }

                                    "HOST_OFFLINE" -> {
                                        showNotice(event.message, key = "HOST_OFFLINE")
                                    }

                                    else -> {
                                        scope.launch(SilentHandler) {
                                            sessionState.value =
                                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                                    message = event.message,
                                                    recoverable = true,
                                                )
                                        }
                                        ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                    }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.HeartbeatPong -> {
                                val clock = togetherClock ?: return@collect
                                clock.onPong(
                                    sentAtElapsedMs = event.pong.clientElapsedRealtimeMs,
                                    receivedAtElapsedMs = event.receivedAtElapsedRealtimeMs,
                                    serverElapsedMs = event.pong.serverElapsedRealtimeMs,
                                )
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.Error -> {
                                scope.launch(SilentHandler) {
                                    sessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message = event.message,
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }

                            com.arturo254.opentune.together.TogetherClientEvent.Disconnected -> {
                                val current = sessionState.value
                                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return@collect
                                scope.launch(SilentHandler) {
                                    val currentState = sessionState.value
                                    sessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message =
                                                if (currentState is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                                                    currentState.role is com.arturo254.opentune.together.TogetherRole.Guest
                                                ) {
                                                    context.getString(R.string.together_host_left_session)
                                                } else {
                                                    context.getString(R.string.network_unavailable)
                                                },
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }
                        }
                    }
                }

            val wsUrl =
                com.arturo254.opentune.together.TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(
                    rawWsUrl = resolved.wsUrl,
                    baseUrl = baseUrl,
                )
            if (wsUrl == null) {
                scope.launch(SilentHandler) {
                    sessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = "Connection failed: Invalid server websocket URL",
                            recoverable = true,
                        )
                }
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                return@launch
            }

            client.connect(
                wsUrl = wsUrl,
                sessionId = resolved.sessionId,
                sessionKey = resolved.guestKey,
                displayName = displayName.trim().ifBlank { context.getString(R.string.together_role_guest) },
            )
        }
    }

    fun leaveTogether() {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            sessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }
        ioScope.launch(SilentHandler) { stopTogetherInternal() }
    }

    fun updateTogetherSettings(settings: com.arturo254.opentune.together.TogetherRoomSettings) {
        val server = togetherServer
        val onlineHost = togetherOnlineHost
        if (server == null && onlineHost == null) return
        ioScope.launch(SilentHandler) {
            server?.updateSettings(settings)
            onlineHost?.updateSettings(settings)
        }
    }

    fun approveTogetherParticipant(participantId: String, approved: Boolean) {
        val server = togetherServer
        val onlineHost = togetherOnlineHost
        if (server == null && onlineHost == null) return
        ioScope.launch(SilentHandler) {
            server?.approveParticipant(participantId, approved)
            onlineHost?.approveParticipant(participantId, approved)
        }
    }

    fun kickTogetherParticipant(participantId: String, reason: String? = null) {
        val onlineHost = togetherOnlineHost ?: return
        ioScope.launch(SilentHandler) {
            onlineHost.kickParticipant(participantId, reason)
        }
    }

    fun banTogetherParticipant(participantId: String, reason: String? = null) {
        val onlineHost = togetherOnlineHost ?: return
        ioScope.launch(SilentHandler) {
            onlineHost.banParticipant(participantId, reason)
        }
    }

    fun requestTogetherControl(action: com.arturo254.opentune.together.ControlAction) {
        val client =
            togetherClient ?: run {
                showNotice(context.getString(R.string.network_unavailable), key = "TOGETHER_CLIENT_MISSING")
                return
            }
        val state = sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined ?: return
        if (state.role !is com.arturo254.opentune.together.TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToControlPlayback) {
            Timber.tag("Together").i("control blocked locally (disabled) action=${action::class.java.simpleName}")
            showNotice(context.getString(R.string.not_allowed), key = "GUEST_CONTROL_DISABLED_LOCAL")
            return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        val lastAction = togetherLastSentControlAction
        val lastAt = togetherLastSentControlAtElapsedMs
        if (lastAction == action && now - lastAt < 350L) return
        togetherLastSentControlAction = action
        togetherLastSentControlAtElapsedMs = now

        val timeout = if (togetherIsOnlineSession) 5000L else 2000L
        togetherPendingGuestControl =
            when (action) {
                com.arturo254.opentune.together.ControlAction.Play ->
                    TogetherPendingGuestControl(desiredIsPlaying = true, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                com.arturo254.opentune.together.ControlAction.Pause ->
                    TogetherPendingGuestControl(desiredIsPlaying = false, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                is com.arturo254.opentune.together.ControlAction.SeekToIndex ->
                    TogetherPendingGuestControl(desiredIndex = action.index.coerceAtLeast(0), requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                is com.arturo254.opentune.together.ControlAction.SeekToTrack ->
                    TogetherPendingGuestControl(
                        desiredTrackId = action.trackId.trim().ifBlank { null },
                        requestedAtElapsedMs = now,
                        expiresAtElapsedMs = now + timeout,
                    )
                else -> togetherPendingGuestControl
            }
        client.requestControl(state.sessionId, action)
    }

    fun requestTogetherAddTrack(
        track: com.arturo254.opentune.together.TogetherTrack,
        mode: com.arturo254.opentune.together.AddTrackMode,
    ) {
        val client = togetherClient ?: return
        val state = sessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined ?: return
        if (state.role !is com.arturo254.opentune.together.TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToAddTracks) {
            Timber.tag("Together").i("add blocked locally (disabled) mode=$mode trackId=${track.id}")
            showNotice(context.getString(R.string.not_allowed), key = "GUEST_ADD_DISABLED_LOCAL")
            return
        }
        client.requestAddTrack(state.sessionId, track, mode)
    }

    private suspend fun handleTogetherHostEvent(
        event: com.arturo254.opentune.together.TogetherServerEvent,
        currentSettings: suspend () -> com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        when (event) {
            is com.arturo254.opentune.together.TogetherServerEvent.ControlRequested -> {
                val settings = currentSettings()
                if (!settings.allowGuestsToControlPlayback) return
                applyHostControl(event.request.action)
            }

            is com.arturo254.opentune.together.TogetherServerEvent.AddTrackRequested -> {
                val settings = currentSettings()
                if (!settings.allowGuestsToAddTracks) return
                applyHostAddTrack(event.request.track, event.request.mode)
            }

            is com.arturo254.opentune.together.TogetherServerEvent.Error -> {
                val current = sessionState.value
                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return
                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = event.message,
                        recoverable = true,
                    )
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
            }

            else -> Unit
        }
    }

    private suspend fun applyHostControl(action: com.arturo254.opentune.together.ControlAction) {
        withContext(Dispatchers.Main) {
            when (action) {
                com.arturo254.opentune.together.ControlAction.Play -> {
                    if (!player.playWhenReady) {
                        player.prepare()
                        player.playWhenReady = true
                    }
                }

                com.arturo254.opentune.together.ControlAction.Pause -> {
                    if (player.playWhenReady) {
                        player.playWhenReady = false
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekTo -> {
                    player.seekTo(action.positionMs.coerceAtLeast(0L))
                    player.prepare()
                }

                com.arturo254.opentune.together.ControlAction.SkipNext -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.playWhenReady = true
                    }
                }

                com.arturo254.opentune.together.ControlAction.SkipPrevious -> {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPrevious()
                        player.playWhenReady = true
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekToTrack -> {
                    val trackId = action.trackId.trim()
                    if (trackId.isNotBlank()) {
                        val idx =
                            player.mediaItems.indexOfFirst {
                                val metaId = it.metadata?.id
                                it.mediaId == trackId || metaId == trackId
                            }
                        if (idx >= 0 && idx < player.mediaItemCount) {
                            player.seekTo(idx, action.positionMs.coerceAtLeast(0L))
                            player.prepare()
                        }
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekToIndex -> {
                    val idx = action.index.coerceAtLeast(0)
                    if (idx < player.mediaItemCount) {
                        player.seekTo(idx, action.positionMs.coerceAtLeast(0L))
                        player.prepare()
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SetRepeatMode -> {
                    if (player.repeatMode != action.repeatMode) {
                        player.repeatMode = action.repeatMode
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SetShuffleEnabled -> {
                    if (player.shuffleModeEnabled != action.shuffleEnabled) {
                        player.shuffleModeEnabled = action.shuffleEnabled
                    }
                }
            }
        }
    }

    private suspend fun applyHostAddTrack(
        track: com.arturo254.opentune.together.TogetherTrack,
        mode: com.arturo254.opentune.together.AddTrackMode,
    ) {
        val mediaItem = track.toMediaMetadata().toMediaItem()
        withContext(Dispatchers.Main) {
            when (mode) {
                com.arturo254.opentune.together.AddTrackMode.PLAY_NEXT -> queueController.playNext(listOf(mediaItem))
                com.arturo254.opentune.together.AddTrackMode.ADD_TO_QUEUE -> queueController.addToQueue(listOf(mediaItem))
            }
        }
    }

    private suspend fun buildTogetherRoomState(
        sessionId: String,
        hostId: String,
    ): com.arturo254.opentune.together.TogetherRoomState {
        return withContext(Dispatchers.Main) {
            val tracks =
                player.mediaItems.mapNotNull { it.metadata }.map { meta ->
                    com.arturo254.opentune.together.TogetherTrack(
                        id = meta.id,
                        title = meta.title,
                        artists = meta.artists.map { it.name },
                        durationSec = meta.duration,
                        thumbnailUrl = meta.thumbnailUrl,
                    )
                }

            val queueHash = com.arturo254.opentune.utils.md5(tracks.joinToString(separator = "|") { it.id })

            com.arturo254.opentune.together.TogetherRoomState(
                sessionId = sessionId,
                hostId = hostId,
                settings = com.arturo254.opentune.together.TogetherRoomSettings(),
                participants = emptyList(),
                queue = tracks,
                queueHash = queueHash,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
            )
        }
    }

    private suspend fun applyRemoteRoomState(state: com.arturo254.opentune.together.TogetherRoomState) {
        val pid = togetherSelfParticipantId ?: return
        val now = android.os.SystemClock.elapsedRealtime()

        val pending = togetherPendingGuestControl
        if (pending != null) {
            val currentTrackId = state.queue.getOrNull(state.currentIndex.coerceAtLeast(0))?.id
            val mismatch =
                (pending.desiredIsPlaying != null && state.isPlaying != pending.desiredIsPlaying) ||
                        (pending.desiredIndex != null && state.currentIndex != pending.desiredIndex) ||
                        (pending.desiredTrackId != null && currentTrackId != pending.desiredTrackId)
            if (now >= pending.expiresAtElapsedMs) {
                if ((pending.desiredIndex != null || pending.desiredTrackId != null) &&
                    now - pending.requestedAtElapsedMs >= 1200L &&
                    mismatch
                ) {
                    showNotice(context.getString(R.string.together_song_change_failed), key = "GUEST_SEEK_TIMEOUT")
                }
                togetherPendingGuestControl = null
            } else {
                if (mismatch) return
                togetherPendingGuestControl = null
            }
        }

        val lastSentAt = togetherLastAppliedRoomStateSentAtElapsedMs
        val sentAt = state.sentAtElapsedRealtimeMs
        if (sentAt > 0L && lastSentAt > 0L && sentAt <= lastSentAt) return

        val offset = if (togetherIsOnlineSession) 0L else (togetherClock?.snapshot()?.estimatedOffsetMs ?: 0L)
        val correctedSentAt = sentAt + offset
        val estimatedOnlineLatency = if (togetherIsOnlineSession) 1200L else 0L
        val delta = if (togetherIsOnlineSession) estimatedOnlineLatency else (now - correctedSentAt).coerceAtLeast(0L)
        val targetPos =
            if (state.isPlaying) (state.positionMs + delta).coerceAtLeast(0L) else state.positionMs.coerceAtLeast(0L)

        withContext(Dispatchers.Main) {
            togetherApplyingRemote = true
            togetherSuppressEchoUntilElapsedMs = android.os.SystemClock.elapsedRealtime() + 450L
            try {
                val desiredItems = state.queue.map { it.toMediaMetadata().toMediaItem() }
                val desiredIds = state.queue.map { it.id }
                val desiredHash = state.queueHash
                val localIds = player.mediaItems.mapNotNull { it.metadata?.id ?: it.mediaId }.filter { it.isNotBlank() }
                val localHash = if (localIds.isEmpty()) "" else com.arturo254.opentune.utils.md5(localIds.joinToString(separator = "|"))
                val needsRebuild =
                    desiredItems.isNotEmpty() &&
                            (
                                    (desiredHash.isNotBlank() && desiredHash != localHash) ||
                                            (desiredHash.isBlank() && desiredIds != localIds)
                                    )

                if (desiredItems.isNotEmpty() && needsRebuild) {
                    togetherLastAppliedQueueHash = desiredHash.ifBlank { localHash }
                    val startIndex = state.currentIndex.coerceIn(0, desiredItems.lastIndex)
                    queueController.suppressAutoPlayback = false
                    queueController.currentQueue =
                        com.arturo254.opentune.playback.queues.ListQueue(
                            title = context.getString(R.string.music_player),
                            items = desiredItems,
                            startIndex = startIndex,
                            position = targetPos,
                        )
                    queueController.queueTitle = null
                    player.setMediaItems(desiredItems, startIndex, targetPos)
                    player.prepare()
                    player.repeatMode = state.repeatMode
                    player.shuffleModeEnabled = state.shuffleEnabled
                    player.playWhenReady = state.isPlaying
                    togetherLastRemoteAppliedIndex = startIndex
                } else {
                    val index = state.currentIndex.coerceAtLeast(0)
                    val indexChanged = player.mediaItemCount > 0 && index != player.currentMediaItemIndex
                    val stateChanged =
                        player.repeatMode != state.repeatMode ||
                                player.shuffleModeEnabled != state.shuffleEnabled ||
                                player.playWhenReady != state.isPlaying

                    if (indexChanged) {
                        player.seekTo(index.coerceAtMost(player.mediaItemCount - 1), targetPos)
                        player.prepare()
                        player.playWhenReady = state.isPlaying
                    } else if (stateChanged) {
                        if (player.repeatMode != state.repeatMode) player.repeatMode = state.repeatMode
                        if (player.shuffleModeEnabled != state.shuffleEnabled) player.shuffleModeEnabled = state.shuffleEnabled
                        if (player.playWhenReady != state.isPlaying) {
                            player.playWhenReady = state.isPlaying
                            val drift = kotlin.math.abs(player.currentPosition - targetPos)
                            if (drift > 100) {
                                player.seekTo(targetPos)
                                player.prepare()
                            }
                        }
                    } else {
                        val drift = kotlin.math.abs(player.currentPosition - targetPos)
                        val seekThreshold = if (togetherIsOnlineSession) 4000L else 2000L
                        val threshold = if (state.isPlaying) seekThreshold else 200L

                        if (drift > threshold) {
                            player.seekTo(targetPos)
                            player.prepare()
                        }
                    }
                    togetherLastRemoteAppliedIndex = index
                }
                togetherLastRemoteAppliedPlayWhenReady = state.isPlaying
                togetherLastAppliedRoomStateSentAtElapsedMs = sentAt

                sessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Joined(
                        role = com.arturo254.opentune.together.TogetherRole.Guest,
                        sessionId = state.sessionId,
                        selfParticipantId = pid,
                        roomState = state,
                    )
            } finally {
                togetherApplyingRemote = false
            }
        }
    }

    private fun startTogetherHeartbeat(sessionId: String, client: com.arturo254.opentune.together.TogetherClient) {
        togetherHeartbeatJob?.cancel()
        togetherHeartbeatJob =
            ioScope.launch(SilentHandler) {
                var pingId = 0L
                while (togetherClient === client) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    client.sendHeartbeat(sessionId = sessionId, pingId = pingId++, clientElapsedRealtimeMs = now)
                    kotlinx.coroutines.delay(2000)
                }
            }
    }

    private suspend fun stopTogetherInternal() {
        togetherBroadcastJob?.cancel()
        togetherBroadcastJob = null

        togetherOnlineConnectJob?.cancel()
        togetherOnlineConnectJob = null

        togetherClientEventsJob?.cancel()
        togetherClientEventsJob = null

        togetherHeartbeatJob?.cancel()
        togetherHeartbeatJob = null

        togetherClock = null
        togetherSelfParticipantId = null
        togetherLastAppliedQueueHash = null
        togetherIsOnlineSession = false
        togetherApplyingRemote = false
        togetherSuppressEchoUntilElapsedMs = 0L
        togetherLastAppliedRoomStateSentAtElapsedMs = 0L
        togetherLastRemoteAppliedPlayWhenReady = null
        togetherLastRemoteAppliedIndex = -1
        togetherLastSentControlAtElapsedMs = 0L
        togetherLastSentControlAction = null
        togetherPendingGuestControl = null

        try {
            togetherClient?.disconnect()
        } catch (_: Exception) {}
        togetherClient = null

        try {
            togetherOnlineHost?.disconnect()
        } catch (_: Exception) {}
        togetherOnlineHost = null

        try {
            togetherServer?.stop()
        } catch (_: Exception) {}
        togetherServer = null
    }

    private fun com.arturo254.opentune.together.TogetherTrack.toMediaMetadata(): com.arturo254.opentune.models.MediaMetadata {
        return com.arturo254.opentune.models.MediaMetadata(
            id = id,
            title = title,
            artists = artists.map { name -> com.arturo254.opentune.models.MediaMetadata.Artist(id = null, name = name) },
            duration = durationSec,
            thumbnailUrl = thumbnailUrl,
        )
    }

    private fun getLocalIpv4Address(): String? {
        return runCatching {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<java.net.Inet4Address>()
                .map { it.hostAddress }
                .firstOrNull { it.isNotBlank() && it != "127.0.0.1" }
        }.getOrNull()
    }


    fun isGuestSession(): Boolean {
        val joined =
            sessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined
        return joined?.role is
            com.arturo254.opentune.together.TogetherRole.Guest
    }

    fun isHostSessionActive(): Boolean {
        val state = sessionState.value
        return state is
            com.arturo254.opentune.together.TogetherSessionState.Hosting ||
            state is
                com.arturo254.opentune.together.TogetherSessionState.HostingOnline ||
            (
                state is
                    com.arturo254.opentune.together.TogetherSessionState.Joined &&
                    state.role is
                        com.arturo254.opentune.together.TogetherRole.Host
            )
    }

    fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ): Boolean {
        val joined =
            sessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined

        if (
            joined?.role !is
                com.arturo254.opentune.together.TogetherRole.Guest ||
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
        ) {
            return false
        }

        if (
            !joined.roomState.settings
                .allowGuestsToControlPlayback
        ) {
            scope.launch(SilentHandler) {
                applyRemoteRoomState(joined.roomState)
            }
            return true
        }

        val now =
            android.os.SystemClock.elapsedRealtime()
        val index =
            player.currentMediaItemIndex
                .coerceAtLeast(0)
        val isEcho =
            isApplyingRemote() ||
                (
                    now <
                        togetherSuppressEchoUntilElapsedMs &&
                        togetherLastRemoteAppliedIndex ==
                            index
                )

        if (!isEcho) {
            val trackId =
                (
                    mediaItem?.metadata
                        ?: player.currentMetadata
                )
                    ?.id
                    ?.trim()
                    .orEmpty()

            requestTogetherControl(
                if (trackId.isBlank()) {
                    com.arturo254.opentune.together.ControlAction.SeekToIndex(
                        index = index,
                        positionMs =
                            player.currentPosition
                                .coerceAtLeast(0L),
                    )
                } else {
                    com.arturo254.opentune.together.ControlAction.SeekToTrack(
                        trackId = trackId,
                        positionMs =
                            player.currentPosition
                                .coerceAtLeast(0L),
                    )
                },
            )
        }

        return false
    }

    fun onPlayWhenReadyChanged() {
        val joined =
            sessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined

        if (
            joined?.role !is
                com.arturo254.opentune.together.TogetherRole.Guest
        ) {
            return
        }

        if (
            !joined.roomState.settings
                .allowGuestsToControlPlayback
        ) {
            scope.launch(SilentHandler) {
                applyRemoteRoomState(joined.roomState)
            }
            return
        }

        val now =
            android.os.SystemClock.elapsedRealtime()
        val playWhenReady = player.playWhenReady
        val isEcho =
            isApplyingRemote() ||
                (
                    now <
                        togetherSuppressEchoUntilElapsedMs &&
                        togetherLastRemoteAppliedPlayWhenReady !=
                            null &&
                        togetherLastRemoteAppliedPlayWhenReady ==
                            playWhenReady
                )

        if (!isEcho) {
            requestTogetherControl(
                if (playWhenReady) {
                    com.arturo254.opentune.together.ControlAction.Play
                } else {
                    com.arturo254.opentune.together.ControlAction.Pause
                },
            )
        }
    }

    fun onShuffleModeChanged(
        shuffleModeEnabled: Boolean,
    ): Boolean {
        val joined =
            sessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined

        if (
            joined?.role !is
                com.arturo254.opentune.together.TogetherRole.Guest
        ) {
            return false
        }

        if (!isApplyingRemote()) {
            if (
                !joined.roomState.settings
                    .allowGuestsToControlPlayback
            ) {
                scope.launch(SilentHandler) {
                    applyRemoteRoomState(
                        joined.roomState,
                    )
                }
                return true
            }

            requestTogetherControl(
                com.arturo254.opentune.together.ControlAction.SetShuffleEnabled(
                    shuffleEnabled =
                        shuffleModeEnabled,
                ),
            )
        }

        return true
    }

    fun onRepeatModeChanged(
        repeatMode: Int,
    ): Boolean {
        val joined =
            sessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined

        if (
            joined?.role !is
                com.arturo254.opentune.together.TogetherRole.Guest
        ) {
            return false
        }

        if (!isApplyingRemote()) {
            if (
                !joined.roomState.settings
                    .allowGuestsToControlPlayback
            ) {
                scope.launch(SilentHandler) {
                    applyRemoteRoomState(
                        joined.roomState,
                    )
                }
                return true
            }

            requestTogetherControl(
                com.arturo254.opentune.together.ControlAction.SetRepeatMode(
                    repeatMode = repeatMode,
                ),
            )
        }

        return true
    }

    fun markIdle() {
        sessionState.value =
            com.arturo254.opentune.together.TogetherSessionState.Idle
    }

    suspend fun stop() {
        stopTogetherInternal()
    }
}
