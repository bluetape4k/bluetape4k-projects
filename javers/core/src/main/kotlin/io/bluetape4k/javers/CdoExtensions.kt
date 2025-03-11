package io.bluetape4k.javers

import org.javers.core.graph.Cdo
import kotlin.jvm.optionals.getOrNull

fun Cdo.getWrappedOrNull(): Any? = this.wrappedCdo.getOrNull()
