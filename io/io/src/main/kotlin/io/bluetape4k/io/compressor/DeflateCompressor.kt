package io.bluetape4k.io.compressor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.zip.ZipException

/**
 * JDK Deflate 알고리즘을 이용한 압축기입니다.
 *
 * `ByteArray` API는 기존 zlib wire 형식을 유지합니다. caller-owned [ByteBuffer] API는
 * 호출할 때마다 새 [Deflater] 또는 [Inflater]를 생성하고 JDK의 `ByteBuffer` 연산에
 * 직접 위임하므로 heap/direct 조합에서 중간 `ByteArray` 복사를 만들지 않습니다.
 *
 * ## Caller-owned 버퍼 계약
 *
 * - source의 `position`, `limit`, byte order, mark는 성공과 실패 모두 보존합니다.
 * - target의 `position`은 성공한 byte 수만큼만 전진합니다.
 * - 실패하면 target의 `position`을 호출 전 값으로 되돌립니다. 이미 기록된 byte의
 *   내용은 보장하지 않으므로 실패한 target 구간을 재사용하거나 읽지 않아야 합니다.
 * - target 용량이 부족하면 [BufferOverflowException]을 던집니다.
 * - 손상되거나 중단된 Deflate payload는 [ZipException]으로 보고합니다.
 *
 * codec은 연산별로 소유하며 성공과 실패 모두에서 정확히 한 번 정리합니다. 연산과
 * 정리가 함께 실패하면 연산 예외를 그대로 유지하고 정리 예외를 suppressed로
 * 추가합니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Deflate!".toByteArray()
 * val compressed = Compressors.Deflate.compress(data)
 * val restored = Compressors.Deflate.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [DeflaterOutputStream]
 * @see [InflaterInputStream]
 * @throws java.io.IOException Deflate stream 처리에 실패한 경우
 * @throws BufferOverflowException caller-owned target 용량이 부족한 경우
 * @throws ZipException caller-owned `ByteBuffer` 복원에서 payload가 손상되었거나 완전하지 않은 경우
 */
