package io.bluetape4k.images

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.coroutines.SuspendWriteContext
import io.bluetape4k.io.readAllBytesSuspending
import io.bluetape4k.io.writeSuspending
import java.awt.Graphics2D
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path

@PublishedApi
internal const val IMAGE_BUFFER_SIZE: Int = 128 * 1024

/**
 * [ByteArray]를 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - Scrimage loader를 사용해 메모리 바이트를 디코딩합니다.
 * - 디코딩 실패 시 loader 예외가 전파됩니다.
 *
 * ```kotlin
 * val image = immutableImageOf(bytes)
 * // image.width > 0
 * ```
 *
 * @param bytes 이미지 정보를 담은 [ByteArray]
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(bytes: ByteArray): ImmutableImage =
    ImmutableImage.loader().fromBytes(bytes)

/**
 * [InputStream]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `buffered()` 스트림을 사용합니다.
 * - 스트림 close는 호출자가 관리해야 합니다.
 *
 * ```kotlin
 * val image = immutableImageOf(File("image.jpg").inputStream())
 * // image.height > 0
 * ```
 *
 * @param inputStream 이미지 정보를 담은 [InputStream]
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(inputStream: InputStream): ImmutableImage =
    ImmutableImage.loader().fromStream(inputStream.buffered())

/**
 * [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - 파일 읽기 실패 시 예외가 전파됩니다.
 *
 * ```kotlin
 * val image = immutableImageOf(file)
 * // image.width > 0
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(file: File): ImmutableImage =
    ImmutableImage.loader().fromFile(file)

/**
 * [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - [Path]를 그대로 Scrimage loader에 전달합니다.
 *
 * ```kotlin
 * val image = immutableImageOf(path)
 * // image.height > 0
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
fun immutableImageOf(path: Path): ImmutableImage =
    ImmutableImage.loader().fromPath(path)

/**
 * Coroutines 환경에서 [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [suspendImmutableImageOf] 경로 변환 버전을 호출합니다.
 *
 * ```kotlin
 * val image = suspendImmutableImageOf(file)
 * // image.width > 0
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendImmutableImageOf(file: File): ImmutableImage =
    suspendImmutableImageOf(file.toPath())

/**
 * Coroutines 환경에서 [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - 비동기로 파일 바이트를 읽은 뒤 [immutableImageOf]로 디코딩합니다.
 *
 * ```kotlin
 * val image = suspendImmutableImageOf(path)
 * // image.height > 0
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendImmutableImageOf(path: Path): ImmutableImage =
    immutableImageOf(path.readAllBytesSuspending())


/**
 * Coroutines 환경에서 [File]을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - [suspendLoadImage]의 `File` 오버로드는 Path 오버로드에 위임합니다.
 *
 * ```kotlin
 * val image = suspendLoadImage(file)
 * // image.width > 0
 * ```
 *
 * @param file 이미지 파일
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendLoadImage(file: File): ImmutableImage =
    suspendLoadImage(file.toPath())

/**
 * Coroutines 환경에서 [Path]의 파일을 읽어 [ImmutableImage]로 변환합니다.
 *
 * ## 동작/계약
 * - `path.readAllBytesSuspending()` 결과를 디코딩합니다.
 *
 * ```kotlin
 * val image = suspendLoadImage(path)
 * // image.height > 0
 * ```
 *
 * @param path 이미지 파일의 경로
 * @return 이미지 정보를 담은 [ImmutableImage]
 */
suspend fun suspendLoadImage(path: Path): ImmutableImage =
    immutableImageOf(path.readAllBytesSuspending())

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [ByteArray]로 변환합니다.
 *
 * ## 동작/계약
 * - `ByteArrayOutputStream`을 새로 할당해 인코딩 결과를 반환합니다.
 * - writer 예외는 호출자에게 그대로 전파됩니다.
 *
 * ```kotlin
 * val bytes = image.suspendBytes(writer)
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @return 이미지 정보를 담은 ByteArray
 */
suspend inline fun ImmutableImage.suspendBytes(writer: SuspendImageWriter): ByteArray =
    ByteArrayOutputStream(IMAGE_BUFFER_SIZE).use { bos ->
        writer.suspendWrite(this, this.metadata, bos)
        bos.toByteArray()
    }

/**
 * Coroutines 환경에서 [ImmutableImage] 정보를 [writer]를 통해 [destPath]에 저장합니다.
 *
 * ## 동작/계약
 * - 먼저 [suspendBytes]로 인코딩한 뒤 파일에 비동기 기록합니다.
 * - 반환값은 기록된 바이트 수입니다.
 *
 * ```kotlin
 * val written = image.suspendWrite(writer, path)
 * // written > 0L
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @param destPath 저장할 파일의 경로
 * @return 저장된 파일의 크기
 */
suspend fun ImmutableImage.suspendWrite(writer: SuspendImageWriter, destPath: Path): Long {
    val bytes = suspendBytes(writer)
    return destPath.writeSuspending(bytes)
}

/**
 * [ImmutableImage] 정보를 쓰기 작업을 위해 [writer]를 사용하는 [SuspendWriteContext]를 생성합니다.
 *
 * ## 동작/계약
 * - 실제 인코딩/출력은 수행하지 않고 컨텍스트만 생성합니다.
 *
 * ```kotlin
 * val context = image.forSuspendWriter(writer)
 * // context != null
 * ```
 *
 * @param writer 이미지를 쓰기 위한 [SuspendImageWriter]
 * @return [SuspendWriteContext] instance
 */
fun ImmutableImage.forSuspendWriter(writer: SuspendImageWriter): SuspendWriteContext =
    SuspendWriteContext(writer, this, this.metadata)


/**
 * [ImmutableImage]의 복사본에 그리기 작업 ([action])을 수행하고 새 [ImmutableImage]를 반환합니다.
 *
 * 원본 이미지는 변경되지 않습니다. `Graphics2D`는 `finally`에서 항상 `dispose()`됩니다.
 *
 * ```kotlin
 * val annotated = image.withGraphics { g ->
 *     g.color = Color.RED
 *     g.drawRect(0, 0, 10, 10)
 * }
 * // image 는 변경되지 않고 annotated 에 사각형이 그려진 새 이미지가 반환된다.
 * ```
 *
 * @param action 복사본에 적용할 그래픽 작업
 * @return 그래픽 작업이 적용된 새 [ImmutableImage]
 */
inline fun ImmutableImage.withGraphics(
    action: (graphics: Graphics2D) -> Unit,
): ImmutableImage {
    val copy = this.copy()
    val graphics: Graphics2D = copy.awt().createGraphics()
    try {
        action(graphics)
    } finally {
        graphics.dispose()
    }
    return copy
}

/**
 * [ImmutableImage]에 그리기 작업 ([action])을 수행합니다.
 *
 * **주의**: 이 함수는 `ImmutableImage`의 내부 backing buffer를 직접 수정합니다.
 * 불변 계약을 위반하므로 [withGraphics]를 사용하세요.
 *
 * @param action 그래픽 작업
 */
@Deprecated(
    message = "ImmutableImage.useGraphics()는 내부 backing buffer를 변경하여 불변 계약을 위반합니다. withGraphics()를 사용하세요.",
    replaceWith = ReplaceWith("withGraphics(action)"),
)
inline fun ImmutableImage.useGraphics(
    action: (graphics: Graphics2D) -> Unit,
) {
    withGraphics(action)
}
