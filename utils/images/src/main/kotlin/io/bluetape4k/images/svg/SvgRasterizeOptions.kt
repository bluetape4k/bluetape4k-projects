package io.bluetape4k.images.svg

import java.awt.Color

/**
 * SVG 래스터화 옵션입니다.
 *
 * ## 동작/계약
 * - `width`/`height`가 모두 `null`이면 SVG 원본 크기를 유지합니다.
 * - `allowExternalResources`는 기본값 `false`이며, SSRF/XXE 공격 방어를 위해 변경을 권장하지 않습니다.
 * - `timeoutMillis`는 Batik 래스터화 작업의 최대 허용 시간입니다. 초과 시 인터럽트됩니다.
 * - `maxWidthPx`/`maxHeightPx`는 래스터화 전 치수 검증에 사용됩니다.
 *
 * ```kotlin
 * val opts = SvgRasterizeOptions(width = 800, height = 600, dpi = 144)
 * val rasterizer: SuspendSvgRasterizer = BatikSvgRasterizer()
 * val image = rasterizer.rasterize(svgInputStream, opts)
 * ```
 *
 * @property width              출력 픽셀 너비 (`null`이면 SVG 원본 너비 사용)
 * @property height             출력 픽셀 높이 (`null`이면 SVG 원본 높이 사용)
 * @property dpi                해상도 (기본값: 96 DPI)
 * @property backgroundColor    배경색 (`null`이면 투명 배경)
 * @property allowExternalResources 외부 리소스 로드 허용 여부 (기본값: `false`, SSRF/XXE 방어)
 * @property allowedSchemes     허용할 URL 스킴 목록 (기본값: `setOf("data")`)
 * @property timeoutMillis      래스터화 최대 허용 시간(ms) (기본값: 10_000)
 * @property maxWidthPx         허용할 최대 출력 너비(px) (기본값: 8192, 리소스 폭탄 방어)
 * @property maxHeightPx        허용할 최대 출력 높이(px) (기본값: 8192, 리소스 폭탄 방어)
 */
data class SvgRasterizeOptions(
    val width: Int? = null,
    val height: Int? = null,
    val dpi: Int = 96,
    val backgroundColor: Color? = null,
    val allowExternalResources: Boolean = false,
    val allowedSchemes: Set<String> = setOf("data"),
    val timeoutMillis: Long = 10_000L,
    val maxWidthPx: Int = 8192,
    val maxHeightPx: Int = 8192,
) {
    companion object {
        @JvmStatic
        val Default = SvgRasterizeOptions()
    }
}
