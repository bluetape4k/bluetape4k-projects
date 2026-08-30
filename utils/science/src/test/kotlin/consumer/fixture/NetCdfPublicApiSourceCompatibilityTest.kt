package consumer.fixture

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.science.exposed.NetCdfException
import io.bluetape4k.science.exposed.model.NetCdfImportProgress
import io.bluetape4k.science.exposed.model.NetCdfImportStatus
import io.bluetape4k.science.exposed.service.NetCdfCatalogService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream

/**
 * 외부 package에서 public NetCDF API의 source compatibility와 caller 책임 예제를 검증합니다.
 *
 * authorize/lifecycle helper는 실제 HTTP/RPC adapter나 worker 통합을 증명하지 않습니다. 공개 API와
 * caller-owned 정책 예제가 함께 compile되고 기대한 결정표를 실행하는지 고정하는 consumer fixture입니다.
 */
class NetCdfPublicApiSourceCompatibilityTest {

    @Test
    fun `public progress API compiles and response allowlist excludes operational fields`() {
        val progress = NetCdfImportProgress(
            fileId = 41L,
            variableName = "temperature",
            status = NetCdfImportStatus.IN_PROGRESS,
            lastSliceIdx = 7L,
            errorMessage = "must-not-leak",
        )

        progress.toCallerResponse(ImportOutcome.RUNNING) shouldBeEqualTo NetCdfImportStatusResponse(
            status = "IN_PROGRESS",
            lastCommittedSlice = 7L,
            outcome = "RUNNING",
        )
        NetCdfImportStatusResponse::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet() shouldBeEqualTo setOf("status", "lastCommittedSlice", "outcome")
    }

    @Test
    fun `sealed exception migration keeps an else fallback`() {
        classify(NetCdfException.FileChanged(1L, "expected", "actual")) shouldBeEqualTo "file-changed"
        classify(NetCdfException.CorruptProgress(2L, "invalid")) shouldBeEqualTo "corrupt-progress"
        classify(NetCdfException.ImportAlreadyRunning(3L, "temperature")) shouldBeEqualTo "running"
        classify(NetCdfException.FileRecordNotFound(4L)) shouldBeEqualTo "unhandled-netcdf"
    }

    @Test
    fun `non-register authorization resolves tenant job and path from the file binding`() {
        var invocations = 0
        val adapter = AuthorizedNetCdfCaller(::authorize)
        val spoofedRequestPath = allowedRequest().copy(registeredPath = "/tmp/caller-supplied.nc")

        listOf(CallerOperation.IMPORT, CallerOperation.PROGRESS, CallerOperation.RETRY).forEach { operation ->
            adapter.execute(operation, spoofedRequestPath) { invocations++ }
        }

        invocations shouldBeEqualTo 3
    }

