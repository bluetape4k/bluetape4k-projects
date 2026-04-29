package io.bluetape4k.images.coroutines.animated

import com.sksamuel.scrimage.nio.AnimatedGif
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

/**
 * [AnimatedGif]를 [writer]로 인코딩하여 [ByteArray]로 반환합니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val bytes = gif.suspendBytes(writer)
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @return 인코딩된 이미지 데이터
 */
suspend fun AnimatedGif.suspendBytes(writer: SuspendGif2WebpWriter): ByteArray {
    return forSuspendWriter(writer).bytes()
}

/**
 * [AnimatedGif]를 [writer]로 인코딩하여 [bos]에 씁니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val bos = ByteArrayOutputStream()
 * gif.suspendWrite(writer, bos)
 * // bos.size() > 0
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @param bos    쓰기 대상 [ByteArrayOutputStream]
 */
suspend fun AnimatedGif.suspendWrite(writer: SuspendGif2WebpWriter, bos: ByteArrayOutputStream) {
    forSuspendWriter(writer).write(bos)
}

/**
 * [AnimatedGif]를 [writer]로 인코딩하여 [path]에 저장합니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val saved = gif.suspendWrite(writer, Path.of("/tmp/output.webp"))
 * // saved.toFile().exists() == true
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @param path   저장할 파일 [Path]
 * @return 저장된 파일의 [Path]
 */
suspend fun AnimatedGif.suspendWrite(writer: SuspendGif2WebpWriter, path: Path): Path {
    return forSuspendWriter(writer).write(path)
}

/**
 * [AnimatedGif]를 [writer]로 인코딩하여 [file]에 저장합니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val saved = gif.suspendOutput(writer, File("/tmp/output.webp"))
 * // saved.exists() == true
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @param file   저장할 대상 [File]
 * @return 저장된 [File]
 */
suspend fun AnimatedGif.suspendOutput(writer: SuspendGif2WebpWriter, file: File): File {
    return forSuspendWriter(writer).write(file)
}

/**
 * [AnimatedGif]를 [writer]로 인코딩하여 [path]에 저장합니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val saved = gif.suspendOutput(writer, Path.of("/tmp/output.webp"))
 * // saved.toFile().exists() == true
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @param path   저장할 파일 [Path]
 * @return 저장된 파일의 [Path]
 */
suspend fun AnimatedGif.suspendOutput(writer: SuspendGif2WebpWriter, path: Path): Path {
    return forSuspendWriter(writer).write(path)
}

/**
 * [AnimatedGif]를 [writer]와 연결하는 [SuspendAnimatedWriteContext]를 생성합니다.
 *
 * ```kotlin
 * val writer = SuspendGif2WebpWriter.Default
 * val gif: AnimatedGif = AnimatedGif.fromFile(File("animation.gif"))
 * val context = gif.forSuspendWriter(writer)
 * // context != null
 * ```
 *
 * @param writer 인코딩에 사용할 [SuspendGif2WebpWriter]
 * @return [SuspendAnimatedWriteContext] 인스턴스
 */
fun AnimatedGif.forSuspendWriter(writer: SuspendGif2WebpWriter): SuspendAnimatedWriteContext =
    SuspendAnimatedWriteContext(writer, this)
