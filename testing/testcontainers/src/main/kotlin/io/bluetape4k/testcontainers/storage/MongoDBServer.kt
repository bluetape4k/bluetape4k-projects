package io.bluetape4k.testcontainers.storage

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Docker를 이용하여 [MongoDB](https://www.mongodb.com/)를 구동해주는 container 입니다.
 *
 * 참고: [Docker official images](https://hub.docker.com/_/mongo?tab=description&page=1&ordering=last_updated)
 *
 * ```
 * // start MongoDB server by docker
 * val mongoServer = MongoDBServer().apply { start() }
 * ```
 *
 * ```
 * // start MongoDB server by docker
 * val mongoServer = MongoDBServer().apply { start() }
 * ```
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

            val imageName = DockerImageName.parse(IMAGE).withTag(tag)
            return MongoDBServer(imageName, useDefaultPort, reuse, databaseName)
        }

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
     * Url (예: mongodb://localhost:27017/test)
     */
    override val url: String get() = this.getReplicaSetUrl(databaseName)

    init {
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

    object Launcher {
        val mongoDB: MongoDBServer by lazy {
            MongoDBServer().apply {
                start()
                // JVM 종료 시, 자동으로 Close 되도록 합니다
                ShutdownQueue.register(this)
            }
        }

        fun getClient(connectionString: String = mongoDB.url): MongoClient {
            return MongoClients.create(connectionString)
        }
    }
}
