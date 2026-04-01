package io.bluetape4k.science.shapefile

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.science.coords.BoundingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.geotools.data.shapefile.dbf.DbaseFileReader
import org.geotools.data.shapefile.files.ShpFiles
import org.geotools.data.shapefile.shp.ShapefileReader
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.File
import java.nio.charset.Charset

/**
 * Shapefile을 읽어 [Shape] 객체로 반환하는 유틸리티 객체입니다.
 *
 * GeoTools 타입을 공개 API에 노출하지 않으며, 반환 타입은 모두 bluetape4k 도메인 모델을 사용합니다.
 */
object ShapefileReaderSupport : KLogging()

/**
 * 주어진 .shp 파일을 읽어 [Shape] 객체로 반환합니다.
 *
 * GeoTools [ShapefileReader]와 [DbaseFileReader]를 사용하여 도형 및 속성 정보를 읽습니다.
 * 공개 API에 GeoTools 타입을 노출하지 않습니다.
 *
 * @param file    .shp 확장자 파일
 * @param charset DBF 파일 인코딩 (기본값: UTF-8)
 * @return 읽어들인 [Shape] 객체
 * @throws IllegalArgumentException 파일이 존재하지 않거나 확장자가 .shp가 아닐 때
 */
fun loadShape(file: File, charset: Charset = Charsets.UTF_8): Shape {
    require(file.exists()) { "Shapefile이 존재하지 않습니다: ${file.absolutePath}" }
    require(file.extension.lowercase() == "shp") { "확장자가 .shp이어야 합니다: ${file.name}" }

    ShapefileReaderSupport.log.debug { "Shapefile 로드 시작: ${file.absolutePath}" }

    val shpFiles = ShpFiles(file)
    val gf = GeometryFactory()

    return ShapefileReader(shpFiles, false, false, gf).use { shpReader ->
        // 헤더 로드
        val header = loadShapeHeader(shpReader)

        // 도형 레코드 로드
        val rawRecords = loadRawRecords(shpReader)

        // DBF 속성 로드
        val (attributeDefs, attributeRows) = loadDbfData(shpFiles, charset, rawRecords.size)

        // ShapeRecord 조합
        val records = rawRecords.mapIndexed { idx, raw ->
            val attrMap = if (idx < attributeRows.size) {
                attributeDefs.indices.associate { i ->
                    attributeDefs[i].name to attributeRows[idx].getOrNull(i)
                }
            } else {
                emptyMap()
            }
            ShapeRecord(
                recordNumber = raw.number,
                shapeType = raw.shapeType,
                bbox = raw.bbox,
                geometry = raw.geometry,
                attributes = attrMap,
            )
        }

        ShapefileReaderSupport.log.debug { "Shapefile 로드 완료: 레코드 수=${records.size}" }
        Shape(header = header, records = records, attributes = attributeDefs)
    }
}

/**
 * .shp 파일을 비동기로 읽어 [Shape] 객체로 반환합니다.
 *
 * 파일 I/O를 [Dispatchers.IO]에서 수행합니다.
 *
 * @param file    .shp 확장자 파일
 * @param charset DBF 파일 인코딩 (기본값: UTF-8)
 * @return 읽어들인 [Shape] 객체
 */
suspend fun loadShapeAsync(file: File, charset: Charset = Charsets.UTF_8): Shape =
    withContext(Dispatchers.IO) { loadShape(file, charset) }

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private data class RawRecord(
    val number: Int,
    val shapeType: Int,
    val bbox: BoundingBox?,
    val geometry: Geometry,
)

private fun loadShapeHeader(reader: ShapefileReader): ShapeHeader {
    val h = reader.header
    val bbox = BoundingBox(
        minLat = h.minY(),
        minLon = h.minX(),
        maxLat = h.maxY(),
        maxLon = h.maxX(),
    )
    return ShapeHeader(
        fileCode = 9994,
        fileLength = h.fileLength,
        version = h.version,
        shapeType = h.shapeType.id,
        bbox = bbox,
    )
}

private fun loadRawRecords(reader: ShapefileReader): List<RawRecord> {
    ShapefileReaderSupport.log.trace { "Shape 레코드 로드 중..." }
    val result = mutableListOf<RawRecord>()
    while (reader.hasNext()) {
        val rec = reader.nextRecord()
        val geom = rec.shape() as? Geometry ?: continue
        val bbox = if (rec.minX.isFinite() && rec.minY.isFinite() &&
            rec.maxX.isFinite() && rec.maxY.isFinite()
        ) {
            BoundingBox(
                minLat = rec.minY,
                minLon = rec.minX,
                maxLat = rec.maxY,
                maxLon = rec.maxX,
            )
        } else null
        result.add(RawRecord(rec.number, rec.type.id, bbox, geom))
    }
    ShapefileReaderSupport.log.debug { "Shape 레코드 로드 완료: ${result.size}건" }
    return result
}

/**
 * DBF 파일에서 속성 정의와 각 행 데이터를 읽어 반환합니다.
 *
 * @return Pair(속성 정의 목록, 각 행의 값 배열 목록)
 */
private fun loadDbfData(
    shpFiles: ShpFiles,
    charset: Charset,
    recordCount: Int,
): Pair<List<ShapeAttribute>, List<Array<Any?>>> {
    ShapefileReaderSupport.log.trace { "DBF 속성 정보 로드 중..." }
    return DbaseFileReader(shpFiles, false, charset).use { dbfReader ->
        val header = dbfReader.header
        val fieldCount = header.numFields

        val attributeDefs = (0 until fieldCount).map { i ->
            ShapeAttribute(
                name = header.getFieldName(i),
                type = header.getFieldType(i),
                length = header.getFieldLength(i),
                decimal = header.getFieldDecimalCount(i),
            )
        }

        val rows = mutableListOf<Array<Any?>>()
        while (dbfReader.hasNext()) {
            @Suppress("UNCHECKED_CAST")
            rows.add(dbfReader.readEntry() as Array<Any?>)
        }
        ShapefileReaderSupport.log.debug { "DBF 속성 로드 완료: 필드=${fieldCount}, 행=${rows.size}" }
        attributeDefs to rows
    }
}
