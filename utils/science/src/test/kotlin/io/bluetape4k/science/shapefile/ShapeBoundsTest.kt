package io.bluetape4k.science.shapefile

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.science.coords.BoundingBox
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ShapeBoundsTest {

    @Test
    fun `투영좌표와 영면적 경계를 허용하고 직렬화로 보존한다`() {
        val bounds = ShapeBounds(minX = -1000000.0, minY = 4000000.0, maxX = 15000000.0, maxY = 4000000.0)
        bounds.copy() shouldBeEqualTo bounds
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(bounds) }
            output.toByteArray()
        }
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } shouldBeEqualTo bounds
    }

    @Test
    fun `생성자와 copy는 유한하지 않은 축 값을 거부한다`() {
        val bounds = ShapeBounds(minX = 0.0, minY = 0.0, maxX = 1.0, maxY = 1.0)
        for (invalid in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { ShapeBounds(minX = invalid, minY = 0.0, maxX = 1.0, maxY = 1.0) }
            assertFailsWith<IllegalArgumentException> { bounds.copy(minX = invalid) }
            assertFailsWith<IllegalArgumentException> { bounds.copy(minY = invalid) }
            assertFailsWith<IllegalArgumentException> { bounds.copy(maxX = invalid) }
            assertFailsWith<IllegalArgumentException> { bounds.copy(maxY = invalid) }
        }
    }

    @Test
    fun `각 축의 최소 최대 역순을 거부한다`() {
        assertFailsWith<IllegalArgumentException> { ShapeBounds(minX = 2.0, minY = 0.0, maxX = 1.0, maxY = 1.0) }
        val bounds = ShapeBounds(minX = 0.0, minY = 0.0, maxX = 1.0, maxY = 1.0)
        assertFailsWith<IllegalArgumentException> { bounds.copy(minY = 2.0) }
    }

    @Test
    fun `여러 투영 경계의 합집합과 필터의 경계 포함을 검증한다`() {
        val shape = shapeOf(Coordinate(14000000.0, 4000000.0), Coordinate(13000000.0, 5000000.0))
        shape.computeBoundingBox() shouldBeEqualTo
                ShapeBounds(minX = 13000000.0, minY = 4000000.0, maxX = 14000000.0, maxY = 5000000.0)
        val boundary = ShapeBounds(minX = 14000000.0, minY = 4000000.0, maxX = 14000000.0, maxY = 4000000.0)
        shape.filterByBoundingBox(boundary).records.map { it.recordNumber } shouldBeEqualTo listOf(0)
        val outside = ShapeBounds(minX = 15000000.0, minY = 6000000.0, maxX = 16000000.0, maxY = 7000000.0)
        shape.filterByBoundingBox(outside).size shouldBeEqualTo 0
    }

    @Test
    fun `위경도 필터 overload는 같은 XY 경계와 같은 레코드를 반환한다`() {
        val shape = shapeOf(Coordinate(126.0, 37.0), Coordinate(128.0, 36.0))
        val geographic = BoundingBox(minLat = 37.0, minLon = 126.0, maxLat = 38.0, maxLon = 127.0)
        val raw = ShapeBounds(minX = 126.0, minY = 37.0, maxX = 127.0, maxY = 38.0)
        shape.filterByBoundingBox(geographic).records.map { it.recordNumber } shouldBeEqualTo listOf(0)
        shape.filterByBoundingBox(geographic) shouldBeEqualTo shape.filterByBoundingBox(raw)
    }

    private fun shapeOf(vararg coordinates: Coordinate): Shape {
        val bounds = ShapeBounds(
            minX = coordinates.minOf { it.x }, minY = coordinates.minOf { it.y },
            maxX = coordinates.maxOf { it.x }, maxY = coordinates.maxOf { it.y },
        )
        val factory = GeometryFactory()
        return Shape(
            header = ShapeHeader(9994, 0, 1000, 1, bounds),
            records = coordinates.mapIndexed { index, coordinate ->
                ShapeRecord(
                    recordNumber = index,
                    shapeType = 1,
                    bbox = ShapeBounds(minX = coordinate.x, minY = coordinate.y, maxX = coordinate.x, maxY = coordinate.y),
                    geometry = factory.createPoint(coordinate),
                )
            },
            attributes = emptyList(),
        )
    }

}
