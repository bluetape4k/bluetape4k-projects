package io.bluetape4k.exposed.clickhouse.engine

/**
 * MergeTree 엔진 DSL 빌더.
 */
class MergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private var primaryKeyColumns: List<String> = emptyList()
    private var sampleByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    fun primaryKey(vararg columns: String) {
        primaryKeyColumns = columns.toList()
    }

    fun sampleBy(expr: String) {
        sampleByExpr = expr
    }

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

/** MergeTree 엔진 DSL */
fun mergeTree(block: MergeTreeBuilder.() -> Unit): MergeTree =
    MergeTreeBuilder().apply(block).build()

/** ReplacingMergeTree 엔진 DSL 빌더. */
class ReplacingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var versionCol: String? = null
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    fun versionColumn(col: String) {
        versionCol = col
    }

    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

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

/** ReplacingMergeTree 엔진 DSL */
fun replacingMergeTree(block: ReplacingMergeTreeBuilder.() -> Unit): ReplacingMergeTree =
    ReplacingMergeTreeBuilder().apply(block).build()

/** SummingMergeTree 엔진 DSL 빌더. */
class SummingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var sumCols: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    fun sumColumns(vararg columns: String) {
        sumCols = columns.toList()
    }

    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

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

/** SummingMergeTree 엔진 DSL */
fun summingMergeTree(block: SummingMergeTreeBuilder.() -> Unit): SummingMergeTree =
    SummingMergeTreeBuilder().apply(block).build()

/** AggregatingMergeTree 엔진 DSL 빌더. */
class AggregatingMergeTreeBuilder {
    private var orderByColumns: List<String> = emptyList()
    private var partitionByExpr: String? = null
    private val settingsMap: MutableMap<String, String> = mutableMapOf()

    fun orderBy(vararg columns: String) {
        orderByColumns = columns.toList()
    }

    fun partitionBy(expr: String) {
        partitionByExpr = expr
    }

    fun settings(vararg pairs: Pair<String, String>) {
        settingsMap.putAll(pairs)
    }

    internal fun build(): AggregatingMergeTree = AggregatingMergeTree(
        orderBy = orderByColumns,
        partitionBy = partitionByExpr,
        settings = settingsMap.toMap(),
    )
}

/** AggregatingMergeTree 엔진 DSL */
fun aggregatingMergeTree(block: AggregatingMergeTreeBuilder.() -> Unit): AggregatingMergeTree =
    AggregatingMergeTreeBuilder().apply(block).build()