    @ParameterizedTest(name = "{0} rejects {1} before invoking service")
    @MethodSource("deniedOperations")
    internal fun `authorization precedes every service operation`(
        operation: CallerOperation,
        reason: DenialReason,
        request: CallerRequest,
    ) {
        var invocations = 0
        val adapter = AuthorizedNetCdfCaller(::authorize)

        reason.matches(operation, request).shouldBeTrue()
        assertFailsWith<SecurityException> {
            adapter.execute(operation, request) {
                invocations++
            }
        }
        invocations shouldBeEqualTo 0
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lifecycleCases")
    internal fun `caller lifecycle policy never retries automatically`(
        name: String,
        input: LifecycleInput,
        expectedOutcome: ImportOutcome,
        expectedAlerts: Set<String>,
    ) {
        val decision = decideLifecycle(input)

        name.isNotBlank().shouldBeTrue()
        decision.outcome shouldBeEqualTo expectedOutcome
        decision.retryInvocations shouldBeEqualTo 0
        decision.alerts shouldBeEqualTo expectedAlerts
        decision.metricTags.keys shouldBeEqualTo setOf("operation", "outcome")
        decision.metricTags.values.none {
            it.contains(input.rawPath) || it.contains(input.tenant) || it.contains(input.rawError)
        }.shouldBeTrue()
        decision.structuredLogFields.keys shouldBeEqualTo setOf("operation", "outcome", "correlation_id")
        decision.structuredLogFields.values.none {
            it.contains(input.rawPath) || it.contains(input.tenant) || it.contains(input.rawError)
        }.shouldBeTrue()
        decision.structuredLogFields.getValue("correlation_id").isNotBlank().shouldBeTrue()
    }

    companion object {
        @JvmStatic
        fun deniedOperations(): Stream<Arguments> = CallerOperation.entries.flatMap { operation ->
            val outsideRootRequest = if (operation == CallerOperation.REGISTER) {
                allowedRequest().copy(registeredPath = "/srv/netcdf/quarantine/../outside.nc")
            } else {
                allowedRequest().copy(fileId = 99L)
            }
            listOf(
                Arguments.of(
                    operation,
                    DenialReason.CROSS_TENANT,
                    allowedRequest().copy(targetTenant = "tenant-b"),
                ),
                Arguments.of(
                    operation,
                    DenialReason.UNAUTHORIZED,
                    allowedRequest().copy(allowedOperations = emptySet()),
                ),
                Arguments.of(
                    operation,
                    DenialReason.CROSS_JOB,
                    allowedRequest().copy(actorJob = "job-b"),
                ),
                Arguments.of(
                    operation,
                    DenialReason.OUTSIDE_ALLOWED_ROOT,
                    outsideRootRequest,
                ),
            )
        }.stream()

        @JvmStatic
        fun lifecycleCases(): Stream<Arguments> = (terminalLifecycleCases() + reviewLifecycleCases()).stream()

        private fun terminalLifecycleCases(): List<Arguments> = listOf(
            Arguments.of(
                "unterminated worker requires isolation",
                lifecycleInput(terminated = false),
                ImportOutcome.RECOVERY_REQUIRED,
                setOf("netcdf.import.timeout", "netcdf.import.worker.stuck"),
            ),
            Arguments.of(
                "completed progress is authoritative after worker exit",
                lifecycleInput(progressStatus = NetCdfImportStatus.COMPLETED),
                ImportOutcome.COMPLETED,
                emptySet<String>(),
            ),
            Arguments.of(
                "first ImportAlreadyRunning trusts the DB lease result",
                lifecycleInput(signal = LifecycleSignal.ALREADY_RUNNING),
                ImportOutcome.RUNNING,
                emptySet<String>(),
            ),
            Arguments.of(
                "repeated ImportAlreadyRunning stops retry",
                lifecycleInput(signal = LifecycleSignal.REPEATED_ALREADY_RUNNING),
                ImportOutcome.RECOVERY_REQUIRED,
                setOf("netcdf.import.retry.exhausted"),
            ),
            Arguments.of(
                "attempt exhaustion stops retry",
                lifecycleInput(attempt = 3, maxAttempts = 3),
                ImportOutcome.RECOVERY_REQUIRED,
                setOf("netcdf.import.retry.exhausted"),
            ),
            Arguments.of(
                "non-transient typed failure requires repair",
                lifecycleInput(signal = LifecycleSignal.NON_TRANSIENT_FAILURE),
                ImportOutcome.RECOVERY_REQUIRED,
                setOf("netcdf.import.retry.exhausted"),
            ),
            Arguments.of(
                "unknown worker failure fails closed",
                lifecycleInput(signal = LifecycleSignal.UNKNOWN_FAILURE),
                ImportOutcome.RECOVERY_REQUIRED,
                setOf("netcdf.import.retry.exhausted"),
            ),
        )

        private fun reviewLifecycleCases(): List<Arguments> = listOf(
            Arguments.of(
                "pending progress needs review",
                lifecycleInput(progressStatus = NetCdfImportStatus.PENDING),
                ImportOutcome.RETRY_REVIEW,
                setOf("netcdf.import.timeout"),
            ),
            Arguments.of(
                "failed progress needs review",
                lifecycleInput(progressStatus = NetCdfImportStatus.FAILED),
                ImportOutcome.RETRY_REVIEW,
                setOf("netcdf.import.timeout"),
            ),
            Arguments.of(
                "missing progress needs review",
                lifecycleInput(progressStatus = null),
                ImportOutcome.RETRY_REVIEW,
                setOf("netcdf.import.timeout"),
            ),
            Arguments.of(
                "in-progress row is not judged by the caller clock",
                lifecycleInput(progressStatus = NetCdfImportStatus.IN_PROGRESS),
                ImportOutcome.RETRY_REVIEW,
                setOf("netcdf.import.timeout"),
            ),
        )

        private fun allowedRequest(): CallerRequest = CallerRequest(
            actor = "operator-1",
            actorTenant = "tenant-a",
            actorJob = "job-a",
            targetTenant = "tenant-a",
            targetJob = "job-a",
            allowedOperations = CallerOperation.entries.toSet(),
            fileId = 41L,
            registeredPath = "/srv/netcdf/quarantine/grid.nc",
        )

        private fun lifecycleInput(
            terminated: Boolean = true,
            progressStatus: NetCdfImportStatus? = null,
            signal: LifecycleSignal = LifecycleSignal.TIMEOUT,
            attempt: Int = 1,
            maxAttempts: Int = 3,
        ): LifecycleInput = LifecycleInput(
            terminated = terminated,
            progressStatus = progressStatus,
            signal = signal,
            attempt = attempt,
            maxAttempts = maxAttempts,
            correlationId = "job-1561",
            rawPath = "/srv/netcdf/quarantine/customer-grid.nc",
            tenant = "tenant-secret",
            rawError = "database detail must not be a tag",
        )
    }
}

internal fun readProgress(
    catalog: NetCdfCatalogService,
    fileId: Long,
    variableName: String,
): NetCdfImportProgress? = catalog.findImportProgress(fileId, variableName)

internal fun classify(exception: NetCdfException): String = when (exception) {
    is NetCdfException.FileChanged -> "file-changed"
    is NetCdfException.CorruptProgress -> "corrupt-progress"
    is NetCdfException.ImportAlreadyRunning -> "running"
    else -> "unhandled-netcdf"
}

private data class NetCdfImportStatusResponse(
    val status: String,
    val lastCommittedSlice: Long?,
    val outcome: String,
)

private fun NetCdfImportProgress.toCallerResponse(outcome: ImportOutcome): NetCdfImportStatusResponse =
    NetCdfImportStatusResponse(
        status = status.name,
        lastCommittedSlice = lastSliceIdx,
        outcome = outcome.name,
    )

internal enum class CallerOperation { REGISTER, IMPORT, PROGRESS, RETRY }

internal enum class DenialReason { CROSS_TENANT, CROSS_JOB, UNAUTHORIZED, OUTSIDE_ALLOWED_ROOT }

internal data class CallerRequest(
    val actor: String,
    val actorTenant: String,
    val actorJob: String,
    val targetTenant: String,
    val targetJob: String,
    val allowedOperations: Set<CallerOperation>,
    val fileId: Long,
    val registeredPath: String,
)

private data class RegisteredTarget(
    val tenant: String,
    val job: String,
    val path: String,
)

private val targetsByFileId = mapOf(
    41L to RegisteredTarget("tenant-a", "job-a", "/srv/netcdf/quarantine/grid.nc"),
    99L to RegisteredTarget("tenant-a", "job-a", "/srv/netcdf/outside.nc"),
)

private fun resolveTarget(operation: CallerOperation, request: CallerRequest): RegisteredTarget? =
    if (operation == CallerOperation.REGISTER) {
        RegisteredTarget(request.targetTenant, request.targetJob, request.registeredPath)
    } else {
        targetsByFileId[request.fileId]
    }

private fun DenialReason.matches(operation: CallerOperation, request: CallerRequest): Boolean = when (this) {
    DenialReason.CROSS_TENANT -> resolveTarget(operation, request)?.let { target ->
        request.actorTenant != target.tenant || request.targetTenant != target.tenant
    } ?: true
    DenialReason.CROSS_JOB -> resolveTarget(operation, request)?.let { target ->
        request.actorJob != target.job || request.targetJob != target.job
    } ?: true
    DenialReason.UNAUTHORIZED -> operation !in request.allowedOperations
    DenialReason.OUTSIDE_ALLOWED_ROOT -> resolveTarget(operation, request)?.let { target ->
        !isWithinAllowedRoot(target.path)
    } ?: true
}

private class AuthorizedNetCdfCaller(
    private val authorize: (CallerOperation, CallerRequest) -> Boolean,
) {
    fun execute(operation: CallerOperation, request: CallerRequest, serviceCall: () -> Unit) {
        if (!authorize(operation, request)) throw SecurityException("NetCDF operation is not authorized")
        serviceCall()
    }
}

private fun authorize(operation: CallerOperation, request: CallerRequest): Boolean {
    val target = resolveTarget(operation, request) ?: return false
    return request.actor.isNotBlank() &&
        request.actorTenant == target.tenant &&
        request.actorJob == target.job &&
        request.targetTenant == target.tenant &&
        request.targetJob == target.job &&
        operation in request.allowedOperations &&
        isWithinAllowedRoot(target.path)
}

private fun isWithinAllowedRoot(rawPath: String): Boolean {
    val allowedRoot = Path.of("/srv/netcdf/quarantine").normalize()
    val candidate = runCatching { Path.of(rawPath).normalize() }.getOrElse { return false }
    return candidate.isAbsolute && candidate != allowedRoot && candidate.startsWith(allowedRoot)
}

internal enum class LifecycleSignal {
    TIMEOUT,
    ALREADY_RUNNING,
    REPEATED_ALREADY_RUNNING,
    NON_TRANSIENT_FAILURE,
    UNKNOWN_FAILURE,
}

internal enum class ImportOutcome { COMPLETED, RUNNING, RETRY_REVIEW, RECOVERY_REQUIRED }

internal data class LifecycleInput(
    val terminated: Boolean,
    val progressStatus: NetCdfImportStatus?,
    val signal: LifecycleSignal,
    val attempt: Int,
    val maxAttempts: Int,
    val correlationId: String,
    val rawPath: String,
    val tenant: String,
    val rawError: String,
)

private data class LifecycleDecision(
    val outcome: ImportOutcome,
    val retryInvocations: Int,
    val alerts: Set<String>,
    val metricTags: Map<String, String>,
    val structuredLogFields: Map<String, String>,
)

private fun decideLifecycle(input: LifecycleInput): LifecycleDecision {
    val outcome = decideOutcome(input)
    return LifecycleDecision(
        outcome = outcome,
        retryInvocations = 0,
        alerts = alertsFor(input, outcome),
        metricTags = mapOf(
            "operation" to "netcdf-import",
            "outcome" to outcome.name.lowercase(),
        ),
        structuredLogFields = mapOf(
            "operation" to "netcdf-import",
            "outcome" to outcome.name.lowercase(),
            "correlation_id" to input.correlationId,
        ),
    )
}

private fun decideOutcome(input: LifecycleInput): ImportOutcome = when {
    !input.terminated -> ImportOutcome.RECOVERY_REQUIRED
    input.progressStatus == NetCdfImportStatus.COMPLETED -> ImportOutcome.COMPLETED
    input.signal == LifecycleSignal.ALREADY_RUNNING -> ImportOutcome.RUNNING
    input.signal == LifecycleSignal.REPEATED_ALREADY_RUNNING -> ImportOutcome.RECOVERY_REQUIRED
    input.attempt >= input.maxAttempts -> ImportOutcome.RECOVERY_REQUIRED
    input.signal == LifecycleSignal.NON_TRANSIENT_FAILURE -> ImportOutcome.RECOVERY_REQUIRED
    input.signal == LifecycleSignal.UNKNOWN_FAILURE -> ImportOutcome.RECOVERY_REQUIRED
    else -> ImportOutcome.RETRY_REVIEW
}

private fun alertsFor(input: LifecycleInput, outcome: ImportOutcome): Set<String> = when {
    !input.terminated -> setOf("netcdf.import.timeout", "netcdf.import.worker.stuck")
    input.signal == LifecycleSignal.REPEATED_ALREADY_RUNNING ||
        input.attempt >= input.maxAttempts ||
        input.signal == LifecycleSignal.NON_TRANSIENT_FAILURE ||
        input.signal == LifecycleSignal.UNKNOWN_FAILURE -> setOf("netcdf.import.retry.exhausted")
    outcome == ImportOutcome.RETRY_REVIEW -> setOf("netcdf.import.timeout")
    else -> emptySet()
}
