@file:JvmName("NearJCacheMBeans")

package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.cache.nearcache.jcache.NearJCache
import java.io.Serializable
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.management.ImmutableDescriptor
import javax.management.InstanceNotFoundException
import javax.management.MBeanInfo
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean
import kotlin.concurrent.withLock

private const val NEAR_JCACHE_OWNERSHIP_DESCRIPTOR_FIELD = "nearJCacheRegistrationToken"

private const val NEAR_JCACHE_MBEAN_DOMAIN = "io.bluetape4k.cache"
private const val MAX_MBEAN_ID_LENGTH = 256
private const val CONFIGURATION_MBEAN_TYPE = "NearJCacheConfiguration"
private const val STATISTICS_MBEAN_TYPE = "NearJCacheStatistics"

/**
 * 이 [NearJCache]의 configured management/statistics MXBean을 명시적으로 등록합니다.
 *
 * [mBeanServer]는 caller 소유이며 이 함수와 반환 handle은 server 자체를 닫지 않습니다.
 * [managerId]와 [cacheId]는 ObjectName과 실패 예외에 노출되는 안정적인 opaque ID이므로
 * credential, access token, 개인 식별 정보(PII)를 넣지 마세요. 두 ID는 원문 그대로
 * case-sensitive하게 사용하며 Unicode normalization을 수행하지 않습니다.
 *
 * handle이 열린 동안 caller는 반환된 exact ObjectName을 외부에서
 * unregister/re-register하지 않는 exclusive namespace를 보장해야 합니다. ownership token
 * 검사는 cleanup 전에 완료된 교체를 방어하지만 JMX가 token 비교와 unregister의 원자 CAS를
 * 제공하지 않으므로 이 선행 조건을 대체하지 않습니다.
 *
 * ```kotlin
 * val registration = nearCache.registerMBeans(
 *     mBeanServer = server,
 *     managerId = "prod-orders-app",
 *     cacheId = "user-profile",
 * )
 * registration.close()
 * ```
 *
 * Java에서는
 * `NearJCacheMBeans.registerMBeans(nearCache, server, managerId, cacheId)`로 호출합니다.
 *
 * @throws IllegalArgumentException ID가 1..256자 범위를 벗어나거나 blank, 앞뒤 whitespace,
 * ISO control character를 포함하는 경우
 * @throws IllegalStateException management와 statistics가 모두 비활성화된 경우
 */
@Suppress("TooGenericExceptionCaught")
fun NearJCache<*, *>.registerMBeans(
    mBeanServer: MBeanServer,
    managerId: String,
    cacheId: String,
): NearJCacheMBeanRegistration {
    validateMBeanId("managerId", managerId)
    validateMBeanId("cacheId", cacheId)

    val snapshot = configurationSnapshot
    check(snapshot.managementEnabled || snapshot.statisticsEnabled) {
        "NearJCache management and statistics are both disabled"
    }

    val owned = LinkedHashMap<ObjectName, String>()
    try {
        if (snapshot.managementEnabled) {
            val token = newOwnershipToken()
            val objectInstance = mBeanServer.registerMBean(
                OwnedStandardMBean(
                    NearJCacheManagementMXBean.fromSnapshot(snapshot) as NearJCacheConfigurationMXBean,
                    NearJCacheConfigurationMXBean::class.java,
                    token,
                ),
                nearJCacheObjectName(CONFIGURATION_MBEAN_TYPE, managerId, cacheId),
            )
            owned[objectInstance.objectName] = token
        }
        if (snapshot.statisticsEnabled) {
            val token = newOwnershipToken()
            val objectInstance = mBeanServer.registerMBean(
                OwnedStandardMBean(
                    NearJCacheStatisticsMXBean.fromRecorder(statisticsRecorder) as NearJCacheTierStatisticsMXBean,
                    NearJCacheTierStatisticsMXBean::class.java,
                    token,
                ),
                nearJCacheObjectName(STATISTICS_MBEAN_TYPE, managerId, cacheId),
            )
            owned[objectInstance.objectName] = token
        }
    } catch (registrationFailure: Throwable) {
        if (owned.isEmpty()) throw registrationFailure
        val recovery = DefaultNearJCacheMBeanRegistration(mBeanServer, managerId, cacheId, owned)
        recovery.rollbackAfterRegistrationFailure(registrationFailure)
    }

    return DefaultNearJCacheMBeanRegistration(mBeanServer, managerId, cacheId, owned)
}

