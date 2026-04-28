package io.bluetape4k.images.thumbnail

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.batch.ImageBatchFailureStage
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.transforms.SaliencyStrategy
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.nio.file.Path

/**
 * 썸네일 출력 크기입니다.
 */
data class ThumbnailSize(
    val width: Int,
    val height: Int,
    val suffix: String = "${width}x$height",
) {
    init {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        suffix.requireNotBlank("suffix")
    }
}

/**
 * 썸네일 크롭 전략입니다.
 */
sealed interface ThumbnailCrop {
    /**
     * 지정 크기로 리사이즈합니다.
     */
    data object Fit: ThumbnailCrop

    /**
     * 살리언시 기반 스마트 크롭을 적용합니다.
     */
    data class Smart(
        val strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy,
    ): ThumbnailCrop
}

/**
 * 썸네일 출력 포맷입니다.
 */
data class ThumbnailFormat(
    val writer: SuspendImageWriter,
    val extension: String,
) {
    val normalizedExtension: String = extension.trim().removePrefix(".").lowercase()

    init {
        normalizedExtension.requireNotBlank("extension")
        require(!normalizedExtension.contains(PATH_SEPARATOR)) { "extension은 경로 구분자를 포함할 수 없습니다." }
        require(!normalizedExtension.contains(WINDOWS_PATH_SEPARATOR)) { "extension은 경로 구분자를 포함할 수 없습니다." }
    }

    companion object {
        private const val PATH_SEPARATOR = '/'
        private const val WINDOWS_PATH_SEPARATOR = '\\'

        /**
         * JPEG 썸네일 포맷입니다.
         */
        val Jpeg: ThumbnailFormat = ThumbnailFormat(SuspendJpegWriter.Default, "jpg")
    }
}

/**
 * 썸네일 출력 이름 생성 함수입니다.
 */
fun interface ThumbnailOutputName {
    /**
     * 원본 경로와 썸네일 크기로 출력 파일 이름을 생성합니다.
     */
    fun create(source: Path, size: ThumbnailSize, format: ThumbnailFormat): String

    companion object {
        /**
         * 원본 파일명에 크기 suffix를 붙이는 기본 생성기입니다.
         */
        val Default = ThumbnailOutputName { source, size, format ->
            val fileName = source.fileName.toString()
            val stem = fileName.substringBeforeLast('.', fileName)
            "$stem-${size.suffix}.${format.normalizedExtension}"
        }
    }
}

/**
 * 썸네일 처리 상태입니다.
 */
sealed interface ThumbnailStatus {
    /**
     * 썸네일 생성에 성공했습니다.
     */
    data class Success(
        val bytes: Long,
    ): ThumbnailStatus

    /**
     * 썸네일 생성에 실패했습니다.
     */
    data class Failure(
        val stage: ImageBatchFailureStage,
        val cause: Throwable,
    ): ThumbnailStatus
}

/**
 * 썸네일 처리 결과입니다.
 */
data class ThumbnailResult(
    val source: Path,
    val output: Path,
    val size: ThumbnailSize,
    val status: ThumbnailStatus,
    val image: ImmutableImage? = null,
) {
    /**
     * 실패 시점의 처리 단계입니다.
     */
    val stage: ImageBatchFailureStage?
        get() = (status as? ThumbnailStatus.Failure)?.stage

    /**
     * 실패 원인입니다.
     */
    val cause: Throwable?
        get() = (status as? ThumbnailStatus.Failure)?.cause
}
