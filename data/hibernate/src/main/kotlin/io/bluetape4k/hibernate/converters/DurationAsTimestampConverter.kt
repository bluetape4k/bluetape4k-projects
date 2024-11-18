package io.bluetape4k.hibernate.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.sql.Timestamp
import java.time.Duration

/**
 * Java Time [Duration]을 [Timestamp]로 변환해서 저장하는 Converter
 *
 * ```
 *
 * @Convert(converter=DurationAsTimestampConverter::class)
 * var duration:Duration? = null
 * ```
 */
@Converter
class DurationAsTimestampConverter: AttributeConverter<Duration?, Timestamp?> {

    override fun convertToDatabaseColumn(attribute: Duration?): Timestamp? {
        return attribute?.run { Timestamp(this.toMillis()) }
    }

    override fun convertToEntityAttribute(dbData: Timestamp?): Duration? {
        return dbData?.run { Duration.ofMillis(this.time) }
    }
}
