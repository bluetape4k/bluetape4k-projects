package io.bluetape4k.testcontainers.infra

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.GenericServer
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
): VaultContainer<VaultServer>(imageName), GenericServer {

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
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): VaultServer {
            val imageName = DockerImageName.parse(IMAGE).withTag(tag)
            return VaultServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "http://$host:$port"

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()

        val extraProps = mapOf("token" to envMap["VAULT_TOKEN"])
        writeToSystemProperties(NAME, extraProps)
    }

    fun createVaultClient(token: String): Vault {
        val config = VaultConfig()
            .address(url)
            .engineVersion(2)
            .token(token)
            .build()
        return Vault(config)
    }

    object Launcher {
        val vault: VaultServer by lazy {
            VaultServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }
    }
}
