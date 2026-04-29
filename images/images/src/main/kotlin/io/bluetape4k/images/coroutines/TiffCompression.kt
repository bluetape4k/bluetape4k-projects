package io.bluetape4k.images.coroutines

/**
 * TIFF 파일 저장 시 사용할 압축 방식입니다.
 *
 * ## 동작/계약
 * - [ioName]은 Java ImageIO (`ImageWriteParam.setCompressionType`)에 전달되는 문자열입니다.
 * - `NONE`은 무압축이며, `DEFLATE`이 기본값입니다.
 * - `JPEG`은 손실 압축으로, `quality` 파라미터와 함께 사용합니다.
 *
 * ```kotlin
 * val writer = SuspendTiffWriter(compression = TiffCompression.LZW)
 * ```
 */
enum class TiffCompression(val ioName: String) {
    /** 무압축. 파일 크기가 가장 크지만 처리 속도가 가장 빠릅니다. */
    NONE("None"),

    /** DEFLATE (zlib) 압축. 기본값. 손실 없음, 범용 호환성 우수. */
    DEFLATE("Deflate"),

    /** LZW 압축. 손실 없음, 범용 호환성 우수. */
    LZW("LZW"),

    /** PackBits 런-렝스 인코딩. 단색 이미지에 효율적. */
    PACKBITS("PackBits"),

    /** JPEG 손실 압축. `quality` 파라미터와 함께 사용. */
    JPEG("JPEG"),
}
