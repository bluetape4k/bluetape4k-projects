package io.bluetape4k.images.vips.coroutines

import io.bluetape4k.images.vips.VipsEncodeOptions
import io.bluetape4k.images.vips.VipsImage
import io.bluetape4k.images.vips.VipsImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.file.Path

/**
 * [VipsImage.toBytes]의 코루틴 suspend 래퍼.
 *
 * 블로킹 인코딩 연산을 [Dispatchers.IO]에서 실행합니다.
 *
 * @param format 출력 포맷 (기본값: [VipsImageFormat.JPEG])
 * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
 * @return 인코딩된 바이트 배열
 */
suspend fun VipsImage.suspendToBytes(
    format: VipsImageFormat = VipsImageFormat.JPEG,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
): ByteArray = withContext(Dispatchers.IO) {
    toBytes(format, options)
}

/**
 * [VipsImage.writeTo] (Path 오버로드)의 코루틴 suspend 래퍼.
 *
 * 블로킹 파일 쓰기 연산을 [Dispatchers.IO]에서 실행합니다.
 *
 * **경로 탐색(Path Traversal) 주의**: 호출자는 `path`가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
 *
 * @param path 출력 파일 경로
 * @param format 출력 포맷 (기본값: [VipsImageFormat.JPEG])
 * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
 */
suspend fun VipsImage.suspendWriteTo(
    path: Path,
    format: VipsImageFormat = VipsImageFormat.JPEG,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
): Unit = withContext(Dispatchers.IO) {
    writeTo(path, format, options)
}

/**
 * [VipsImage.writeTo] (OutputStream 오버로드)의 코루틴 suspend 래퍼.
 *
 * 블로킹 스트림 쓰기 연산을 [Dispatchers.IO]에서 실행합니다.
 *
 * @param out 출력 스트림
 * @param format 출력 포맷
 * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
 */
suspend fun VipsImage.suspendWriteTo(
    out: OutputStream,
    format: VipsImageFormat,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
): Unit = withContext(Dispatchers.IO) {
    writeTo(out, format, options)
}
