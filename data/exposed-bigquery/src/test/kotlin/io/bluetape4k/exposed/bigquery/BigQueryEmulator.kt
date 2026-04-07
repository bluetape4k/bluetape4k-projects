package io.bluetape4k.exposed.bigquery

import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * BigQuery 에뮬레이터 컨테이너 (goccy/bigquery-emulator)
 *
 * 로컬에 설치된 에뮬레이터(localhost:9050) 또는 Testcontainers Docker 컨테이너를 자동 선택합니다.
 *
 * ```bash
 * brew install goccy/bigquery-emulator/bigquery-emulator
 * bigquery-emulator --project=test --dataset=testdb --port=9050
 * ```
 */
object BigQueryEmulator: KLogging() {

    const val PROJECT_ID = "test"
    const val DATASET = "testdb"
    const val IMAGE = "ghcr.io/goccy/bigquery-emulator:0.6.3"
    const val HTTP_PORT = 9050

    /** brew install goccy/bigquery-emulator/bigquery-emulator 로 설치된 로컬 에뮬레이터 확인 */
    private fun isLocalRunning(): Boolean = runCatching {
        java.net.Socket("localhost", HTTP_PORT).use { true }
    }.getOrDefault(false)

    val container: GenericContainer<*> by lazy {
        GenericContainer(IMAGE)
            .withExposedPorts(HTTP_PORT)
            .withCommand("--project=$PROJECT_ID", "--dataset=$DATASET")
            .waitingFor(
                Wait.forHttp("/discovery/v1/apis/bigquery/v2/rest")
                    .forPort(HTTP_PORT)
                    .forStatusCode(200)
            )
            .also {
                it.start()
                ShutdownQueue.register { it.stop() }
            }
    }

    private val useLocal: Boolean by lazy {
        isLocalRunning().also { local ->
            if (local) log.info("로컬 BigQuery 에뮬레이터 사용 (localhost:$HTTP_PORT)")
            else log.info("Testcontainers BigQuery 에뮬레이터 시작")
        }
    }

    val host: String by lazy { if (useLocal) "localhost" else container.host }
    val port: Int by lazy { if (useLocal) HTTP_PORT else container.getMappedPort(HTTP_PORT) }

}
