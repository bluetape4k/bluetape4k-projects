package io.bluetape4k.testcontainers.storage

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * 2.0.0에서 공개했던 [Ignite2Server] 호출 경계를 2.1.x에서도 컴파일할 수 있는지 검증합니다.
 */
@Suppress("DEPRECATION")
class Ignite2ServerApiCompatibilityTest {

    @Test
    fun `2_0_0 public constructors remain source compatible`() {
        Ignite2Server().use { server ->
            server.configuredImageName().toString() shouldBeEqualTo
                "${Ignite2Server.IMAGE}:${Ignite2Server.DEFAULT_TAG}"
            server.propertyNamespace shouldBeEqualTo Ignite2Server.NAME
            server.propertyKeys() shouldBeEqualTo setOf("host", "port", "url")
        }

        Ignite2Server(DockerImageName.parse(Ignite2Server.IMAGE)).use { server ->
            server.configuredImageName().toString() shouldBeEqualTo
                "${Ignite2Server.IMAGE}:${Ignite2Server.DEFAULT_TAG}"
        }

        Ignite2Server(
            DockerImageName.parse("custom/ignite:2.18.0-custom"),
            useDefaultPort = true,
            reuse = true,
        ).use { server ->
            server.configuredImageName().toString() shouldBeEqualTo "custom/ignite:2.18.0-custom"
        }

        Ignite2Server(
            image = "custom/ignite",
            tag = "2.18.0-custom",
            useDefaultPort = true,
            reuse = true,
        ).use { server ->
            server.configuredImageName().toString() shouldBeEqualTo "custom/ignite:2.18.0-custom"
        }
    }

    @Test
    fun `tagless custom images remain rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(image = "custom/ignite")
        }
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(DockerImageName.parse("custom/ignite"))
        }
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(image = "custom/ignite", tag = "latest")
        }
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(image = Ignite2Server.IMAGE, tag = "latest")
        }
        assertFailsWith<IllegalArgumentException> {
            Ignite2Server(DockerImageName.parse("custom/ignite:latest"))
        }
    }

    private fun Ignite2Server.configuredImageName(): DockerImageName {
        val imageField = GenericContainer::class.java.getDeclaredField("image").apply {
            isAccessible = true
        }
        val remoteImage = imageField.get(this)
        val imageNameFutureField = remoteImage.javaClass.getDeclaredField("imageNameFuture").apply {
            isAccessible = true
        }
        val imageNameFuture = imageNameFutureField.get(remoteImage) as java.util.concurrent.Future<*>
        return imageNameFuture.get() as DockerImageName
    }
}
