package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Local.save
import java.io.Serializable
import java.util.*

/**
 * Thread context 별로 Local storage를 제공하는 object 입니다.
 *
 * ```
 * Local["key"] = "42"        // set
 * val value = Local["key"]   // 42
 * ```
 *
 * @see ThreadLocal
 */
@Suppress("UNCHECKED_CAST")
object Local : KLogging() {
    private val threadLocal: ThreadLocal<java.util.HashMap<Any, Any?>> by lazy {
        object : ThreadLocal<java.util.HashMap<Any, Any?>>() {
            override fun initialValue(): java.util.HashMap<Any, Any?> = HashMap()
        }
    }

    private val storage: java.util.HashMap<Any, Any?> get() = threadLocal.get()

    /**
     * 현재 스레드의 로컬 저장소 스냅샷을 반환합니다.
     *
     * ```kotlin
     * Local["user"] = "alice"
     * val snapshot = Local.save()   // 현재 상태 저장
     * Local["user"] = "bob"
     * Local.restore(snapshot)
     * val user = Local["user"]      // "alice"
     * ```
     */
    fun save(): java.util.HashMap<Any, Any?> = storage.clone() as java.util.HashMap<Any, Any?>

    /**
     * [save]로 보관한 저장소를 현재 스레드에 복원합니다.
     *
     * ```kotlin
     * val snapshot = Local.save()
     * Local["key"] = "changed"
     * Local.restore(snapshot)       // 이전 상태로 복원
     * ```
     */
    fun restore(saved: java.util.HashMap<Any, Any?>) {
        threadLocal.set(saved)
    }

    @JvmName("getObject")
    operator fun get(key: Any): Any? = storage[key]

    operator fun <T : Any> get(key: Any): T? = storage[key] as? T

    operator fun <T : Any> set(
        key: Any,
        value: T?,
    ) {
        when (value) {
            null -> storage.remove(key)
            else -> storage[key] = value
        }
    }

    /**
     * 현재 스레드의 로컬 저장소를 모두 비웁니다.
     *
     * ```kotlin
     * Local["a"] = 1
     * Local["b"] = 2
     * Local.clearAll()
     * val a = Local["a"]  // null
     * ```
     */
    fun clearAll() {
        log.debug { "Clear local storage." }
        storage.clear()
    }

    /**
     * 키가 없으면 [defaultValue] 결과를 저장하고 반환합니다.
     *
     * ```kotlin
     * val value = Local.getOrPut("count") { 0 }  // 0 (처음에는 없으므로 저장 후 반환)
     * val same  = Local.getOrPut("count") { 99 } // 0 (이미 존재하므로 기존 값 반환)
     * ```
     */
    fun <T : Any> getOrPut(
        key: Any,
        defaultValue: () -> T?,
    ): T? = storage.getOrPut(key, defaultValue) as? T

    /**
     * 키를 제거하고 기존 값을 반환합니다.
     *
     * ```kotlin
     * Local["name"] = "alice"
     * val removed = Local.remove<String>("name")  // "alice"
     * val gone    = Local["name"]                 // null
     * ```
     */
    fun <T : Any> remove(key: Any): T? = storage.remove(key) as? T
}

/**
 * [Local]을 사용해 타입별 키를 숨긴 스토리지를 제공합니다.
 */
internal class LocalStorage<T : Any> : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    private val key: UUID = UUID.randomUUID()

    fun get(): T? = Local.get<T>(key)

    fun set(value: T?) {
        Local[key] = value
    }

    fun update(value: T?) {
        set(value)
    }

    fun clear(): T? = Local.remove(key)
}
