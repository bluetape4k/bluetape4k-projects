package io.bluetape4k.junit5.utils

import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import java.nio.file.Files

class ExtensionTesterTest {

    @Test
    fun `execute 는 테스트 이벤트를 기록한다`() {
        val listener = ExtensionTester.execute(selectClass(SampleJupiterTest::class.java))

        (listener.countEventsByType(ExecutionEvent.EventType.STARTED) > 0).shouldBeTrue()
        (listener.countEventsByType(ExecutionEvent.EventType.FINISHED) > 0).shouldBeTrue()
    }

    @Test
    fun `temporary output directory 는 root 하위 절대 경로로 생성된다`() {
        val creator = ExtensionTester.TemporaryOutputDirectoryCreator()
        val root = creator.rootDirectory
        val descriptor = discoverDescriptor(SampleJupiterTest::class.java)

        val outputDir = creator.createOutputDirectory(descriptor)

        outputDir.isAbsolute.shouldBeTrue()
        outputDir.startsWith(root).shouldBeTrue()
        Files.exists(outputDir).shouldBeTrue()
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
            true.shouldBeTrue()
        }
    }
}
