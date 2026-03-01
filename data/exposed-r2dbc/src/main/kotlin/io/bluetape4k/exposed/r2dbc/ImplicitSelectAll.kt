package io.bluetape4k.exposed.r2dbc

import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.r2dbc.Query

/**
 * `SELECT` 절에 모든 컬럼을 명시적으로 표현하지 않고, `*` 를 사용하여 모든 컬럼을 조회하는 쿼리를 생성합니다.
 *
 * ```sql
 * SELECT column_1, column_2, ... FROM
 * ```
 * -->
 * ```sql
 * SELECT * FROM
 * ```
 */
class ImplicitQuery(set: FieldSet, where: Op<Boolean>?): Query(set, where) {
    override fun prepareSQL(builder: QueryBuilder): String {
        return super.prepareSQL(builder).replaceBefore(" FROM ", "SELECT *")
    }
}

/**
 * `SELECT` 절에 모든 컬럼을 명시적으로 표현하지 않고, `*` 를 사용하여 모든 컬럼을 조회하는 쿼리를 생성합니다.
 *
 * ```kotlin
 * TestTable.selectImplicitAll().where { TestTable.amount greater 100 }
 * ```
 * 을 수행하면, 명시적인 컬럼명이 아닌 `*` 를 사용하여 모든 컬럼을 조회하는 쿼리가 생성됩니다.
 *
 * ```sql
 * SELECT * FROM TESTER WHERE TESTER.AMOUNT > 100
 * ```
 */
fun FieldSet.selectImplicitAll(): Query = ImplicitQuery(this, null)
