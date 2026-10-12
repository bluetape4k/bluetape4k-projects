package io.bluetape4k.testcontainers.storage

import com.sun.net.httpserver.HttpServer
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.lettuce.core.resource.DefaultClientResources
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

/** Docker 없이 Redis와 Kubernetes 클라이언트의 런타임 호환성을 검증한다. */
class ClientRuntimeCompatibilityTest {

    @Test
    fun `Lettuce native transport의 event loop 클래스를 로드한다`() {
        // macOS에서도 Linux CI가 사용하는 클래스 연결을 검증하며 native socket은 열지 않는다.
        val resources = DefaultClientResources.create()
        try {
            Class.forName("io.netty.channel.MultiThreadIoEventLoopGroup").name shouldBeEqualTo
                "io.netty.channel.MultiThreadIoEventLoopGroup"
        } finally {
            resources.shutdown().get()
        }
    }

    @Test
    fun `Kubernetes HTTP 클라이언트를 같은 런타임에서 생성한다`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        try {
            server.createContext("/api/v1/namespaces/default/pods") { exchange ->
                val body = """{"apiVersion":"v1","kind":"PodList","metadata":{},"items":[]}"""
                    .toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
                exchange.close()
            }
            server.start()
            val config = ConfigBuilder()
                .withMasterUrl("http://127.0.0.1:${server.address.port}")
                .withAutoConfigure(false)
                .build()
            KubernetesClientBuilder().withConfig(config).build().use { client ->
                client.httpClient.shouldNotBeNull()
                client.pods().inNamespace("default").list().items.shouldBeEmpty()
            }
        } finally {
            server.stop(0)
        }
    }
}
