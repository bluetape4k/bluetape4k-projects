package io.bluetape4k.exposed.core.inet

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.net.InetAddress

/**
 * [InetAddress]를 DB 컬럼에 저장하는 컬럼 타입.
 *
 * PostgreSQL에서는 네이티브 `INET` 타입을 사용하고,
 * 그 외 DB(H2 등)에서는 `VARCHAR(45)`로 fallback한다.
 *
 * - IPv4: 최대 15자 (`255.255.255.255`)
 * - IPv6: 최대 45자 (`0000:0000:0000:0000:0000:ffff:255.255.255.255`)
 *
 * ```kotlin
 * object Hosts : Table("hosts") {
 *     val ip = inetAddress("ip")
 * }
 * // Hosts.ip.columnType is InetAddressColumnType
 * ```
 */
class InetAddressColumnType : ColumnType<InetAddress>() {

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return PostgreSQL이면 `"INET"`, 그 외는 `"VARCHAR(45)"`
     */
    override fun sqlType(): String = when (currentDialect) {
        is PostgreSQLDialect -> "INET"
        else -> "VARCHAR(45)"
    }

    /**
     * [InetAddress] 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 [InetAddress] 객체
     * @return IP 주소 문자열 (예: `"192.168.1.1"`)
     */
    override fun notNullValueToDB(value: InetAddress): Any = value.hostAddress

    /**
     * DB에서 읽은 값을 [InetAddress]로 변환한다.
     *
     * @param value DB에서 읽은 값 (문자열 또는 기타 타입)
     * @return 파싱된 [InetAddress] 객체
     */
    override fun valueFromDB(value: Any): InetAddress = InetAddress.getByName(value.toString())

    /**
     * PostgreSQL에서 `VARCHAR`를 `INET` 컬럼에 바인딩할 때 타입 캐스트를 추가한다.
     *
     * @return PostgreSQL이면 `"?::inet"`, 그 외는 `"?"`
     */
    override fun parameterMarker(value: InetAddress?): String = when (currentDialect) {
        is PostgreSQLDialect -> "?::inet"
        else -> "?"
    }
}

/**
 * CIDR 표기법 문자열(`"192.168.0.0/24"` 등)을 DB 컬럼에 저장하는 컬럼 타입.
 *
 * PostgreSQL에서는 네이티브 `CIDR` 타입을 사용하고,
 * 그 외 DB(H2 등)에서는 `VARCHAR(50)`으로 fallback한다.
 *
 * CIDR 형식: `<네트워크 주소>/<프리픽스 길이>` (예: `"10.0.0.0/8"`, `"2001:db8::/32"`)
 *
 * ```kotlin
 * object Networks : Table("networks") {
 *     val network = cidr("network")
 * }
 * // Networks.network.columnType is CidrColumnType
 * ```
 */
class CidrColumnType : ColumnType<String>() {

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return PostgreSQL이면 `"CIDR"`, 그 외는 `"VARCHAR(50)"`
     */
    override fun sqlType(): String = when (currentDialect) {
        is PostgreSQLDialect -> "CIDR"
        else -> "VARCHAR(50)"
    }

    /**
     * CIDR 문자열 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 CIDR 문자열
     * @return CIDR 문자열 그대로 반환
     */
    override fun notNullValueToDB(value: String): Any = value

    /**
     * DB에서 읽은 값을 CIDR 문자열로 변환한다.
     *
     * @param value DB에서 읽은 값
     * @return CIDR 문자열
     */
    override fun valueFromDB(value: Any): String = value.toString()

    /**
     * PostgreSQL에서 `VARCHAR`를 `CIDR` 컬럼에 바인딩할 때 타입 캐스트를 추가한다.
     *
     * @return PostgreSQL이면 `"?::cidr"`, 그 외는 `"?"`
     */
    override fun parameterMarker(value: String?): String = when (currentDialect) {
        is PostgreSQLDialect -> "?::cidr"
        else -> "?"
    }
}
