package io.bluetape4k.testcontainers.infra

import com.bettercloud.vault.Vault
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class VaultServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val VAULT_TOKEN = "my-root-token"
    }

    private fun createVaultServer(): VaultServer {
        return VaultServer()
            .withVaultToken(VAULT_TOKEN)
            .withInitCommand(
                "kv put secret/testing1 top_secret=top_password123 db_password=db_password123"
            )
            .withInitCommand(
                "kv put secret/testing2 secret_one=password1 secret_two=password2 secret_three=password3 secret_four=password4",
            )
            .apply {
                start()
            }
    }

    @Test
    fun `connect to vault server`() {
        createVaultServer().use { server ->
            server.isRunning.shouldBeTrue()

            val result = server.execInContainer("vault", "kv", "get", "-format=json", "secret/testing1")
            val output = result.stdout

            log.debug { "output: $output" }
            output shouldContain "top_password123"
            output shouldContain "db_password123"

            // Use [VaultClient]
            val client: Vault = server.createVaultClient(VAULT_TOKEN)
            val kv = client.logical().read("secret/testing1").data
            log.debug { "kv=$kv" }
            kv["top_secret"] shouldBeEqualTo "top_password123"
            kv["db_password"] shouldBeEqualTo "db_password123"
        }
    }
}
