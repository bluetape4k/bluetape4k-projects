package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * [Apache Ignite 3.x](https://ignite.apache.org/) 서버 컨테이너입니다.
 *
 * Ignite 3.x는 씬 클라이언트 전용으로, 외부 서버에 연결하여 사용합니다.
 * 이 컨테이너는 테스트 환경에서 Ignite 3.x 서버를 Docker로 실행합니다.
 *
 * **중요**: Ignite 3.x 서버는 시작 후 반드시 클러스터 초기화([initCluster])가 필요합니다.
 * [start] 메서드는 자동으로 클러스터를 초기화합니다.
 *
 * Docker Hub: [apacheignite/ignite](https://hub.docker.com/r/apacheignite/ignite/tags)
 *
 * **사용 예시:**
 * ```kotlin
 * val ignite3 = Ignite3Server().apply { start() }
 * val client = IgniteClient.builder()
 *     .addresses("${ignite3.host}:${ignite3.port}")
 *     .build()
 * ```
 *
 * 또는 싱글턴 [Launcher]를 통해 사용:
 * ```kotlin
 * val client = IgniteClient.builder()
 *     .addresses(Ignite3Server.Launcher.ignite3.url)
 *     .build()
 * ```
 *
 * @param imageName Docker 이미지 이름 ([DockerImageName])
 * @param useDefaultPort 기본 포트(10800)를 그대로 사용할지 여부. `false`이면 임의 포트가 할당됩니다.
 * @param reuse 컨테이너 재사용 여부
 */
class Ignite3Server private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<Ignite3Server>(imageName), GenericServer {

    companion object: KLogging() {
        /** Apache Ignite 3.x Docker Hub 이미지 이름 */
        const val IMAGE = "apacheignite/ignite"

        /** 기본 태그 (안정 버전) */
        const val TAG = "3.1.0"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 이름 */
        const val NAME = "ignite3"

        /** Ignite 3.x 씬 클라이언트 기본 포트 */
        const val CLIENT_PORT = 10800

        /** Ignite 3.x REST API 기본 포트 */
        const val REST_PORT = 10300

        /** 테스트 환경에서 사용하는 기본 JVM 최대 메모리 (저메모리 Docker 환경 대응) */
        const val DEFAULT_JVM_MAX_MEM = "1g"

        /** 테스트 환경에서 사용하는 기본 JVM 최소 메모리 (저메모리 Docker 환경 대응) */
        const val DEFAULT_JVM_MIN_MEM = "256m"

        /** 클러스터 초기화 시 사용하는 기본 클러스터 이름 */
        const val DEFAULT_CLUSTER_NAME = "default"

        /** 클러스터 초기화 시 사용하는 기본 노드 이름 */
        const val DEFAULT_NODE_NAME = "defaultNode"

        /**
         * [DockerImageName]으로 [Ignite3Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite3Server = Ignite3Server(imageName, useDefaultPort, reuse)

        /**
         * 이미지 이름과 태그로 [Ignite3Server]를 생성합니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): Ignite3Server {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return Ignite3Server(imageName, useDefaultPort, reuse)
        }
    }

    /** 씬 클라이언트 연결 포트 (매핑된 포트) */
    override val port: Int get() = getMappedPort(CLIENT_PORT)

    /** REST API 포트 (매핑된 포트) */
    val restPort: Int get() = getMappedPort(REST_PORT)

    /** 씬 클라이언트 연결 주소 (`host:port` 형식) */
    override val url: String get() = "$host:$port"

    init {
        addExposedPorts(CLIENT_PORT, REST_PORT)
        withReuse(reuse)
        withEnv("JVM_MAX_MEM", DEFAULT_JVM_MAX_MEM)
        withEnv("JVM_MIN_MEM", DEFAULT_JVM_MIN_MEM)

        // REST 서버가 준비될 때까지 대기 (REST가 준비되어야 클러스터 초기화 가능)
        waitingFor(
            Wait.forLogMessage(".*REST server started successfully.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2))
        )

        if (useDefaultPort) {
            exposeCustomPorts(CLIENT_PORT, REST_PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME, mapOf("rest.port" to restPort))
        initCluster()
    }

    /**
     * REST API를 통해 Ignite 3.x 클러스터를 초기화합니다.
     *
     * Ignite 3.x는 첫 시작 시 반드시 클러스터 초기화가 필요합니다.
     * 이미 초기화된 경우(409 응답)에는 무시합니다.
     */
    private fun initCluster() {
        val client = HttpClient.newHttpClient()
        val restBase = "http://$host:$restPort"

        // 이미 초기화되었는지 확인
        val stateRequest = HttpRequest.newBuilder()
            .uri(URI.create("$restBase/management/v1/cluster/state"))
            .GET()
            .build()

        try {
            val stateResponse = client.send(stateRequest, HttpResponse.BodyHandlers.ofString())
            if (stateResponse.statusCode() == 200) {
                log.info { "Ignite 3.x 클러스터가 이미 초기화되어 있습니다." }
                return
            }
        } catch (e: Exception) {
            log.debug { "클러스터 상태 확인 중 예외 (초기화 진행): ${e.message}" }
        }

        // 클러스터 초기화
        log.info { "Ignite 3.x 클러스터 초기화 시작 (clusterName=$DEFAULT_CLUSTER_NAME)" }
        val initBody = """{"clusterName":"$DEFAULT_CLUSTER_NAME","metaStorageNodes":["$DEFAULT_NODE_NAME"]}"""
        val initRequest = HttpRequest.newBuilder()
            .uri(URI.create("$restBase/management/v1/cluster/init"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(initBody))
            .build()

        try {
            val initResponse = client.send(initRequest, HttpResponse.BodyHandlers.ofString())
            log.info { "클러스터 초기화 응답: status=${initResponse.statusCode()}, body=${initResponse.body()}" }
        } catch (e: Exception) {
            log.warn(e) { "클러스터 초기화 요청 실패" }
        }

        // 클러스터가 준비될 때까지 대기
        waitForClusterReady(client, restBase)
    }

    /**
     * Ignite 3.x 클러스터가 완전히 준비될 때까지 대기합니다.
     *
     * `/management/v1/cluster/state` 엔드포인트가 200 OK를 반환할 때까지 폴링합니다.
     */
    private fun waitForClusterReady(client: HttpClient, restBase: String) {
        val maxAttempts = 30
        val delayMs = 2000L

        log.debug { "Ignite 3.x 클러스터 준비 대기 중 (최대 ${maxAttempts}회)" }

        repeat(maxAttempts) { attempt ->
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$restBase/management/v1/cluster/state"))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    log.info { "Ignite 3.x 클러스터 준비 완료 (${attempt + 1}회차)" }
                    return
                }
                log.debug { "클러스터 준비 대기 (${attempt + 1}/$maxAttempts): status=${response.statusCode()}" }
            } catch (e: Exception) {
                log.debug { "클러스터 상태 확인 실패 (${attempt + 1}/$maxAttempts): ${e.message}" }
            }
            Thread.sleep(delayMs)
        }

        throw IllegalStateException("Ignite 3.x 클러스터가 ${maxAttempts * delayMs / 1000}초 내에 준비되지 않았습니다.")
    }

    /**
     * 테스트 환경에서 공유하는 싱글턴 [Ignite3Server] 인스턴스를 제공합니다.
     */
    object Launcher {
        val ignite3: Ignite3Server by lazy {
            Ignite3Server().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
