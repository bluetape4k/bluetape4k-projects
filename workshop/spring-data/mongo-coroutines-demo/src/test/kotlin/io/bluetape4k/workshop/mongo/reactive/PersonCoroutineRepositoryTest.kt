package io.bluetape4k.workshop.mongo.reactive

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.flow.extensions.subject.PublishSubject
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.domain.Person
import io.bluetape4k.workshop.mongo.domain.PersonCoroutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

class PersonCoroutineRepositoryTest @Autowired constructor(
    private val repository: PersonCoroutineRepository,
    private val operations: ReactiveMongoOperations,
): AbstractReactiveMongoTest(operations) {

    companion object: KLogging()

    @Test
    fun `insert and count`() = runTest {
        val prevCount = repository.count()
        println("prevCount=$prevCount")

        // 신규 Person 2명 추가 
        repository.saveAll(flowOf(newPerson(), newPerson())).log("save").collect()

        val saveAndCount = repository.count()
        println(saveAndCount)

        saveAndCount shouldBeEqualTo prevCount + 2
    }

    @Test
    fun `perform conversion before result processing`() = runTest {
        repository.findAll().log("#1").count() shouldBeEqualTo 4
    }

    @Test
    fun `stream data with tailable cursor`() = runTest {
        val prevCount = repository.count().toInt()

        val queue = ConcurrentLinkedQueue<Person>()

        val flux = repository.findWithTailableCursorBy()
            .doOnNext { println("new added person: $it") }
            .doOnNext(queue::add)                      // Person Collection에 새로 추가될 때마다 queue에 추가한다  
            .doOnComplete { println("Complete") }
            .doOnTerminate { println("Terminated") }
            .subscribe()

        await coUntil { queue.size >= prevCount }

        repository.save(newPerson())
        await coUntil { queue.size >= prevCount + 1 }

        repository.save(newPerson())
        await coUntil { queue.size >= prevCount + 2 }

        // flux가 dispose 되면 doOnNext 를 실행하지 않습니다.
        flux.dispose()

        repository.save(newPerson())
        delay(100)

        // flux 의 dispose 된 후에 추가된 Person은 는 받지 않습니다.
        queue.size shouldBeEqualTo prevCount + 2
    }

    /**
     * NOTE: kommons-coroutines 의 [PublishSubject] 를 이용하여 해결했습니다.
     */
    @Test
    fun `stream data with tailable cursor with PublishSubject`() = runTest(timeout = 10.seconds) {
        val prevCount = repository.count().toInt()

        val queue = ConcurrentLinkedQueue<Person>()
        val subject = PublishSubject<Person>()

        // subject 를 collect 하여 queue 에 추가합니다.
        val job = launch(Dispatchers.IO) {
            subject
                .log("#1")
                .collect {
                    queue.add(it)
                }
        }.log("job")

        // tailable cursor 를 이용하여 새로 추가되는 Person 을 subject 에 emit 합니다.
        repository.findWithTailableCursorBy()
            .doOnNext { runBlocking { subject.emit(it) } }
            .doOnComplete { println("Complete") }
            .doOnTerminate { println("Terminated") }
            .subscribe()

        await coUntil { queue.size >= prevCount }

        repository.save(newPerson())
        await coUntil { queue.size >= prevCount + 1 }

        repository.save(newPerson())
        await coUntil { queue.size >= prevCount + 2 }

        // subject 의 complete 가 호출되면 collect 가 종료됩니다.
        subject.complete()
        job.join()

        repository.save(newPerson())
        delay(100)

        // subject의 collect 가 완료 된 후의 추가된 Person는 제외
        queue.size shouldBeEqualTo prevCount + 2
    }

    @Test
    fun `query data with query derivation`() = runTest {
        val people = repository.findByLastname("White").log("person")
        people.count() shouldBeEqualTo 2
    }

    @Test
    fun `query data with string query`() = runTest {
        val person = repository.findByFirstnameAndLastname("Walter", "White")
        person.shouldNotBeNull()
    }

    @Test
    fun `query data with deferred query deviation`() = runTest {
        val people = repository.findByLastname(mono { "White" }).log("person")
        people.count() shouldBeEqualTo 2
    }

    @Test
    fun `query data with mixed deferred query deviation`() = runTest {
        val person = repository.findByFirstnameAndLastname(mono { "Walter" }, "White")
        person.shouldNotBeNull()
    }
}
