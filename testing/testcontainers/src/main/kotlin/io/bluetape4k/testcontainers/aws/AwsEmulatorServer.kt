package io.bluetape4k.testcontainers.aws

import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import java.net.URI

/**
 * AWS 에뮬레이터 서버 공통 인터페이스.
 *
 * LocalStack, Floci, MiniStack 등 다양한 AWS 에뮬레이터를 동일한 방식으로 사용할 수 있도록
 * 공통 계약을 정의합니다.
 *
 * > ⚠️ **주의 (R7)**: 이 인터페이스는 AWS SDK 타입을 포함하지 않습니다.
 * > AWS SDK 의존성은 선택적이므로, SDK 타입이 필요한 경우
 * > `AwsEmulatorServerExtensions.kt`의 확장 함수를 사용하세요.
 *
 * > ⚠️ **주의 (R8)**: [withServices]는 구현체마다 동작이 다릅니다.
 * > - LocalStack: 활성화할 서비스를 지정합니다.
 * > - Floci: 모든 서비스가 항상 활성화되므로 이 메서드는 no-op입니다.
 * > - MiniStack: 모든 서비스가 항상 활성화되므로 이 메서드는 no-op입니다.
 *
 * ```kotlin
 * // LocalStack (서비스 선택 가능)
 * val server: AwsEmulatorServer = LocalStackServer()
 *     .withServices("s3", "sqs")
 * server.start()
 *
 * // MiniStack (모든 서비스 항상 활성화)
 * val server: AwsEmulatorServer = MiniStackServer()
 * server.start()
 *
 * // 에뮬레이터 엔드포인트와 자격 증명 정보를 SDK 타입에 의존하지 않고 사용
 * val endpoint: URI = server.awsEndpoint
 * val accessKey: String = server.awsAccessKey
 * val secretKey: String = server.awsSecretKey
 * val region: String = server.regionName
 * ```
 */
interface AwsEmulatorServer: GenericServer, PropertyExportingServer {

    /**
     * AWS 에뮬레이터의 엔드포인트 URI.
     *
     * 일반적으로 `http://localhost:PORT` 형식이며, AWS SDK 클라이언트의
     * `endpointOverride` 파라미터로 사용합니다.
     *
     * AWS SDK 타입(`software.amazon.awssdk.regions.*` 등)에 의존하지 않도록
     * 표준 [java.net.URI] 타입으로 노출합니다.
     *
     * > 참고: 프로퍼티 이름에 `aws` 접두어를 붙여 `LocalStackContainer.getEndpoint()`와의
     * > JVM 시그니처 충돌을 방지합니다.
     */
    val awsEndpoint: URI

    /**
     * 에뮬레이터에서 사용할 AWS access key.
     *
     * 대부분의 로컬 에뮬레이터(LocalStack, Floci 등)는 임의의 값을 허용하며,
     * 기본값으로 `"test"`를 사용합니다.
     */
    val awsAccessKey: String

    /**
     * 에뮬레이터에서 사용할 AWS secret key.
     *
     * 대부분의 로컬 에뮬레이터(LocalStack, Floci 등)는 임의의 값을 허용하며,
     * 기본값으로 `"test"`를 사용합니다.
     */
    val awsSecretKey: String

    /**
     * 에뮬레이터에 적용할 AWS region 이름.
     *
     * 기본값은 `"us-east-1"` 입니다. AWS SDK의 `Region` 타입에 직접 의존하지 않도록
     * 문자열로 노출합니다.
     */
    val regionName: String

    /**
     * 활성화할 AWS 서비스 목록을 지정합니다.
     *
     * 구현체마다 동작이 다릅니다:
     * - **LocalStack**: 인자로 전달된 서비스만 활성화합니다.
     * - **Floci**: 모든 서비스가 항상 활성화되므로 이 메서드는 no-op이며,
     *   동일한 인스턴스를 그대로 반환합니다.
     * - **MiniStack**: 모든 서비스가 항상 활성화되므로 이 메서드는 no-op이며,
     *   동일한 인스턴스를 그대로 반환합니다.
     *
     * @param services 활성화할 AWS 서비스 이름 (예: `"s3"`, `"sqs"`, `"dynamodb"`)
     * @return 메서드 체이닝을 위한 현재 [AwsEmulatorServer] 인스턴스
     */
    fun withServices(vararg services: String): AwsEmulatorServer
}
