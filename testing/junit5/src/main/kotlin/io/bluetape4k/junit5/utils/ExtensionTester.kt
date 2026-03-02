package io.bluetape4k.junit5.utils

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.engine.JupiterTestEngine
import org.junit.platform.engine.CancellationToken
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.OutputDirectoryCreator
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * JUnit Jupiter 엔진을 직접 구동해 확장 동작을 검증하는 테스트 유틸리티입니다.
 *
 * ## 동작/계약
 * - 전달한 selector로 테스트를 discover/execute하고 이벤트를 [RecordingExecutionListener]에 기록합니다.
 * - launcher 외부 API 대신 engine 직접 호출 방식을 사용합니다.
 * - 출력 디렉터리는 임시 폴더에 생성되며 호출자가 별도 지정하지 않습니다.
 *
 * ```kotlin
 * val listener = ExtensionTester.execute(selectClass(MyTest::class.java))
 * // listener.countEventsByType(ExecutionEvent.EventType.FINISHED) >= 1
 * ```
 */
object ExtensionTester {

    /**
     * 전달한 셀렉터를 Jupiter 엔진으로 실행하고 이벤트 리스너를 반환합니다.
     *
     * ## 동작/계약
     * - launcher가 아닌 엔진 직접 호출 방식으로 discover/execute를 수행합니다.
     * - 실행 중 발생한 이벤트는 반환되는 [RecordingExecutionListener]에 누적됩니다.
     * - 출력 디렉터리는 [TemporaryOutputDirectoryCreator]가 임시 경로를 생성해 제공합니다.
     *
     * ```kotlin
     * val listener = ExtensionTester.execute(selectClass(MyExtensionTest::class.java))
     * // listener.countEventsByType(ExecutionEvent.EventType.STARTED) >= 1
     * ```
     *
     * @param selectors 실행할 테스트 선택자들
     * @return 실행 이벤트가 기록된 리스너
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
        val testDescriptor: TestDescriptor = testEngine.discover(discoveryRequest, UniqueId.forEngine(testEngine.id))
        testEngine.execute(
            ExecutionRequest.create(
                testDescriptor,
                listener,
                discoveryRequest.configurationParameters,
                TemporaryOutputDirectoryCreator(),
                NamespacedHierarchicalStore(NamespacedHierarchicalStore(null)),
                CancellationToken.create()
            )
        )

        return listener
    }

    /**
     * 테스트 엔진 출력 파일 루트를 임시 디렉터리로 제공하는 구현체입니다.
     *
     * ## 동작/계약
     * - 루트 경로는 최초 접근 시 한 번 생성됩니다.
     * - 테스트별 출력 경로는 루트 하위에 클래스명 기준으로 생성됩니다.
     */
    class TemporaryOutputDirectoryCreator: OutputDirectoryCreator {

        companion object: KLogging() {
            private const val PREFIX = "bluetape4k_"
        }

        private val root by lazy { Files.createTempDirectory(PREFIX) }

        override fun getRootDirectory(): Path = root

        /**
         * 테스트별 출력 경로를 root 하위의 절대 경로로 생성한다.
         */
        override fun createOutputDirectory(testDescriptor: TestDescriptor): Path {
            return root.resolve(testDescriptor.javaClass.canonicalName).createDirectories()
        }
    }
}
