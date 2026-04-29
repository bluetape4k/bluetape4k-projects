package io.bluetape4k.images.vips

import io.bluetape4k.images.IncubatingImageApi

/**
 * libvips가 지원하는 이미지 출력 포맷.
 *
 * 안정적 포맷 (JPEG, PNG, WEBP)은 즉시 사용 가능합니다.
 * AVIF, HEIC는 [IncubatingImageApi] 상태이며 libvips 빌드에 libaom/libheif가 포함되어 있어야 합니다.
 */
enum class VipsImageFormat {

    /** JPEG — 손실 압축, 인터넷 범용 포맷 */
    JPEG,

    /** PNG — 무손실 압축, 투명도 지원 */
    PNG,

    /** WebP — Google 고효율 포맷, 손실/무손실 모두 지원 */
    WEBP,

    /** AVIF — AV1 기반 고효율 포맷. libvips 빌드에 libaom이 필요합니다. */
    @IncubatingImageApi
    AVIF,

    /** HEIC — Apple 고효율 포맷. libvips 빌드에 libheif가 필요합니다. */
    @IncubatingImageApi
    HEIC,
}
