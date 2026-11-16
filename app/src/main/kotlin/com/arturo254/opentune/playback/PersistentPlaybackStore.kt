package com.arturo254.opentune.playback

import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal class PersistentPlaybackStore(
    private val directory: File,
    private val atomicMove: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    },
    private val replaceMove: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    },
) {
    @Synchronized
    fun read(name: String): Any? {
        val backup = directory.resolve("$name.bak")
        val source = directory.resolve(name)
        if (backup.isFile) {
            try {
                return ObjectInputStream(backup.inputStream()).use { it.readObject() }
            } catch (error: Exception) {
                if (!source.isFile) throw error
            }
        }
        if (!source.isFile) return null
        return ObjectInputStream(source.inputStream()).use { it.readObject() }
    }

    @Synchronized
    fun write(name: String, payload: Serializable) {
        val target = directory.resolve(name).toPath()
        val backup = directory.resolve("$name.bak")
        val temporary = Files.createTempFile(directory.toPath(), "$name-", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use { raw ->
                ObjectOutputStream(raw).use { output ->
                    output.writeObject(payload)
                    output.flush()
                    raw.fd.sync()
                }
            }
            val validBackup = if (backup.isFile) runCatching {
                ObjectInputStream(backup.inputStream()).use { it.readObject() }
            }.isSuccess else false
            if (validBackup) {
                Files.copy(backup.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
            }
            try {
                atomicMove(temporary, target)
            } catch (_: AtomicMoveNotSupportedException) {
                replaceWithBackup(temporary, target, backup)
            } catch (_: UnsupportedOperationException) {
                replaceWithBackup(temporary, target, backup)
            }
            Files.deleteIfExists(backup.toPath())
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun replaceWithBackup(temporary: Path, target: Path, backup: File) {
        if (Files.exists(target)) {
            val candidate = Files.createTempFile(directory.toPath(), "backup-", ".tmp")
            try {
                FileOutputStream(candidate.toFile()).use { output ->
                    Files.newInputStream(target).use { it.copyTo(output) }
                    output.flush()
                    output.fd.sync()
                }
                Files.move(candidate, backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(candidate)
            }
        }
        try {
            replaceMove(temporary, target)
        } catch (error: Exception) {
            if (backup.isFile) {
                try {
                    FileOutputStream(target.toFile()).use { output ->
                        backup.inputStream().use { it.copyTo(output) }
                        output.flush()
                        output.fd.sync()
                    }
                } catch (restoreError: Exception) {
                    error.addSuppressed(restoreError)
                }
            }
            throw error
        }
    }
}
