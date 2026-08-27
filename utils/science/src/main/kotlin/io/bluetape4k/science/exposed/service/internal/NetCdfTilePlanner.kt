package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.science.exposed.NetCdfException

/** 좌표 격자를 bounded row-major tile sequence로 나눕니다. */
internal data class NetCdfTile(
    val rowOrigin: Int,
    val columnOrigin: Int,
    val rowCount: Int,
    val columnCount: Int,
)

internal object NetCdfTilePlanner {

    /**
     * 입력 순서와 동일한 row-major 순서로 tile을 반환합니다.
     * 한 tile은 항상 [MAX_TILE_CELLS] 이하이며, 빈 격자는 허용하지 않습니다.
     */
    fun plan(rows: Int, columns: Int): List<NetCdfTile> {
        if (rows <= 0 || columns <= 0) {
            throw NetCdfException.UnsupportedCoordinateAxis(
                variableName = "grid",
                coordinateName = null,
                reason = "empty-grid-dimension:${maxOf(rows, columns)}",
            )
        }
        val columnTileSize = minOf(columns.toLong(), MAX_TILE_CELLS).toInt()
        val rowTileSize = maxOf(1L, MAX_TILE_CELLS / columnTileSize).toInt()
        val tiles = ArrayList<NetCdfTile>()
        var rowOrigin = 0
        while (rowOrigin < rows) {
            val rowCount = minOf(rowTileSize, rows - rowOrigin)
            var columnOrigin = 0
            while (columnOrigin < columns) {
                val columnCount = minOf(columnTileSize, columns - columnOrigin)
                tiles += NetCdfTile(rowOrigin, columnOrigin, rowCount, columnCount)
                columnOrigin += columnCount
            }
            rowOrigin += rowCount
        }
        return tiles
    }
}
