package io.bluetape4k.avro

import org.apache.avro.file.CodecFactory

/**
 * Avro 직렬화 시 사용할 [CodecFactory]의 기본 인스턴스를 제공합니다.
 *
 * Zstandard 압축 레벨 3을 사용하여, 압축/복원 속도와 압축률의 균형을 제공합니다.
 * 일반적인 용도로 적합합니다.
 *
 * @see [CodecFactory]
 * @see [CodecFactory.zstandardCodec]
 * @see [FAST_CODEC_FACTORY]
 * @see [ARCHIVE_CODEC_FACTORY]
 */
val DEFAULT_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(3, true, true)
}

/**
 * Avro 직렬화 시 빠른 압축/복원 속도를 위한 [CodecFactory] 인스턴스입니다.
 *
 * Zstandard 압축 레벨 -1 을 사용하며, LZ4나 Snappy와 유사한 수준의 빠른 속도를 제공합니다.
 * 실시간 처리나 낮은 지연시간이 요구되는 환경에 적합합니다.
 *
 * @see [DEFAULT_CODEC_FACTORY]
 * @see [ARCHIVE_CODEC_FACTORY]
 */
val FAST_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(-1, true, true)
}

/**
 * Avro 직렬화 시 높은 압축률을 위한 [CodecFactory] 인스턴스입니다.
 *
 * Zstandard 압축 레벨 9를 사용하여, 압축 속도는 느리지만 최대한의 압축률을 제공합니다.
 * 장기 보관용 데이터나 네트워크 대역폭이 제한된 환경에 적합합니다.
 *
 * @see [DEFAULT_CODEC_FACTORY]
 * @see [FAST_CODEC_FACTORY]
 */
val ARCHIVE_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(9, true, true)
}

/**
 * 압축을 사용하지 않는 [CodecFactory] 인스턴스입니다.
 *
 * 압축 오버헤드 없이 가장 빠른 직렬화/역직렬화 속도를 제공합니다.
 * 데이터 크기보다 처리 속도가 중요한 경우에 사용합니다.
 *
 * @see [DEFAULT_CODEC_FACTORY]
 */
val NULL_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.nullCodec()
}

/**
 * Deflate 알고리즘을 사용하는 [CodecFactory] 인스턴스입니다.
 *
 * Deflate 압축 레벨 6 (기본값)을 사용합니다.
 * 호환성이 중요한 환경에서 널리 사용되는 표준 압축 방식입니다.
 *
 * @see [DEFAULT_CODEC_FACTORY]
 */
val DEFLATE_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.deflateCodec(6)
}

/**
 * Snappy 압축 알고리즘을 사용하는 [CodecFactory] 인스턴스입니다.
 *
 * Google에서 개발한 Snappy는 빠른 압축/복원 속도를 제공합니다.
 * Hadoop, Kafka 등에서 널리 사용됩니다.
 *
 * @see [DEFAULT_CODEC_FACTORY]
 * @see [FAST_CODEC_FACTORY]
 */
val SNAPPY_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.snappyCodec()
}

/**
 * 코덱 이름을 기반으로 [CodecFactory] 인스턴스를 생성합니다.
 *
 * 지원하는 코덱 이름:
 * - `"null"`, `"none"`: 압축 없음
 * - `"deflate"`: Deflate 압축 (레벨 6)
 * - `"snappy"`: Snappy 압축
 * - `"zstd"`, `"zstandard"`: Zstandard 압축 (레벨 3)
 * - `"zstd-fast"`: Zstandard 빠른 압축 (레벨 -1)
 * - `"bzip2"`: Bzip2 압축
 * - `"xz"`: XZ 압축
 *
 * ```
 * val codec = codecFactoryOf("snappy")
 * val serializer = DefaultAvroSpecificRecordSerializer(codec)
 * ```
 *
 * @param name 코덱 이름 (대소문자 무시)
 * @return 해당 코덱의 [CodecFactory] 인스턴스
 * @throws IllegalArgumentException 지원하지 않는 코덱 이름인 경우
 */
fun codecFactoryOf(name: String): CodecFactory {
    return when (name.lowercase().trim()) {
        "null", "none"      -> NULL_CODEC_FACTORY
        "deflate"           -> DEFLATE_CODEC_FACTORY
        "snappy"            -> SNAPPY_CODEC_FACTORY
        "zstd", "zstandard" -> DEFAULT_CODEC_FACTORY
        "zstd-fast"         -> FAST_CODEC_FACTORY
        "bzip2"             -> CodecFactory.bzip2Codec()
        "xz"                -> CodecFactory.xzCodec(6)
        else                -> throw IllegalArgumentException("지원하지 않는 Avro 코덱입니다: $name")
    }
}
