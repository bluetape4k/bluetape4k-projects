package io.bluetape4k.spring.jpa.stateless

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
 * Spring 트랜잭션에 바인딩된 Hibernate [StatelessSession] 프록시를 제공하는 FactoryBean입니다.
 *
 * ## 동작/계약
 * - `getObject()`는 매 호출마다 프록시를 반환하고, 실제 세션은 메서드 호출 시 트랜잭션 컨텍스트에서 조회/생성됩니다.
 * - 활성 트랜잭션이 없으면 인터셉터의 `check(...)`에 의해 `IllegalStateException`이 발생합니다.
 * - 현재 트랜잭션 리소스에 세션이 없으면 JDBC 연결 기반으로 새 `StatelessSession`을 열고 트랜잭션 종료 시 close 합니다.
 * - 수신 객체를 직접 변경하지 않으며, 트랜잭션 리소스 바인딩으로 동작을 연결합니다.
 *
 * ```kotlin
 * entityManager.withStateless { stateless ->
 *     repeat(COUNT) {
 *         val master = createMaster("master-$it")
 *         stateless.insert(master)
 *         master.details.forEach { detail ->
 *             stateless.insert(detail)
 *         }
 *     }
 * }
 * ```
 *
 * 참고 : https://gist.github.com/jelies/5181262
 *
 * @param sf 트랜잭션 리소스 키와 세션 생성을 위한 Hibernate [SessionFactory]
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

        override fun invoke(invocation: MethodInvocation): Any? {
            val stateless = getCurrentStatelessSession()
            return ReflectionUtils.invokeMethod(invocation.method, stateless, invocation.arguments)
        }

        private fun getCurrentStatelessSession(): StatelessSession {
            check(TransactionSynchronizationManager.isActualTransactionActive()) {
                "현 스레드에 활성화된 Transaction이 없습니다. StatelessSession은 Transaction하에서만 작동됩니다."
            }

            return TransactionSynchronizationManager.getResource(sf) as? StatelessSession
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
            TransactionSynchronizationManager.registerSynchronization(StatelessSessionSynchronization(sf, stateless))
            TransactionSynchronizationManager.bindResource(sf, stateless)
        }
    }

    private class StatelessSessionSynchronization(
        private val sf: SessionFactory,
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
            TransactionSynchronizationManager.unbindResource(sf)
            stateless.close()
        }
    }
}