/**
 * 명시적으로 등록한 NearJCache MXBean의 cleanup handle입니다.
 *
 * [activeObjectNames]는 handle이 아직 소유권을 추적하는 이름의 immutable snapshot입니다.
 * [close]는 해당 이름만 해제하며 caller-owned `MBeanServer`, back cache, provider를 닫지 않습니다.
 */
interface NearJCacheMBeanRegistration: AutoCloseable {
    val managerId: String
    val cacheId: String
    val state: NearJCacheMBeanRegistrationState
    val activeObjectNames: Set<ObjectName>
    val isClosed: Boolean
    override fun close()
}

/** NearJCache MXBean registration handle의 cleanup 상태입니다. */
enum class NearJCacheMBeanRegistrationState {
    /** 등록된 이름을 정상적으로 추적하고 있습니다. */
    REGISTERED,

    /** 일부 이름을 안전하게 해제하지 못해 재시도가 필요합니다. */
    RECOVERY_REQUIRED,

    /** 현재 cleanup attempt가 진행 중입니다. */
    CLOSING,

    /** 추적하던 이름을 모두 해제했습니다. */
    CLOSED,
}

/**
 * MXBean rollback 또는 cleanup이 일부 완료되지 않았음을 나타냅니다.
 *
 * [recoveryRegistration]은 현재 process에서 즉시 재시도하기 위한 transient handle입니다.
 * 직렬화 뒤에는 `null`이며 [remainingObjectNames]만 immutable 진단 정보로 남습니다.
 * ObjectName에는 caller가 제공한 ID가 포함되므로 ID에 credential, token, PII를 넣지 마세요.
 */
