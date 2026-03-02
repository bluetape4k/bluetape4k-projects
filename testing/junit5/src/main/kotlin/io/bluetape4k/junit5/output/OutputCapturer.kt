package io.bluetape4k.junit5.output

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream


/**
 * `System.out/err` 출력을 메모리에 복사해 테스트에서 조회할 수 있게 합니다.
 *
 * ## 동작/계약
 * - [startCapture] 이후 출력은 원본 스트림과 메모리 버퍼에 동시에 기록됩니다.
 * - [capture]는 flush 후 UTF-8 문자열 스냅샷을 반환합니다.
 * - [finishCapture]는 표준 스트림을 원복하고 내부 스트림을 닫습니다.
 * - 한 인스턴스는 테스트 단위 재사용을 가정하며, 스레드 동기화는 제공하지 않습니다.
 *
 * ```kotlin
 * @OutputCapture
 * class CaptureTest {
 *   @org.junit.jupiter.api.Test
 *   fun sample(c: OutputCapturer) {
 *      println("hello") /* c.capture().contains("hello") == true */
 *   }
 * }
 * ```
 */
class OutputCapturer {

    private var copy: ByteArrayOutputStream? = null
    private var captureOut: CaptureOutputStream? = null
    private var captureErr: CaptureOutputStream? = null

    /**
     * 현재까지 캡처한 출력 내용을 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - 반환 전 [flush]를 호출해 버퍼를 동기화합니다.
     * - 캡처가 시작되지 않았으면 빈 문자열을 반환합니다.
     */
    fun capture(): String {
        flush()
        return copy?.toString(Charsets.UTF_8).orEmpty()
    }

    /**
     * 캡처 문자열을 계산해 [body]에 전달합니다.
     */
    inline fun expect(body: (String) -> Unit) {
        body(capture())
    }

    /**
     * out/err 캡처 스트림을 flush합니다.
     */
    fun flush() {
        captureOut?.flush()
        captureErr?.flush()
    }

    override fun toString(): String {
        return capture()
    }

    internal fun startCapture() {
        copy = ByteArrayOutputStream()
        captureOut = CaptureOutputStream(System.out, copy!!)
        captureErr = CaptureOutputStream(System.err, copy!!)

        System.setOut(PrintStream(captureOut!!))
        System.setErr(PrintStream(captureErr!!))
    }

    internal fun finishCapture() {
        System.setOut(captureOut?.origin)
        System.setErr(captureErr?.origin)

        copy?.close()
        captureOut?.close()
        captureErr?.close()
    }


    private class CaptureOutputStream(
        val origin: PrintStream,
        val copy: OutputStream,
    ): OutputStream() {

        override fun write(b: Int) {
            copy.write(b)
            origin.write(b)
            origin.flush()
        }

        override fun write(b: ByteArray) {
            write(b, 0, b.size)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            copy.write(b, off, len)
            origin.write(b, off, len)
            origin.flush()
        }

        override fun flush() {
            origin.flush()
            copy.flush()
        }

        override fun close() {
            runCatching { origin.close() }
            runCatching { copy.close() }
        }
    }
}
