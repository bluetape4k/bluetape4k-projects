package io.bluetape4k.aws.core

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class AwsRequestOverrideConfigurationSupportTest {

    @Test
    fun `awsRequestOverrideConfigurationOf는 credentials provider를 설정한다`() {
        val provider = staticCredentialsProviderOf("ak", "sk")

        val configuration = awsRequestOverrideConfigurationOf(provider)

        configuration.credentialsProvider().get().resolveCredentials().accessKeyId() shouldBeEqualTo "ak"
    }
}

