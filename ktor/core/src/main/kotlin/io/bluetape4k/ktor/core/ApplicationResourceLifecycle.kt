package io.bluetape4k.ktor.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.closeSafe
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.util.AttributeKey
import kotlinx.coroutines.DisposableHandle as CoroutinesDisposableHandle
import java.io.Serializable as JavaSerializable
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 등록된 애플리케이션 리소스를 닫을 때 사용한 lifecycle 단계입니다.
 */
public enum class ApplicationResourceClosePhase {
    /** 애플리케이션 종료 전에 registration token으로 직접 닫은 단계입니다. */
    EARLY,

    /** `ApplicationStopped`에 연결된 registry 종료 단계입니다. */
    SHUTDOWN,

    /** registry 종료가 시작된 뒤 새로 등록되어 즉시 닫힌 단계입니다. */
    LATE_REGISTRATION,
}

/**
 * 애플리케이션 리소스 registry의 lifecycle 상태입니다.
 */
public enum class ApplicationResourceRegistryState {
    /** 새 리소스를 등록할 수 있는 상태입니다. */
    OPEN,

    /** 기존 리소스를 닫는 중이며 새 등록은 즉시 닫히는 상태입니다. */
    DRAINING,

    /** 정상적인 registry 종료가 끝난 상태입니다. */
    CLOSED,
}

/**
 * 리소스 하나의 close 실패를 민감한 리소스 정보 없이 표현합니다.
 *
 * @property registrationId registry lifetime 안에서만 의미가 있는 opaque ID입니다.
 * @property phase close가 시도된 lifecycle 단계입니다.
 * @property fatal JVM `Error` 계열 실패인지 여부입니다.
 */
public data class ApplicationResourceCloseFailure(
    val registrationId: Long,
    val phase: ApplicationResourceClosePhase,
    val fatal: Boolean,
): JavaSerializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * registry close 진행 상황을 읽은 시점의 불변 결과입니다.
 *
 * `attempted == inFlight + closed + failures.size` invariant는 close 중에도 유지됩니다.
 * 등록된 close action은 caller thread에서 동기적으로 실행되므로 이 report는 완료를
 * 기다리거나 timeout을 제공하지 않습니다.
 *
 * @property state 결과를 읽은 순간의 registry 상태입니다.
 * @property attempted claim되어 close를 시도한 항목 수입니다.
 * @property inFlight 현재 close action이 실행 중인 항목 수입니다.
 * @property closed 예외 없이 close가 끝난 항목 수입니다.
 * @property failures 실패한 항목의 opaque 진단 정보입니다.
 */
