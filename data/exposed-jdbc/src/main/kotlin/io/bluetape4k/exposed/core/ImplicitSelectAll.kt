package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Query

/**
 * SELECT 절을 명시 컬럼 목록 대신 `SELECT *`로 바꿔 생성하는 Query 구현입니다.
 *
 * ## 동작/계약
 * - `prepareSQL`에서 상위 Query SQL을 생성한 뒤 `" FROM "` 이전 문자열을 `"SELECT *"`로 치환합니다.
 * - WHERE/ORDER BY/LIMIT 등 나머지 절은 상위 Query 동작을 그대로 유지합니다.
 *
 * ```kotlin
 * val query = ImplicitQuery(table.selectAll().set, null)
 * // query.prepareSQL(builder).startsWith("SELECT *")
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
 * - 현재 FieldSet과 where 조건(`null`)로 [ImplicitQuery] 인스턴스를 새로 만듭니다.
 * - 반환된 Query 객체는 호출자가 추가 where/order/limit를 체이닝해서 사용합니다.
 *
 * ```kotlin
 * val query = TestTable.selectImplicitAll().where { TestTable.amount greater 100 }
 * // query.prepareSQL(builder).contains("SELECT * FROM")
 * ```
 */
fun FieldSet.selectImplicitAll(): Query = ImplicitQuery(this, null)
