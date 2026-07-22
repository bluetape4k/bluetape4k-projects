package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.JdkBinarySerializer
import io.bluetape4k.io.serializer.KryoBinarySerializer
import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceJsonCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import org.openjdk.jmh.annotations.Benchmark
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.security.MessageDigest

private const val PREFLIGHT_SCHEMA_VERSION = 1

private enum class PreflightBackend(
    val id: String,
    val methodPrefix: String,
) {
    JDK("jdk", "jdk"),
    KRYO("kryo", "kryo"),
    JACKSON2("jackson2", "jackson2"),
    JACKSON3("jackson3", "jackson3"),
}

private enum class PreflightPath(val id: String) {
    BASELINE("baseline"),
    CANDIDATE("candidate"),
}

private data class ResetSnapshot(
    val capacity: Int,
    val maxCapacity: Int,
    val readerIndex: Int,
    val writerIndex: Int,
) {
    fun toJson(): String =
        "{\"capacity\":$capacity,\"max_capacity\":$maxCapacity," +
                "\"reader_index\":$readerIndex,\"writer_index\":$writerIndex}"
}

private data class PreflightCell(
    val backend: String,
    val target: String,
    val path: String,
    val method: String,
    val pairedBaseline: String,
    val backendClass: String,
    val backendConfigSha256: String,
    val payloadSha256: String,
    val targetKind: String,
    val wireSha256: String,
    val writtenCount: Int,
    val prefixPreserved: Boolean,
    val baselineDispatchCount: Int,
    val candidateDispatchCount: Int,
    val resetBefore: ResetSnapshot,
    val resetAfter: ResetSnapshot,
) {
    fun fixtureIdentity(): String = listOf(
        backend,
        target,
        path,
        method,
        pairedBaseline,
        backendClass,
        backendConfigSha256,
        payloadSha256,
        targetKind,
        wireSha256,
        writtenCount.toString(),
        prefixPreserved.toString(),
        baselineDispatchCount.toString(),
        candidateDispatchCount.toString(),
        resetBefore.toString(),
        resetAfter.toString(),
    ).joinToString("|")

    fun toJson(): String = buildString {
        append('{')
        appendJsonField("backend", backend)
        appendJsonField("target", target)
        appendJsonField("path", path)
        appendJsonField("method", method)
        appendJsonField("paired_baseline", pairedBaseline)
        appendJsonField("backend_class", backendClass)
        appendJsonField("backend_config_sha256", backendConfigSha256)
        appendJsonField("payload_sha256", payloadSha256)
        appendJsonField("target_kind", targetKind)
        appendJsonField("wire_sha256", wireSha256)
        append("\"written_count\":$writtenCount,")
        append("\"prefix_preserved\":$prefixPreserved,")
        append("\"baseline_dispatch_count\":$baselineDispatchCount,")
        append("\"candidate_dispatch_count\":$candidateDispatchCount,")
        append("\"reset_before\":${resetBefore.toJson()},")
        append("\"reset_after\":${resetAfter.toJson()}")
        append('}')
    }
}

private data class RetainedBackendCheck(
    val status: String,
    val exceptionParity: Boolean,
    val statePreserved: Boolean,
) {
    fun toJson(): String =
        "{\"status\":${status.jsonString()},\"exception_parity\":$exceptionParity," +
                "\"state_preserved\":$statePreserved}"
}

private data class DispatchReport(
    val declaringClass: String,
    val dispatchKind: String,
    val runtimeDeclaringClass: String,
    val runtimeDispatchKind: String,
) {
    fun fixtureIdentity(): String =
        "$declaringClass|$dispatchKind|$runtimeDeclaringClass|$runtimeDispatchKind"

    fun toJson(): String = buildString {
        append('{')
        appendJsonField("declaring_class", declaringClass)
        appendJsonField("dispatch_kind", dispatchKind)
        appendJsonField("runtime_declaring_class", runtimeDeclaringClass)
        appendJsonField("runtime_dispatch_kind", runtimeDispatchKind, trailingComma = false)
        append('}')
    }
}

