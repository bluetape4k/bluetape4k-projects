package io.bluetape4k.images.vips

/**
 * libvips 연산 실패 시 발생하는 최상위 예외.
 *
 * **메시지 보안 정책**: `message`에는 포맷 이름·연산 종류 등 안전한 정보만 포함합니다.
 * libvips 내부 에러 버퍼(파일 경로·메모리 주소 포함 가능)는 `cause`에만 보존하여 서버 로그용으로만 사용합니다.
 * `e.message`를 엔드유저에게 그대로 반환하지 마십시오.
 *
 * ```kotlin
 * // BAD — libvips 내부 경로가 메시지에 노출될 수 있음
 * throw VipsDecodeException(jvipsException.message ?: "decode failed", jvipsException)
 *
 * // GOOD — 안전한 메시지, cause 에 원인 보존
 * throw VipsDecodeException("Image decode failed: unsupported format or corrupted input", jvipsException)
 * ```
 */
open class VipsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 이미지 디코딩(읽기) 실패 시 발생하는 예외.
 *
 * 지원하지 않는 포맷, 손상된 입력, 크기 초과 등의 경우에 발생합니다.
 */
class VipsDecodeException(message: String, cause: Throwable? = null) : VipsException(message, cause)

/**
 * 이미지 인코딩(쓰기) 실패 시 발생하는 예외.
 *
 * 출력 스트림 오류, 인코딩 옵션 범위 초과 등의 경우에 발생합니다.
 */
class VipsEncodeException(message: String, cause: Throwable? = null) : VipsException(message, cause)

/**
 * 이미지 연산(resize/thumbnail/crop) 실패 시 발생하는 예외.
 *
 * 잘못된 연산 파라미터, 이미지 범위 초과, 연산 중 libvips 오류 등의 경우에 발생합니다.
 */
class VipsOperationException(message: String, cause: Throwable? = null) : VipsException(message, cause)

/**
 * libvips 런타임 초기화 실패 시 발생하는 예외.
 *
 * `VipsRuntime.init()` 호출 실패 또는 이미 종료된 런타임에 대해 `init()`을 재호출하는 경우에 발생합니다.
 * libvips는 `vips_shutdown()` 이후 `VIPS_INIT()`을 재호출하는 것을 지원하지 않으므로,
 * 이 예외가 발생하면 프로세스를 재시작해야 합니다.
 */
class VipsInitializationException(message: String, cause: Throwable? = null) : VipsException(message, cause)
