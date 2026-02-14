package io.bluetape4k.jwt.provider

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.jwt.AbstractJwtTest
import io.bluetape4k.jwt.codec.Lz4Codec
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.utils.dateOfEpochSeconds
import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertFailsWith

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractJwtProviderTest: AbstractJwtTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val compressCodec = Lz4Codec()
    }

    abstract val repository: KeyChainRepository

    abstract val provider: JwtProvider

    @BeforeEach
    fun beforeEach() {
        repository.deleteAll()
        provider.rotate()
    }

    @Test
    fun `명시적으로 rotate 하지 않는 한 Current KeyChain은 같아야 한다`() {
        val keyChain1 = provider.currentKeyChain()
        val keyChain2 = provider.currentKeyChain()

        keyChain2 shouldBeEqualTo keyChain1

        Thread.sleep(10)
        provider.currentKeyChain() shouldBeEqualTo keyChain1
    }

    @Test
    fun `유효기간이 지나지 않은 rotate 를 하면 Current KeyChain이 변경되지 않는다`() {
        val keyChain1 = provider.currentKeyChain()

        // rotate 하면 key chain 이 변경됩니다.
        provider.rotate().shouldBeFalse()

        val keyChain2 = provider.currentKeyChain()

        keyChain2 shouldBeEqualTo keyChain1
        provider.currentKeyChain() shouldBeEqualTo keyChain1
    }

    @Test
    fun `forced rotate 를 하면 Current KeyChain이 변경되어야 한다`() {
        val keyChain1 = provider.currentKeyChain()

        // rotate 하면 key chain 이 변경됩니다.
        provider.forcedRotate().shouldBeTrue()

        val keyChain2 = provider.currentKeyChain()

        keyChain2 shouldNotBeEqualTo keyChain1
        provider.currentKeyChain() shouldBeEqualTo keyChain2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt token`() {
        val now = Date()
        val jwt = provider.compose {
            claim("author", "debop")
            issuer = LibraryName
            issuedAt = now
            claim("custom-data", randomString(1024))
            compressionCodec = compressCodec
        }

        val reader = provider.parse(jwt)
        reader.issuer shouldBeEqualTo LibraryName
        reader.issuedAt shouldBeEqualTo dateOfEpochSeconds(now.epochSeconds)
        reader.claim<String>("author") shouldBeEqualTo "debop"
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt in multit-hreading`() {
        val customData = randomString(1024)
        val now = Date()
        val jwts = CopyOnWriteArrayList<String>()

        MultithreadingTester()
            .workers(16)
            .rounds(32)
            .add {
                val jwt = provider.compose {
                    claim("author", "debop")
                    claim("service", LibraryName)
                    issuer = LibraryName
                    issuedAt = now
                    claim("custom-data", customData)
                    compressionCodec = compressCodec
                }
                log.trace { "created jwt=$jwt" }
                jwts.add(jwt)
            }
            .run()

        Thread.sleep(10L)

        jwts.size shouldBeEqualTo 16 * 32
        val uniqueJwts = jwts.distinct()
        uniqueJwts.forEach { jwt ->
            log.trace { "jwt=$jwt" }
        }
        uniqueJwts.size shouldBeEqualTo 1
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt in virtual threads`() {
        val customData = randomString(1024)
        val now = Date()
        val jwts = CopyOnWriteArrayList<String>()

        StructuredTaskScopeTester()
            .rounds(16 * 32)
            .add {
                val jwt = provider.compose {
                    claim("author", "debop")
                    claim("service", LibraryName)
                    issuer = LibraryName
                    issuedAt = now
                    claim("custom-data", customData)
                    compressionCodec = compressCodec
                }
                log.trace { "created jwt=$jwt" }
                jwts.add(jwt)
            }
            .run()

        Thread.sleep(10L)

        jwts.size shouldBeEqualTo 16 * 32
        val uniqueJwts = jwts.distinct()
        uniqueJwts.forEach { jwt ->
            log.trace { "jwt=$jwt" }
        }
        uniqueJwts.size shouldBeEqualTo 1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compose jwt in coroutines`() = runSuspendDefault {
        val customData = randomString(1024)
        val now = Date()
        val jwts = CopyOnWriteArrayList<String>()

        SuspendedJobTester()
            .numThreads(16)
            .roundsPerJob(16 * 32)
            .add {
                withContext(Dispatchers.Default) {
                    val jwt = provider.compose {
                        claim("author", "debop")
                        claim("service", LibraryName)
                        issuer = LibraryName
                        issuedAt = now
                        claim("custom-data", customData)
                        compressionCodec = compressCodec
                    }
                    log.trace { "created jwt=$jwt" }
                    jwts.add(jwt)
                }
            }
            .run()

        Thread.sleep(10L)

        jwts.size shouldBeEqualTo 16 * 32
        val uniqueJwts = jwts.distinct()
        uniqueJwts.forEach { jwt ->
            log.trace { "jwt=$jwt" }
        }
        uniqueJwts.size shouldBeEqualTo 1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `삭제된 KeyChain으로 구성된 jwt는 parsing이 실패해야 한다`() {
        Assumptions.assumeTrue { provider is DefaultJwtProvider }

        val jwt = provider.compose {
            claim("author", "debop")
            expirationAfterSeconds = 30
        }
        val reader = provider.parse(jwt)
        log.debug { "kid=${reader.kid}" }
        reader.isExpired.shouldBeFalse()

        // KeyChain 이 변경되었으므로,
        provider.forcedRotate().shouldBeTrue()

        val reader2 = provider.parse(jwt)
        log.debug { "kid=${reader2.kid}" }

        // 오래된 key chanin을 삭제하도록 충분히 rotate 한다
        repeat(repository.capacity * 2) {
            provider.forcedRotate()
        }

        // jwt 발급에 쓰인 KeyChain을 provider의 repository에 없으므로, parsing 이 되지 않는다.
        assertFailsWith<JwtException> {
            provider.parse(jwt)
        }
    }
}
