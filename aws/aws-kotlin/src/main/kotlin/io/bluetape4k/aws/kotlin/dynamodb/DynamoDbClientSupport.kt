package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.useSafe


/**
 * [DynamoDbClient]를 생성합니다.
 *
 * @param endpointUrl DynamoDB 엔드포인트 URL
 * @param region AWS 리전
 * @param credentialsProvider AWS 자격 증명 제공자
 * @param httpClient [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder [DynamoDbClient.Config.Builder] 를 통해 [DynamoDbClient.Config] 를 설정합니다.
 *
 * @return [DynamoDbClient] 인스턴스
 */
inline fun dynamoDbClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine? = HttpClientEngineProvider.defaultHttpEngine,
    crossinline builder: DynamoDbClient.Config.Builder.() -> Unit = {},
): DynamoDbClient {
    region.requireNotBlank("region")

    return DynamoDbClient {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        httpClient?.let { this.httpClient = it }

        builder()
    }
}

/**
 * [DynamoDbClient]를 생성하고 [block]을 실행한 후 자동으로 닫습니다.
 *
 * SDK가 내부 HTTP 엔진을 직접 관리하므로 close() 시 엔진도 함께 종료됩니다.
 *
 * ```kotlin
 * withDynamoDbClient(endpointUrl, region, credentialsProvider) { client ->
 *     client.putItem(tableName, item)
 * }
 * ```
 *
 * @param region AWS 리전 (필수)
 * @param block suspend 블록. AWS SDK의 모든 operations는 suspend 함수이므로 이 블록도 suspend로 선언합니다.
 * @throws IllegalArgumentException [region]이 blank인 경우
 */
suspend fun <R> withDynamoDbClient(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    block: suspend (DynamoDbClient) -> R,
): R {
    region.requireNotBlank("region")
    return dynamoDbClientOf(endpointUrl, region, credentialsProvider).useSafe { client ->
        block(client)
    }
}
