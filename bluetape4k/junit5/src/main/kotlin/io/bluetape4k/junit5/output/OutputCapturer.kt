package io.bluetape4k.junit5.output

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream


/**
 * 테스트 시 [System.out], [System.err]로 출력되는 정보를 메모리에 저장했다가 `capture` 메소드를 통해 제공합니다.
 *
 * ```kotlin
 * @OutputCapture
 * class TestClass {
 *     fun testOutput(capturer: OutputCapturer) {
 *         println("Print to System.out!")
 *
 *         capturer.expect { "System.out!" }
 *         capturer.capture() shouldContain "System.out!"
 *     }
 * }
 * ```
 *
 * 테스트 메소드에 직접 적용
 * ```kotlin
 * @OutputCapture
 * fun testOutput(capturer: OutputCapturer) {
 *     System.err.println("Print to System.err!")
 *
 *     capturer.expect { "System.err!" }
 *     capturer.capture() shouldContain "System.err!"
 * }
 * ```
 *
 * @see [OutputCapture]
 */
class OutputCapturer {

    private var copy: ByteArrayOutputStream? = null
    private var captureOut: CaptureOutputStream? = null
    private var captureErr: CaptureOutputStream? = null

    fun capture(): String {
        flush()
        return copy?.toString(Charsets.UTF_8) ?: ""
    }

    inline fun expect(body: (String) -> Unit) {
        body(capture())
    }

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
