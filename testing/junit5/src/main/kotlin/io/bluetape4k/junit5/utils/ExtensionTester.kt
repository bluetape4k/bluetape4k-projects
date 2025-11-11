package io.bluetape4k.junit5.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.platformClassName
import org.junit.jupiter.engine.JupiterTestEngine
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.OutputDirectoryCreator
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name


object ExtensionTester {

    /**
     * 테스트 클래스를 Jupiter engine으로 수행합니다.
     *
     * ```
     * ExtensionTester.execute(selectClass(SystemPropertyExtensionClassTest::class.java))
     * ```
     *
     * @param selectors Array<out DiscoverySelector>
     * @return RecordingExecutionListener
     */
    fun execute(vararg selectors: DiscoverySelector): RecordingExecutionListener {
        val testEngine = JupiterTestEngine()

        // 요청된 테스트 리소스를 찾습니다.
        val discoveryRequest = LauncherDiscoveryRequestBuilder
            .request()
            .selectors(*selectors)
            .build()
        val listener = RecordingExecutionListener()

        // 찾은 테스트 리소스를 실행합니다.
        val testDescriptor = testEngine.discover(discoveryRequest, UniqueId.forEngine(testEngine.id))!!
        testEngine.execute(
            ExecutionRequest.create(
                testDescriptor,
                listener,
                discoveryRequest.configurationParameters,
                TemporaryOutputDirectoryCreator(),
                NamespacedHierarchicalStore(NamespacedHierarchicalStore(null))
            )
        )

        return listener
    }

    class TemporaryOutputDirectoryCreator: OutputDirectoryCreator {

        companion object: KLogging() {
            private const val PREFIX = "bluetape4k_"
        }

        private val root by lazy { Files.createTempDirectory(PREFIX) }

        override fun getRootDirectory(): Path? = root

        override fun createOutputDirectory(testDescriptor: TestDescriptor): Path? {
            return Path.of(root.name, testDescriptor.platformClassName())
        }
    }
}
