package io.bluetape4k.r2dbc.pool

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.r2dbc.spi.Closeable as R2dbcCloseable
import io.r2dbc.spi.ConnectionFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/** registry가 각 [ConnectionFactory]를 종료할 책임을 가지는지 나타냅니다. */
enum class ConnectionFactoryOwnership {
    /** factory의 lifecycle은 caller가 소유하므로 registry가 종료하지 않습니다. */
    BORROWED,

    /** factory의 lifecycle을 registry가 소유하고 종료합니다. */
    OWNED,
}

/**
 * registry에 등록할 factory와 lifecycle 정책을 함께 표현합니다.
 *
 * [borrowed]는 조회만 허용하고 resource를 종료하지 않습니다. [owned]는
 * R2DBC [R2dbcCloseable] 또는 Reactor [Disposable] resource를 종료하며,
 * closeable 구현이 없는 factory는 명시적인 close action을 받아야 합니다.
 */
sealed interface R2dbcConnectionFactoryEntry {
    val connectionFactory: ConnectionFactory
    val ownership: ConnectionFactoryOwnership

    companion object {
        /** caller가 resource를 소유하는 entry를 만듭니다. */
        fun borrowed(connectionFactory: ConnectionFactory): R2dbcConnectionFactoryEntry =
            BorrowedEntry(connectionFactory)

        /** closeable/disposable resource를 registry가 소유하는 entry를 만듭니다. */
        fun owned(connectionFactory: ConnectionFactory): R2dbcConnectionFactoryEntry {
            val closeAction: () -> Mono<Void>
            when (connectionFactory) {
                is R2dbcCloseable -> {
                    closeAction = { Mono.from(connectionFactory.close()).then() }
                }

                is Disposable -> {
                    closeAction = {
                        connectionFactory.dispose()
                        Mono.empty()
                    }
                }

                else ->
                    throw IllegalArgumentException(
                        "An owned ConnectionFactory must implement " +
                            "io.r2dbc.spi.Closeable or reactor.core.Disposable. " +
                            "Use owned(connectionFactory, closeAction) for a custom lifecycle.",
                    )
            }
            return OwnedEntry(connectionFactory, closeAction, connectionFactory)
        }

        /** caller가 비동기 종료 동작을 명시하는 owned entry를 만듭니다. */
        fun owned(
            connectionFactory: ConnectionFactory,
            closeAction: () -> Mono<Void>,
        ): R2dbcConnectionFactoryEntry {
            requireNotNull(closeAction) { "closeAction must not be null" }
            return OwnedEntry(connectionFactory, closeAction, closeAction)
        }
    }
}

private val borrowedLifecycleIdentity = Any()

private class BorrowedEntry(
    override val connectionFactory: ConnectionFactory,
) : R2dbcConnectionFactoryEntry {
    override val ownership: ConnectionFactoryOwnership = ConnectionFactoryOwnership.BORROWED
}

private class OwnedEntry(
    override val connectionFactory: ConnectionFactory,
    val closeAction: () -> Mono<Void>,
    val lifecycleIdentity: Any,
) : R2dbcConnectionFactoryEntry {
    override val ownership: ConnectionFactoryOwnership = ConnectionFactoryOwnership.OWNED
}

private fun R2dbcConnectionFactoryEntry.lifecycleIdentity(): Any =
    when (this) {
        is BorrowedEntry -> borrowedLifecycleIdentity
        is OwnedEntry -> lifecycleIdentity
    }

private fun R2dbcConnectionFactoryEntry.closeResource(): Mono<Void> =
    when (this) {
        is BorrowedEntry -> Mono.empty()
        is OwnedEntry -> closeAction()
    }

/**
 * 임의의 non-null key를 R2DBC [ConnectionFactory]에 매핑하는 정적 registry입니다.
 *
 * registry는 생성 시점의 map을 snapshot으로 보관합니다. key parsing, tenant
 * authorization, request context, transaction, pool 생성은 caller 책임입니다.
 * resource를 종료할 책임은 [R2dbcConnectionFactoryEntry.ownership]으로
 * 명시해야 하며, 등록된 factory의 identity가 중복되면 ownership와 lifecycle
 * action이 일치해야 합니다.
 */