private data class FixtureReport(
    val payloadSha256: String,
    val allocatorClass: String,
    val heapAllocatorClass: String,
    val directAllocatorClass: String,
    val heapBufferClass: String,
    val directBufferClass: String,
    val numHeapArenas: Int,
    val numDirectArenas: Int,
) {
    fun toJson(): String = buildString {
        append('{')
        appendJsonField("payload_sha256", payloadSha256)
        appendJsonField("allocator_class", allocatorClass)
        appendJsonField("heap_allocator_class", heapAllocatorClass)
        appendJsonField("direct_allocator_class", directAllocatorClass)
        appendJsonField("heap_buffer_class", heapBufferClass)
        appendJsonField("direct_buffer_class", directBufferClass)
        append("\"num_heap_arenas\":$numHeapArenas,")
        append("\"num_direct_arenas\":$numDirectArenas,")
        append("\"pooled\":true,")
        append("\"capacity\":$ISSUE756_TARGET_CAPACITY,")
        append("\"max_capacity\":$ISSUE756_TARGET_CAPACITY,")
        append("\"reader_index\":$ISSUE756_READER_INDEX,")
        append("\"writer_index\":$ISSUE756_START_INDEX,")
        append("\"headroom\":${ISSUE756_TARGET_CAPACITY - ISSUE756_START_INDEX}")
        append('}')
    }
}

private class CountingBinarySerializer(
    private val delegate: BinarySerializer,
): BinarySerializer {
    var arrayCalls: Int = 0
        private set
    var streamCalls: Int = 0
        private set

    override fun serialize(graph: Any?): ByteArray {
        arrayCalls++
        return delegate.serialize(graph)
    }

    override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
        streamCalls++
        return delegate.serializeBinaryToStream(graph, target)
    }

    override fun <T: Any> deserialize(bytes: ByteArray?): T? =
        delegate.deserialize(bytes)
}

private class CountingJsonSerializer(
    private val delegate: JsonSerializer,
): JsonSerializer {
    var arrayCalls: Int = 0
        private set
    var streamCalls: Int = 0
        private set

    override fun serialize(graph: Any?): ByteArray {
        arrayCalls++
        return delegate.serialize(graph)
    }

    override fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
        streamCalls++
        return delegate.serializeJsonToStream(graph, target)
    }

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        delegate.deserialize(bytes, clazz)
}

/** Executable, fail-closed fixture for the issue #756 JMH matrix. */
object LettuceCodecBenchmarkPreflight {

