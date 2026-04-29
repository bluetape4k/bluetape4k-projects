package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DSL 블록으로 지정한 필터 체인을 이미지에 일괄 적용합니다.
 *
 * 인접한 scrimage 네이티브 필터는 [com.sksamuel.scrimage.filter.PipelineFilter]로 묶여
 * 단일 패스로 처리됩니다. 원본 이미지는 변경되지 않으며, 항상 새 [ImmutableImage]를 반환합니다.
 *
 * ```kotlin
 * val result = image.applyFilters {
 *     brightness(1.2f)
 *     contrast(1.1)
 *     sepia()
 * }
 * ```
 *
 * @param block 필터를 구성하는 DSL 블록
 * @return 필터가 적용된 새 [ImmutableImage]
 */
fun ImmutableImage.applyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage {
    val chain = ImageFilterChain()
    chain.block()
    return chain.compactAndApply(this)
}

/**
 * 코루틴 환경에서 DSL 블록으로 지정한 필터 체인을 이미지에 비동기 적용합니다.
 *
 * 픽셀 연산은 [Dispatchers.Default]에서 실행되어 메인/IO 스레드를 차단하지 않습니다.
 * 원본 이미지는 변경되지 않으며, 항상 새 [ImmutableImage]를 반환합니다.
 *
 * ```kotlin
 * val result = image.suspendApplyFilters {
 *     brightness(1.2f)
 *     gaussianBlur(3)
 * }
 * ```
 *
 * @param block 필터를 구성하는 DSL 블록
 * @return 필터가 적용된 새 [ImmutableImage]
 */
suspend fun ImmutableImage.suspendApplyFilters(block: ImageFilterChain.() -> Unit): ImmutableImage =
    withContext(Dispatchers.Default) {
        applyFilters(block)
    }
