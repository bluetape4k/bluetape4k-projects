package io.bluetape4k.images.batch

/**
 * 배치 이미지 처리의 기본 픽셀 한도입니다.
 */
const val DEFAULT_MAX_PIXELS: Long = 16_777_216L

internal const val DEFAULT_MAX_IN_FLIGHT_MULTIPLIER: Long = 2L

/**
 * 배치 이미지 처리에서 동시에 허용하는 기본 픽셀 총량입니다.
 */
const val DEFAULT_MAX_IN_FLIGHT_PIXELS: Long = DEFAULT_MAX_PIXELS * DEFAULT_MAX_IN_FLIGHT_MULTIPLIER

/**
 * 타일 분할에서 허용하는 기본 최대 타일 수입니다.
 */
const val DEFAULT_MAX_TILE_COUNT: Int = 65_536

/**
 * JPEG 압축 품질의 최소값입니다.
 */
const val JPEG_QUALITY_MIN: Int = 0

/**
 * JPEG 압축 품질의 최대값입니다.
 */
const val JPEG_QUALITY_MAX: Int = 100

/**
 * 비차단 성능 샘플의 기본 이미지 수입니다.
 */
const val PERFORMANCE_SAMPLE_IMAGE_COUNT: Int = 100

internal const val LARGE_JOB_MAX_PIXELS_MULTIPLIER: Long = 16L
internal const val LARGE_JOB_MAX_IN_FLIGHT_MULTIPLIER: Long = 2L

/**
 * 대용량 배치 처리에서 단일 이미지에 허용하는 기본 픽셀 수입니다.
 */
const val LARGE_JOB_MAX_PIXELS: Long = DEFAULT_MAX_PIXELS * LARGE_JOB_MAX_PIXELS_MULTIPLIER

/**
 * 대용량 배치 처리에서 동시에 허용하는 기본 픽셀 총량입니다.
 */
const val LARGE_JOB_MAX_IN_FLIGHT_PIXELS: Long = LARGE_JOB_MAX_PIXELS * LARGE_JOB_MAX_IN_FLIGHT_MULTIPLIER

internal const val MIN_IMAGE_BATCH_PARALLELISM: Int = 1
internal const val MIN_PIXEL_PERMIT: Long = 1L
internal const val PIXEL_PERMIT_RETRY_DELAY_MILLIS: Long = 1L

/**
 * 현재 런타임에 맞춘 이미지 배치 기본 병렬도를 반환합니다.
 */
fun defaultImageBatchParallelism(): Int =
    Runtime.getRuntime().availableProcessors().coerceAtLeast(MIN_IMAGE_BATCH_PARALLELISM)