    @JvmStatic
    fun main(args: Array<String>) {
        check(args.isEmpty()) { "Issue 756 preflight does not accept arguments." }
        silenceThirdPartyLogging()
        val payloadSha256 = sha256(issue756PayloadIdentity())
        val expectedMethods = expectedMethodNames()
        verifyBenchmarkMethods(expectedMethods)

        val cells = PreflightBackend.entries.flatMap { backend ->
            Issue756TargetKind.entries.flatMap { targetKind ->
                PreflightPath.entries.map { path ->
                    executeCell(backend, targetKind, path, payloadSha256)
                }
            }
        }
        check(cells.size == 16) { "Issue 756 preflight must execute exactly 16 cells." }
        check(cells.map { it.method }.toSet() == expectedMethods) {
            "Issue 756 preflight cell methods do not match the benchmark matrix."
        }
        verifyPairedCells(cells)

        val retainedChecks = PreflightBackend.entries.associateWith(::verifyReadOnlyParity)
        val dispatch = PreflightBackend.entries.associateWith(::dispatchReport)
        val heapTarget = newTarget(Issue756TargetKind.HEAP)
        val directTarget = newTarget(Issue756TargetKind.DIRECT)
        val fixture = try {
            val heapBufferClass = requireIssue756PooledTarget(heapTarget, Issue756TargetKind.HEAP)
            val directBufferClass = requireIssue756PooledTarget(directTarget, Issue756TargetKind.DIRECT)
            val heapAllocator = heapTarget.alloc() as PooledByteBufAllocator
            val directAllocator = directTarget.alloc() as PooledByteBufAllocator
            FixtureReport(
                payloadSha256 = payloadSha256,
                allocatorClass = PooledByteBufAllocator::class.java.name,
                heapAllocatorClass = heapAllocator.javaClass.name,
                directAllocatorClass = directAllocator.javaClass.name,
                heapBufferClass = heapBufferClass,
                directBufferClass = directBufferClass,
                numHeapArenas = heapAllocator.metric().numHeapArenas(),
                numDirectArenas = directAllocator.metric().numDirectArenas(),
            )
        } finally {
            heapTarget.release()
            directTarget.release()
        }
        val fixtureSha256 = sha256(
            buildString {
                append(fixture.toJson())
                cells.forEach { append('|').append(it.fixtureIdentity()) }
                dispatch.forEach { (backend, report) ->
                    append('|').append(backend.id).append('|').append(report.fixtureIdentity())
                }
            },
        )

        println(
            buildString {
                append('{')
                append("\"schema_version\":$PREFLIGHT_SCHEMA_VERSION,")
                appendJsonField("status", "passed")
                appendJsonField("fixture_sha256", fixtureSha256)
                append("\"fixture\":${fixture.toJson()},")
                append("\"cells\":[${cells.joinToString(",") { it.toJson() }}],")
                append("\"retained_backend_checks\":{")
                append(retainedChecks.entries.joinToString(",") { (backend, report) ->
                    "${backend.id.jsonString()}:${report.toJson()}"
                })
                append("},\"dispatch\":{")
                append(dispatch.entries.joinToString(",") { (backend, report) ->
                    "${backend.id.jsonString()}:${report.toJson()}"
                })
                append("}}")
            },
        )
    }
}

private fun executeCell(
    backend: PreflightBackend,
    targetKind: Issue756TargetKind,
    path: PreflightPath,
    payloadSha256: String,
): PreflightCell {
    val serializer = serializerFor(backend)
    val backendClass = serializer.javaClass.name
    val target = newTarget(targetKind)
    return try {
        resetTarget(target)
        val resetBefore = target.snapshot()
        var baselineDispatchCount = 0
        var candidateDispatchCount = 0
        val written = when (serializer) {
            is BinarySerializer -> {
                val counting = CountingBinarySerializer(serializer)
                val count = when (path) {
                    PreflightPath.BASELINE -> {
                        baselineDispatchCount++
                        val wire = counting.serialize(ISSUE756_PAYLOAD)
                        target.writeBytes(wire)
                        wire.size
                    }
                    PreflightPath.CANDIDATE -> {
                        candidateDispatchCount++
                        LettuceBinaryCodec<Issue756BenchmarkData>(counting).encodeValue(ISSUE756_PAYLOAD, target)
                        target.writerIndex() - ISSUE756_START_INDEX
                    }
                }
                check(counting.arrayCalls == if (path == PreflightPath.BASELINE) 1 else 0) {
                    "Issue 756 binary baseline dispatch count drifted."
                }
                check(counting.streamCalls == if (path == PreflightPath.CANDIDATE) 1 else 0) {
                    "Issue 756 binary candidate dispatch count drifted."
                }
                count
            }
            is JsonSerializer -> {
                val counting = CountingJsonSerializer(serializer)
                val count = when (path) {
                    PreflightPath.BASELINE -> {
                        baselineDispatchCount++
                        val wire = counting.serialize(ISSUE756_PAYLOAD)
                        target.writeBytes(wire)
                        wire.size
                    }
                    PreflightPath.CANDIDATE -> {
                        candidateDispatchCount++
                        LettuceJsonCodec(counting, Issue756BenchmarkData::class.java)
                            .encodeValue(ISSUE756_PAYLOAD, target)
                        target.writerIndex() - ISSUE756_START_INDEX
                    }
                }
                check(counting.arrayCalls == if (path == PreflightPath.BASELINE) 1 else 0) {
                    "Issue 756 JSON baseline dispatch count drifted."
                }
                check(counting.streamCalls == if (path == PreflightPath.CANDIDATE) 1 else 0) {
                    "Issue 756 JSON candidate dispatch count drifted."
                }
                count
            }
            else              -> error("Unsupported issue 756 serializer: $backendClass")
        }
        check(written > 0) { "Issue 756 preflight wire must be non-empty." }
        check(target.writerIndex() == ISSUE756_START_INDEX + written) {
            "Issue 756 preflight writer index drifted."
        }
        check(target.capacity() == ISSUE756_TARGET_CAPACITY) {
            "Issue 756 preflight target capacity grew."
        }
        val prefixPreserved = target.getByte(ISSUE756_PREFIX_INDEX) == ISSUE756_PREFIX
        check(prefixPreserved) { "Issue 756 preflight prefix changed." }
        val wire = ByteArray(written).also { target.getBytes(ISSUE756_START_INDEX, it) }

        resetTarget(target)
        val resetAfter = target.snapshot()
        check(resetBefore == resetAfter) { "Issue 756 invocation reset drifted." }

        val baselineMethod = methodName(backend, targetKind, PreflightPath.BASELINE)
        PreflightCell(
            backend = backend.id,
            target = targetKind.name.lowercase(),
            path = path.id,
            method = methodName(backend, targetKind, path),
            pairedBaseline = baselineMethod,
            backendClass = backendClass,
            backendConfigSha256 = sha256("${backend.id}|$backendClass|default"),
            payloadSha256 = payloadSha256,
            targetKind = targetKind.name.lowercase(),
            wireSha256 = sha256(wire),
            writtenCount = written,
            prefixPreserved = prefixPreserved,
            baselineDispatchCount = baselineDispatchCount,
            candidateDispatchCount = candidateDispatchCount,
            resetBefore = resetBefore,
            resetAfter = resetAfter,
        )
    } finally {
        target.release()
    }
}

