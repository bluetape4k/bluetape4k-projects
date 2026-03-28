package io.bluetape4k.exposed.core.inet

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.net.InetAddress

/**
 * [InetAddress] 타입의 컬럼을 테이블에 등록한다.
 *
 * PostgreSQL에서는 `INET` 네이티브 타입을, 그 외 DB에서는 `VARCHAR(45)`를 사용한다.
 *
 * @param name 컬럼 이름
 * @return [InetAddress] 타입의 [Column]
 */
fun Table.inetAddress(name: String): Column<InetAddress> =
    registerColumn(name, InetAddressColumnType())

/**
 * CIDR 표기법 문자열을 저장하는 컬럼을 테이블에 등록한다.
 *
 * PostgreSQL에서는 `CIDR` 네이티브 타입을, 그 외 DB에서는 `VARCHAR(50)`를 사용한다.
 *
 * @param name 컬럼 이름
 * @return [String] 타입의 [Column]
 */
fun Table.cidr(name: String): Column<String> =
    registerColumn(name, CidrColumnType())

/**
 * PostgreSQL `<<` 연산자를 나타내는 SQL 표현식.
 *
 * INET 값이 CIDR 네트워크에 포함되는지 확인한다.
 * PostgreSQL 전용이며 다른 dialect에서는 사용할 수 없다.
 *
 * SQL: `left_expr << right_expr`
 */
class InetContainedByOp(
    expr1: Expression<*>,
    expr2: Expression<*>,
): ComparisonOp(expr1, expr2, "<<")

/**
 * 이 INET 컬럼 값이 지정한 CIDR [cidr] 네트워크에 포함되는지 확인하는 `<<` 연산자.
 *
 * **PostgreSQL 전용**: 다른 dialect에서 호출하면 [IllegalStateException]이 발생한다.
 *
 * @param cidr CIDR 표기법 문자열을 담은 [Expression]
 * @return `<<` 연산 결과를 나타내는 [Op]<[Boolean]>
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<InetAddress>.isContainedBy(cidr: Expression<String>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "isContainedBy (<<) 는 PostgreSQL dialect 에서만 지원됩니다."
    }
    return InetContainedByOp(this, cidr)
}
