package io.bluetape4k.testcontainers.storage

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.storage.MongoDBServer.Launcher.getClient
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import com.mongodb.kotlin.client.coroutine.MongoClient as CoroutineMongoClient

/**
 * MongoDB 테스트 서버 컨테이너를 생성하고 연결 URL을 제공합니다.
 *
 * ## 동작/계약
 * - 인스턴스 생성만으로는 컨테이너가 시작되지 않으며, `start()` 호출 이후에 접속할 수 있습니다.
 * - `url`은 `databaseName`을 포함한 replica-set URL(`mongodb://.../<db>`)을 반환합니다.
 * - `useDefaultPort=true`이면 `27017` 포트 고정 바인딩을 시도하고, 아니면 동적 포트를 사용합니다.
 *
 * ```kotlin
 * val server = MongoDBServer(databaseName = "sample")
 * server.start()
 * // server.url.contains("/sample") == true
 * ```
 *
 * 참고: [Docker official images](https://hub.docker.com/_/mongo?tab=description&page=1&ordering=last_updated)
 */
class MongoDBServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
    private val databaseName: String,
): MongoDBContainer(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "mongo"
        const val TAG = "8"
        const val NAME = "mongo"
        const val PORT = 27017
        const val DATABASE_NAME = "test"

        /**
         * 이미지 이름/태그로 [MongoDBServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - `databaseName`은 검증 없이 URL 생성 시 DB 경로로 사용됩니다.
         * - 컨테이너 시작은 수행하지 않고 새 인스턴스만 반환합니다.
         *
         * ```kotlin
         * val server = MongoDBServer(image = "mongo", tag = "8", databaseName = "app")
         * // server.url.contains("/app") == true
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 27017 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         * @param databaseName 연결 URL에 포함할 데이터베이스 이름입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            databaseName: String = DATABASE_NAME,
        ): MongoDBServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return MongoDBServer(imageName, useDefaultPort, reuse, databaseName)
        }

        /**
         * [DockerImageName]으로 [MongoDBServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `imageName`과 `databaseName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 직접 수행해야 합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("mongo").withTag("8")
         * val server = MongoDBServer(image, databaseName = "test")
         * // server.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
            databaseName: String = DATABASE_NAME,
        ): MongoDBServer {
            return MongoDBServer(imageName, useDefaultPort, reuse, databaseName)
        }
    }

    override val port: Int get() = getMappedPort(PORT)

    /**
     * 현재 컨테이너에 접속하는 MongoDB URL입니다.
     *
     * ## 동작/계약
     * - `databaseName`을 경로에 포함한 replica-set URL을 반환합니다.
     * - 단일 노드 테스트 환경 호환을 위해 `retryWrites=false` 옵션을 항상 포함합니다.
     * - 호출 시마다 문자열을 새로 계산해 반환하며 컨테이너 상태는 변경하지 않습니다.
     *
     * ```kotlin
     * val url = server.url
     * // url.startsWith("mongodb://") == true
     * ```
     */
    override val url: String
        get() {
            val replicaSetUrl = this.getReplicaSetUrl(databaseName)
            return if (replicaSetUrl.contains("retryWrites=")) {
                replicaSetUrl
            } else {
                val separator = if (replicaSetUrl.contains('?')) '&' else '?'
                "$replicaSetUrl${separator}retryWrites=false"
            }
        }

    init {
        withReplicaSet()
        addExposedPorts(PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    /**
     * 테스트 전역에서 재사용할 MongoDB 서버와 클라이언트 생성 헬퍼를 제공합니다.
     *
     * ## 동작/계약
     * - `mongoDB`는 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - `getClient`는 매 호출마다 새 [MongoClient]를 생성하며 호출자가 닫아야 합니다.
     *
     * ```kotlin
     * val server = MongoDBServer.Launcher.mongoDB
     * val client = MongoDBServer.Launcher.getClient()
     * // server.isRunning == true
     * ```
     */
    object Launcher {
        /** 지연 초기화되는 재사용용 MongoDB 서버입니다. */
        val mongoDB: MongoDBServer by lazy {
            MongoDBServer().apply {
                start()
                // JVM 종료 시, 자동으로 Close 되도록 합니다
                ShutdownQueue.register(this)
            }
        }

        /**
         * 지정한 연결 문자열로 [MongoClient]를 생성합니다.
         *
         * ## 동작/계약
         * - `connectionString`이 유효하지 않으면 Mongo 드라이버에서 예외가 발생할 수 있습니다.
         * - 반환된 클라이언트는 공유하지 않으며, 사용 후 호출자가 종료해야 합니다.
         *
         * ```kotlin
         * val client = MongoDBServer.Launcher.getClient()
         * // client != null
         * ```
         */
        fun getClient(connectionString: String = mongoDB.url): MongoClient {
            return MongoClients.create(connectionString)
        }

        /**
         * 지정한 연결 문자열로 Kotlin Coroutine 드라이버 기반 [CoroutineMongoClient]를 생성합니다.
         *
         * ## 동작/계약
         * - [getClient]와 달리 `suspend` 함수 및 `Flow`를 네이티브로 지원하는 코루틴 클라이언트를 반환합니다.
         * - 반환된 클라이언트는 공유하지 않으며, 사용 후 호출자가 닫아야 합니다.
         *
         * ```kotlin
         * val client = MongoDBServer.Launcher.getCoroutineClient()
         * val db = client.getDatabase("test")
         * val names = db.listCollectionNames().toList()
         * ```
         */
        fun getCoroutineClient(connectionString: String = mongoDB.url): CoroutineMongoClient {
            return CoroutineMongoClient.create(connectionString)
        }
    }
}
