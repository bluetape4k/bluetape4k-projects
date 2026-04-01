package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.Test
import kotlin.math.abs

class CoordConvertersTest {

    companion object: KLogging() {
        const val EPSILON = 1e-4
    }

    @Test
    fun `Double을 DM으로 변환한다`() {
        val degree = 37.5665
        val dm = degree.toDM()
        dm.degree shouldBeEqualTo 37
        (abs(dm.minute - 33.99) < 0.01).let { assert(it) { "minute 오차: ${dm.minute}" } }
    }

    @Test
    fun `DM을 십진도로 변환한다`() {
        val dm = DM(degree = 37, minute = 33.99)
        val result = dm.toDegree()
        (abs(result - 37.5665) < EPSILON).let { assert(it) { "변환 오차: $result" } }
    }

    @Test
    fun `Double을 DMS로 변환한다`() {
        val degree = 126.9780
        val dms = degree.toDMS()
        dms.degree shouldBeEqualTo 126
        dms.minute shouldBeEqualTo 58
        // second는 약 40.8 근처
        dms.second.shouldBeInRange(40.0..42.0)
    }

    @Test
    fun `DMS를 십진도로 변환한다`() {
        val dms = DMS(degree = 126, minute = 58, second = 40.8)
        val result = dms.toDegree()
        (abs(result - 126.9780) < EPSILON).let { assert(it) { "변환 오차: $result" } }
    }

    @Test
    fun `DM 왕복 변환 정확도 검증 - 서울 위도`() {
        val original = 37.5665
        val dm = original.toDM()
        val restored = dm.toDegree()
        (abs(restored - original) < EPSILON).let { assert(it) { "왕복 오차: ${abs(restored - original)}" } }
    }

    @Test
    fun `DMS 왕복 변환 정확도 검증 - 서울 경도`() {
        val original = 126.9780
        val dms = original.toDMS()
        val restored = dms.toDegree()
        (abs(restored - original) < EPSILON).let { assert(it) { "왕복 오차: ${abs(restored - original)}" } }
    }

    @Test
    fun `음수 위도에 대한 DM 변환`() {
        val degree = -33.8688  // 시드니 위도
        val dm = degree.toDM()
        dm.degree shouldBeEqualTo -33
        // 음수 좌표에서 minute은 음수로 표현됨 (-52.128 ≈ -0.8688 * 60)
        (abs(dm.minute) > 0.0).let { assert(it) { "분의 절대값이 0보다 커야 합니다: ${dm.minute}" } }
        // 왕복 변환 정확도 검증
        val restored = dm.toDegree()
        (abs(restored - degree) < EPSILON).let { assert(it) { "왕복 오차: ${abs(restored - degree)}" } }
    }
}
