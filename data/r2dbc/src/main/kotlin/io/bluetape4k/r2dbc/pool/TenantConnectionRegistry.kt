package io.bluetape4k.r2dbc.pool

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 정적으로 구성된 tenant key와 R2DBC connection factory를 연결하는 조회 계약입니다.
 *
 * registry는 생성 시 전달받은 map의 복사본만 보관하므로 조회와 routing map 변환은
 * 동시 호출에서 서로의 상태를 오염시키지 않습니다. key가 없으면 [NoSuchElementException]을
 * 즉시 던집니다. 등록/해제나 요청 context 해석은 이 계약의 책임이 아닙니다.
 *
 * 구현체가 factory 또는 pool의 lifecycle을 소유하는지는 구현체의 API와 KDoc에서 명시합니다.
 * [TenantConnectionFactoryRegistry]는 caller-owned factory 조회만 제공하며, 실제 pool을
 * 종료하지 않습니다. [TenantConnectionPoolRegistry]는 전달받은 pool을 소유하고 `close()`합니다.
 */
interface TenantConnectionRegistry<K: Any, out F: ConnectionFactory> {

    /** registry 생성 시 구성된 tenant key의 불변 복사본입니다. */
    val configuredKeys: Set<K>

    /**
     * [key]에 해당하는 factory를 반환합니다.
     *
     * @throws NoSuchElementException [key]가 구성되어 있지 않을 때
     */
    operator fun get(key: K): F

    /**
     * registry가 보관한 key와 factory를 framework-neutral routing map으로 반환합니다.
     * 반환된 map은 registry의 내부 상태를 변경하지 않는 복사본입니다.
     */
    fun asRoutingMap(): Map<K, F>

    /**
     * 외부 routing key로 변환한 routing map을 생성합니다.
     *
     * key parsing이나 request context 해석은 하지 않고, 호출자가 제공한 mapper만 적용합니다.
     * 서로 다른 tenant key가 같은 routing key로 변환되면 무음 덮어쓰기를 허용하지 않고
     * [IllegalArgumentException]을 던집니다.
     */
    fun <R: Any> asRoutingMap(keyMapper: (K) -> R): Map<R, F> {
        val mappedRoutes = LinkedHashMap<R, F>()
        asRoutingMap().forEach { (key, factory) ->
            val routingKey = keyMapper(key)
            require(!mappedRoutes.containsKey(routingKey)) {
                "Duplicate routing key mapped from configured tenant keys."
            }
            mappedRoutes[routingKey] = factory
        }
        return mappedRoutes
    }

}

/**
 * caller가 lifecycle을 소유하는 connection factory 조회 registry입니다.
 *
 * 전달된 [factories]의 복사본을 보관하며, 값이 [ConnectionPool]이어도 이 타입은 pool을
 * 종료하지 않습니다. pool을 registry lifecycle에 맡겨야 한다면 [TenantConnectionPoolRegistry]를
 * 사용하세요. 이 타입은 [AutoCloseable]을 구현하지 않으므로 ownership을 API에서 구분합니다.
 */
class TenantConnectionFactoryRegistry<K: Any>(
    factories: Map<K, ConnectionFactory>,
): TenantConnectionRegistry<K, ConnectionFactory> {

    private val routes = RegistryRoutes(factories)

    override val configuredKeys: Set<K>
        get() = routes.configuredKeys

    override fun get(key: K): ConnectionFactory = routes[key]

    override fun asRoutingMap(): Map<K, ConnectionFactory> = routes.asMap()
}

/**
 * registry가 lifecycle을 소유하는 tenant별 [ConnectionPool] 조회 registry입니다.
 *
 * [pools]는 생성 시 복사되며 동적 tenant onboarding을 지원하지 않습니다. [close]는
 * registry가 소유한 각 pool의 [ConnectionPool.dispose]를 최대 한 번 호출하고, 한 pool의
 * 종료가 실패해도 나머지 pool을 계속 정리합니다. 여러 종료가 실패하면 첫 실패를 그대로
 * 던지고 이후 실패를 suppressed exception으로 연결합니다. 단, JVM [Error]는 복구 가능한
 * 종료 실패로 집계하지 않고 즉시 전파하므로 이후 pool 정리를 중단합니다.
 *
 * `close()`가 시작되면 registry는 닫힌 것으로 표시되고 이후 조회는 [IllegalStateException]으로
 * 실패합니다. 첫 종료가 실패해도 이후 `close()`는 재시도하지 않고 같은 실패를 다시 던집니다.
 * 동일한 pool instance가 여러 key에 연결된 경우에도 identity 기준으로 한 번만 종료합니다. 진행
 * 중인 조회와 `close()`의 동시 실행은 지원하지 않으므로 lifecycle adapter가 둘을 직렬화해야
 * 합니다. 여러 thread가 `close()`를 호출하면 첫 종료가 끝날 때까지 직렬화되고 같은 실패 결과를
 * 관찰합니다.
 *
 * @param closeDispatcher [closeSuspending]에서 blocking pool dispose를 격리할 dispatcher
 */
