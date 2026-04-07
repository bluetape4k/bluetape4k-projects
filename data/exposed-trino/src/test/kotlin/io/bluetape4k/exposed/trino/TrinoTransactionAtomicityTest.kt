package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.domain.Events
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * Trino autocommit 모드에서의 트랜잭션 원자성 동작을 검증하는 테스트.
 *
 * Trino memory 커넥터는 진정한 트랜잭션을 지원하지 않으므로,
 * 각 DML 문은 autocommit 으로 즉시 반영됩니다.
 * 이 테스트는 해당 특성에 따른 부분 반영 및 nested transaction 동작을 명시적으로 검증합니다.
 */
class TrinoTransactionAtomicityTest: AbstractTrinoTest() {

    @Test
    fun `정상 흐름 - INSERT 2건 모두 반영`() = withEventsTable {
        transaction(db) {
            Events.insert {
                it[eventId] = 1L
                it[eventName] = "click"
                it[region] = "kr"
            }
            Events.insert {
                it[eventId] = 2L
                it[eventName] = "view"
                it[region] = "us"
            }
        }

        transaction(db) {
            Events.selectAll().count() shouldBeEqualTo 2L
        }
    }

    /**
     * **Trino autocommit 모드에서는 블록 중간 실패 시 앞선 DML이 롤백되지 않습니다.**
     *
     * INSERT 1건 실행 후 의도적 예외를 발생시키면,
     * 일반 RDBMS에서는 전체 롤백이 기대되지만 Trino에서는 이미 반영된 INSERT가 남습니다.
     */
    @Test
    fun `부분 반영 - INSERT 후 예외 발생 시 앞선 DML은 롤백되지 않음`() = withEventsTable {
        assertFailsWith<RuntimeException> {
            transaction(db) {
                Events.insert {
                    it[eventId] = 1L
                    it[eventName] = "click"
                    it[region] = "kr"
                }
                throw RuntimeException("의도적 예외")
            }
        }

        // Trino autocommit 모드에서는 INSERT가 이미 반영되어 롤백되지 않음
        transaction(db) {
            Events.selectAll().count() shouldBeEqualTo 1L
        }
    }

    /**
     * **nested transaction은 호출은 허용되나 원자성 없음 -- inner DML은 outer rollback으로 취소되지 않음.**
     *
     * Trino autocommit 모드에서는 nested transaction 호출이 예외 없이 실행되며,
     * inner 블록의 DML은 즉시 반영되어 outer 블록의 롤백으로 취소할 수 없습니다.
     */
    @Test
    fun `nested transaction - inner DML은 outer rollback으로 취소되지 않음`() = withEventsTable {
        assertFailsWith<RuntimeException> {
            transaction(db) {
                transaction(db) {
                    Events.insert {
                        it[eventId] = 1L
                        it[eventName] = "nested-click"
                        it[region] = "kr"
                    }
                }
                throw RuntimeException("outer 예외")
            }
        }

        // inner DML이 반영됨 - outer가 inner를 롤백하지 못함
        transaction(db) {
            Events.selectAll().count() shouldBeEqualTo 1L
        }
    }
}
