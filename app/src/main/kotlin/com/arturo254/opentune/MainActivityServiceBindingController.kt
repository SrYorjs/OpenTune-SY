package com.arturo254.opentune

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.playback.MusicService
import com.arturo254.opentune.playback.MusicService.MusicBinder
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope

/**
 * Owns MainActivity's binding lifecycle to [MusicService].
 *
 * This intentionally preserves the previous activity behavior: binding is
 * started from onStart, unbound from onStop, and the connection object is only
 * disposed when Android reports a service disconnection.
 */
internal class MainActivityServiceBindingController(
    private val context: Context,
    private val database: MusicDatabase,
    private val scope: CoroutineScope,
    private val onConnected: () -> Unit,
) {
    var playerConnection by mutableStateOf<PlayerConnection?>(null)
        private set

    var isBound: Boolean = false
        private set

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                isBound = true
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(
                            context,
                            service,
                            database,
                            scope,
                        )
                    onConnected()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isBound = false
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    fun bind() {
        isBound =
            context.bindService(
                Intent(context, MusicService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
    }

    fun safeUnbind() {
        if (!isBound) return

        try {
            context.unbindService(serviceConnection)
        } catch (_: IllegalArgumentException) {
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isBound = false
        }
    }

    fun clearPlayerConnectionReference() {
        playerConnection = null
    }
}
