package io.bluetape4k.avro

import org.apache.avro.file.CodecFactory

/**
 * 균형형 Zstandard(레벨 3) 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 최초 접근 시 1회 생성되고 이후 동일 인스턴스를 재사용합니다.
 * - 압축률과 속도의 균형이 필요한 기본 경로에 적합합니다.
 *
 * ```kotlin
 * val same = DEFAULT_CODEC_FACTORY === DEFAULT_CODEC_FACTORY
 * // same == true
 * ```
 */
val DEFAULT_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(3, true, true)
}

/**
 * 빠른 처리 중심 Zstandard(레벨 -1) 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 낮은 압축 비용을 우선하는 경로에서 사용합니다.
 * - lazy 프로퍼티로 한 번 생성된 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val codec = FAST_CODEC_FACTORY
 * // codec.toString().contains("zstandard") == true
 * ```
 */
val FAST_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(-1, true, true)
}

/**
 * 높은 압축률 중심 Zstandard(레벨 9) 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 압축 속도보다 저장 공간 절감이 중요한 경로에 적합합니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val codec = ARCHIVE_CODEC_FACTORY
 * // codec.toString().contains("zstandard") == true
 * ```
 */
val ARCHIVE_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.zstandardCodec(9, true, true)
}

/**
 * 압축을 사용하지 않는 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 압축/해제 할당 비용 없이 데이터 파일을 기록합니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val codec = NULL_CODEC_FACTORY
 * // codec.toString().contains("null") == true
 * ```
 */
val NULL_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.nullCodec()
}

/**
 * Deflate(레벨 6) 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 범용 호환성이 필요한 경로에서 사용할 수 있습니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val codec = DEFLATE_CODEC_FACTORY
 * // codec.toString().contains("deflate") == true
 * ```
 */
val DEFLATE_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.deflateCodec(6)
}

/**
 * Snappy 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 빠른 압축/복원 속도가 필요한 경로에 적합합니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val codec = SNAPPY_CODEC_FACTORY
 * // codec.toString().contains("snappy") == true
 * ```
 */
val SNAPPY_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.snappyCodec()
}

/**
 * BZip2 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 느리지만 높은 압축률이 필요한 경로에 적합합니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 */
val BZIP2_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.bzip2Codec()
}

/**
 * XZ(레벨 6) 코덱 팩토리 인스턴스입니다.
 *
 * ## 동작/계약
 * - 높은 압축률이 필요한 아카이브 경로에서 사용할 수 있습니다.
 * - lazy 초기화 후 동일 인스턴스를 재사용합니다.
 */
val XZ_CODEC_FACTORY: CodecFactory by lazy {
    CodecFactory.xzCodec(6)
}

/**
 * 코덱 이름 문자열을 [CodecFactory] 인스턴스로 변환합니다.
 *
 * ## 동작/계약
 * - [name]은 `lowercase().trim()` 기준으로 비교하므로 대소문자/앞뒤 공백을 무시합니다.
 * - `"null"`, `"none"`, `"deflate"`, `"snappy"`, `"zstd"`, `"zstandard"`, `"zstd-fast"`, `"bzip2"`, `"xz"`를 지원합니다.
 * - 지원하지 않는 이름은 [IllegalArgumentException]을 던집니다.
 *
 * ```kotlin
 * val codec = codecFactoryOf("  Snappy  ")
 * // codec == SNAPPY_CODEC_FACTORY
 * ```
 *
 * @param name 코덱 이름입니다. 지원 목록 외 값이면 [IllegalArgumentException]이 발생합니다.
 */
fun codecFactoryOf(name: String): CodecFactory {
    return when (name.lowercase().trim()) {
        "null", "none"      -> NULL_CODEC_FACTORY
        "deflate"           -> DEFLATE_CODEC_FACTORY
        "snappy"            -> SNAPPY_CODEC_FACTORY
        "zstd", "zstandard" -> DEFAULT_CODEC_FACTORY
        "zstd-fast"         -> FAST_CODEC_FACTORY
        "bzip2"             -> BZIP2_CODEC_FACTORY
        "xz"                -> XZ_CODEC_FACTORY
        else                -> throw IllegalArgumentException("지원하지 않는 Avro 코덱입니다: $name")
    }
}
