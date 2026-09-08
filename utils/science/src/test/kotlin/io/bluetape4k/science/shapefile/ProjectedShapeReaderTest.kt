package io.bluetape4k.science.shapefile

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.science.projection.transform
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.math.abs

class ProjectedShapeReaderTest {

    @Test
    fun `투영좌표 파일을 동기와 비동기로 읽어 원본 좌표를 보존한다`(@TempDir dir: Path) = runTest {
        val file = createWebMercatorPointShapefile(dir, 126.9780, 37.5665)
        val (expectedX, expectedY) = transform("EPSG:4326", "EPSG:3857", 126.9780, 37.5665)
        for (shape in listOf(loadShape(file), loadShapeAsync(file))) {
            shape.size shouldBeEqualTo 1
            val coordinate = shape.records.single().geometry.coordinate
            abs(coordinate.x - expectedX) shouldBeLessThan 1e-5
            abs(coordinate.y - expectedY) shouldBeLessThan 1e-5
            val bounds = shape.computeBoundingBox().shouldNotBeNull()
            for (box in listOf(shape.header.bbox, shape.records.single().bbox.shouldNotBeNull(), bounds)) {
                abs(box.minX - expectedX) shouldBeLessThan 1e-5
                abs(box.maxX - expectedX) shouldBeLessThan 1e-5
                abs(box.minY - expectedY) shouldBeLessThan 1e-5
                abs(box.maxY - expectedY) shouldBeLessThan 1e-5
            }
            shape.filterByBoundingBox(shape.header.bbox).size shouldBeEqualTo 1
        }
    }
}