public data class ApplicationResourceCloseReport(
    val state: ApplicationResourceRegistryState,
    val attempted: Int,
    val inFlight: Int,
    val closed: Int,
    val failures: List<ApplicationResourceCloseFailure>,
): JavaSerializable {

    init {
        require(attempted >= 0) { "attempted must be non-negative." }
        require(inFlight >= 0) { "inFlight must be non-negative." }
        require(closed >= 0) { "closed must be non-negative." }
        require(attempted == inFlight + closed + failures.size) {
            "attempted must equal inFlight + closed + failures.size."
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * registry에 등록된 항목을 application 종료 전에 조기에 닫는 token입니다.
 *
 * token과 registry close가 동시에 호출되어도 한 항목은 최대 한 번만 close됩니다.
 * close action은 token을 호출한 caller thread에서 동기적으로 실행되며, 이 API는
 * timeout·dispatcher·coroutine scope를 소유하지 않습니다.
 */
public class ApplicationResourceRegistration private constructor(
    public val id: Long,
    private val closeAction: () -> Unit,
): AutoCloseable {

    internal object Factory {
        internal fun create(
            id: Long,
            closeAction: () -> Unit,
        ): ApplicationResourceRegistration = ApplicationResourceRegistration(id, closeAction)
    }

    /**
     * 이 token이 소유한 항목을 조기에 닫습니다. 이미 claim된 token이면 아무 작업도 하지 않습니다.
     */
    override fun close(): Unit = closeAction()
}

/**
 * Ktor application이 소유한 동기식 리소스의 등록·종료 registry입니다.
 *
 * registry는 애플리케이션 시작 코드가 소유하는 trusted, bounded close action만 받습니다.
 * `close()`와 token close는 caller thread에서 동기적으로 실행되며, registry가 새 thread,
 * dispatcher, coroutine scope 또는 timeout을 만들지 않습니다. 등록 순서의 역순으로
 * shutdown close를 수행하고, 동일 객체의 중복 등록을 identity 기준으로 거부하며,
 * 종료 중 새 등록은 호출 thread에서 즉시 닫습니다.
 *
 * registry를 직접 닫는 경우 caller가 lifecycle ownership을 하나만 연결해야 합니다.
 * `installApplicationResourceLifecycle`이 반환한 registry는 installer가 `ApplicationStopped`
 * 이벤트에서 닫으므로 caller가 별도로 close하지 않아야 합니다. 이 계약은 graceful shutdown
 * 전용이며 `SIGKILL`, JVM crash, OOM 등 `ApplicationStopped`가 발생하지 않는 종료는 보장하지
 * 않습니다.
 */
public class ApplicationResourceRegistry : AutoCloseable {

    public companion object : KLogging()

    private val lock = ReentrantLock()
    private val entries = ArrayList<Entry>()
    private val ownedIdentities = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
    private val failures = ArrayList<ApplicationResourceCloseFailure>()

    private var nextRegistrationId: Long = 1L
    private var state: ApplicationResourceRegistryState = ApplicationResourceRegistryState.OPEN
    private var attempted: Int = 0
    private var inFlight: Int = 0
    private var closed: Int = 0

    /**
     * 현재 registry 상태와 close 누적 결과를 읽기 시점의 불변 값으로 반환합니다.
     */
    public val closeReport: ApplicationResourceCloseReport
        get() = lock.withLock {
            ApplicationResourceCloseReport(
                state = state,
                attempted = attempted,
                inFlight = inFlight,
                closed = closed,
                failures = failures.toList()
            )
        }

    /**
     * [resource]를 등록하고 조기 close용 token을 반환합니다.
     *
     * 동일한 [resource] identity는 registry lifetime 동안 한 번만 등록할 수 있습니다.
     * 종료가 시작된 뒤 등록하면 resource는 보관되지 않고 호출 thread에서 즉시 닫힙니다.
     */
    public fun register(resource: AutoCloseable): ApplicationResourceRegistration =
        registerInternal(resource) { resource.close() }

    /**
     * 동기식 [closeAction]을 등록하고 조기 close용 token을 반환합니다.
     *
     * 동일한 close-action 객체 identity는 registry lifetime 동안 한 번만 등록할 수 있습니다.
     * action은 application startup code가 소유하는 trusted 작업이어야 하며 유한한 시간 안에
     * 끝나야 합니다.
     */
    public fun register(closeAction: () -> Unit): ApplicationResourceRegistration =
        registerInternal(closeAction, closeAction)

    /**
     * 아직 claim되지 않은 항목을 등록 역순으로 동기적으로 닫습니다.
     *
     * 한 항목의 일반 예외나 JVM `Error`가 나도 나머지 항목을 계속 시도합니다. fatal 실패가
     * 있었으면 모든 항목을 시도한 뒤 원본 message·cause가 없는 sanitized marker를 던집니다.
     */
    override fun close() {
        val shutdownEntries = lock.withLock {
            if (state != ApplicationResourceRegistryState.OPEN) {
                return
            }

            state = ApplicationResourceRegistryState.DRAINING
            entries
                .asReversed()
                .toList()
                .onEach { entry -> claimLocked(entry) }
                .also { entries.clear() }
        }

        var fatalFailure = false
        try {
            shutdownEntries.forEach { entry ->
                fatalFailure = executeClose(entry, ApplicationResourceClosePhase.SHUTDOWN) || fatalFailure
            }
        } finally {
            lock.withLock {
                state = ApplicationResourceRegistryState.CLOSED
            }
        }

        if (fatalFailure) {
            throw SanitizedFatalCloseMarker()
        }
    }

    private fun registerInternal(
        identity: Any,
        closeAction: () -> Unit,
    ): ApplicationResourceRegistration {
        val registration = lock.withLock {
            if (!ownedIdentities.add(identity)) {
                throw IllegalArgumentException("Application resource is already registered.")
            }

            val entry = Entry(
                id = nextIdLocked(),
                closeAction = closeAction,
            )
            if (state == ApplicationResourceRegistryState.OPEN) {
                entries += entry
                RegistrationPlan(
                    registration = ApplicationResourceRegistration.Factory.create(entry.id) {
                        closeEntry(entry)
                    },
                    lateEntry = null
                )
            } else {
                claimLocked(entry)
                RegistrationPlan(
                    registration = ApplicationResourceRegistration.Factory.create(entry.id) {},
                    lateEntry = entry
                )
            }
        }

        registration.lateEntry?.let { entry ->
            val fatalFailure = executeClose(entry, ApplicationResourceClosePhase.LATE_REGISTRATION)
            if (fatalFailure) {
                throw SanitizedFatalCloseMarker()
            }
        }
        return registration.registration
    }

    private fun closeEntry(entry: Entry) {
        val claimed = lock.withLock {
            if (entry.claimed) {
                false
            } else {
                entries.remove(entry)
                claimLocked(entry)
                true
            }
        }
        if (claimed) {
            if (executeClose(entry, ApplicationResourceClosePhase.EARLY)) {
                throw SanitizedFatalCloseMarker()
            }
        }
    }

    private fun claimLocked(entry: Entry) {
        check(!entry.claimed) { "A resource entry may only be claimed once." }
        entry.claimed = true
        attempted += 1
        inFlight += 1
    }

    private fun executeClose(
        entry: Entry,
        phase: ApplicationResourceClosePhase,
    ): Boolean {
        var fatalFailure = false
        var failure: ApplicationResourceCloseFailure? = null
        entry.closeSafe { cause ->
            fatalFailure = cause is Error
            failure = ApplicationResourceCloseFailure(
                registrationId = entry.id,
                phase = phase,
                fatal = fatalFailure
            )
        }
        finishClose(failure)
        return fatalFailure
    }

    private fun finishClose(failure: ApplicationResourceCloseFailure?) {
        lock.withLock {
            check(inFlight > 0) { "A resource close must have an in-flight claim." }
            inFlight -= 1
            if (failure == null) {
                closed += 1
            } else {
                failures += failure
            }
        }

        if (failure != null) {
            try {
                log.warn {
                    "Application resource close failed: registrationId=${failure.registrationId}, " +
                        "phase=${failure.phase}, fatal=${failure.fatal}"
                }
            } catch (_: Throwable) {
                // 로깅 backend 자체의 실패가 나머지 cleanup을 중단시키면 안 됩니다.
            }
        }
    }

    private fun nextIdLocked(): Long {
        check(nextRegistrationId > 0) { "Application resource registration IDs are exhausted." }
        return nextRegistrationId++
    }

    private class Entry(
        val id: Long,
        val closeAction: () -> Unit,
        var claimed: Boolean = false,
    ): AutoCloseable {
        override fun close(): Unit = closeAction()
    }

    private data class RegistrationPlan(
        val registration: ApplicationResourceRegistration,
        val lateEntry: Entry?,
    )
}

private class SanitizedFatalCloseMarker : Error("Application resource close failed")

private val applicationResourceLifecycleKey =
    AttributeKey<ApplicationResourceLifecycleHolder>("bluetape4k.application.resource.lifecycle")

private typealias ApplicationResourceSubscriptionRegistrar =
    (onStopped: () -> Unit) -> CoroutinesDisposableHandle

/**
 * registry와 lifecycle subscription의 소유권을 같은 lock으로 직렬화합니다.
 *
 * `Attributes.computeIfAbsent`가 supplier를 둘 이상 평가할 수 있으므로 이 holder
 * 자체는 side effect 없이 만들어지고, 실제 subscription은 winner의 [install]에서
 * 정확히 한 번만 생성됩니다.
 */
private class ApplicationResourceLifecycleHolder(
    private val registrar: ApplicationResourceSubscriptionRegistrar,
    private val lock: ReentrantLock = ReentrantLock(),
) {
    private enum class State {
        NEW,
        READY,
        FAILED,
        STOPPED,
    }

    private var state = State.NEW
    private var subscription: CoroutinesDisposableHandle? = null

    val registry = ApplicationResourceRegistry()

    /**
     * subscription을 정확히 한 번 설치하고 winner의 registry를 반환합니다.
     * 설치 실패는 민감한 원인 없이 sticky `FAILED` 상태로 보존합니다.
     */
    fun install(): ApplicationResourceRegistry {
        val failure = lock.withLock {
            when (state) {
                State.READY, State.STOPPED -> return registry
                State.FAILED -> throw sanitizedInstallationFailure()
                State.NEW -> try {
                    val candidate = registrar(::onStopped)
                    subscription = candidate
                    state = State.READY
                    return registry
                } catch (_: Throwable) {
                    state = State.FAILED
                    sanitizedInstallationFailure()
                }
            }
        }

        registry.closeSafe()
        throw failure
    }

    private fun onStopped() {
        val handle = lock.withLock {
            if (state != State.READY) {
                return
            }

            state = State.STOPPED
            subscription.also { subscription = null }
        }

        try {
            registry.close()
        } finally {
            handle?.let(::disposeWithoutCauseLogging)
        }
    }

    private fun sanitizedInstallationFailure(): IllegalStateException =
        IllegalStateException("Application resource lifecycle installation failed.")

    private fun disposeWithoutCauseLogging(handle: CoroutinesDisposableHandle) {
        try {
            handle.dispose()
        } catch (_: Throwable) {
            try {
                ApplicationResourceRegistry.log.warn {
                    "Application resource lifecycle subscription disposal failed."
                }
            } catch (_: Throwable) {
                // 로깅 backend 자체의 실패가 lifecycle cleanup을 중단시키면 안 됩니다.
            }
        }
    }
}

/**
 * `ApplicationStopped`에 [ApplicationResourceRegistry]를 명시적으로 연결합니다.
 *
 * application attribute에 registry와 단일 event subscription을 원자적으로 저장하므로
 * 같은 application에서 반복 또는 동시 호출해도 동일 registry를 반환합니다. `ApplicationStopped`
 * callback은 registry를 동기적으로 닫고 subscription을 dispose합니다. caller는 반환된
 * registry를 직접 close하지 말고, 등록 action이 startup 소유의 bounded 동기 작업인지 확인해야
 * 합니다. Ktor disposal timeout은 이 callback을 중단하지 않으며, `ApplicationStopped`가
 * 발생하지 않는 강제 종료에서는 cleanup을 보장하지 않습니다.
 */
public fun Application.installApplicationResourceLifecycle(): ApplicationResourceRegistry {
    val slot = attributes.computeIfAbsent(applicationResourceLifecycleKey) {
        ApplicationResourceLifecycleHolder(
            { onStopped ->
                monitor.subscribe(ApplicationStopped) { onStopped() }
            }
        )
    }
    return slot.install()
}
