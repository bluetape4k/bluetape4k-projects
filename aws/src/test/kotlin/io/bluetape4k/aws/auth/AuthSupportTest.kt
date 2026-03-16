package io.bluetape4k.aws.auth

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class AuthSupportTest {

    @Test
    fun `awsBasicCredentialsOf는 accessKey와 secretKey를 보존한다`() {
        val credentials = awsBasicCredentialsOf("ak", "sk")

        credentials.accessKeyId() shouldBeEqualTo "ak"
        credentials.secretAccessKey() shouldBeEqualTo "sk"
    }

    @Test
    fun `staticCredentialsProviderOf는 AwsBasicCredentials를 감싼다`() {
        val provider = staticCredentialsProviderOf("ak2", "sk2")
        val resolved = provider.resolveCredentials()

        resolved.accessKeyId() shouldBeEqualTo "ak2"
        resolved.secretAccessKey() shouldBeEqualTo "sk2"
    }
}

