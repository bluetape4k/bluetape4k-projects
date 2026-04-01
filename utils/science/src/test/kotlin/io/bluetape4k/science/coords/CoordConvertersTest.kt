package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
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

    @Test
    fun `음수 위도를 DM으로 변환하면 minute은 양수이다`() {
        val dm = (-33.8688).toDM()
        dm.degree shouldBeEqualTo -33
        (dm.minute > 0).shouldBeTrue()
        // 왕복 정확도
        abs(dm.toDegree() - (-33.8688)) shouldBeLessThan 1e-10
    }

    @Test
    fun `음수 경도를 DMS로 변환하면 minute과 second는 음수가 아니다`() {
        // -74.006: 0.006 * 60 = 0.36 → minute=0, second≈21.6
        val dms = (-74.006).toDMS()
        dms.degree shouldBeEqualTo -74
        (dms.minute >= 0).shouldBeTrue()
        (dms.second >= 0.0).shouldBeTrue()
        abs(dms.toDegree() - (-74.006)) shouldBeLessThan 1e-8
    }

    @Test
    fun `DM compareTo — 같은 도에서 minute 비교`() {
        val a = DM(37, 30.0)
        val b = DM(37, 45.0)
        (a < b).shouldBeTrue()
        (b > a).shouldBeTrue()
        (a == a).shouldBeTrue()
    }

    @Test
    fun `DM compareTo — 다른 도 비교`() {
        val seoul = DM(37, 33.99)
        val busan = DM(35, 10.776)
        (seoul > busan).shouldBeTrue()
    }

    @Test
    fun `DMS compareTo — 도·분·초 순서로 비교한다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 58.00)
        (a < b).shouldBeTrue()
    }
}