private fun verifyPairedCells(cells: List<PreflightCell>) {
    cells.groupBy { it.backend to it.target }.forEach { (pair, pairedCells) ->
        check(pairedCells.size == 2) { "Issue 756 cell pair cardinality drifted: $pair" }
        val baseline = pairedCells.single { it.path == PreflightPath.BASELINE.id }
        val candidate = pairedCells.single { it.path == PreflightPath.CANDIDATE.id }
        check(baseline.wireSha256 == candidate.wireSha256) { "Issue 756 paired wire drifted: $pair" }
        check(baseline.writtenCount == candidate.writtenCount) { "Issue 756 paired count drifted: $pair" }
        check(baseline.backendClass == candidate.backendClass) { "Issue 756 paired backend drifted: $pair" }
        check(baseline.backendConfigSha256 == candidate.backendConfigSha256) {
            "Issue 756 paired backend config drifted: $pair"
        }
    }
}

private fun verifyReadOnlyParity(backend: PreflightBackend): RetainedBackendCheck {
    val serializer = serializerFor(backend)
    val byteBuffer = ByteBuffer.allocate(ISSUE756_TARGET_CAPACITY)
        .asReadOnlyBuffer()
        .position(ISSUE756_START_INDEX)
        .mark()
    val byteBufferPosition = byteBuffer.position()
    val byteBufferLimit = byteBuffer.limit()
    val byteBufferFailure = captureFailure {
        when (serializer) {
            is BinarySerializer -> serializer.serializeTo(ISSUE756_PAYLOAD, byteBuffer)
            is JsonSerializer   -> serializer.serializeTo(ISSUE756_PAYLOAD, byteBuffer)
            else                -> error("Unsupported issue 756 serializer: ${serializer.javaClass.name}")
        }
    }
    val byteBufferStatePreserved =
        byteBuffer.position() == byteBufferPosition && byteBuffer.limit() == byteBufferLimit &&
                runCatching { byteBuffer.reset().position() == byteBufferPosition }.getOrDefault(false)

    val mutableTarget = newTarget(Issue756TargetKind.HEAP)
    resetTarget(mutableTarget)
    val readOnlyTarget = mutableTarget.asReadOnly()
    val readerIndex = readOnlyTarget.readerIndex()
    val writerIndex = readOnlyTarget.writerIndex()
    val refCnt = readOnlyTarget.refCnt()
    readOnlyTarget.markReaderIndex()
    readOnlyTarget.markWriterIndex()
    val codecFailure: Throwable
    val byteBufStatePreserved: Boolean
    try {
        codecFailure = captureFailure {
            when (serializer) {
                is BinarySerializer -> LettuceBinaryCodec<Issue756BenchmarkData>(serializer)
                    .encodeValue(ISSUE756_PAYLOAD, readOnlyTarget)
                is JsonSerializer   -> LettuceJsonCodec(serializer, Issue756BenchmarkData::class.java)
                    .encodeValue(ISSUE756_PAYLOAD, readOnlyTarget)
                else                -> error("Unsupported issue 756 serializer: ${serializer.javaClass.name}")
            }
        }
        byteBufStatePreserved =
            readOnlyTarget.readerIndex() == readerIndex && readOnlyTarget.writerIndex() == writerIndex &&
                    readOnlyTarget.refCnt() == refCnt &&
                    runCatching {
                        readOnlyTarget.resetReaderIndex()
                        readOnlyTarget.resetWriterIndex()
                        readOnlyTarget.readerIndex() == readerIndex && readOnlyTarget.writerIndex() == writerIndex
                    }.getOrDefault(false)
    } finally {
        // `asReadOnly()` shares the original reference count without retaining it.
        readOnlyTarget.release()
    }

    val exceptionParity = byteBufferFailure.signature() == codecFailure.signature() &&
            byteBufferFailure is ReadOnlyBufferException
    val statePreserved = byteBufferStatePreserved && byteBufStatePreserved
    check(exceptionParity) { "Issue 756 read-only exception parity failed for ${backend.id}." }
    check(statePreserved) { "Issue 756 read-only state preservation failed for ${backend.id}." }
    return RetainedBackendCheck("passed", exceptionParity, statePreserved)
}

