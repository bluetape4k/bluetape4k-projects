package io.bluetape4k.hibernate.spring.stateless

import io.bluetape4k.hibernate.asSessionImpl
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.aopalliance.intercept.MethodInvocation
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession
import org.hibernate.internal.StatelessSessionImpl
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.ReflectionUtils
import java.sql.Connection

/**
 * Provides a Hibernate [StatelessSession] proxy bound to the current Spring transaction.
 *
 * ## Contract
 * - [getObject] returns a proxy; the real session is resolved lazily for each method call.
 * - Calls require an active Spring transaction and fail with [IllegalStateException] outside one.
 * - The stateless session is stored under a dedicated transaction resource key, so it cannot
 *   collide with Spring's `SessionFactory` or `EntityManager` resource binding.
 * - The session created by this factory is unbound and closed when the transaction completes.
 *
 * ```kotlin
 * statelessSession.insert(entity)
 * ```
 *
 * @param sf Hibernate [SessionFactory] used to create stateless sessions.
 */
class StatelessSessionFactoryBean(
    @field:Autowired val sf: SessionFactory,
): FactoryBean<StatelessSession> {

    companion object: KLogging()

    override fun getObject(): StatelessSession {
        val interceptor = StatelessSessionInterceptor(sf)
        return ProxyFactory.getProxy(StatelessSession::class.java, interceptor)
    }

    override fun getObjectType(): Class<*> {
        return StatelessSession::class.java
    }

    class StatelessSessionInterceptor(private val sf: SessionFactory): org.aopalliance.intercept.MethodInterceptor {

        private val resourceKey = StatelessSessionResourceKey(sf)

        override fun invoke(invocation: MethodInvocation): Any? {
            val stateless = getCurrentStatelessSession()
            return ReflectionUtils.invokeMethod(invocation.method, stateless, *invocation.arguments)
        }

        private fun getCurrentStatelessSession(): StatelessSession {
            check(TransactionSynchronizationManager.isActualTransactionActive()) {
                "현 스레드에 활성화된 Transaction이 없습니다. StatelessSession은 Transaction하에서만 작동됩니다."
            }

            return TransactionSynchronizationManager.getResource(resourceKey) as? StatelessSession
                ?: run {
                    log.info { "현 스레드에 새로운 StatelessSession 인스턴스를 생성합니다." }
                    newStatelessSession().apply {
                        bindWithTransaction(this)
                    }
                }
        }

        private fun newStatelessSession(): StatelessSession {
            val conn = obtainPysicalConnection()
            return sf.openStatelessSession(conn)
        }

        private fun obtainPysicalConnection(): Connection? {
            val em = EntityManagerFactoryUtils.getTransactionalEntityManager(sf)
            val session = em?.asSessionImpl()
            return session?.jdbcCoordinator?.logicalConnection?.physicalConnection
        }

        private fun bindWithTransaction(stateless: StatelessSession) {
            log.debug { "bind stateless session with transaction. statelessSession=$stateless" }
            TransactionSynchronizationManager.registerSynchronization(
                StatelessSessionSynchronization(resourceKey, stateless)
            )
            TransactionSynchronizationManager.bindResource(resourceKey, stateless)
        }
    }

    private class StatelessSessionResourceKey(
        private val sessionFactory: SessionFactory,
    ) {
        override fun equals(other: Any?): Boolean {
            return other is StatelessSessionResourceKey && sessionFactory === other.sessionFactory
        }

        override fun hashCode(): Int {
            return System.identityHashCode(sessionFactory)
        }

        override fun toString(): String {
            return "StatelessSessionResourceKey(sessionFactory=$sessionFactory)"
        }
    }

    private class StatelessSessionSynchronization(
        private val resourceKey: StatelessSessionResourceKey,
        private val stateless: StatelessSession,
    ): TransactionSynchronization {

        override fun getOrder(): Int {
            return EntityManagerFactoryUtils.ENTITY_MANAGER_SYNCHRONIZATION_ORDER - 100
        }

        override fun beforeCommit(readOnly: Boolean) {
            if (!readOnly) {
                (stateless as? StatelessSessionImpl)?.flushBeforeTransactionCompletion()
            }
        }

        override fun beforeCompletion() {
            try {
                if (TransactionSynchronizationManager.getResource(resourceKey) === stateless) {
                    TransactionSynchronizationManager.unbindResource(resourceKey)
                }
            } finally {
                stateless.close()
            }
        }
    }
}
