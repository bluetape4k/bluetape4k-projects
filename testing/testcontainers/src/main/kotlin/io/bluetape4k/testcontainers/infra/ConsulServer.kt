package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * [Consul](https://www.consul.io/) server 를 testcontainers 를 이용하여 실행합니다.
 *
 * 참고: [Consul docker image](https://hub.docker.com/r/hashicorp/consul)
 *
 */
class ConsulServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<ConsulServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        /** Consul 서버의 Docker Hub 이미지 이름입니다. */
        const val IMAGE = "hashicorp/consul"

        /** 기본 Docker 이미지 태그입니다. */
        const val TAG = "1.20"

        /** 시스템 프로퍼티 등록 시 사용하는 서버 식별자입니다. */
        const val NAME = "consul"

        /** Consul DNS 서비스 포트입니다. */
        const val DNS_PORT = 8600

        /** Consul HTTP API 포트입니다. */
        const val HTTP_PORT = 8500

        /** Consul RPC 통신 포트입니다. */
        const val RPC_PORT = 8300

        /** 컨테이너에 노출할 포트 목록입니다. */
        val EXPORT_PORTS = intArrayOf(DNS_PORT, HTTP_PORT, RPC_PORT)

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): ConsulServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return ConsulServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(HTTP_PORT)
    override val url: String get() = "http://$host:$port"

    /** DNS 질의용 Consul 포트의 매핑 결과입니다. */
    val dnsPort: Int get() = getMappedPort(DNS_PORT)

    /** HTTP API용 Consul 포트의 매핑 결과입니다. */
    val httpPort: Int get() = getMappedPort(HTTP_PORT)

    /** RPC 통신용 Consul 포트의 매핑 결과입니다. */
    val rpcPort: Int get() = getMappedPort(RPC_PORT)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "dns-port", "http-port", "rpc-port")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "dns-port" to dnsPort.toString(),
        "http-port" to httpPort.toString(),
        "rpc-port" to rpcPort.toString(),
    )

    init {
        addExposedPorts(*EXPORT_PORTS)
        withReuse(reuse)

        if (useDefaultPort) {
            // 위에 addExposedPorts 를 등록했으면, 따로 지정하지 않으면 그 값들을 사용합니다.
            exposeCustomPorts(*EXPORT_PORTS)
        }
    }

    override fun start() {
        super.start()

        writeToSystemProperties()
    }

    /**
     * 테스트에서 재사용할 Consul 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val consul: ConsulServer by lazy {
            ConsulServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