private fun dispatchReport(backend: PreflightBackend): DispatchReport {
    val serializer = serializerFor(backend)
    val interfaceClass: Class<*>
    val methodName: String
    when (serializer) {
        is BinarySerializer -> {
            interfaceClass = BinarySerializer::class.java
            methodName = "serializeBinaryToStream"
        }
        is JsonSerializer   -> {
            interfaceClass = JsonSerializer::class.java
            methodName = "serializeJsonToStream"
        }
        else                -> error("Unsupported issue 756 serializer: ${serializer.javaClass.name}")
    }
    val method = serializer.javaClass.getMethod(methodName, Any::class.java, OutputStream::class.java)
    val dispatchKind = if (method.declaringClass == interfaceClass) "inherited-default" else "declared-direct"
    return DispatchReport(
        declaringClass = method.declaringClass.name,
        dispatchKind = dispatchKind,
        runtimeDeclaringClass = method.declaringClass.name,
        runtimeDispatchKind = dispatchKind,
    )
}

private fun serializerFor(backend: PreflightBackend): Any = when (backend) {
    PreflightBackend.JDK      -> JdkBinarySerializer()
    PreflightBackend.KRYO     -> KryoBinarySerializer()
    PreflightBackend.JACKSON2 -> jackson2Serializer()
    PreflightBackend.JACKSON3 -> JacksonSerializer()
}

private fun newTarget(targetKind: Issue756TargetKind): ByteBuf = when (targetKind) {
    Issue756TargetKind.HEAP -> PooledByteBufAllocator.DEFAULT.heapBuffer(
        ISSUE756_TARGET_CAPACITY,
        ISSUE756_TARGET_CAPACITY,
    )
    Issue756TargetKind.DIRECT -> PooledByteBufAllocator.DEFAULT.directBuffer(
        ISSUE756_TARGET_CAPACITY,
        ISSUE756_TARGET_CAPACITY,
    )
}

