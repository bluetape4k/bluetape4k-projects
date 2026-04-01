package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BoundingBoxTest {

    companion object: KLogging()

    // 한반도 대략적인 BoundingBox
    val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)

    // 서울 근방 BoundingBox
    val seoulArea = BoundingBox(minLat = 37.4, minLon = 126.7, maxLat = 37.7, maxLon = 127.2)

    @Test
    fun `서울 좌표가 한반도 BoundingBox 안에 포함된다`() {
        val seoul = GeoLocation(37.5665, 126.9780)
        korea.contains(seoul).shouldBeTrue()
    }

    @Test
    fun `뉴욕 좌표는 한반도 BoundingBox 밖에 있다`() {
        val newYork = GeoLocation(40.7128, -74.0060)
        korea.contains(newYork).shouldBeFalse()
    }

    @Test
    fun `서울 영역은 한반도 BoundingBox 안에 포함된다`() {
        korea.contains(seoulArea).shouldBeTrue()
    }

    @Test
    fun `한반도 BoundingBox와 서울 영역이 겹친다`() {
        korea.intersects(seoulArea).shouldBeTrue()
    }

    @Test
    fun `겹치지 않는 두 BoundingBox는 intersects가 false이다`() {
        val japan = BoundingBox(minLat = 30.0, minLon = 130.0, maxLat = 46.0, maxLon = 146.0)
        val usa = BoundingBox(minLat = 24.0, minLon = -125.0, maxLat = 49.0, maxLon = -66.0)
        japan.intersects(usa).shouldBeFalse()
    }

    @Test
    fun `union이 두 BoundingBox를 모두 포함한다`() {
        val bbox1 = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 10.0, maxLon = 10.0)
        val bbox2 = BoundingBox(minLat = 5.0, minLon = 5.0, maxLat = 20.0, maxLon = 20.0)
        val union = bbox1.union(bbox2)
        union.minLat shouldBeEqualTo 0.0
        union.minLon shouldBeEqualTo 0.0
        union.maxLat shouldBeEqualTo 20.0
        union.maxLon shouldBeEqualTo 20.0
    }

    @Test
    fun `center가 BoundingBox의 중심 좌표를 반환한다`() {
        val bbox = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 10.0, maxLon = 20.0)
        val center = bbox.center()
        center.latitude shouldBeEqualTo 5.0
        center.longitude shouldBeEqualTo 10.0
    }

    @Test
    fun `relationTo - CONTAINS 관계`() {
        korea.relationTo(seoulArea) shouldBeEqualTo BoundingBoxRelation.CONTAINS
    }

    @Test
    fun `relationTo - WITHIN 관계`() {
        seoulArea.relationTo(korea) shouldBeEqualTo BoundingBoxRelation.WITHIN
    }

    @Test
    fun `relationTo - DISJOINT 관계`() {
        val japan = BoundingBox(minLat = 30.0, minLon = 130.0, maxLat = 46.0, maxLon = 146.0)
        val usa = BoundingBox(minLat = 24.0, minLon = -125.0, maxLat = 49.0, maxLon = -66.0)
        japan.relationTo(usa) shouldBeEqualTo BoundingBoxRelation.DISJOINT
    }

    @Test
    fun `toEnvelope가 올바른 JTS Envelope을 반환한다`() {
        val bbox = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
        val env = bbox.toEnvelope()
        env.minX shouldBeEqualTo 124.0
        env.maxX shouldBeEqualTo 131.0
        env.minY shouldBeEqualTo 33.0
        env.maxY shouldBeEqualTo 38.9
    }

    @Test
    fun `cellBoundingBox size가 0이하이면 예외를 발생시킨다`() {
        val zone = UtmZone(52, 'S')
        assertThrows<IllegalArgumentException> { zone.cellBoundingBox(size = 0.0, row = 0, col = 0) }
        assertThrows<IllegalArgumentException> { zone.cellBoundingBox(size = -1.0, row = 0, col = 0) }
    }

    @Test
    fun `cellBoundingBox row나 col이 음수이면 예외를 발생시킨다`() {
        val zone = UtmZone(52, 'S')
        assertThrows<IllegalArgumentException> { zone.cellBoundingBox(size = 1.0, row = -1, col = 0) }
        assertThrows<IllegalArgumentException> { zone.cellBoundingBox(size = 1.0, row = 0, col = -1) }
    }
}
