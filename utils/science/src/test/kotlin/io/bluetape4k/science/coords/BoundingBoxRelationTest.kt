package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BoundingBoxRelationTest {

    companion object: KLogging()

    // 한반도 대략적인 BoundingBox
    private val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)

    // 서울 근방 BoundingBox (한반도 내부)
    private val seoulArea = BoundingBox(minLat = 37.4, minLon = 126.7, maxLat = 37.7, maxLon = 127.2)

    // 미국 BoundingBox (겹치지 않음)
    private val usa = BoundingBox(minLat = 24.0, minLon = -125.0, maxLat = 49.0, maxLon = -66.0)

    // 부분적으로 겹치는 BoundingBox
    private val partialOverlap = BoundingBox(minLat = 37.0, minLon = 128.0, maxLat = 40.0, maxLon = 133.0)

    @Test
    fun `BoundingBoxRelation 열거형에 4가지 값이 있다`() {
        val values = BoundingBoxRelation.values()
        values.size shouldBeEqualTo 4
    }

    @Test
    fun `CONTAINS 관계 - 한반도가 서울 영역을 포함한다`() {
        korea.relationTo(seoulArea) shouldBeEqualTo BoundingBoxRelation.CONTAINS
    }

    @Test
    fun `WITHIN 관계 - 서울 영역이 한반도 안에 있다`() {
        seoulArea.relationTo(korea) shouldBeEqualTo BoundingBoxRelation.WITHIN
    }

    @Test
    fun `DISJOINT 관계 - 한반도와 미국은 겹치지 않는다`() {
        korea.relationTo(usa) shouldBeEqualTo BoundingBoxRelation.DISJOINT
    }

    @Test
    fun `DISJOINT 관계 - 미국과 한반도는 겹치지 않는다`() {
        usa.relationTo(korea) shouldBeEqualTo BoundingBoxRelation.DISJOINT
    }

    @Test
    fun `INTERSECTS 관계 - 부분적으로 겹치는 영역`() {
        korea.relationTo(partialOverlap) shouldBeEqualTo BoundingBoxRelation.INTERSECTS
    }

    @Test
    fun `INTERSECTS 관계 - 반대 방향도 INTERSECTS이다`() {
        partialOverlap.relationTo(korea) shouldBeEqualTo BoundingBoxRelation.INTERSECTS
    }

    @Test
    fun `자기 자신과의 관계는 CONTAINS이다`() {
        // 자기 자신을 포함하면 CONTAINS가 반환된다 (contains가 먼저 체크됨)
        korea.relationTo(korea) shouldBeEqualTo BoundingBoxRelation.CONTAINS
    }

    @Test
    fun `BoundingBoxRelation 이름 검증`() {
        BoundingBoxRelation.DISJOINT.name shouldBeEqualTo "DISJOINT"
        BoundingBoxRelation.INTERSECTS.name shouldBeEqualTo "INTERSECTS"
        BoundingBoxRelation.CONTAINS.name shouldBeEqualTo "CONTAINS"
        BoundingBoxRelation.WITHIN.name shouldBeEqualTo "WITHIN"
    }

    @Test
    fun `BoundingBoxRelation valueOf로 이름으로 값을 얻을 수 있다`() {
        BoundingBoxRelation.valueOf("CONTAINS") shouldBeEqualTo BoundingBoxRelation.CONTAINS
        BoundingBoxRelation.valueOf("WITHIN") shouldBeEqualTo BoundingBoxRelation.WITHIN
        BoundingBoxRelation.valueOf("DISJOINT") shouldBeEqualTo BoundingBoxRelation.DISJOINT
        BoundingBoxRelation.valueOf("INTERSECTS") shouldBeEqualTo BoundingBoxRelation.INTERSECTS
    }

    @Test
    fun `동일한 크기로 겹치는 두 BoundingBox의 관계`() {
        val a = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 10.0, maxLon = 10.0)
        val b = BoundingBox(minLat = 5.0, minLon = 5.0, maxLat = 15.0, maxLon = 15.0)
        a.relationTo(b) shouldBeEqualTo BoundingBoxRelation.INTERSECTS
        b.relationTo(a) shouldBeEqualTo BoundingBoxRelation.INTERSECTS
    }
}
