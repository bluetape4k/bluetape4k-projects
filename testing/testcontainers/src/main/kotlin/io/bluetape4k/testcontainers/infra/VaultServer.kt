package io.bluetape4k.testcontainers.infra

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.utility.DockerImageName
import org.testcontainers.vault.VaultContainer


/**
 * [Vault](https://www.vaultproject.io/)를 testcontainers 를 이용하여 실행합니다.
 *
 * 참고: [Vault docker image](https://hub.docker.com/_/vault)
 */
class VaultServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean = false,
    reuse: Boolean = true,
): VaultContainer<VaultServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "hashicorp/vault"
        const val TAG = "1.13.1"
        const val NAME = "vault"
        const val PORT = 8200

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): VaultServer {
            return VaultServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): VaultServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            
            val imageName = DockerImageName.parse(image).withTag(tag)
            return VaultServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "token")

    override fun properties(): Map<String, String> = buildMap {
        put("host", host)
        put("port", port.toString())
        put("url", url)
        envMap["VAULT_TOKEN"]?.let { put("token", it) }
    }

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()

        writeToSystemProperties()
    }

    /**
     * 현재 Vault 서버 URL과 토큰으로 [Vault] 클라이언트를 생성합니다.
     *
     * ## 동작/계약
     * - 전달한 `token`을 그대로 사용해 [VaultConfig]를 빌드합니다.
     * - 서버 상태를 변경하지 않고 새 클라이언트 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val client = vault.createVaultClient("root-token")
     * // client.logical() 사용 가능
     * ```
     */
    fun createVaultClient(token: String): Vault {
        val config = VaultConfig()
            .address(url)
            .engineVersion(2)
            .token(token)
            .build()
        return Vault(config)
    }

    /**
     * 테스트에서 재사용할 Vault 서버 싱글턴을 제공합니다.
     */
    object Launcher {
        val vault: VaultServer by lazy {
            VaultServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