class R2dbcConnectionFactoryRegistry<K : Any>(
    entries: Map<K, R2dbcConnectionFactoryEntry>,
) : R2dbcCloseable, Disposable {

    private val entries: Map<K, R2dbcConnectionFactoryEntry>
    private val factories: Map<K, ConnectionFactory>
    private val keySnapshot: Set<K>
    private val ownedEntries: List<R2dbcConnectionFactoryEntry>
    private val closeSignal = AtomicReference<Mono<Void>?>(null)

    init {
        val entrySnapshot = LinkedHashMap<K, R2dbcConnectionFactoryEntry>(entries.size)
        entrySnapshot.putAll(entries)
        validateAliases(entrySnapshot.values)

        this.entries = Collections.unmodifiableMap(entrySnapshot)
        this.factories = Collections.unmodifiableMap(
            LinkedHashMap<K, ConnectionFactory>(entrySnapshot.size).apply {
                entrySnapshot.forEach { (key, entry) -> put(key, entry.connectionFactory) }
            },
        )
        this.keySnapshot = Collections.unmodifiableSet(LinkedHashSet(entrySnapshot.keys))
        this.ownedEntries = distinctOwnedEntries(entrySnapshot.values)
    }

    /** 생성 당시 등록된 key의 immutable snapshot입니다. */
    val keys: Set<K>
        get() = keySnapshot

    /**
     * 열린 registry에서 key에 해당하는 factory를 반환합니다.
     *
     * 등록되지 않은 key는 다른 factory로 fallback하지 않고
     * [NoSuchElementException]을 던집니다. 종료가 시작된 뒤의 lookup은
     * [IllegalStateException]으로 거부합니다.
     */
    operator fun get(key: K): ConnectionFactory {
        checkOpen()
        return entries[key]?.connectionFactory
            ?: throw NoSuchElementException(
                "No ConnectionFactory configured for key '$key'. Configured keys: $keySnapshot",
            )
    }

    /** 열린 registry의 key-to-factory immutable snapshot을 반환합니다. */
    fun asMap(): Map<K, ConnectionFactory> {
        checkOpen()
        return factories
    }

    /**
     * key mapper를 적용한 routing map을 만듭니다.
     *
     * mapper 결과가 중복되면 한 factory가 조용히 덮어써지지 않도록
     * [IllegalArgumentException]으로 실패합니다.
     */
    fun <R : Any> routingMap(keyMapper: (K) -> R): Map<R, ConnectionFactory> {
        checkOpen()
        val mapped = LinkedHashMap<R, ConnectionFactory>(entries.size)
        entries.forEach { (key, entry) ->
            val mappedKey = keyMapper(key)
            require(!mapped.containsKey(mappedKey)) {
                "Routing key '$mappedKey' is produced more than once."
            }
            mapped[mappedKey] = entry.connectionFactory
        }
        return Collections.unmodifiableMap(mapped)
    }

    /**
     * owned resource를 모두 종료하는 cached signal을 반환합니다.
     *
     * 모든 종료를 시도한 후 첫 오류를 반환하며, 이후 오류는 첫 오류의
     * suppressed 예외로 보존합니다. 동시 호출은 동일한 종료 signal을 공유합니다.
     */
    override fun close(): Mono<Void> {
        val existing = closeSignal.get()
        if (existing != null) return existing

        val signal = Mono.defer { closeOwnedEntries() }.cache()
        val selected = if (closeSignal.compareAndSet(null, signal)) {
            signal
        } else {
            closeSignal.get() ?: signal
        }
        return selected
    }

    /** 종료 signal을 구독하는 fire-and-forget lifecycle adapter입니다. */
    override fun dispose() {
        close().subscribe({}, { failure ->
            log.warn(failure) { "R2DBC ConnectionFactory registry dispose failed" }
        })
    }

    /** close가 시작되었거나 완료되었는지 반환합니다. */
    override fun isDisposed(): Boolean = closeSignal.get() != null

    private fun checkOpen() {
        check(closeSignal.get() == null) { "ConnectionFactory registry is closed" }
    }

    private fun closeOwnedEntries(): Mono<Void> {
        val failures = CopyOnWriteArrayList<Throwable>()
        return Flux.fromIterable(ownedEntries)
            .concatMap { entry ->
                Mono.defer { entry.closeResource() }
                    .onErrorResume { failure ->
                        failures += failure
                        Mono.empty()
                    }
            }
            .then(
                Mono.defer {
                    val primary = failures.firstOrNull()
                    if (primary == null) {
                        Mono.empty()
                    } else {
                        failures.drop(1).forEach(primary::addSuppressed)
                        Mono.error(primary)
                    }
                },
            )
    }

    private fun validateAliases(values: Collection<R2dbcConnectionFactoryEntry>) {
        val aliases = IdentityHashMap<ConnectionFactory, R2dbcConnectionFactoryEntry>()
        values.forEach { entry ->
            val previous = aliases.putIfAbsent(entry.connectionFactory, entry)
            if (previous != null) {
                require(previous.ownership == entry.ownership) {
                    "The same ConnectionFactory cannot be both borrowed and owned."
                }
                require(previous.lifecycleIdentity() === entry.lifecycleIdentity()) {
                    "The same ConnectionFactory must use one lifecycle action."
                }
            }
        }
    }

    private fun distinctOwnedEntries(
        values: Collection<R2dbcConnectionFactoryEntry>,
    ): List<R2dbcConnectionFactoryEntry> {
        val seen = Collections.newSetFromMap(IdentityHashMap<ConnectionFactory, Boolean>())
        return values.filter { it.ownership == ConnectionFactoryOwnership.OWNED }
            .filter { seen.add(it.connectionFactory) }
    }

    companion object : KLogging() {
        /** 모든 factory를 borrowed entry로 감싸는 registry를 만듭니다. */
        fun <K : Any> borrowed(
            entries: Map<K, ConnectionFactory>,
        ): R2dbcConnectionFactoryRegistry<K> {
            val wrapped = entries.mapValues { (_, factory) -> R2dbcConnectionFactoryEntry.borrowed(factory) }
            return R2dbcConnectionFactoryRegistry(wrapped)
        }

        /** 모든 factory를 owned entry로 감싸는 registry를 만듭니다. */
        fun <K : Any> owned(
            entries: Map<K, ConnectionFactory>,
        ): R2dbcConnectionFactoryRegistry<K> {
            val wrapped = entries.mapValues { (_, factory) -> R2dbcConnectionFactoryEntry.owned(factory) }
            return R2dbcConnectionFactoryRegistry(wrapped)
        }
    }
}
