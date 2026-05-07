package io.bluetape4k.science.shapefile

import io.bluetape4k.logging.KLogging
import io.bluetape4k.science.coords.BoundingBox
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class ShapeModelsTest {

    companion object: KLogging()

    private val geometryFactory = GeometryFactory()

    private val koreaBbox = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)

    private val sampleHeader = ShapeHeader(
        fileCode = 9994,
        fileLength = 1024,
        version = 1000,
        shapeType = 1,
        bbox = koreaBbox,
    )

    @Test
    fun `ShapeHeader 데이터 클래스가 올바르게 생성된다`() {
        sampleHeader.fileCode shouldBeEqualTo 9994
        sampleHeader.fileLength shouldBeEqualTo 1024
        sampleHeader.version shouldBeEqualTo 1000
        sampleHeader.shapeType shouldBeEqualTo 1
        sampleHeader.bbox shouldBeEqualTo koreaBbox
    }

    @Test
    fun `ShapeHeader equality가 올바르게 동작한다`() {
        val a = ShapeHeader(9994, 1024, 1000, 1, koreaBbox)
        val b = ShapeHeader(9994, 1024, 1000, 1, koreaBbox)
        (a == b).shouldBeTrue()
    }

    @Test
    fun `ShapeHeader copy가 올바르게 동작한다`() {
        val copy = sampleHeader.copy(shapeType = 5)
        copy.shapeType shouldBeEqualTo 5
        copy.fileCode shouldBeEqualTo 9994
        (sampleHeader == copy).shouldBeFalse()
    }

    @Test
    fun `ShapeAttribute 데이터 클래스가 올바르게 생성된다`() {
        val attr = ShapeAttribute(name = "NAME", type = 'C', length = 80, decimal = 0)
        attr.name shouldBeEqualTo "NAME"
        attr.type shouldBeEqualTo 'C'
        attr.length shouldBeEqualTo 80
        attr.decimal shouldBeEqualTo 0
    }

    @Test
    fun `ShapeAttribute equality가 올바르게 동작한다`() {
        val a = ShapeAttribute("NAME", 'C', 80, 0)
        val b = ShapeAttribute("NAME", 'C', 80, 0)
        (a == b).shouldBeTrue()
    }

    @Test
    fun `ShapeAttribute 다양한 타입 문자를 지원한다`() {
        val charAttr = ShapeAttribute("NAME", 'C', 80, 0)
        val numAttr = ShapeAttribute("AREA", 'N', 10, 2)
        val dateAttr = ShapeAttribute("DATE", 'D', 8, 0)
        val logicalAttr = ShapeAttribute("FLAG", 'L', 1, 0)

        charAttr.type shouldBeEqualTo 'C'
        numAttr.type shouldBeEqualTo 'N'
        dateAttr.type shouldBeEqualTo 'D'
        logicalAttr.type shouldBeEqualTo 'L'
    }

    @Test
    fun `ShapeRecord 데이터 클래스가 올바르게 생성된다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val record = ShapeRecord(
            recordNumber = 0,
            shapeType = 1,
            bbox = koreaBbox,
            geometry = point,
            attributes = mapOf("NAME" to "Seoul"),
        )
        record.recordNumber shouldBeEqualTo 0
        record.shapeType shouldBeEqualTo 1
        record.bbox shouldBeEqualTo koreaBbox
        record.geometry shouldBeEqualTo point
        record.attributes["NAME"] shouldBeEqualTo "Seoul"
    }

    @Test
    fun `ShapeRecord bbox가 null일 수 있다 (NULL 도형)`() {
        val point = geometryFactory.createPoint(Coordinate(0.0, 0.0))
        val record = ShapeRecord(
            recordNumber = 0,
            shapeType = 0,
            bbox = null,
            geometry = point,
        )
        record.bbox.shouldBeNull()
    }

    @Test
    fun `ShapeRecord attributes가 기본으로 빈 맵이다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val record = ShapeRecord(
            recordNumber = 1,
            shapeType = 1,
            bbox = koreaBbox,
            geometry = point,
        )
        record.attributes.isEmpty().shouldBeTrue()
    }

    @Test
    fun `Shape 데이터 클래스가 올바르게 생성된다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val record = ShapeRecord(0, 1, koreaBbox, point, mapOf("NAME" to "Seoul"))
        val attr = ShapeAttribute("NAME", 'C', 80, 0)
        val shape = Shape(
            header = sampleHeader,
            records = listOf(record),
            attributes = listOf(attr),
        )

        shape.header shouldBeEqualTo sampleHeader
        shape.records.size shouldBeEqualTo 1
        shape.attributes.size shouldBeEqualTo 1
    }

    @Test
    fun `Shape size가 레코드 수를 반환한다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val records = listOf(
            ShapeRecord(0, 1, koreaBbox, point),
            ShapeRecord(1, 1, koreaBbox, point),
            ShapeRecord(2, 1, koreaBbox, point),
        )
        val shape = Shape(sampleHeader, records, emptyList())
        shape.size shouldBeEqualTo 3
    }

    @Test
    fun `Shape isEmpty가 빈 레코드 목록일 때 true이다`() {
        val shape = Shape(sampleHeader, emptyList(), emptyList())
        shape.isEmpty.shouldBeTrue()
        shape.size shouldBeEqualTo 0
    }

    @Test
    fun `Shape isEmpty가 레코드가 있을 때 false이다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val record = ShapeRecord(0, 1, koreaBbox, point)
        val shape = Shape(sampleHeader, listOf(record), emptyList())
        shape.isEmpty.shouldBeFalse()
    }

    @Test
    fun `Shape Serializable - 예외 없이 직렬화된다`() {
        val shape = Shape(sampleHeader, emptyList(), emptyList())
        java.io.ObjectOutputStream(java.io.ByteArrayOutputStream()).use { out ->
            out.writeObject(shape)
        }
    }

    @Test
    fun `ShapeRecord attributes에서 NAME 속성을 찾을 수 있다`() {
        val point = geometryFactory.createPoint(Coordinate(126.978, 37.566))
        val record = ShapeRecord(
            recordNumber = 0,
            shapeType = 1,
            bbox = koreaBbox,
            geometry = point,
            attributes = mapOf("NAME" to "Seoul", "CODE" to 11),
        )
        record.attributes["NAME"].shouldNotBeNull()
        record.attributes["NAME"] shouldBeEqualTo "Seoul"
        record.attributes["CODE"] shouldBeEqualTo 11
    }
}
