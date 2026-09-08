package io.bluetape4k.temporal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.activity.ActivityOptions
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowStub
import io.temporal.client.newWorkflowStub
import io.temporal.common.RetryOptions
import io.temporal.failure.ApplicationFailure
import io.temporal.failure.CanceledFailure
import io.temporal.testing.TestWorkflowEnvironment
import io.temporal.testing.WorkflowReplayer
import io.temporal.workflow.Functions
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.Saga
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import io.temporal.worker.Worker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TEMPORAL_TASK_QUEUE = "bluetape4k-temporal-test"

@WorkflowInterface
interface ApprovalWorkflow {
    @WorkflowMethod
    fun run(orderId: String): String

    @SignalMethod
    fun approve()

    @QueryMethod
    fun status(): String
}

class ApprovalWorkflowImpl : ApprovalWorkflow {
    private var approved = false

    override fun run(orderId: String): String {
        Workflow.sleep(Duration.ofHours(1))
        Workflow.await { approved }
        return "$orderId:approved"
    }

    override fun approve() {
        approved = true
    }

    override fun status(): String = if (approved) "approved" else "pending"
}

@WorkflowInterface
interface WaitingWorkflow {
    @WorkflowMethod
    fun run(label: String): String

    @SignalMethod
    fun release()

    @QueryMethod
    fun status(): String
}

class WaitingWorkflowImpl : WaitingWorkflow {
    private var released = false

    override fun run(label: String): String {
        Workflow.await { released }
        return "$label:released"
    }

    override fun release() {
        released = true
    }

    override fun status(): String = if (released) "released" else "waiting"
}

@WorkflowInterface
interface ListWorkflow {
    @WorkflowMethod
    fun run(orderId: String): List<String>

    @SignalMethod
    fun complete()

    @QueryMethod
    fun items(): List<String>
}

class ListWorkflowImpl : ListWorkflow {
    private var completed = false

    override fun run(orderId: String): List<String> {
        Workflow.await { completed }
        return listOf(orderId, "completed")
    }

    override fun complete() {
        completed = true
    }

    override fun items(): List<String> = if (completed) listOf("completed") else listOf("pending")
}

@ActivityInterface
interface OrderActivities {
    @ActivityMethod
    fun reserve(orderId: String): String

    @ActivityMethod
    fun charge(orderId: String)

    @ActivityMethod
    fun release(orderId: String)
}

class RecordingOrderActivities : OrderActivities {
    val reserveAttempts = AtomicInteger()
    val reservedOrders = ConcurrentHashMap.newKeySet<String>()
    val calls = CopyOnWriteArrayList<String>()

    override fun reserve(orderId: String): String {
        reserveAttempts.incrementAndGet()
        calls += "reserve:$orderId"

        // 첫 시도에서 멱등 key를 기록한 뒤 일시적인 오류를 발생시킵니다. 재시도는 새 예약을
        // 만들지 않고 같은 idempotency key를 관찰해야 합니다.
        if (reservedOrders.add(orderId)) {
            throw ApplicationFailure.newFailure("transient reserve failure", "ReserveTransient")
        }
        return "reservation:$orderId"
    }

    override fun charge(orderId: String) {
        calls += "charge:$orderId"
        throw ApplicationFailure.newNonRetryableFailure("card declined", "ChargeDeclined")
    }

    override fun release(orderId: String) {
        calls += "release:$orderId"
    }
}

@WorkflowInterface
interface OrderWorkflow {
    @WorkflowMethod
    fun run(orderId: String): String
}

class OrderWorkflowImpl : OrderWorkflow {
    private val activities = Workflow.newActivityStub(
        OrderActivities::class.java,
        ActivityOptions {
            setStartToCloseTimeout(Duration.ofSeconds(5))
            setRetryOptions(
                RetryOptions {
                    setInitialInterval(Duration.ofMillis(1))
                    setBackoffCoefficient(1.0)
                    setMaximumAttempts(2)
                },
            )
        },
    )

    override fun run(orderId: String): String {
        val saga = Saga { setContinueWithError(true) }
        return try {
            val reservation = activities.reserve(orderId)
            saga.addCompensation(Functions.Proc { activities.release(orderId) })
            activities.charge(orderId)
            reservation
        } catch (error: Exception) {
            saga.compensate()
            throw error
        }
    }
}

class TemporalWorkflowIntegrationTest {
    private lateinit var environment: TestWorkflowEnvironment
    private lateinit var worker: Worker
    private lateinit var activities: RecordingOrderActivities

    @BeforeEach
    fun setUp() {
        environment = TestWorkflowEnvironment.newInstance()
        worker = environment.newWorker(TEMPORAL_TASK_QUEUE)
        worker.registerWorkflowImplementationTypes(
            ApprovalWorkflowImpl::class.java,
            WaitingWorkflowImpl::class.java,
            ListWorkflowImpl::class.java,
            OrderWorkflowImpl::class.java,
        )
        activities = RecordingOrderActivities()
        worker.registerActivitiesImplementations(activities)
        environment.start()
    }

