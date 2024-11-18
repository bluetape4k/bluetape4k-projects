package io.bluetape4k.hibernate.converters

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 문자열을 압축해서 문자열로 저장하는 JPA Converter 의 추상 클래스입니다.
 *
 * ```
 * @Entity
 * class Purchase {
 *    @Id
 *    @GeneratedValue
 *    var id:Long? = null
 *
 *    @Convert(converter=DeflateStringConverter::class)
 *    var description: String? = null
 * }
 */
@Converter
abstract class AbstractCompressedStringConverter(
    private val compressor: Compressor,
): AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.run { compressor.compress(this) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.run { compressor.decompress(this) }
    }

}

/**
 * 문자열을 BZip2 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class BZip2StringConverter: AbstractCompressedStringConverter(Compressors.BZip2)

/**
 * 문자열을 Deflate 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class DeflateStringConverter: AbstractCompressedStringConverter(Compressors.Deflate)

/**
 * 문자열을 GZip 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class GZipStringConverter: AbstractCompressedStringConverter(Compressors.GZip)

/**
 * 문자열을 LZ4 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class LZ4StringConverter: AbstractCompressedStringConverter(Compressors.LZ4)

/**
 * 문자열을 Snappy 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class SnappyStringConverter: AbstractCompressedStringConverter(Compressors.Snappy)

/**
 * 문자열을 Zstd 알고리즘으로 압축해서 저장하는 JPA Converter 입니다.
 */
@Converter
class ZstdStringConverter: AbstractCompressedStringConverter(Compressors.Zstd)