class NearJCacheMBeanRegistrationException(
    @Transient val recoveryRegistration: NearJCacheMBeanRegistration?,
    remainingObjectNames: Set<ObjectName>,
    cause: Throwable,
): RuntimeException(cause), Serializable {
    val remainingObjectNames: Set<ObjectName> =
        Collections.unmodifiableSet(LinkedHashSet(remainingObjectNames))

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private fun nearJCacheObjectName(
    type: String,
    managerId: String,
    cacheId: String,
): ObjectName {
    validateMBeanId("managerId", managerId)
    validateMBeanId("cacheId", cacheId)
    return ObjectName(
        "$NEAR_JCACHE_MBEAN_DOMAIN:type=$type," +
                "manager=${ObjectName.quote(managerId)},cache=${ObjectName.quote(cacheId)}"
    )
}

private fun validateMBeanId(name: String, value: String) {
    require(value.length in 1..MAX_MBEAN_ID_LENGTH) {
        "$name must contain 1..$MAX_MBEAN_ID_LENGTH characters"
    }
    require(value.isNotBlank()) { "$name must not be blank" }
    require(value == value.trim()) { "$name must not contain leading or trailing whitespace" }
    require(value.none(Char::isISOControl)) { "$name must not contain ISO control characters" }
}

private fun newOwnershipToken(): String = UUID.randomUUID().toString()

private class OwnedStandardMBean<I: Any>(
    implementation: I,
    mBeanInterface: Class<I>,
    private val ownershipToken: String,
): StandardMBean(implementation, mBeanInterface, true) {

    override fun getMBeanInfo(): MBeanInfo {
        val info = super.getMBeanInfo()
        val descriptor = ImmutableDescriptor.union(
            info.descriptor,
            ImmutableDescriptor(mapOf(NEAR_JCACHE_OWNERSHIP_DESCRIPTOR_FIELD to ownershipToken)),
        )
        return MBeanInfo(
            info.className,
            info.description,
            info.attributes,
            info.constructors,
            info.operations,
            info.notifications,
            descriptor,
        )
    }
}

private class DefaultNearJCacheMBeanRegistration(
    private val mBeanServer: MBeanServer,
    override val managerId: String,
    override val cacheId: String,
    ownedNames: Map<ObjectName, String>,
): NearJCacheMBeanRegistration {
    private val stateLock = ReentrantLock()
    private val owned = LinkedHashMap(ownedNames)

    @Volatile
    private var currentState = NearJCacheMBeanRegistrationState.REGISTERED

    override val state: NearJCacheMBeanRegistrationState
        get() = currentState

    override val activeObjectNames: Set<ObjectName>
        get() = stateLock.withLock {
            Collections.unmodifiableSet(LinkedHashSet(owned.keys))
        }

    override val isClosed: Boolean
        get() = currentState == NearJCacheMBeanRegistrationState.CLOSED

    override fun close() {
        val names = stateLock.withLock {
            if (currentState == NearJCacheMBeanRegistrationState.CLOSED) return
            check(currentState != NearJCacheMBeanRegistrationState.CLOSING) {
                "NearJCache MBean cleanup is already in progress"
            }
            currentState = NearJCacheMBeanRegistrationState.CLOSING
            owned.entries.map { it.key to it.value }.asReversed()
        }

        val result = cleanup(names)
        stateLock.withLock {
            result.removedNames.forEach(owned::remove)
            currentState = if (owned.isEmpty()) {
                NearJCacheMBeanRegistrationState.CLOSED
            } else {
                NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED
            }
        }
        if (result.failures.isNotEmpty()) {
            val primary = result.failures.first()
            result.failures.drop(1).forEach(primary::addSuppressed)
            throw NearJCacheMBeanRegistrationException(this, activeObjectNames, primary)
        }
    }

    fun rollbackAfterRegistrationFailure(registrationFailure: Throwable): Nothing {
        val names = stateLock.withLock {
            currentState = NearJCacheMBeanRegistrationState.CLOSING
            owned.entries.map { it.key to it.value }.asReversed()
        }
        val result = cleanup(names)
        result.failures.forEach { rollbackFailure ->
            if (rollbackFailure !== registrationFailure) registrationFailure.addSuppressed(rollbackFailure)
        }
        stateLock.withLock {
            result.removedNames.forEach(owned::remove)
            currentState = if (owned.isEmpty()) {
                NearJCacheMBeanRegistrationState.CLOSED
            } else {
                NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED
            }
        }
        if (activeObjectNames.isEmpty()) throw registrationFailure
        throw NearJCacheMBeanRegistrationException(this, activeObjectNames, registrationFailure)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun cleanup(names: List<Pair<ObjectName, String>>): CleanupResult {
        val removed = LinkedHashSet<ObjectName>()
        val failures = mutableListOf<Throwable>()
        names.forEach { (objectName, expectedToken) ->
            try {
                val actualToken = mBeanServer.getMBeanInfo(objectName)
                    .descriptor
                    .getFieldValue(NEAR_JCACHE_OWNERSHIP_DESCRIPTOR_FIELD)
                if (actualToken != expectedToken) {
                    failures += IllegalStateException(
                        "NearJCache MBean ownership changed before cleanup: $objectName"
                    )
                } else {
                    try {
                        mBeanServer.unregisterMBean(objectName)
                        removed += objectName
                    } catch (_: InstanceNotFoundException) {
                        removed += objectName
                    }
                }
            } catch (_: InstanceNotFoundException) {
                removed += objectName
            } catch (cleanupFailure: Throwable) {
                failures += cleanupFailure
            }
        }
        return CleanupResult(removed, failures)
    }

    private data class CleanupResult(
        val removedNames: Set<ObjectName>,
        val failures: List<Throwable>,
    )
}