    @AfterEach
    fun tearDown() {
        environment.close()
    }

    @Test
    fun `start signal query and time skipping work with the in-memory service`() = runSuspendIO(timeout = 120.seconds) {
        val stub = approvalStub("approval-time-skip")
        val execution = stub.startSuspending("order-1")

        stub.querySuspending<String>("status") shouldBeEqualTo "pending"
        environment.sleep(Duration.ofHours(1))
        stub.signalSuspending("approve")

        stub.awaitResult<String>() shouldBeEqualTo "order-1:approved"

        val history = environment.getWorkflowClient().fetchHistory(execution.workflowId, execution.runId)
        WorkflowReplayer.replayWorkflowExecution(history, ApprovalWorkflowImpl::class.java)
    }

    @Test
    fun `generic list query and result preserve the element type`() = runSuspendIO(timeout = 120.seconds) {
        val typed = environment.getWorkflowClient().newWorkflowStub<ListWorkflow> {
            setWorkflowId("list-result")
            setTaskQueue(TEMPORAL_TASK_QUEUE)
        }
        val stub = WorkflowStub.fromTyped(typed)
        stub.startSuspending("order-list")

        stub.querySuspending<List<String>>("items") shouldBeEqualTo listOf("pending")
        stub.signalSuspending("complete")
        stub.awaitResult<List<String>>() shouldBeEqualTo listOf("order-list", "completed")
    }

    @Test
    fun `local result cancellation leaves the workflow running`() = runSuspendIO(timeout = 120.seconds) {
        val stub = waitingStub("approval-local-cancel")
        stub.startSuspending("order-2")
        stub.querySuspending<String>("status") shouldBeEqualTo "waiting"

        val resultJob = launch { stub.awaitResult<String>() }
        delay(100)
        resultJob.cancel()
        resultJob.join()
        resultJob.isCancelled.shouldBeTrue()

        environment.sleep(Duration.ofHours(1))
        stub.signalSuspending("release")
        environment.sleep(Duration.ofHours(1))
        stub.awaitResult<String>() shouldBeEqualTo "order-2:released"
    }

    @Test
    fun `explicit remote cancellation fails the workflow result`() = runSuspendIO(timeout = 120.seconds) {
        val stub = waitingStub("approval-remote-cancel")
        stub.startSuspending("order-3")
        stub.cancelSuspending("test cancellation")

        val failure = assertFailsWith<WorkflowFailedException> {
            stub.awaitResult<String>()
        }
        failure.cause.shouldBeInstanceOf<CanceledFailure>()
    }

    @Test
    fun `worker factory shutdown is graceful and repeatable`() = runSuspendIO(timeout = 120.seconds) {
        val workerFactory = environment.getWorkerFactory()

        workerFactory.shutdownSuspending(5.seconds).shouldBeTrue()
        workerFactory.shutdownSuspending(5.seconds).shouldBeTrue()
    }

    @Test
    fun `activity retry idempotency compensation and history replay are preserved`() =
        runSuspendIO(timeout = 120.seconds) {
            val typed = environment.getWorkflowClient().newWorkflowStub<OrderWorkflow> {
                setWorkflowId("order-compensation")
                setTaskQueue(TEMPORAL_TASK_QUEUE)
            }
            val stub = WorkflowStub.fromTyped(typed)
            val execution = stub.startSuspending("order-4")

            assertFailsWith<WorkflowFailedException> {
                stub.awaitResult<String>()
            }

            activities.reserveAttempts.get() shouldBeEqualTo 2
            activities.reservedOrders.size shouldBeEqualTo 1
            activities.calls shouldBeEqualTo listOf(
                "reserve:order-4",
                "reserve:order-4",
                "charge:order-4",
                "release:order-4",
            )

            val history = environment.getWorkflowClient().fetchHistory(execution.workflowId, execution.runId)
            WorkflowReplayer.replayWorkflowExecution(history, OrderWorkflowImpl::class.java)
        }

    private fun approvalStub(workflowId: String): WorkflowStub {
        val typed = environment.getWorkflowClient().newWorkflowStub<ApprovalWorkflow> {
            setWorkflowId(workflowId)
            setTaskQueue(TEMPORAL_TASK_QUEUE)
        }
        return WorkflowStub.fromTyped(typed)
    }

    private fun waitingStub(workflowId: String): WorkflowStub {
        val typed = environment.getWorkflowClient().newWorkflowStub<WaitingWorkflow> {
            setWorkflowId(workflowId)
            setTaskQueue(TEMPORAL_TASK_QUEUE)
        }
        return WorkflowStub.fromTyped(typed)
    }
}
