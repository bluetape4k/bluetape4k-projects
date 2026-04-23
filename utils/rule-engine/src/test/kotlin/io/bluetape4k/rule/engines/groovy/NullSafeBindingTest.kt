package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class NullSafeBindingTest {

    companion object : KLogging()

    @Test
    fun `초기화된 변수는 정상 반환`() {
        val binding = NullSafeBinding(mapOf("amount" to 1000, "name" to "alice"))
        binding.getVariable("amount") shouldBeEqualTo 1000
        binding.getVariable("name") shouldBeEqualTo "alice"
    }

    @Test
    fun `존재하지 않는 변수는 null 반환`() {
        val binding = NullSafeBinding(mapOf("amount" to 1000))
        binding.getVariable("unknown").shouldBeNull()
    }

    @Test
    fun `빈 초기 맵으로 생성 시 모든 변수 null 반환`() {
        val binding = NullSafeBinding()
        binding.getVariable("anything").shouldBeNull()
    }

    @Test
    fun `setVariable로 변수 추가 후 조회`() {
        val binding = NullSafeBinding()
        binding.setVariable("key", "value")
        binding.getVariable("key") shouldBeEqualTo "value"
    }

    @Test
    fun `hasVariable로 존재 여부 확인`() {
        val binding = NullSafeBinding(mapOf("x" to 42))
        binding.hasVariable("x").shouldBeTrue()
        binding.hasVariable("y") shouldBeEqualTo false
    }

    @Test
    fun `null 값 변수 저장 및 조회`() {
        val binding = NullSafeBinding(mapOf("nullKey" to null))
        // When a variable is explicitly set to null, hasVariable returns true
        // but the returned value should be null
        binding.getVariable("nullKey").shouldBeNull()
    }

    @Test
    fun `GroovyCondition에서 NullSafeBinding으로 누락 변수 null 처리`() {
        // When "name" is not in facts, NullSafeBinding returns null → safe call returns null
        val condition = GroovyCondition("name?.toUpperCase() == 'ALICE'")
        val factsWithName = io.bluetape4k.rule.api.Facts.of("name" to "alice")
        condition.evaluate(factsWithName).shouldBeTrue()

        val factsWithoutName = io.bluetape4k.rule.api.Facts.empty()
        // Should return false (null != 'ALICE'), not throw exception
        condition.evaluate(factsWithoutName) shouldBeEqualTo false
    }
}
