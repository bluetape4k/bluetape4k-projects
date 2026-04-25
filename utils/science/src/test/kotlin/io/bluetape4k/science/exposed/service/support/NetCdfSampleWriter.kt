package io.bluetape4k.science.exposed.service.support

import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter
import java.nio.file.Path

/**
 * 테스트용 NetCDF 파일을 동적 생성하는 헬퍼.
 *
 * `ucar.nc2.write.NetcdfFormatWriter` 기반. NetCDF-3 (CDM-1) 포맷.
 *
 * 격자 기본값: timeN=2, levelN=2, latN=3, lonN=4, lat=[0.0, 45.5, 89.9], lon=[-180.0, -90.0, 0.0, 90.0].
 *
 * @param path                  출력 파일 경로 (`@TempDir` 권장)
 * @param rank                  변수 rank (1/2/3/4)
 * @param withLatAxis           lat 축 생성 여부 (false 면 `MissingCoordinate` 테스트용)
 * @param withLevelAxisByName   true 면 AxisType 없이 이름만 `lev` 로 생성 — fallback 검증
 * @param withFillValue         `_FillValue` 속성 추가 + 일부 셀에 fill value 주입
 * @param sourceCrs             grid_mapping 속성 EPSG (`"EPSG:4326"`/`"EPSG:3857"`/`"EPSG:32633"` 등)
 * @param nonStandardDimOrder   true 면 (lat, lon, time) 같은 비표준 dim order 로 생성 — Codex C5 검증
 * @return 생성된 파일 경로
 */
internal object NetCdfSampleWriter {

    const val DEFAULT_TIME_N: Int = 2
    const val DEFAULT_LEVEL_N: Int = 2
    const val DEFAULT_LAT_N: Int = 3
    const val DEFAULT_LON_N: Int = 4

    val DEFAULT_LAT_VALUES: DoubleArray = doubleArrayOf(0.0, 45.5, 89.9)
    val DEFAULT_LON_VALUES: DoubleArray = doubleArrayOf(-180.0, -90.0, 0.0, 90.0)

    const val FILL_VALUE: Double = -9999.0

    fun writeSample(
        path: Path,
        rank: Int,
        withLatAxis: Boolean = true,
        withLevelAxisByName: Boolean = false,
        withFillValue: Boolean = false,
        sourceCrs: String = "EPSG:4326",
        nonStandardDimOrder: Boolean = false,
    ): Path {
        require(rank in 1..4) { "rank must be 1..4: $rank" }

        val builder = NetcdfFormatWriter.createNewNetcdf3(path.toAbsolutePath().toString())

        // dimensions
        val timeDim = builder.addDimension("time", DEFAULT_TIME_N)
        val levelDim = if (rank == 4) {
            builder.addDimension(
                if (withLevelAxisByName) "lev" else "level",
                DEFAULT_LEVEL_N,
            )
        } else null
        val latDim = if (withLatAxis) builder.addDimension("lat", DEFAULT_LAT_N) else null
        val lonDim = builder.addDimension("lon", DEFAULT_LON_N)

        // coordinate variables
        builder.addVariable("time", DataType.DOUBLE, listOf(timeDim))
            .addAttribute(Attribute("units", "hours since 2024-01-01"))
            .addAttribute(Attribute("axis", "T"))

        if (rank == 4 && levelDim != null) {
            val levelVar = builder.addVariable(levelDim.shortName, DataType.DOUBLE, listOf(levelDim))
                .addAttribute(Attribute("units", "Pa"))
            if (!withLevelAxisByName) {
                levelVar.addAttribute(Attribute("axis", "Z"))
                levelVar.addAttribute(Attribute("positive", "down"))
            }
        }

        if (latDim != null) {
            builder.addVariable("lat", DataType.DOUBLE, listOf(latDim))
                .addAttribute(Attribute("units", "degrees_north"))
                .addAttribute(Attribute("axis", "Y"))
        }

        builder.addVariable("lon", DataType.DOUBLE, listOf(lonDim))
            .addAttribute(Attribute("units", "degrees_east"))
            .addAttribute(Attribute("axis", "X"))

        // grid_mapping for non-WGS84
        if (sourceCrs != "EPSG:4326") {
            val epsgCode = sourceCrs.removePrefix("EPSG:").toInt()
            builder.addVariable("crs", DataType.INT, emptyList())
                .addAttribute(Attribute("grid_mapping_name", "transverse_mercator"))
                .addAttribute(Attribute("epsg_code", epsgCode))
        }

        // data variable — temperature
        val dataDims = when (rank) {
            1 -> listOf(timeDim)
            2 -> if (latDim != null) listOf(latDim, lonDim) else listOf(lonDim, lonDim)
            3 -> if (nonStandardDimOrder && latDim != null) listOf(latDim, lonDim, timeDim)
            else if (latDim != null) listOf(timeDim, latDim, lonDim)
            else listOf(timeDim, lonDim)
            4 -> if (nonStandardDimOrder && latDim != null && levelDim != null)
                listOf(latDim, lonDim, timeDim, levelDim)
            else if (latDim != null && levelDim != null)
                listOf(timeDim, levelDim, latDim, lonDim)
            else listOf(timeDim, lonDim)
            else -> error("unreachable")
        }
        val dataVar = builder.addVariable("temperature", DataType.DOUBLE, dataDims)
            .addAttribute(Attribute("units", "K"))
            .addAttribute(Attribute("long_name", "Air Temperature"))

        if (withFillValue) {
            dataVar.addAttribute(Attribute("_FillValue", FILL_VALUE))
        }
        if (sourceCrs != "EPSG:4326") {
            dataVar.addAttribute(Attribute("grid_mapping", "crs"))
        }

        // build + write
        builder.build().use { writer ->
            // coord values
            writer.write(writer.findVariable("time"), Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_TIME_N), DoubleArray(DEFAULT_TIME_N) { it.toDouble() }))
            if (rank == 4 && levelDim != null) {
                writer.write(
                    writer.findVariable(levelDim.shortName),
                    Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LEVEL_N), DoubleArray(DEFAULT_LEVEL_N) { (it + 1) * 100.0 }),
                )
            }
            if (latDim != null) {
                writer.write(writer.findVariable("lat"), Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LAT_N), DEFAULT_LAT_VALUES))
            }
            writer.write(writer.findVariable("lon"), Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LON_N), DEFAULT_LON_VALUES))

            // temperature data
            val v = writer.findVariable("temperature")
            val totalSize = dataDims.fold(1) { acc, d -> acc * d.length }
            val arr = DoubleArray(totalSize) { i ->
                if (withFillValue && i % 5 == 0) FILL_VALUE else 273.15 + i.toDouble()
            }
            val shape = dataDims.map { it.length }.toIntArray()
            writer.write(v, Array.factory(DataType.DOUBLE, shape, arr))
        }
        return path
    }
}
