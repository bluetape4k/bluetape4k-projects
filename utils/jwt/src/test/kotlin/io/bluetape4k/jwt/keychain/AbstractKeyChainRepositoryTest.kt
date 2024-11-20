package io.bluetape4k.jwt.keychain

import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration
import kotlin.test.assertTrue

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractKeyChainRepositoryTest {

    companion object: KLogging()

    abstract val repository: KeyChainRepository

    @BeforeEach
    fun beforeEach() {
        // deleteAll 은 테스트 시에만 사용하세요
        repository.deleteAll()
    }

    @Test
    fun `current 가 만료되지 않은 상태에서 rotate를 호출해도 key chain 이 교체되지 않아야 한다`() {
        val prevKeyChain = KeyChain(expiredTtl = Duration.ofDays(1))
        repository.rotate(prevKeyChain).shouldBeTrue()

        repository.current() shouldBeEqualTo prevKeyChain

        // 기존 key chain 이 만료되지 않았는데도 rotate 하려고 한다
        val newKeyChain = KeyChain()
        prevKeyChain.isExpired.shouldBeFalse()
        repository.rotate(newKeyChain).shouldBeFalse()

        repository.current() shouldBeEqualTo prevKeyChain
        repository.current() shouldNotBeEqualTo newKeyChain
    }

    @Test
    fun `current 가 만료된 상태라면 rotate가 성공해야 한다`() {
        val prevKeyChain = KeyChain(expiredTtl = Duration.ofMillis(1))
        repository.rotate(prevKeyChain).shouldBeTrue()
        repository.current() shouldBeEqualTo prevKeyChain

        Thread.sleep(10)
        assertTrue { prevKeyChain.createdAt + 1 < System.currentTimeMillis() }

        // 기존 key chain 이 만료되었으므로 rotate 되어야 한다 
        val newKeyChain = KeyChain()
        repository.rotate(newKeyChain).shouldBeTrue()

        repository.current() shouldNotBeEqualTo prevKeyChain
        repository.current() shouldBeEqualTo newKeyChain
    }

    @Test
    fun `현 keychain의 expiredTtl이 0 이라면 rotate 되지 않아야 합니다`() {
        // 기존
        val prevKeyChain = KeyChain(expiredTtl = Duration.ZERO)
        repository.rotate(prevKeyChain).shouldBeTrue()

        val newKeyChain = KeyChain()
        repository.rotate(newKeyChain).shouldBeFalse()

        repository.current() shouldBeEqualTo prevKeyChain

    }

    @Test
    fun `새로운 KeyChain을 저장합니다`() {
        val keyChain = KeyChain(expiredTtl = Duration.ZERO)
        repository.rotate(keyChain).shouldBeTrue()

        repository.current() shouldBeEqualTo keyChain
    }

    @Test
    fun `Current와 같은 KeyChain으로 rotate하면 무시한다`() {
        val keyChain = KeyChain(expiredTtl = Duration.ZERO)
        repository.rotate(keyChain).shouldBeTrue()

        Thread.sleep(10)
        repository.rotate(keyChain).shouldBeFalse()
    }

    @Test
    fun `current KeyChain이 가장 최신이어야 한다`() {
        repository.rotate(KeyChain(expiredTtl = Duration.ofMillis(1))).shouldBeTrue()
        Thread.sleep(1)
        repository.rotate(KeyChain(expiredTtl = Duration.ofMillis(1))).shouldBeTrue()
        Thread.sleep(1)

        val newestKeyChain = KeyChain(expiredTtl = Duration.ZERO)
        repository.rotate(newestKeyChain).shouldBeTrue()

        repository.current() shouldBeEqualTo newestKeyChain
    }

    @Test
    fun `read KeyChain by id`() {
        repository.rotate(KeyChain(expiredTtl = Duration.ofMillis(1))).shouldBeTrue()
        Thread.sleep(1)

        val keyChain = KeyChain(expiredTtl = Duration.ZERO)
        repository.rotate(keyChain).shouldBeTrue()
        Thread.sleep(1)

        repository.rotate(KeyChain(expiredTtl = Duration.ZERO)).shouldBeFalse()

        val loaded = repository.findOrNull(keyChain.id)
        loaded shouldBeEqualTo keyChain
    }

    @Test
    fun `capacity 이상으로 rotate를 하면, 오래된 것은 삭제된다`() {

        repeat(repository.capacity * 2) {
            repository.rotate(KeyChain(expiredTtl = Duration.ofMillis(1))).shouldBeTrue()
            Thread.sleep(2)
        }

        val keyChain = KeyChain()
        repository.rotate(keyChain).shouldBeTrue()

        repository.current() shouldBeEqualTo keyChain
    }

    @Test
    fun `capacity 이상으로 forced rotate를 하면, 오래된 것은 삭제된다`() {

        repeat(repository.capacity * 2) {
            repository.forcedRotate(KeyChain()).shouldBeTrue()
        }

        val keyChain = KeyChain()
        repository.forcedRotate(keyChain).shouldBeTrue()

        repository.current() shouldBeEqualTo keyChain
    }
}
