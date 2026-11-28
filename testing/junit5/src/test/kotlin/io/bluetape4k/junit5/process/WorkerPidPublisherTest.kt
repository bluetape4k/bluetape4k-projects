package io.bluetape4k.junit5.process

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

internal class WorkerPidPublisherTest {

    @TempDir
    lateinit var tempDir: Path

    private var previousWorkerPidFile: String? = null
    private var previousProcessOwner: String? = null

    @BeforeEach
    fun rememberSystemProperties() {
        previousWorkerPidFile = System.getProperty(WORKER_PID_FILE_PROPERTY)
        previousProcessOwner = System.getProperty(PROCESS_OWNER_PROPERTY)
    }

    @AfterEach
    fun restoreSystemProperties() {
        restoreProperty(WORKER_PID_FILE_PROPERTY, previousWorkerPidFile)
        restoreProperty(PROCESS_OWNER_PROPERTY, previousProcessOwner)
    }

    @Test
    fun `missing worker pid property is a no-op`() {
        System.clearProperty(WORKER_PID_FILE_PROPERTY)
        System.clearProperty(PROCESS_OWNER_PROPERTY)

        WorkerPidPublisher.publishIfConfigured()
    }

    @Test
    fun `blank process owner is rejected before creating the pid file`() {
        val target = tempDir.resolve("worker.pid")
        System.setProperty(WORKER_PID_FILE_PROPERTY, target.toString())
        System.setProperty(PROCESS_OWNER_PROPERTY, " ")

        val failure = assertFailsWith<IllegalArgumentException> {
            WorkerPidPublisher.publishIfConfigured()
        }

        failure.message shouldContain PROCESS_OWNER_PROPERTY
        Files.exists(target).shouldBeFalse()
    }

    @Test
    fun `symbolic link parent is rejected without writing through the link`() {
        val outside = Files.createDirectory(tempDir.resolve("outside"))
        val linkedParent = tempDir.resolve("linked-parent")
        Files.createSymbolicLink(linkedParent, outside)
        val target = linkedParent.resolve("worker.pid")
        System.setProperty(WORKER_PID_FILE_PROPERTY, target.toString())
        System.setProperty(PROCESS_OWNER_PROPERTY, "phase-c-owner")

        val failure = assertFailsWith<IllegalArgumentException> {
            WorkerPidPublisher.publishIfConfigured()
        }

        failure.message shouldContain "parent"
        Files.exists(outside.resolve("worker.pid")).shouldBeFalse()
    }

    @Test
    fun `create new writes exact pid bytes and rejects replacement`() {
        val target = tempDir.resolve("worker.pid")
        System.setProperty(WORKER_PID_FILE_PROPERTY, target.toString())
        System.setProperty(PROCESS_OWNER_PROPERTY, "phase-c-owner")

        WorkerPidPublisher.publishIfConfigured()

        val expected = "${ProcessHandle.current().pid()}\n".toByteArray(Charsets.UTF_8)
        Files.readAllBytes(target) shouldContentEqual expected
        Files.size(target) shouldBeEqualTo expected.size.toLong()

        val failure = assertFailsWith<FileAlreadyExistsException> {
            WorkerPidPublisher.publishIfConfigured()
        }

        failure.message.shouldNotBeNull()
        Files.readAllBytes(target).toList() shouldBeEqualTo expected.toList()
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }

    private companion object {
        const val WORKER_PID_FILE_PROPERTY = WorkerPidPublisher.DEFAULT_PID_FILE_PROPERTY
        const val PROCESS_OWNER_PROPERTY = WorkerPidPublisher.DEFAULT_PROCESS_OWNER_PROPERTY
    }
}