class TenantConnectionPoolRegistry<K: Any>(
    pools: Map<K, ConnectionPool>,
    private val closeDispatcher: CoroutineDispatcher = Dispatchers.IO,
): TenantConnectionRegistry<K, ConnectionPool>, AutoCloseable {

    private val poolSnapshot = LinkedHashMap(pools)
    private val routes = RegistryRoutes(poolSnapshot)
    private val ownedPools = distinctByIdentity(poolSnapshot.values)
    private val closed = AtomicBoolean(false)
    private val closeLock = ReentrantLock()
    private var closeFailure: Throwable? = null

    override val configuredKeys: Set<K>
        get() = routes.configuredKeys

    override fun get(key: K): ConnectionPool {
        ensureOpen()
        return routes[key]
    }

    override fun asRoutingMap(): Map<K, ConnectionPool> {
        ensureOpen()
        return routes.asMap()
    }

    /**
     * 소유한 pool을 순서대로 종료합니다.
     *
     * 종료는 동기적으로 실행되며, 첫 실패 이후에도 모든 pool을 시도합니다. 이 메서드는
     * framework lifecycle callback에 직접 결합되지 않으므로 caller가 적절한 adapter에서
     * 호출해야 합니다. event-loop에서 직접 호출하지 말고 blocking lifecycle executor에서
     * 실행해야 합니다. 여기서 실패는 [Exception]을 뜻하며 [Error]는 즉시 전파합니다.
     */
    override fun close() = closeLock.withLock {
        throwFailure(closeFailure)
        if (!closed.compareAndSet(false, true)) {
            return@withLock
        }

        val failure = disposeOwnedPools()
        closeFailure = failure
        throwFailure(failure)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun disposeOwnedPools(): Throwable? {
        var firstFailure: Throwable? = null
        ownedPools.forEach { pool ->
            try {
                pool.dispose()
            } catch (failure: Error) {
                closeFailure = failure
                throw failure
            } catch (failure: Exception) {
                val primary = firstFailure
                if (primary == null) {
                    firstFailure = failure
                } else if (primary !== failure) {
                    primary.addSuppressed(failure)
                }
            }
        }
        return firstFailure
    }

    /**
     * coroutine caller의 event-loop를 차단하지 않도록 [close]를 [Dispatchers.IO]에서 실행합니다.
     * 이미 취소된 caller에서도 [NonCancellable] 경계에서 cleanup을 완료한 뒤 caller cancellation을
     * 다시 전파합니다. concurrent 종료, idempotency와 실패 전파 계약은 [close]와 같습니다.
     */
    suspend fun closeSuspending() {
        val callerContext = currentCoroutineContext()
        withContext(NonCancellable + closeDispatcher) {
            close()
        }
        callerContext.ensureActive()
    }

    private fun ensureOpen() {
        check(!closed.get()) { "Tenant connection pool registry is closed." }
    }

    private fun throwFailure(failure: Throwable?) {
        if (failure != null) {
            throw failure
        }
    }

    private companion object {
        fun distinctByIdentity(pools: Collection<ConnectionPool>): List<ConnectionPool> {
            val seen = IdentityHashMap<ConnectionPool, Boolean>()
            return pools.filter { seen.put(it, true) == null }
        }
    }
}

private class RegistryRoutes<K: Any, F: ConnectionFactory>(
    factories: Map<K, F>,
) {

    private val routes: Map<K, F> = LinkedHashMap(factories)

    private val configuredKeySnapshot: Set<K> =
        Collections.unmodifiableSet(LinkedHashSet(routes.keys))

    val configuredKeys: Set<K>
        get() = configuredKeySnapshot

    operator fun get(key: K): F = routes[key]
        ?: throw NoSuchElementException("No connection factory is configured for the requested tenant key.")

    fun asMap(): Map<K, F> = LinkedHashMap(routes)
}
