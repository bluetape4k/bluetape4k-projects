package io.bluetape4k.images.vips

import java.io.OutputStream
import java.nio.file.Path

/**
 * libvips 이미지를 나타내는 바인딩 중립(binding-neutral) 인터페이스.
 *
 * 이 인터페이스는 JVips(Java 21 모듈)와 vips-ffm(Java 25 모듈) 모두에서 구현됩니다.
 * 구현체는 [AutoCloseable]을 통해 네이티브 리소스를 반드시 해제해야 합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * vipsImageOf(file).use { image ->
 *     val thumbnail = image.thumbnail(800)
 *     thumbnail.writeTo(outputPath, VipsImageFormat.WEBP)
 * }
 * ```
 *
 * **스레드 안전성**: 구현체는 단일 스레드 전용입니다. 여러 코루틴에서 공유하지 마십시오.
 */
interface VipsImage : AutoCloseable {

    /** 이미지 너비 (픽셀) */
    val width: Int

    /** 이미지 높이 (픽셀) */
    val height: Int

    /** 채널 수 (예: RGB=3, RGBA=4, 그레이스케일=1) */
    val bands: Int

    /**
     * 지정한 크기로 이미지를 리사이즈합니다.
     *
     * @param width 목표 너비 (픽셀, 양수)
     * @param height 목표 높이 (픽셀, 양수)
     * @return 리사이즈된 새 [VipsImage] 인스턴스
     * @throws VipsOperationException 연산 실패 시
     */
    fun resize(width: Int, height: Int): VipsImage

    /**
     * 긴 변을 [maxDimension]에 맞추고 비율을 유지하며 썸네일을 생성합니다.
     *
     * @param maxDimension 긴 변의 최대 크기 (픽셀, 양수)
     * @return 썸네일 [VipsImage] 인스턴스
     * @throws VipsOperationException 연산 실패 시
     */
    fun thumbnail(maxDimension: Int): VipsImage

    /**
     * 이미지의 일부 영역을 잘라냅니다.
     *
     * @param left 좌측 시작 좌표 (픽셀, 0 이상)
     * @param top 상단 시작 좌표 (픽셀, 0 이상)
     * @param width 잘라낼 너비 (픽셀, 양수)
     * @param height 잘라낼 높이 (픽셀, 양수)
     * @return 크롭된 새 [VipsImage] 인스턴스
     * @throws VipsOperationException 연산 실패 시
     */
    fun crop(left: Int, top: Int, width: Int, height: Int): VipsImage

    /**
     * 이미지를 지정 포맷의 바이트 배열로 인코딩합니다.
     *
     * @param format 출력 포맷 (기본값: [VipsImageFormat.JPEG])
     * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
     * @return 인코딩된 바이트 배열
     * @throws VipsEncodeException 인코딩 실패 시
     */
    fun toBytes(
        format: VipsImageFormat = VipsImageFormat.JPEG,
        options: VipsEncodeOptions = VipsEncodeOptions.Default,
    ): ByteArray

    /**
     * 이미지를 파일 경로에 씁니다.
     *
     * **경로 탐색(Path Traversal) 주의**: 호출자는 `path`가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다.
     * 이 함수는 경로 탐색을 방지하지 않습니다.
     *
     * @param path 출력 파일 경로
     * @param format 출력 포맷 (기본값: [VipsImageFormat.JPEG])
     * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
     * @throws VipsEncodeException 인코딩 또는 파일 쓰기 실패 시
     */
    fun writeTo(
        path: Path,
        format: VipsImageFormat = VipsImageFormat.JPEG,
        options: VipsEncodeOptions = VipsEncodeOptions.Default,
    )

    /**
     * 이미지를 출력 스트림에 씁니다.
     *
     * @param out 출력 스트림
     * @param format 출력 포맷
     * @param options 인코딩 옵션 (기본값: [VipsEncodeOptions.Default])
     * @throws VipsEncodeException 인코딩 또는 스트림 쓰기 실패 시
     */
    fun writeTo(
        out: OutputStream,
        format: VipsImageFormat,
        options: VipsEncodeOptions = VipsEncodeOptions.Default,
    )
}
