package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.collections.eclipse.fastListOf

fun interface EmitHandler {
    fun emit(emit: Emit): Boolean
}

interface StatefulEmitHandler: EmitHandler {
    val emits: MutableList<Emit>
}

abstract class AbstractStatefulEmitHandler: StatefulEmitHandler {
    override val emits: MutableList<Emit> = fastListOf()
    fun addEmit(emit: Emit): Boolean = emits.add(emit)
}

class DefaultEmitHandler: AbstractStatefulEmitHandler() {
    override fun emit(emit: Emit): Boolean = addEmit(emit)
}
