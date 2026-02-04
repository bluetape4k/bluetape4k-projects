package io.bluetape4k.cassandra.examples

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.cql.suspendExecute
import io.bluetape4k.cassandra.querybuilder.bindMarker
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import java.io.Serializable
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class LimitConcurrencyExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val CONCURRENCY_LEVEL = 32
        private const val TOTAL_NUMBER_OF_INSERTS = 1_000
        private const val IN_FLIGHT_REQUESTS = 500

        private const val REPEAT_SIZE = 3
    }

    private val semaphore = Semaphore(IN_FLIGHT_REQUESTS)
    private val requestLatch = CountDownLatch(TOTAL_NUMBER_OF_INSERTS)

    private val insertAsyncCount = AtomicInteger(0)

    @BeforeAll
    fun setup() {
        createSchema(session)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `동기 방식의 Session 작업을 Thread Pool 을 이용하여 작업`() {
        val elapsed = measureTimeMillis {
            insertConcurrent(session)
        }
        println("Sync Elapsed time=$elapsed msec.")
    }

    private fun createSchema(session: CqlSession) {
        session.execute("CREATE TABLE IF NOT EXISTS tbl_sample_kv (id uuid, value int, PRIMARY KEY(id))")
    }

    private fun prepareStatemet(session: CqlSession): PreparedStatement {
        val stmt = insertInto("tbl_sample_kv")
            .value("id", "id".bindMarker())
            .value("value", "value".bindMarker())
            .build()

        log.debug { "query=${stmt.query}" }
        return session.prepare(stmt)
    }

    private fun insertConcurrent(session: CqlSession) {
        val pst = prepareStatemet(session)

        val insertsCounter = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(CONCURRENCY_LEVEL)

        repeat(TOTAL_NUMBER_OF_INSERTS) { counter ->
            semaphore.acquire()

            executor.submit {
                try {
                    session.execute(pst.bind().setUuid("id", UUID.randomUUID()).setInt("value", counter))
                    insertsCounter.incrementAndGet()
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    requestLatch.countDown()
                    semaphore.release()
                }
            }
        }

        requestLatch.await(10, TimeUnit.SECONDS)

        println("Finish executing ${insertsCounter.get()} queries with a concurrency level of $CONCURRENCY_LEVEL")
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `비동기 방식으로 Session 작업`() {
        val elapsed = measureTimeMillis {
            insertConcurrentAsync(session)
        }
        println("Async Elapsed time=$elapsed msec.")
    }

    private fun insertConcurrentAsync(session: CqlSession) {
        val pst = prepareStatemet(session)

        val ranges = createRanges(CONCURRENCY_LEVEL, TOTAL_NUMBER_OF_INSERTS)
        val pending = fastListOf<CompletionStage<AsyncResultSet>>()

        // Every range will have dedicated CompletableFuture handling the execution.
        ranges.forEach { range ->
            pending.add(executeOneAtATime(session, pst, range))
        }

        CompletableFuture.allOf(*pending.map { it.toCompletableFuture() }.toTypedArray()).get()
        println("Finish executing coroutines $insertAsyncCount queries with a concurrency level of $CONCURRENCY_LEVEL")
    }

    private fun executeOneAtATime(
        session: CqlSession,
        pst: PreparedStatement,
        range: Range,
    ): CompletionStage<AsyncResultSet> {
        var lastFeature: CompletionStage<AsyncResultSet>? = null
        for (i in range.first until range.last) {
            lastFeature = lastFeature?.thenCompose { insertAsync(session, pst, i) } ?: insertAsync(session, pst, i)
        }
        return lastFeature!!
    }

    private fun insertAsync(
        session: CqlSession,
        pst: PreparedStatement,
        counter: Int,
    ): CompletionStage<AsyncResultSet> {
        val stmt = pst.bind().setUuid("id", UUID.randomUUID()).setInt("value", counter)

        return session.executeAsync(stmt)
            .whenComplete { _, err ->
                if (err == null) insertAsyncCount.incrementAndGet()
                else err.printStackTrace()
            }
    }

    private fun createRanges(concurrencyLevel: Int, totalNumberOfInserts: Int): List<Range> {
        val ranges = fastListOf<Range>()
        val numberOfElementsInRange = totalNumberOfInserts / concurrencyLevel

        repeat(concurrencyLevel) {
            if (it == concurrencyLevel - 1) {
                ranges.add(Range(it * numberOfElementsInRange, totalNumberOfInserts))
            } else {
                ranges.add(Range(it * numberOfElementsInRange, (it + 1) * numberOfElementsInRange))
            }
        }

        return ranges
    }

    private data class Range(val first: Int, val last: Int): Serializable

    @RepeatedTest(REPEAT_SIZE)
    fun `개별 작업을 모두 비동기로 수행 - 기본 Throttle 적용`() {
        val elapsed = measureTimeMillis {
            insertIndividual(session)
        }
        println("Individuals Elapsed time=$elapsed msec.")
    }

    private fun insertIndividual(session: CqlSession) {
        val throttle =
            session.context.config.defaultProfile.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS)
        println("throttle=$throttle")

        val pst = prepareStatemet(session)
        val pending = fastListOf<CompletableFuture<AsyncResultSet>>()

        repeat(TOTAL_NUMBER_OF_INSERTS) {
            val stmt = pst.bind().setUuid("id", UUID.randomUUID()).setInt("value", it)
            val future = session.executeAsync(stmt).toCompletableFuture()
            pending.add(future)
        }

        CompletableFuture.allOf(*pending.toTypedArray()).get()

        println("Finish executing ${pending.size} queries with a concurrency level of $throttle")
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `개발 작업을 Coroutines 로 수행 - 기본 Throttle 적용`() {
        val elapsed = measureTimeMillis {
            insertIndividualInCoroutines(session)
        }
        println("Individuals in Coroutines elapsed time=$elapsed msec.")
    }

    private fun insertIndividualInCoroutines(session: CqlSession) {
        val throttle =
            session.context.config.defaultProfile.getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS)
        println("throttle=$throttle")

        val pst = prepareStatemet(session)
        runSuspendIO {
            val tasks = fastList(TOTAL_NUMBER_OF_INSERTS) {
                async(Dispatchers.IO) {
                    val stmt = pst.bind().setUuid("id", UUID.randomUUID()).setInt("value", it)
                    session.suspendExecute(stmt)
                }
            }
            tasks.awaitAll()
            println("Finish executing ${tasks.size} queries with a concurrency level of $throttle")
        }
    }
}
