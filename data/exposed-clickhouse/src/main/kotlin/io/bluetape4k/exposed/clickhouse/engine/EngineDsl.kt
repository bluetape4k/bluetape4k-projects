package io.bluetape4k.exposed.clickhouse.engine

/**
 * [MergeTree] 엔진 DSL 빌더.
 *
 * [mergeTree] 함수와 함께 사용하며, ORDER BY, PARTITION BY, PRIMARY KEY, SAMPLE BY, SETTINGS 절을 설정합니다.
 */
class MergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private var primaryKeyColumns: List<String> = emptyList()
    private var sampleByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    /** ORDER BY 컬럼을 지정합니다. 최소 1개 이상 필수입니다. */
    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    /** PARTITION BY 절을 지정합니다. (선택) */
    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    /** PRIMARY KEY 컬럼을 지정합니다. 미지정 시 ORDER BY와 동일하게 처리됩니다. */
    fun primaryKey(vararg columns: String) {
        primaryKeyColumns = columns.toList()
    }

    /** SAMPLE BY 절을 지정합니다. (선택) */
    fun sampleBy(expr: String) {
        sampleByExpr = expr
    }

    /** SETTINGS 키-값 쌍을 추가합니다. (선택) */
    fun settings(vararg pairs: Pair<String, String>) {
        settingsMap.putAll(pairs)
    }

    internal fun build(): MergeTree = MergeTree(
        orderBy = orderByColumns,
        partitionBy = partitionByExpr,
        primaryKeyColumns = primaryKeyColumns,
        sampleBy = sampleByExpr,
        settings = settingsMap.toMap(),
    )
}

/**
 * [MergeTree] 엔진을 DSL로 생성합니다.
 *
 * ```kotlin
 * val engine = mergeTree {
 *     orderBy("event_date", "user_id")
 *     partitionBy("toYYYYMM(event_date)")
 * }
 * ```
 */
fun mergeTree(block: MergeTreeBuilder.() -> Unit): MergeTree =
    MergeTreeBuilder().apply(block).build()

/**
 * [ReplacingMergeTree] 엔진 DSL 빌더.
 *
 * [replacingMergeTree] 함수와 함께 사용하며, ORDER BY, 버전 컬럼, PARTITION BY, SETTINGS 절을 설정합니다.
 */
class ReplacingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var versionCol: String? = null
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    /** ORDER BY 컬럼을 지정합니다. 최소 1개 이상 필수입니다. */
    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    /** 버전 컬럼을 지정합니다. 중복 행 제거 시 최신 버전을 유지합니다. (선택) */
    fun versionColumn(col: String) {
        versionCol = col
    }

    /** PARTITION BY 절을 지정합니다. (선택) */
    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    /** SETTINGS 키-값 쌍을 추가합니다. (선택) */
    fun settings(vararg pairs: Pair<String, String>) {
        settingsMap.putAll(pairs)
    }

    internal fun build(): ReplacingMergeTree = ReplacingMergeTree(
        orderBy = orderByColumns,
        versionColumn = versionCol,
        partitionBy = partitionByExpr,
        settings = settingsMap.toMap(),
    )
}

/**
 * [ReplacingMergeTree] 엔진을 DSL로 생성합니다.
 *
 * ```kotlin
 * val engine = replacingMergeTree {
 *     orderBy("user_id", "event_date")
 *     versionColumn("version")
 * }
 * ```
 */
fun replacingMergeTree(block: ReplacingMergeTreeBuilder.() -> Unit): ReplacingMergeTree =
    ReplacingMergeTreeBuilder().apply(block).build()

/**
 * [SummingMergeTree] 엔진 DSL 빌더.
 *
 * [summingMergeTree] 함수와 함께 사용하며, ORDER BY, 합산 컬럼, PARTITION BY, SETTINGS 절을 설정합니다.
 */
class SummingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var sumCols: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    /** ORDER BY 컬럼을 지정합니다. 최소 1개 이상 필수입니다. */
    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    /** 합산할 컬럼 목록을 지정합니다. 미지정 시 숫자형 컬럼 전체를 합산합니다. */
    fun sumColumns(vararg columns: String) {
        sumCols = columns.toList()
    }

    /** PARTITION BY 절을 지정합니다. (선택) */
    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    /** SETTINGS 키-값 쌍을 추가합니다. (선택) */
    fun settings(vararg pairs: Pair<String, String>) {
        settingsMap.putAll(pairs)
    }

    internal fun build(): SummingMergeTree = SummingMergeTree(
        orderBy = orderByColumns,
        sumColumns = sumCols,
        partitionBy = partitionByExpr,
        settings = settingsMap.toMap(),
    )
}

/**
 * [SummingMergeTree] 엔진을 DSL로 생성합니다.
 *
 * ```kotlin
 * val engine = summingMergeTree {
 *     orderBy("product_id", "region")
 *     sumColumns("quantity", "amount")
 * }
 * ```
 */
fun summingMergeTree(block: SummingMergeTreeBuilder.() -> Unit): SummingMergeTree =
    SummingMergeTreeBuilder().apply(block).build()

/**
 * [AggregatingMergeTree] 엔진 DSL 빌더.
 *
 * [aggregatingMergeTree] 함수와 함께 사용하며, ORDER BY, PARTITION BY, SETTINGS 절을 설정합니다.
 */
class AggregatingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    /** ORDER BY 컬럼을 지정합니다. 최소 1개 이상 필수입니다. */
    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    /** PARTITION BY 절을 지정합니다. (선택) */
    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    /** SETTINGS 키-값 쌍을 추가합니다. (선택) */
    fun settings(vararg pairs: Pair<String, String>) {
        settingsMap.putAll(pairs)
    }

    internal fun build(): AggregatingMergeTree = AggregatingMergeTree(
        orderBy = orderByColumns,
        partitionBy = partitionByExpr,
        settings = settingsMap.toMap(),
    )
}

/**
 * [AggregatingMergeTree] 엔진을 DSL로 생성합니다.
 *
 * ```kotlin
 * val engine = aggregatingMergeTree {
 *     orderBy("user_id", "event_date")
 *     partitionBy("toYYYYMM(event_date)")
 * }
 * ```
 */
fun aggregatingMergeTree(block: AggregatingMergeTreeBuilder.() -> Unit): AggregatingMergeTree =
    AggregatingMergeTreeBuilder().apply(block).build()
