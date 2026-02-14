package io.bluetape4k.junit5.utils

import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import java.nio.file.Files
import kotlin.test.assertTrue

class ExtensionTesterTest {

    @Test
    fun `execute 는 테스트 이벤트를 기록한다`() {
        val listener = ExtensionTester.execute(selectClass(SampleJupiterTest::class.java))

        assertTrue(listener.countEventsByType(ExecutionEvent.EventType.STARTED) > 0)
        assertTrue(listener.countEventsByType(ExecutionEvent.EventType.FINISHED) > 0)
    }

    @Test
    fun `temporary output directory 는 root 하위 절대 경로로 생성된다`() {
        val creator = ExtensionTester.TemporaryOutputDirectoryCreator()
        val root = creator.rootDirectory
        val descriptor = discoverDescriptor(SampleJupiterTest::class.java)

        val outputDir = creator.createOutputDirectory(descriptor)

        assertTrue(outputDir.isAbsolute)
        assertTrue(outputDir.startsWith(root))
        assertTrue(Files.exists(outputDir))
    }

    private fun discoverDescriptor(testClass: Class<*>): TestDescriptor {
        val engine = org.junit.jupiter.engine.JupiterTestEngine()
        val request = LauncherDiscoveryRequestBuilder
            .request()
            .selectors(selectClass(testClass))
            .build()

        return engine.discover(request, UniqueId.forEngine(engine.id))
    }

    class SampleJupiterTest {
        @Test
        fun pass() {
            assertTrue(true)
        }
    }
}
