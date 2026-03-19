package io.bluetape4k.aws.kotlin.auth

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AuthSupportTest {

    @Test
    fun `credentialsOf는 access key와 secret key를 보존한다`() {
        val credentials = credentialsOf("ak", "sk")

        credentials.accessKeyId shouldBeEqualTo "ak"
        credentials.secretAccessKey shouldBeEqualTo "sk"
    }

    @Test
    fun `staticCredentialsProviderOf는 credentials 기반으로 생성된다`() {
        val credentials = credentialsOf("ak2", "sk2")
        val provider = staticCredentialsProviderOf(credentials)

        provider::class shouldBeEqualTo LocalCredentialsProvider::class
    }

    @Test
    fun `credentialsOf는 빈 access key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            credentialsOf("", "sk")
        }
    }

    @Test
    fun `staticCredentialsProviderOf는 빈 secret key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            staticCredentialsProviderOf("ak", "")
        }
    }
}
