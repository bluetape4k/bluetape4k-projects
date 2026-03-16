package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.KmsClientBuilder
import java.net.URI

/**
 * KMS 동기/비동기 클라이언트 생성을 모아둔 팩토리입니다.
 *
 * ## 동작/계약
 * - 실제 클라이언트 생성은 [kmsClient], [kmsClientOf], [kmsAsyncClient], [kmsAsyncClientOf]에 위임합니다.
 * - 팩토리 메서드가 반환한 클라이언트는 위임 대상 함수의 동작에 따라 [io.bluetape4k.utils.ShutdownQueue] 등록이 수행됩니다.
 *
 * ```kotlin
 * val syncClient = KmsClientFactory.Sync.create { region(Region.AP_NORTHEAST_2) }
 * val asyncClient = KmsClientFactory.Async.create { region(Region.AP_NORTHEAST_2) }
 * // syncClient.serviceName() == "kms"
 * ```
 */
object KmsClientFactory: KLogging() {

    /**
     * 동기식 [KmsClient] 생성 진입점을 제공합니다.
     *
     * ## 동작/계약
     * - 두 `create` 오버로드 모두 [kmsClient] 계열 함수에 위임합니다.
     *
     * ```kotlin
     * val client = KmsClientFactory.Sync.create {
     *     region(Region.AP_NORTHEAST_2)
     * }
     * // client.serviceName() == "kms"
     * ```
     */
    object Sync {

        /**
         * 빌더 람다를 받아 동기식 [KmsClient]를 생성합니다.
         *
         * ## 동작/계약
         * - 인자로 받은 [builder]를 [kmsClient]에 그대로 전달합니다.
         *
         * ```kotlin
         * val client = KmsClientFactory.Sync.create {
         *     region(Region.AP_NORTHEAST_2)
         * }
         * // client.serviceName() == "kms"
         * ```
         */
        inline fun create(
            builder: KmsClientBuilder.() -> Unit,
        ): KmsClient = kmsClient(builder)

        /**
         * 주요 연결 정보를 받아 동기식 [KmsClient]를 생성합니다.
         *
         * ## 동작/계약
         * - 인자를 [kmsClientOf]에 순서대로 전달합니다.
         * - [credentialsProvider]를 생략하면 기본 인증 체인 해석은 AWS SDK 빌더 기본 동작에 따릅니다.
         *
         * ```kotlin
         * val client = KmsClientFactory.Sync.create(
         *     endpointOverride = URI.create("http://localhost:4566"),
         *     region = Region.US_EAST_1
         * )
         * // client.serviceName() == "kms"
         * ```
         */
        inline fun create(
            endpointOverride: URI,
            region: Region,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            builder: KmsClientBuilder.() -> Unit = {},
        ): KmsClient =
            kmsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기식 [KmsAsyncClient] 생성 진입점을 제공합니다.
     *
     * ## 동작/계약
     * - 두 `create` 오버로드 모두 [kmsAsyncClient] 계열 함수에 위임합니다.
     *
     * ```kotlin
     * val client = KmsClientFactory.Async.create {
     *     region(Region.AP_NORTHEAST_2)
     * }
     * // client.serviceName() == "kms"
     * ```
     */
    object Async {
        /**
         * 빌더 람다를 받아 비동기식 [KmsAsyncClient]를 생성합니다.
         *
         * ## 동작/계약
         * - 인자로 받은 [builder]를 [kmsAsyncClient]에 그대로 전달합니다.
         *
         * ```kotlin
         * val client = KmsClientFactory.Async.create {
         *     region(Region.AP_NORTHEAST_2)
         * }
         * // client.serviceName() == "kms"
         * ```
         */
        inline fun create(
            builder: KmsAsyncClientBuilder.() -> Unit,
        ): KmsAsyncClient = kmsAsyncClient(builder)

        /**
         * 주요 연결 정보를 받아 비동기식 [KmsAsyncClient]를 생성합니다.
         *
         * ## 동작/계약
         * - 인자를 [kmsAsyncClientOf]에 순서대로 전달합니다.
         * - [credentialsProvider]를 생략하면 기본 인증 체인 해석은 AWS SDK 빌더 기본 동작에 따릅니다.
         *
         * ```kotlin
         * val client = KmsClientFactory.Async.create(
         *     endpointOverride = URI.create("http://localhost:4566"),
         *     region = Region.US_EAST_1
         * )
         * // client.serviceName() == "kms"
         * ```
         */
        inline fun create(
            endpointOverride: URI,
            region: Region,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: KmsAsyncClientBuilder.() -> Unit = {},
        ): KmsAsyncClient =
            kmsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
