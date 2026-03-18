package io.bluetape4k.spring.cassandra.domain.model

import com.datastax.oss.driver.api.core.data.TupleValue
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@Table
data class AllPossibleTypes(
    @field:PrimaryKey
    val id: String = "",

    var inet: InetAddress? = null,
    var uuid: UUID? = null,

    @field:CassandraType(type = CassandraType.Name.INT)
    var justNumber: java.lang.Number? = null,

    var boxedByte: java.lang.Byte? = null,
    var primitiveByte: Byte? = null,

    var boxedLong: java.lang.Long? = null,
    var primitiveLong: Long? = null,

    var boxedInteger: java.lang.Integer? = null,
    var primitiveInteger: Int? = null,

    var boxedFloat: java.lang.Float? = null,
    var primitiveFloat: Float? = null,

    var boxedDouble: java.lang.Double? = null,
    var primitiveDouble: Double? = null,

    var boxedBoolean: java.lang.Boolean? = null,
    var primitiveBoolean: Boolean? = null,

    var instant: Instant? = null,
    var date: LocalDate? = null,
    var time: LocalTime? = null,

    var timestamp: Date? = null,

    var bigDecimal: BigDecimal = BigDecimal.ZERO,
    var bigInteger: BigInteger = BigInteger.ZERO,
    var blob: ByteBuffer? = null,

    // NOTE: ByteArray는 지원하지 않습니다. -> ByteBuffer를 사용하세요
    // var bytes: ByteArray = ByteArray(0),

    var setOfString: MutableSet<String> = mutableSetOf(),
    var listOfString: MutableList<String> = mutableListOf(),

    var onEnum: Condition? = null,
    var setOfEnum: MutableSet<Condition> = mutableSetOf(),
    var listOfEnum: MutableList<Condition?> = mutableListOf(),

    @field:CassandraType(
        type = CassandraType.Name.TUPLE,
        typeArguments = [CassandraType.Name.VARCHAR, CassandraType.Name.BIGINT]
    )
    var tupleValue: TupleValue? = null,

    // supported by conversion
    var localDateTime: LocalDateTime? = null,
    var zoneId: ZoneId = ZoneId.systemDefault(),
): Serializable
