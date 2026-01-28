package io.bluetape4k.examples.coroutines.context

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Counter 를 가지는 [CoroutineContext]
 *
 * @property name coroutine context name
 */
class CounterCoroutineContext(private val name: String): AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<CounterCoroutineContext>, KLogging()

    private val nextNumber = atomic(0L)

    val number: Long by nextNumber

    fun printNextCount() {
        log.debug { this }
        nextNumber.incrementAndGet()
    }

    override fun toString(): String {
        return "CounterCoroutineContext(name='$name', number=$number)"
    }
}

/**
 * Current CoroutineScope 에서 [CounterCoroutineContext]를 찾아서 [CounterCoroutineContext.number]를 출력합니다.
 */
internal suspend inline fun printNextCount() =
    currentCoroutineContext()[CounterCoroutineContext]?.printNextCount()
