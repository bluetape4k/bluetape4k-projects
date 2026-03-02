package io.bluetape4k.exposed.r2dbc

import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.r2dbc.Query

/**
 * `SELECT` 절을 명시 컬럼 목록 대신 `SELECT *`로 치환하는 R2DBC Query 구현입니다.
 *
 * ## 동작/계약
 * - `prepareSQL`에서 상위 Query SQL을 생성한 뒤 `" FROM "` 이전 문자열을 `"SELECT *"`로 교체합니다.
 * - WHERE/ORDER BY/LIMIT 등 나머지 절은 상위 Query 동작을 그대로 유지합니다.
 *
 * ```kotlin
 * val query = Tester.selectImplicitAll()
 * val sql = query.prepareSQL(QueryBuilder(prepared = true))
 * // sql.contains("SELECT * FROM") == true
 * ```
 */
class ImplicitQuery(set: FieldSet, where: Op<Boolean>?): Query(set, where) {
    override fun prepareSQL(builder: QueryBuilder): String {
        return super.prepareSQL(builder).replaceBefore(" FROM ", "SELECT *")
    }
}

/**
 * [FieldSet]에서 `SELECT *` 형태의 Query를 생성합니다.
 *
 * ## 동작/계약
 * - 현재 FieldSet과 where 조건(`null`)으로 [ImplicitQuery]를 생성합니다.
 * - 테스트 기준으로 `selectAll()`과 동일한 결과를 반환하면서 SQL의 SELECT 절만 `*`로 표현됩니다.
 *
 * ```kotlin
 * val implicit = Tester.selectImplicitAll().single()
 * val explicit = Tester.selectAll().single()
 * // implicit[Tester.amount] == explicit[Tester.amount]
 * ```
 */
fun FieldSet.selectImplicitAll(): Query = ImplicitQuery(this, null)
