package io.bluetape4k.images.svg

import com.sksamuel.scrimage.ImmutableImage
import java.io.InputStream

/**
 * SVG [InputStream]을 래스터 [ImmutableImage]로 변환하는 코루틴 기반 래스터라이저 인터페이스입니다.
 *
 * ## 동작/계약
 * - [rasterize]는 SVG를 파싱하고 [SvgRasterizeOptions]에 따라 크기·배경·DPI를 적용한 뒤 반환합니다.
 * - `input` 스트림은 호출자가 닫을 책임이 있습니다.
 * - 구현체는 외부 리소스 접근을 기본적으로 금지해야 합니다 ([SvgRasterizeOptions.allowExternalResources] = `false`).
 *
 * ```kotlin
 * val rasterizer: SuspendSvgRasterizer = BatikSvgRasterizer()
 * val svg = File("diagram.svg").inputStream()
 * val image: ImmutableImage = rasterizer.rasterize(svg)
 * ```
 *
 * @see BatikSvgRasterizer
 * @see SvgRasterizeOptions
 */
interface SuspendSvgRasterizer {

    /**
     * [input] SVG 스트림을 래스터 [ImmutableImage]로 변환합니다.
     *
     * @param input   SVG 데이터를 담은 [InputStream]
     * @param options 래스터화 옵션 (기본값: [SvgRasterizeOptions.Default])
     * @return 래스터화된 [ImmutableImage]
     * @throws IllegalArgumentException 옵션 제약 위반(크기 초과 등) 시
     * @throws java.io.IOException      SVG 파싱 또는 래스터화 실패 시
     */
    suspend fun rasterize(
        input: InputStream,
        options: SvgRasterizeOptions = SvgRasterizeOptions.Default,
    ): ImmutableImage
}
