package io.bluetape4k.io.benchmark

import io.bluetape4k.io.digest.DigestSession
import io.bluetape4k.io.digest.Digesters
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@Threads(1)
class DigestBenchmark {

    @Param("64", "1024", "65536")
    var payloadSize: Int = 64

    private lateinit var payload: ByteArray
    private lateinit var expectedDigest: ByteArray
    private lateinit var reusedSession: DigestSession
    private var splitIndex: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        payload = ByteArray(payloadSize) { index -> ((index * 31 + payloadSize) % 256).toByte() }
        splitIndex = payloadSize / 2
        expectedDigest = MessageDigest.getInstance("SHA-256").digest(payload)
        reusedSession = Digesters.SHA256.newSession()

        check(MessageDigest.getInstance("SHA-256").digest(payload).contentEquals(expectedDigest))
        check(Digesters.SHA256.digest(payload).contentEquals(expectedDigest))
        check(updateInChunks(Digesters.SHA256.newSession()).contentEquals(expectedDigest))
        check(updateInChunks(reusedSession).contentEquals(expectedDigest))
    }

    /** `MessageDigest.getInstance` 생성과 단일 digest를 함께 측정합니다. */
    @Benchmark
    fun jdkOneShot(): ByteArray = MessageDigest.getInstance("SHA-256").digest(payload)

    /** 공유 API의 one-shot 경로를 측정합니다. 호출마다 새 JDK session을 생성합니다. */
    @Benchmark
    fun apiOneShot(): ByteArray = Digesters.SHA256.digest(payload)

    /** 새 session 생성, 두 조각 update, digest를 모두 측정합니다. */
    @Benchmark
    fun apiIncrementalNewSession(): ByteArray = updateInChunks(Digesters.SHA256.newSession())

    /** Trial setup에서 만든 thread-local session의 두 조각 update와 digest만 측정합니다. */
    @Benchmark
    fun apiIncrementalReusedSession(): ByteArray = updateInChunks(reusedSession)

    private fun updateInChunks(session: DigestSession): ByteArray =
        session.update(payload, 0, splitIndex)
            .update(payload, splitIndex, payload.size - splitIndex)
            .digest()
}
