package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.filter.PipelineFilter
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * 이미지 필터 체인을 선언적으로 구성하는 DSL 빌더.
 * [applyFilters] 또는 [suspendApplyFilters]에서 생성합니다.
 */
@ImageFilterDsl
class ImageFilterChain internal constructor() {

    companion object : KLoggingChannel()

    internal sealed interface Op {
        class Native(val filter: Filter) : Op
        class Pixel(val transform: (ImmutableImage) -> ImmutableImage) : Op
    }

    private val ops: MutableList<Op> = mutableListOf()

    internal fun addNative(filter: Filter) {
        ops.add(Op.Native(filter))
    }

    internal fun addPixel(transform: (ImmutableImage) -> ImmutableImage) {
        ops.add(Op.Pixel(transform))
    }

    internal fun build(): List<Op> = ops.toList()

    /**
     * Scrimage [Filter] 인스턴스를 직접 체인에 추가합니다.
     * DSL에 없는 필터를 사용할 때 이스케이프 해치로 활용합니다.
     *
     * @param filter 적용할 [Filter] 인스턴스
     */
    fun raw(filter: Filter) {
        addNative(filter)
    }

    /**
     * 픽셀 변환 람다를 체인에 추가합니다.
     * [ImmutableImage]를 받아 새 [ImmutableImage]를 반환하는 함수를 등록합니다.
     *
     * @param transform 이미지를 변환하는 람다
     */
    fun pixel(transform: (ImmutableImage) -> ImmutableImage) {
        addPixel(transform)
    }

    /**
     * 등록된 ops를 컴팩트하여 [source]에 적용합니다.
     *
     * - ops가 비어 있으면 [source]를 그대로 반환합니다 (복사 없음).
     * - ops가 있으면 [source.copy()]로 방어 복사를 1회 수행하고 사본에 필터를 적용합니다.
     * - 인접한 [Op.Native] 필터는 [PipelineFilter]로 묶어 단일 패스로 적용합니다.
     */
    internal fun compactAndApply(source: ImmutableImage): ImmutableImage {
        val opList = build()
        if (opList.isEmpty()) return source

        var current = source.copy()
        var i = 0
        while (i < opList.size) {
            when (val op = opList[i]) {
                is Op.Native -> {
                    // 인접한 Native ops 그룹화
                    val group = mutableListOf<Filter>()
                    while (i < opList.size && opList[i] is Op.Native) {
                        group.add((opList[i] as Op.Native).filter)
                        i++
                    }
                    current = current.filter(PipelineFilter(group))
                }
                is Op.Pixel -> {
                    current = op.transform(current)
                    i++
                }
            }
        }
        return current
    }
}
