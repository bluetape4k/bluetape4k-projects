package io.bluetape4k.examples.coroutines.context

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import kotlin.coroutines.CoroutineContext

/**
 * Unique uuid string 을 제공하는 [CoroutineContext] 구현체입니다.
 */
abstract class UuidProviderCoroutineContext: CoroutineContext.Element {

    companion object Key: CoroutineContext.Key<UuidProviderCoroutineContext>

    override val key: CoroutineContext.Key<*>
        get() = Key

    /**
     * 새로운 unique uuid 를 문자열로 제공합니다.
     */
    abstract fun nextUuid(): String
}

/**
 * Timebased uuid 를 제공하는 [UuidProviderCoroutineContext] 구현체입니다.
 */
class TimebasedUuidProviderCoroutineContext: UuidProviderCoroutineContext() {

    override fun nextUuid(): String {
        return TimebasedUuid.Reordered.nextId().toString()
    }
}

/**
 * [fakeUuid]를 제공하는 [UuidProviderCoroutineContext] 구현체입니다.
 *
 * @property fakeUuid fake uuid string
 */
class FakeUuidProviderCoroutineContext(private val fakeUuid: String): UuidProviderCoroutineContext() {
    override fun nextUuid(): String {
        return fakeUuid
    }
}
