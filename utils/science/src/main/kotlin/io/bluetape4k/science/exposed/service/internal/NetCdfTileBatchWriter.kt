package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.science.exposed.NetCdfException
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types

/** 한 Exposed transaction이 소유하는 현재 JDBC connection으로 tile 행을 기록합니다. */
internal fun interface TileBatchWriter {
    fun write(connection: Connection, rows: List<TileRow>): BatchWriteResult
}

/** PostGIS geometry와 JSONB를 typed placeholder로 기록하는 JDBC 구현입니다. */
internal class JdbcTileBatchWriter : TileBatchWriter {

    override fun write(connection: Connection, rows: List<TileRow>): BatchWriteResult {
        if (rows.isEmpty()) return BatchWriteResult(0, 0)
        if (rows.size.toLong() > MAX_BATCH_ROWS) {
            throw NetCdfException.ResourceLimitExceeded("batch-rows", MAX_BATCH_ROWS, rows.size.toLong())
        }
        val hasNullLocation = rows.any { it.longitude == null || it.latitude == null }
        val hasSpatialLocation = rows.any { it.longitude != null || it.latitude != null }
        if (hasNullLocation && hasSpatialLocation) {
            val first = rows.first()
            throw NetCdfException.UnsupportedCoordinateAxis(
                variableName = first.variableName,
                coordinateName = "location",
                reason = "mixed-nullability",
            )
        }
        val spatial = hasSpatialLocation
        val inserted = writeBatch(connection, rows, spatial)
        return BatchWriteResult(inserted = inserted, conflicts = rows.size - inserted)
    }

    private fun writeBatch(connection: Connection, rows: List<TileRow>, spatial: Boolean): Int {
        val sql = if (spatial) SPATIAL_SQL else NULL_LOCATION_SQL
        return connection.prepareStatement(sql).use { statement ->
            rows.forEach { row -> bind(statement, row, spatial) }
            val results = statement.executeBatch()
            if (results.size != rows.size || results.any { it == PreparedStatement.SUCCESS_NO_INFO || it !in 0..1 }) {
                // A driver that cannot report one result per row is not allowed to
                // silently turn an import into a false success. Audit the bounded
                // canonical keys first, then throw so the enclosing transaction rolls
                // the batch back even when the audit happens to find identical rows.
                auditConflicts(connection, rows, rows.indices.toList(), spatial)
                throw IllegalStateException("JDBC batch result is not fully auditable")
            }
            val inserted = countInserted(results, rows.size)
            val conflicts = results.indices.filter { results[it] == 0 }
            if (conflicts.isNotEmpty()) {
                auditConflicts(connection, rows, conflicts, spatial)
            }
            inserted
        }
    }

    private fun bind(statement: PreparedStatement, row: TileRow, spatial: Boolean) {
        var index = 1
        statement.setLong(index++, row.fileId)
        statement.setString(index++, row.variableName)
        if (spatial) {
            statement.setDouble(index++, checkNotNull(row.longitude))
            statement.setDouble(index++, checkNotNull(row.latitude))
        }
        statement.setInt(index++, row.timeIdx)
        statement.setInt(index++, row.levelIdx)
        statement.setDouble(index++, row.value)
        row.attrsJson?.let { statement.setString(index, it) }
            ?: statement.setNull(index, Types.VARCHAR)
        statement.addBatch()
    }

    private fun countInserted(results: IntArray, rowCount: Int): Int {
        if (results.isEmpty()) return 0
        if (results.size != rowCount) {
            throw IllegalStateException("JDBC batch returned an unexpected result count")
        }
        var inserted = 0
        results.forEach { result ->
            if (result !in 0..1) {
                throw IllegalStateException("JDBC batch returned an unsupported update count")
            }
            inserted += result
        }
        return inserted
    }

    private fun auditConflicts(
        connection: Connection,
        rows: List<TileRow>,
        conflictIndices: List<Int>,
        spatial: Boolean,
    ) {
        val values = conflictIndices.joinToString(",") {
            "(?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)"
        }
        val sql = """
            WITH incoming(ordinal, file_id, variable_name, time_idx, level_idx,
                          longitude, latitude, value, attrs) AS (VALUES $values)
            SELECT i.ordinal,
                   COALESCE(
                       n.value = i.value
                       AND n.attrs IS NOT DISTINCT FROM i.attrs
                       AND (
                           (i.longitude IS NULL AND n.location IS NULL)
                           OR (i.longitude IS NOT NULL
                               AND ST_X(n.location) = i.longitude
                               AND ST_Y(n.location) = i.latitude)
                       ),
                       FALSE
                   ) AS matches
            FROM incoming i
            LEFT JOIN netcdf_grid_values n
              ON n.file_id = i.file_id
             AND n.variable_name = i.variable_name
             AND n.time_idx = i.time_idx
             AND n.level_idx = i.level_idx
             AND ${if (spatial) {
            "i.longitude IS NOT NULL AND n.location IS NOT NULL " +
                "AND ST_X(n.location) = i.longitude AND ST_Y(n.location) = i.latitude"
        } else {
            "i.longitude IS NULL AND n.location IS NULL"
        }}
            ORDER BY i.ordinal
        """.trimIndent()
        connection.prepareStatement(sql).use { statement ->
            var parameter = 1
            conflictIndices.forEach { index ->
                val row = rows[index]
                statement.setInt(parameter++, index)
                statement.setLong(parameter++, row.fileId)
                statement.setString(parameter++, row.variableName)
                statement.setInt(parameter++, row.timeIdx)
                statement.setInt(parameter++, row.levelIdx)
                row.longitude?.let { statement.setDouble(parameter, it) }
                    ?: statement.setNull(parameter, Types.DOUBLE)
                parameter++
                row.latitude?.let { statement.setDouble(parameter, it) }
                    ?: statement.setNull(parameter, Types.DOUBLE)
                parameter++
                statement.setDouble(parameter++, row.value)
                row.attrsJson?.let { statement.setString(parameter, it) }
                    ?: statement.setNull(parameter, Types.VARCHAR)
                parameter++
            }
            statement.executeQuery().use { resultSet ->
                val audited = HashSet<Int>(conflictIndices.size)
                while (resultSet.next()) {
                    val ordinal = resultSet.getInt("ordinal")
                    audited += ordinal
                    if (!resultSet.getBoolean("matches")) {
                        val row = rows[ordinal]
                        throw NetCdfException.DuplicateCoordinate(
                            fileId = row.fileId,
                            variableName = row.variableName,
                            timeIdx = row.timeIdx,
                            levelIdx = row.levelIdx,
                            longitude = row.longitude ?: 0.0,
                            latitude = row.latitude ?: 0.0,
                        )
                    }
                }
                if (audited.size != conflictIndices.size) {
                    throw IllegalStateException("JDBC conflict row was not found for audit")
                }
            }
        }
    }

    private companion object {
        const val NULL_LOCATION_SQL = """
            INSERT INTO netcdf_grid_values
                (file_id, variable_name, location, time_idx, level_idx, value, attrs)
            VALUES (?, ?, NULL::geometry, ?, ?, ?, ?::jsonb)
            ON CONFLICT DO NOTHING
        """
        const val SPATIAL_SQL = """
            INSERT INTO netcdf_grid_values
                (file_id, variable_name, location, time_idx, level_idx, value, attrs)
            VALUES (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?, ?, ?::jsonb)
            ON CONFLICT DO NOTHING
        """
    }
}
