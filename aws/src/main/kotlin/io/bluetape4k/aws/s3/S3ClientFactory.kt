package io.bluetape4k.aws.s3

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.Runtimex
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.SizeConstant.MB
import java.net.URI
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * [S3Client], [S3AsyncClient], [S3TransferManager] 인스턴스를 생성하는 함수를 제공합니다.
 */
object S3ClientFactory {

    /**
     * S3를 동기방식으로 사용하는 [S3Client] 를 생성하는 메소드를 제공합니다.
     */
    object Sync {

        /**
         * [S3Client] 를 생성합니다.
         *
         * @param builder [S3ClientBuilder]를 이용하여 [S3Client]를 설정합니다.
         * @return [S3Client] 인스턴스
         */
        inline fun create(
            @BuilderInference builder: S3ClientBuilder.() -> Unit,
        ): S3Client {
            return S3Client.builder().apply(builder).build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }

        /**
         * [S3Client] 를 생성합니다.
         *
         * @param endpointOverride      S3 endpoint
         * @param region                S3 region
         * @param credentialsProvider   AWS [AwsCredentialsProvider] 인스턴스
         * @param builder           [S3ClientBuilder]를 이용하여 [S3Client]를 설정합니다.
         * @return [S3Client] 인스턴스
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            accelerate: Boolean = false,
            @BuilderInference builder: S3ClientBuilder.() -> Unit = {},
        ): S3Client {
            return create {
                endpointOverride?.let { endpointOverride(it) }
                region?.let { region(it) }
                credentialsProvider?.let { credentialsProvider(it) }
                httpClient(httpClient)

                // Transfer Acceleration requires bucket-level enablement; leave disabled by default.
                accelerate(accelerate)

                builder()
            }
        }
    }

    /**
     * S3를 비동기방식으로 사용하는 [S3AsyncClient] 를 생성하는 메소드를 제공합니다.
     */
    object Async {

        /**
         * [S3AsyncClient] 를 생성합니다.
         *
         * @param builder [S3AsyncClientBuilder]를 이용하여 [S3AsyncClient]를 설정합니다.
         * @return [S3AsyncClient] 인스턴스
         */
        inline fun create(
            @BuilderInference builder: S3AsyncClientBuilder.() -> Unit,
        ): S3AsyncClient {
            return S3AsyncClient.builder()
                .apply(builder)
                .build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }

        /**
         * [S3AsyncClient] 를 생성합니다.
         *
         * @param endpointOverride      S3 endpoint
         * @param region                S3 region
         * @param credentialsProvider   AWS [AwsCredentialsProvider] 인스턴스
         * @param builder           [S3AsyncClientBuilder]를 이용하여 [S3Client]를 설정합니다.
         * @return [S3AsyncClient] 인스턴스
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            accelerate: Boolean = false,
            @BuilderInference builder: S3AsyncClientBuilder.() -> Unit = {},
        ): S3AsyncClient {
            return create {
                endpointOverride?.let { endpointOverride(it) }
                region?.let { region(it) }
                credentialsProvider?.let { credentialsProvider(it) }
                httpClient(httpClient)

                // Transfer Acceleration requires bucket-level enablement; leave disabled by default.
                accelerate(accelerate)

                builder()
            }
        }
    }


    /**
     * S3를 비동기방식으로 사용하는 [S3AsyncClient] 를 생성하는 메소드를 제공합니다.
     *
     * 참고: [AWSCRT 기반 HTTP 클라이언트 설정](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration-crt.html)
     */
    object CrtAsync {

        /**
         * [S3AsyncClient] 를 생성합니다.
         *
         * @param builder [S3AsyncClientBuilder]를 이용하여 [S3AsyncClient]를 설정합니다.
         * @return [S3AsyncClient] 인스턴스
         */
        inline fun create(
            @BuilderInference builder: S3CrtAsyncClientBuilder.() -> Unit,
        ): S3AsyncClient {
            return S3AsyncClient.crtBuilder().apply(builder).build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }

        /**
         * [S3AsyncClient] 를 생성합니다.
         *
         * @param endpointOverride      S3 endpoint
         * @param region                S3 region
         * @param credentialsProvider   AWS [AwsCredentialsProvider] 인스턴스
         * @param builder           [S3AsyncClientBuilder]를 이용하여 [S3Client]를 설정합니다.
         * @return [S3AsyncClient] 인스턴스
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            miminumPartSizeInBytes: Long = 1 * MB,
            @BuilderInference builder: S3CrtAsyncClientBuilder.() -> Unit = {},
        ): S3AsyncClient {
            return create {
                endpointOverride?.let { endpointOverride(it) }
                region?.let { region(it) }
                credentialsProvider?.let { credentialsProvider(it) }
                maxConcurrency(Runtimex.availableProcessors * 2)
                minimumPartSizeInBytes(miminumPartSizeInBytes)
                // Transfer Acceleration requires bucket-level enablement; leave disabled by default.
                // accelerate(false)
                builder()
            }
        }
    }

    /**
     * [S3TransferManager]를 생성하는 Factory
     */
    object TransferManager {

        /**
         * [S3TransferManager]를 생성합니다.
         *
         * @param  builder [S3TransferManager] Builder를 이용하여 설정하는 코드 블럭
         * @return [S3TransferManager] 인스턴스
         */
        inline fun create(
            @BuilderInference builder: S3TransferManager.Builder.() -> Unit,
        ): S3TransferManager {
            return S3TransferManager.builder().apply(builder).build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }

        /**
         * [S3TransferManager]를 생성합니다.
         *
         * 참고: [Amazon S3 Transfer Manager](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/transfer-manager.html)
         *
         * @param endpointOverride      S3 endpoint
         * @param region                S3 region
         * @param credentialsProvider   AWS [AwsCredentialsProvider] 인스턴스
         * @param executor               [Executor] 인스턴스
         * @param builder  [S3TransferManager] Builder를 이용하여 설정하는 코드 블럭
         * @return [S3TransferManager] 인스턴스
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            executor: Executor = Executors.newVirtualThreadPerTaskExecutor(),
            uploadDirectoryMaxDepth: Int? = null,
            @BuilderInference builder: S3TransferManager.Builder.() -> Unit = {},
        ): S3TransferManager {
            return create {
                // AWS CRT-based S3AsyncClient 를 사용하는 것을 추천한다
                val asyncClient = CrtAsync.create(endpointOverride, region, credentialsProvider)

                s3Client(asyncClient)
                executor(executor)
                uploadDirectoryMaxDepth?.let { this.uploadDirectoryMaxDepth(it) }
                builder()
            }
        }

        /**
         * [S3TransferManager]를 생성합니다.
         *
         * @param  asyncClient            [S3AsyncClient] 인스턴스
         * @param  executor               [Executor] 인스턴스
         * @param  builder  [S3TransferManager] Builder를 이용하여 설정하는 코드 블럭
         * @return [S3TransferManager] 인스턴스
         */
        inline fun create(
            asyncClient: S3AsyncClient,
            executor: Executor = Executors.newVirtualThreadPerTaskExecutor(),
            uploadDirectoryMaxDepth: Int? = null,
            @BuilderInference builder: S3TransferManager.Builder.() -> Unit = {},
        ): S3TransferManager {
            return create {
                s3Client(asyncClient)
                executor(executor)
                uploadDirectoryMaxDepth?.let { uploadDirectoryMaxDepth(it) }

                builder()
            }
        }
    }
}
