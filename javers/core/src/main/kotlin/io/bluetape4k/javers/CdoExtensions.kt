package io.bluetape4k.javers

import org.javers.core.graph.Cdo
import kotlin.jvm.optionals.getOrNull

/**
 * [Cdo]가 감싸고 있는 원본 객체를 반환하거나, 없으면 null을 반환한다.
 *
 * ```kotlin
 * val wrapped = cdo.getWrappedOrNull()
 * // wrapped == null 또는 원본 도메인 객체
 * ```
 */
fun Cdo.getWrappedOrNull(): Any? = this.wrappedCdo.getOrNull()
