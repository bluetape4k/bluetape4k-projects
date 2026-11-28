package io.bluetape4k.junit5.process

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE

/** 소유자 검증 후 현재 프로세스 PID를 한 번만 handoff하는 공용 test fixture입니다. */
object WorkerPidPublisher {

    const val DEFAULT_PID_FILE_PROPERTY = "workerPidFile"
    const val DEFAULT_PROCESS_OWNER_PROPERTY = "processOwner"

    /** 설정된 handoff 경로에 현재 프로세스 PID를 원자적으로 기록합니다. */
    fun publishIfConfigured(
        pidFileProperty: String = DEFAULT_PID_FILE_PROPERTY,
        processOwnerProperty: String = DEFAULT_PROCESS_OWNER_PROPERTY,
    ) {
        val configured = System.getProperty(pidFileProperty) ?: return
        val processOwner = System.getProperty(processOwnerProperty)
        require(!processOwner.isNullOrBlank()) {
            "$processOwnerProperty must not be blank"
        }

        val path = Path.of(configured).toAbsolutePath().normalize()
        val parent = path.parent
        require(
            parent != null &&
                Files.isDirectory(parent, NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(parent),
        ) {
            "worker PID parent is not trusted"
        }

        val bytes = "${ProcessHandle.current().pid()}\n".toByteArray(Charsets.UTF_8)
        FileChannel.open(path, CREATE_NEW, WRITE).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            channel.force(true)
        }
        try {
            FileChannel.open(parent, READ).use { it.force(true) }
        } catch (_: UnsupportedOperationException) {
            // Directory fsync is not supported by every file provider.
        }
    }

}
