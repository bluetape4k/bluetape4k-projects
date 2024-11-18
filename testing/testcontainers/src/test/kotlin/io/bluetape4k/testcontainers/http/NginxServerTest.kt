package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.URL

@Execution(ExecutionMode.SAME_THREAD)
class NginxServerTest {

    companion object: KLogging() {
        val tempDir = System.getProperty("user.home") + "/.tmp-test-container"
    }

    @BeforeAll
    fun beforeAll() {
        // add custom contents to the temp directory
        val contentFolder = File(tempDir)
        if (!contentFolder.exists()) {
            contentFolder.mkdirs()
            contentFolder.deleteOnExit()
        }

        // Add "hello world" HTTP file
        val indexFile = File(contentFolder, "index.html").apply { deleteOnExit() }
        PrintStream(FileOutputStream(indexFile)).use {
            it.println("<html><body><h1>Hello World!</h1></body></html>")
        }
    }

    @Test
    fun `launch nginx with default port`() {
        // NginxServer(useDefaultPort = true).use { nginx ->
        //    nginx.withCopyFileToContainer(MountableFile.forHostPath(tempDir), NginxServer.NGINX_PATH)
        //    nginx.start()
        NginxServer.Launcher.launch(tempDir).use { nginx ->
            val baseUrl = nginx.getBaseUrl("http", 80)

            val response = responseFromNginx(baseUrl)
            log.debug { "response=$response" }
            response shouldContain "Hello World!"

            // assert default port
            assertNginxDefaultPort(nginx)
        }
    }

    private fun responseFromNginx(baseUrl: URL): String {
        val connection = baseUrl.openConnection()
        return InputStreamReader(connection.getInputStream()).buffered().use { reader ->
            reader.readLine()
        }
    }

    private fun assertNginxDefaultPort(nginx: NginxServer) {
        nginx.exposedPorts shouldContainSame listOf(NginxServer.PORT)
        nginx.livenessCheckPortNumbers shouldContainSame listOf(NginxServer.PORT)
    }
}
