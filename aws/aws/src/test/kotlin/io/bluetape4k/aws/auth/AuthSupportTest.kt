package io.bluetape4k.aws.auth

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "  "])
    fun `awsBasicCredentialsOf는 빈 accessKeyId에 대해 예외를 던진다`(blankKey: String) {
        assertThrows<IllegalArgumentException> {
            awsBasicCredentialsOf(blankKey, "valid-secret")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "  "])
    fun `awsBasicCredentialsOf는 빈 securityAccessKey에 대해 예외를 던진다`(blankSecret: String) {
        assertThrows<IllegalArgumentException> {
            awsBasicCredentialsOf("valid-key", blankSecret)
        }
    }

    @Test
    fun `LocalAwsCredentialsProvider는 로컬 기본 키를 반환한다`() {
        val resolved = LocalAwsCredentialsProvider.resolveCredentials()

        resolved.accessKeyId() shouldBeEqualTo AWS_LOCAL_ACCESS_KEY
        resolved.secretAccessKey() shouldBeEqualTo AWS_LOCAL_SECURITY_KEY
    }

    @Test
    fun `staticCredentialsProviderOf로 AwsBasicCredentials 인스턴스를 감싼다`() {
        val credentials = awsBasicCredentialsOf("myKey", "mySecret")
        val provider = staticCredentialsProviderOf(credentials)

        provider.resolveCredentials() shouldBeInstanceOf software.amazon.awssdk.auth.credentials.AwsBasicCredentials::class
        provider.resolveCredentials().accessKeyId() shouldBeEqualTo "myKey"
    }
}

