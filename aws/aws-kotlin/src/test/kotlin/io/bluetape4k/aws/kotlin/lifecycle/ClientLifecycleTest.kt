package io.bluetape4k.aws.kotlin.lifecycle

import io.bluetape4k.aws.kotlin.http.crtHttpEngineOf
import io.bluetape4k.aws.kotlin.s3.s3ClientOf
import io.bluetape4k.aws.kotlin.s3.withS3Client
import io.bluetape4k.aws.kotlin.ses.sesClientOf
import io.bluetape4k.aws.kotlin.ses.withSesClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * `xxxClientOf()` 및 `withXxx()` 함수의 종료 보장 회귀 테스트.
 *
 * ## 검증 항목
 * - `httpClient` 미지정 시 SDK가 엔진을 직접 관리(`isManaged=true`)하여 `close()` 시 CRT 스레드까지 종료됨
 * - `withXxx { }` 패턴은 블록 종료 후 클라이언트와 엔진이 함께 종료됨
 * - 외부 엔진을 명시적으로 전달한 경우 `client.close()` 후에도 엔진이 살아있음
 *
 * LocalStack 없이 네트워크 호출 없이 생명주기만 검증합니다.
 */
class ClientLifecycleTest {

    companion object: KLogging()

    // ── SDK 관리 엔진 (httpClient 미지정) ──────────────────────────────────

    @Test
    fun `s3ClientOf - httpClient 미지정 시 close가 제한 시간 내에 완료된다`() = runTest(timeout = 10.seconds) {
        val client = s3ClientOf(region = "us-east-1")
        client.shouldNotBeNull()
        log.debug { "S3Client 생성 완료: $client" }
        client.close()
        log.debug { "S3Client close 완료 — SDK 관리 엔진이 종료됨" }
    }

    @Test
    fun `sesClientOf - httpClient 미지정 시 close가 제한 시간 내에 완료된다`() = runTest(timeout = 10.seconds) {
        val client = sesClientOf(region = "us-east-1")
        client.shouldNotBeNull()
        client.close()
    }

    // ── withXxx 패턴 ───────────────────────────────────────────────────────

    @Test
    fun `withS3Client 블록 종료 후 클라이언트가 제한 시간 내에 닫힌다`() = runTest(timeout = 10.seconds) {
        withS3Client(region = "us-east-1") { client ->
            client.shouldNotBeNull()
            log.debug { "withS3Client 블록 내 클라이언트: $client" }
        }
        log.debug { "withS3Client 블록 종료 — 클라이언트 및 SDK 관리 엔진 종료됨" }
    }

    @Test
    fun `withSesClient 블록 종료 후 클라이언트가 제한 시간 내에 닫힌다`() = runTest(timeout = 10.seconds) {
        withSesClient(region = "us-east-1") { client ->
            client.shouldNotBeNull()
        }
    }

    // ── 외부 엔진 전달 시 엔진이 client.close() 후에도 살아있다 ─────────────

    @Test
    fun `s3ClientOf - 외부 httpClient 전달 시 client close 후에도 엔진이 종료되지 않는다`() =
        runTest(timeout = 10.seconds) {
            val sharedEngine = crtHttpEngineOf()
            try {
                val client1 = s3ClientOf(region = "us-east-1", httpClient = sharedEngine)
                client1.close()  // 외부 엔진은 닫히지 않음 (isManaged=false)

                // 같은 엔진으로 다시 클라이언트를 만들 수 있어야 함
                val client2 = s3ClientOf(region = "us-east-1", httpClient = sharedEngine)
                client2.shouldNotBeNull()
                log.debug { "외부 엔진으로 두 번째 클라이언트 생성 성공: $client2" }
                client2.close()
            } finally {
                // 외부 엔진은 호출자가 직접 종료해야 함
                sharedEngine.closeSafe()
                log.debug { "외부 엔진 종료 완료" }
            }
        }

    // ── use 블록 (kotlin AutoCloseable) ───────────────────────────────────

    @Test
    fun `s3ClientOf 생성 후 use 블록으로 SDK 관리 엔진이 자동 종료된다`() = runTest(timeout = 10.seconds) {
        s3ClientOf(region = "us-east-1").use { client ->
            client.shouldNotBeNull()
            log.debug { "use 블록 내 S3Client: $client" }
        }
        log.debug { "use 블록 종료 — SDK 관리 엔진이 종료됨" }
    }
}
