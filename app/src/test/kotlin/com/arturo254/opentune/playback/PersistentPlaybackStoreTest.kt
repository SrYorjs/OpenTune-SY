package com.arturo254.opentune.playback

import com.arturo254.opentune.models.*
import java.io.*
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistentPlaybackStoreTest {
    @get:Rule val temporary = TemporaryFolder(File("build/test-file-store").apply { mkdirs() })

    private fun queue() = PersistQueue("fixture", listOf(
        MediaMetadata("-9l5t3MWss8", "first", emptyList(), 100),
        MediaMetadata("Mcj75l2gJcY", "second", emptyList(), 200),
    ), 1, 25000L)

    @Test fun queueAndTrackStateRestoreAfterRepeatedFlushSyncAndClose() {
        val store = PersistentPlaybackStore(temporary.root)
        val queue = queue()
        val state = PersistPlayerState(true, 1, true, .75f, 25000, 1, 3, 1234)
        repeat(5) {
            store.write("persistent_queue.data", queue)
            store.write("persistent_player_state.data", state)
        }
        val fresh = PersistentPlaybackStore(temporary.root)
        assertQueueState(queue, fresh.read("persistent_queue.data"))
        assertEquals(state, fresh.read("persistent_player_state.data"))
    }

    @Test fun legacySerializationRemainsReadableAndWriteFailurePreservesIt() {
        val file = temporary.root.resolve("persistent_queue.data")
        ObjectOutputStream(file.outputStream()).use { it.writeObject(queue()) }
        val store = PersistentPlaybackStore(temporary.root)
        try { store.write(file.name, BrokenPayload()); fail("write failure ignored") } catch (_: IOException) { }
        assertQueueState(queue(), store.read(file.name))
        assertTrue(file.isFile)
    }

    @Test fun filesystemWithoutAtomicMoveUsesSafeBackup() {
        val store = PersistentPlaybackStore(temporary.root, atomicMove = { source, target ->
            throw AtomicMoveNotSupportedException(source.toString(), target.toString(), "fixture")
        })
        store.write("state", "previous")
        store.write("state", "current")
        assertEquals("current", store.read("state"))
    }

    @Test fun replacementFailureRestoresPreviousFileAndKeepsRecoverableBackup() {
        val initial = PersistentPlaybackStore(temporary.root)
        initial.write("state", "previous")
        val failing = PersistentPlaybackStore(temporary.root,
            atomicMove = { source, target -> throw AtomicMoveNotSupportedException(source.toString(), target.toString(), "fixture") },
            replaceMove = { _, target -> Files.write(target, byteArrayOf(1)); throw IOException("fixture") })
        try { failing.write("state", "current"); fail("failure ignored") } catch (_: IOException) { }
        assertEquals("previous", PersistentPlaybackStore(temporary.root).read("state"))
        assertEquals("previous", ObjectInputStream(temporary.root.resolve("state").inputStream()).use { it.readObject() })
        initial.write("state", "next")
        assertEquals("next", initial.read("state"))
    }

    private class BrokenPayload : Serializable {
        private fun writeObject(output: ObjectOutputStream) {
            output.writeInt(1)
            throw IOException("fixture")
        }
    }

    private fun assertQueueState(expected: PersistQueue, value: Any?) {
        assertTrue(value is PersistQueue)
        val restored = value as PersistQueue
        assertEquals(expected.title, restored.title)
        assertEquals(expected.items, restored.items)
        assertEquals(expected.mediaItemIndex, restored.mediaItemIndex)
        assertEquals(expected.position, restored.position)
        assertEquals(expected.queueData, restored.queueData)
        assertEquals(expected.queueType.javaClass, restored.queueType.javaClass)
    }
}
