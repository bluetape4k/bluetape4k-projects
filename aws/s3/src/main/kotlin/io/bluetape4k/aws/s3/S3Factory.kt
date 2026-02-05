package io.bluetape4k.aws.s3

import io.bluetape4k.aws.auth.LocalAwsCredentialsProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
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

/**
 * [S3Client], [S3AsyncClient], [S3TransferManager] 인스턴스를 생성하는 함수를 제공합니다.
 */
object S3Factory {

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
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            @BuilderInference builder: S3ClientBuilder.() -> Unit = {},
        ): S3Client {
            return create {
                endpointOverride(endpointOverride)
                region(region)
                credentialsProvider(credentialsProvider)
                accelerate(true) // Enables this client to use S3 Transfer Acceleration endpoints.
                httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

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
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            @BuilderInference builder: S3AsyncClientBuilder.() -> Unit = {},
        ): S3AsyncClient {
            return create {
                endpointOverride(endpointOverride)
                region(region)
                credentialsProvider(credentialsProvider)
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
            return S3AsyncClient.crtBuilder()
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
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            @BuilderInference builder: S3CrtAsyncClientBuilder.() -> Unit = {},
        ): S3AsyncClient {
            return create {
                endpointOverride(endpointOverride)
                region(region)
                credentialsProvider(credentialsProvider)
                maxConcurrency(Runtime.getRuntime().availableProcessors())
                minimumPartSizeInBytes(1 * MB)
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
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            executor: Executor = Dispatchers.IO.asExecutor(),
            @BuilderInference builder: S3TransferManager.Builder.() -> Unit = {},
        ): S3TransferManager {
            return create {
                // AWS CRT-based S3AsyncClient 를 사용하는 것을 추천한다
                val asyncClient = CrtAsync.create(endpointOverride, region, credentialsProvider) {
                    maxConcurrency(Runtime.getRuntime().availableProcessors())
                    minimumPartSizeInBytes(1 * MB)
                    // .initialReadBufferSizeInBytes(8 * KB)
                }

                this.s3Client(asyncClient)
                this.executor(executor)
                this.uploadDirectoryMaxDepth(3)
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
            executor: Executor = Dispatchers.IO.asExecutor(),
            @BuilderInference builder: S3TransferManager.Builder.() -> Unit = {},
        ): S3TransferManager {
            return create {
                this.s3Client(asyncClient)
                this.executor(executor)
                this.uploadDirectoryMaxDepth(3)
                builder()
            }
        }
    }
}
