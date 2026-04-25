package io.bluetape4k.exposed.clickhouse.engine

import java.io.Serializable

/**
 * ClickHouse 테이블 엔진을 나타내는 sealed interface.
 * [toClause]는 CREATE TABLE DDL에 부착될 ENGINE 절을 반환합니다.
 */
sealed interface ClickHouseEngine: Serializable {
    /** DDL에 부착할 ENGINE 절 문자열 */
    fun toClause(): String
}

/** Memory 엔진 (테스트용, 서버 재시작 시 데이터 손실) */
data object Memory: ClickHouseEngine {
    private const val serialVersionUID: Long = 1L
    override fun toClause(): String = "ENGINE = Memory()"
}

/** TinyLog 엔진 (소규모 데이터, 컬럼 파일 저장) */
data object TinyLog: ClickHouseEngine {
    private const val serialVersionUID: Long = 1L
    override fun toClause(): String = "ENGINE = TinyLog()"
}

/** Log 엔진 (소규모 데이터, 빠른 쓰기) */
data object Log: ClickHouseEngine {
    private const val serialVersionUID: Long = 1L
    override fun toClause(): String = "ENGINE = Log()"
}

/**
 * MergeTree 계열 엔진 기본 클래스.
 * ClickHouse의 가장 일반적인 OLAP 엔진입니다.
 *
 * @param orderBy ORDER BY 컬럼 목록 (필수, 최소 1개)
 * @param partitionBy PARTITION BY 절 (선택)
 * @param primaryKeyColumns PRIMARY KEY 컬럼 목록 (선택, 미지정 시 ORDER BY와 동일)
 * @param sampleBy SAMPLE BY 절 (선택)
 * @param settings SETTINGS 키-값 목록 (선택)
 */
data class MergeTree(
    val orderBy: List<String>,
    val partitionBy: String? = null,
    val primaryKeyColumns: List<String> = emptyList(),
    val sampleBy: String? = null,
    val settings: Map<String, String> = emptyMap(),
): ClickHouseEngine {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        require(orderBy.isNotEmpty()) { "MergeTree requires at least one ORDER BY column" }
    }

    override fun toClause(): String = buildString {
        append("ENGINE = MergeTree()")
        append("\nORDER BY (${orderBy.joinToString(", ")})")
        partitionBy?.let { append("\nPARTITION BY $it") }
        if (primaryKeyColumns.isNotEmpty()) {
            append("\nPRIMARY KEY (${primaryKeyColumns.joinToString(", ")})")
        }
        sampleBy?.let { append("\nSAMPLE BY $it") }
        if (settings.isNotEmpty()) {
            append("\nSETTINGS ${settings.entries.joinToString(", ") { "${it.key} = ${it.value}" }}")
        }
    }
}

/**
 * ReplacingMergeTree 엔진 — 중복 행을 버전 기반으로 제거합니다.
 * @param versionColumn 버전 컬럼 이름 (선택, UInt/Date/DateTime 타입)
 */
data class ReplacingMergeTree(
    val orderBy: List<String>,
    val versionColumn: String? = null,
    val partitionBy: String? = null,
    val settings: Map<String, String> = emptyMap(),
): ClickHouseEngine {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        require(orderBy.isNotEmpty()) { "ReplacingMergeTree requires at least one ORDER BY column" }
    }

    override fun toClause(): String = buildString {
        val engineArgs = if (versionColumn != null) "($versionColumn)" else "()"
        append("ENGINE = ReplacingMergeTree$engineArgs")
        append("\nORDER BY (${orderBy.joinToString(", ")})")
        partitionBy?.let { append("\nPARTITION BY $it") }
        if (settings.isNotEmpty()) {
            append("\nSETTINGS ${settings.entries.joinToString(", ") { "${it.key} = ${it.value}" }}")
        }
    }
}

/**
 * SummingMergeTree 엔진 — 같은 키의 숫자 컬럼을 자동 합산합니다.
 * @param sumColumns 합산할 컬럼 목록 (선택, 미지정 시 숫자형 전체)
 */
data class SummingMergeTree(
    val orderBy: List<String>,
    val sumColumns: List<String> = emptyList(),
    val partitionBy: String? = null,
    val settings: Map<String, String> = emptyMap(),
): ClickHouseEngine {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        require(orderBy.isNotEmpty()) { "SummingMergeTree requires at least one ORDER BY column" }
    }

    override fun toClause(): String = buildString {
        val engineArgs = if (sumColumns.isNotEmpty()) "(${sumColumns.joinToString(", ")})" else "()"
        append("ENGINE = SummingMergeTree$engineArgs")
        append("\nORDER BY (${orderBy.joinToString(", ")})")
        partitionBy?.let { append("\nPARTITION BY $it") }
        if (settings.isNotEmpty()) {
            append("\nSETTINGS ${settings.entries.joinToString(", ") { "${it.key} = ${it.value}" }}")
        }
    }
}

/**
 * AggregatingMergeTree 엔진 — AggregateFunction 컬럼을 병합합니다.
 */
data class AggregatingMergeTree(
    val orderBy: List<String>,
    val partitionBy: String? = null,
    val settings: Map<String, String> = emptyMap(),
): ClickHouseEngine {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        require(orderBy.isNotEmpty()) { "AggregatingMergeTree requires at least one ORDER BY column" }
    }

    override fun toClause(): String = buildString {
        append("ENGINE = AggregatingMergeTree()")
        append("\nORDER BY (${orderBy.joinToString(", ")})")
        partitionBy?.let { append("\nPARTITION BY $it") }
        if (settings.isNotEmpty()) {
            append("\nSETTINGS ${settings.entries.joinToString(", ") { "${it.key} = ${it.value}" }}")
        }
    }
}
