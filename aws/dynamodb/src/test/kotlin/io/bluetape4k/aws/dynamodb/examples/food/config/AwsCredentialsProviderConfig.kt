package io.bluetape4k.aws.dynamodb.examples.food.config

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

@Configuration
class AwsCredentialsProviderConfig {

    @Bean
    @Primary
    fun defaultCredentials(
        @Value("\${aws.accessKey:accessKey}") accessKey: String,
        @Value("\${aws.securityKey:securityKey") securityKey: String,
    ): AwsCredentialsProvider {
        return staticCredentialsProviderOf(accessKey, securityKey)
    }
}