private fun resetTarget(target: ByteBuf) {
    check(target.capacity() == ISSUE756_TARGET_CAPACITY) { "Issue 756 target capacity drifted before reset." }
    check(target.maxCapacity() == ISSUE756_TARGET_CAPACITY) { "Issue 756 target maxCapacity drifted before reset." }
    target.setZero(0, ISSUE756_START_INDEX + 1)
    target.setByte(ISSUE756_PREFIX_INDEX, ISSUE756_PREFIX.toInt())
    target.setByte(ISSUE756_START_INDEX, ISSUE756_SENTINEL.toInt())
    target.setIndex(ISSUE756_READER_INDEX, ISSUE756_START_INDEX)
    target.markReaderIndex()
    target.markWriterIndex()
}

private fun ByteBuf.snapshot(): ResetSnapshot =
    ResetSnapshot(capacity(), maxCapacity(), readerIndex(), writerIndex())

private fun methodName(
    backend: PreflightBackend,
    targetKind: Issue756TargetKind,
    path: PreflightPath,
): String {
    val target = targetKind.name.lowercase().replaceFirstChar(Char::uppercase)
    val suffix = when (path) {
        PreflightPath.BASELINE  -> "CopiedBaseline"
        PreflightPath.CANDIDATE -> "Candidate"
    }
    return "${backend.methodPrefix}$target$suffix"
}

private fun expectedMethodNames(): Set<String> =
    PreflightBackend.entries.flatMap { backend ->
        Issue756TargetKind.entries.flatMap { targetKind ->
            PreflightPath.entries.map { path -> methodName(backend, targetKind, path) }
        }
    }.toSet()

private fun verifyBenchmarkMethods(expectedMethods: Set<String>) {
    val actualMethods = LettuceCodecBenchmark::class.java.declaredMethods
        .filter { it.isAnnotationPresent(Benchmark::class.java) }
        .map { it.name }
        .toSet()
    check(actualMethods == expectedMethods) { "Issue 756 JMH method matrix drifted." }
}

private fun issue756PayloadIdentity(): String =
    "${ISSUE756_PAYLOAD.id}|${ISSUE756_PAYLOAD.name}|${ISSUE756_PAYLOAD.description}"

private fun captureFailure(block: () -> Unit): Throwable {
    var captured: Throwable? = null
    try {
        block()
    } catch (failure: Throwable) {
        captured = failure
    }
    return checkNotNull(captured) { "Issue 756 read-only fixture unexpectedly succeeded." }
}

private fun silenceThirdPartyLogging() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off")
    val loggerFactory = Class.forName("org.slf4j.LoggerFactory")
        .getMethod("getILoggerFactory")
        .invoke(null)
    if (loggerFactory.javaClass.name == "ch.qos.logback.classic.LoggerContext") {
        val levelClass = Class.forName("ch.qos.logback.classic.Level")
        val off = levelClass.getField("OFF").get(null)
        val root = loggerFactory.javaClass
            .getMethod("getLogger", String::class.java)
            .invoke(loggerFactory, "ROOT")
        root.javaClass.getMethod("setLevel", levelClass).invoke(root, off)
    }
}

private fun Throwable.signature(): Pair<String, String?> =
    javaClass.name to cause?.javaClass?.name

private fun sha256(value: String): String = sha256(value.encodeToByteArray())

private fun sha256(value: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(it) }

private fun String.jsonString(): String = buildString(length + 2) {
    append('"')
    this@jsonString.forEach { char ->
        when (char) {
            '"'  -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

private fun StringBuilder.appendJsonField(
    name: String,
    value: String,
    trailingComma: Boolean = true,
) {
    append(name.jsonString())
    append(':')
    append(value.jsonString())
    if (trailingComma) append(',')
}