class DeflateCompressor private constructor(
    private val deflaterFactory: () -> Deflater,
    private val inflaterFactory: () -> Inflater,
    private val endDeflater: (Deflater) -> Unit,
    private val endInflater: (Inflater) -> Unit,
): AbstractCompressor() {

    constructor(): this(
        deflaterFactory = ::Deflater,
        inflaterFactory = ::Inflater,
        endDeflater = Deflater::end,
        endInflater = Inflater::end,
    )

    companion object {
        /**
         * codec 상태와 정리 실패를 결정적으로 검증하기 위한 내부 테스트 팩토리입니다.
         *
         * 운영 호출은 public no-arg 생성자를 사용해야 합니다.
         */
        internal fun forTesting(
            deflaterFactory: () -> Deflater,
            inflaterFactory: () -> Inflater,
            endDeflater: (Deflater) -> Unit,
            endInflater: (Inflater) -> Unit,
        ): DeflateCompressor =
            DeflateCompressor(
                deflaterFactory = deflaterFactory,
                inflaterFactory = inflaterFactory,
                endDeflater = endDeflater,
                endInflater = endInflater,
            )
    }

    /**
     * [source]의 남은 데이터를 Deflate zlib 형식으로 압축해 [target]에 기록합니다.
     *
     * JDK [Deflater]의 `ByteBuffer` 경로를 사용하며 caller가 소유한 버퍼 상태는 클래스
     * 계약에 따라 보존하거나 commit합니다.
     *
     * @return [target]에 기록한 압축 데이터 길이
     * @throws BufferOverflowException target 용량이 압축 결과보다 작은 경우
     * @throws IllegalStateException codec이 완료되지 않은 채 진행을 멈춘 경우
     */
    override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
        writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            val input = sourceView
                .position(sourcePosition)
                .limit(sourcePosition + sourceRemaining)
            val output = targetView
                .position(targetPosition)
                .limit(targetPosition + targetRemaining)

            useDeflateCodec(deflaterFactory(), endDeflater) { deflater ->
                deflater.setInput(input)
                deflater.finish()

                while (!deflater.finished()) {
                    val inputBefore = deflater.bytesRead
                    val outputBefore = deflater.bytesWritten
                    val produced = deflater.deflate(output)

                    if (deflater.finished()) break
                    if (!output.hasRemaining()) throw BufferOverflowException()
                    if (
                        produced > 0 ||
                        deflater.bytesRead != inputBefore ||
                        deflater.bytesWritten != outputBefore
                    ) {
                        continue
                    }
                    if (deflater.needsInput()) {
                        throw IllegalStateException("Deflater needs input before finishing")
                    }
                    throw IllegalStateException("Deflater made no progress")
                }
                output.position() - targetPosition
            }
        }

    /**
     * [source]의 Deflate zlib payload를 복원해 [target]에 기록합니다.
     *
     * 손상, 중단, 사전 요구 상태를 서로 구분하는 안정된 [ZipException]으로 변환합니다.
     * target이 먼저 소진되면 payload 상태보다 [BufferOverflowException]을 우선합니다.
     *
     * @return [target]에 기록한 복원 데이터 길이
     * @throws BufferOverflowException target 용량이 복원 데이터보다 작은 경우
     * @throws ZipException payload가 손상, 중단되었거나 사전을 요구하는 경우
     */
    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        writeToCallerBufferViews(source, target) {
                sourceView,
                targetView,
                sourcePosition,
                sourceRemaining,
                targetPosition,
                targetRemaining,
            ->
            val input = sourceView
                .position(sourcePosition)
                .limit(sourcePosition + sourceRemaining)
            val output = targetView
                .position(targetPosition)
                .limit(targetPosition + targetRemaining)

            useDeflateCodec(inflaterFactory(), endInflater) { inflater ->
                inflater.setInput(input)

                while (!inflater.finished()) {
                    val inputBefore = inflater.bytesRead
                    val outputBefore = inflater.bytesWritten
                    val produced = try {
                        inflater.inflate(output)
                    } catch (failure: DataFormatException) {
                        throw ZipException("Invalid Deflate payload").apply {
                            initCause(failure)
                        }
                    }

                    if (inflater.finished()) break
                    if (!output.hasRemaining()) throw BufferOverflowException()
                    if (inflater.needsDictionary()) {
                        throw ZipException("Deflate preset dictionary is required")
                    }
                    if (inflater.needsInput()) {
                        throw ZipException("Truncated Deflate payload")
                    }
                    if (
                        produced == 0 &&
                        inflater.bytesRead == inputBefore &&
                        inflater.bytesWritten == outputBefore
                    ) {
                        throw ZipException("Inflater made no progress")
                    }
                }
                output.position() - targetPosition
            }
        }

    /**
     * [plain]을 기존 allocating Deflate zlib wire 형식으로 압축합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        DeflaterOutputStream(output).use { deflate ->
            deflate.write(plain)
            deflate.finish()
        }
        return output.toByteArray()
    }

    /**
     * 기존 allocating API가 받은 Deflate zlib payload를 복원합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            InflaterInputStream(input).use { inflate ->
                inflate.readBytes()
            }
        }
    }
}

/**
 * Deflate codec의 단일 연산과 정리 순서를 보존합니다.
 *
 * 연산 실패가 있으면 같은 throwable identity를 primary로 유지하고 정리 실패를
 * suppressed로 추가합니다. 연산이 성공한 경우에만 정리 실패 자체를 전파합니다.
 */
internal inline fun <R, T> useDeflateCodec(
    resource: R,
    cleanup: (R) -> Unit,
    block: (R) -> T,
): T {
    var operationFailure: Throwable? = null
    try {
        return block(resource)
    } catch (failure: Throwable) {
        operationFailure = failure
        throw failure
    } finally {
        try {
            cleanup(resource)
        } catch (cleanupFailure: Throwable) {
            val operation = operationFailure
            if (operation == null) {
                throw cleanupFailure
            }
            if (
                operation !== cleanupFailure &&
                operation.suppressed.none { suppressed -> suppressed === cleanupFailure }
            ) {
                operation.addSuppressed(cleanupFailure)
            }
        }
    }
}
