package io.bluetape4k.coroutines.support

import kotlinx.coroutines.CoroutineScope

/**
 * Current CoroutineContext를 특정하는 Identifier.
 *
 * Redisson이 Thread Id 기반으로 소유권을 인식하는데, Coroutines 환경에서는 `Thread.currentThred().id` 값은 계속 변할 수 있다.
 * 같은 Coroutine Scope 이라면 Redisson 객체를 소유하고 있다고 인식할 수 있도록 `currentCoroutineId` 를 사용하게 한다.
 * 특히, Lock 처럼 lock/unlock 시 같은 소유권자임을 알리기 위해 사용해야 한다
 *
 * // HINT: 아오 coroutineContext.coroutineName 이 internal 이다. 이것 때문에 coroutineContext.coroutineName 을 사용하지 못한다
 *
 * ```
 * val lock = redisson.getLock("lock")
 * launch {
 *      if(lock.tryLock(1, 60, SECOND, currentCoroutineId)) {
 *          try {
 *              // Do something
 *          } finally {
 *              lock.unlock(currentCoroutineId)
 *          }
 *      }
 * }.join()
 * ```
 */
val CoroutineScope.currentCoroutineId: Long
    get() = this.coroutineContext.hashCode().toLong()
