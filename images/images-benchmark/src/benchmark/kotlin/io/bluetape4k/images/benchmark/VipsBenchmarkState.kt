package io.bluetape4k.images.benchmark

import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.images.vips.VipsImage
import io.bluetape4k.images.vips.VipsRuntime
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.nio.file.Paths

/**
 * JMH 벤치마크에서 vips 런타임 생명주기와 이미지 바이트를 관리하는 Thread-scope State.
 *
 * vips 구현체는 런타임에만 클래스패스에 존재하므로 리플렉션으로 초기화합니다.
 * 초기화 실패 시 [vipsAvailable] = false 로 설정하며, 벤치마크 메서드는 이를 확인해야 합니다.
 *
 * 사용 예:
 * ```kotlin
 * @Benchmark
 * fun myBenchmark(state: VipsBenchmarkState, bh: Blackhole) {
 *     if (!state.vipsAvailable) { bh.consume(null); return }
 *     state.createVipsImage().use { img -> bh.consume(img.resize(800, 600)) }
 * }
 * ```
 */
@State(Scope.Thread)
class VipsBenchmarkState {

    companion object : KLogging() {
        private const val FFM_RUNTIME_CLASS = "io.bluetape4k.images.vips.java25.FfmVipsRuntime"
        private const val JNI_RUNTIME_CLASS = "io.bluetape4k.images.vips.java21.JVipsRuntime"
        private const val FFM_IMAGE_SUPPORT_CLASS = "io.bluetape4k.images.vips.java25.FfmVipsImageSupportKt"
        private const val JNI_IMAGE_SUPPORT_CLASS = "io.bluetape4k.images.vips.java21.JVipsImageSupportKt"

        // vips-ffm이 사용하는 라이브러리 경로 override 프로퍼티 (VipsLibLookup.java 참고)
        private const val PROP_VIPS_PATH = "vipsffm.libpath.vips.override"
        private const val PROP_GLIB_PATH = "vipsffm.libpath.glib.override"
        private const val PROP_GOBJECT_PATH = "vipsffm.libpath.gobject.override"

        // Homebrew 기본 설치 경로
        private const val HOMEBREW_LIB = "/opt/homebrew/lib"
    }

    /** vips 런타임이 성공적으로 초기화되었는지 여부. false 이면 vips 벤치마크를 skip해야 합니다. */
    var vipsAvailable: Boolean = false

    /** vips 이미지 생성에 사용할 JPEG 바이트 (4K 사진) */
    var photo4kJpegBytes: ByteArray = ByteArray(0)

    /** vips 이미지 생성에 사용할 JPEG 바이트 (썸네일) */
    var thumbnailJpegBytes: ByteArray = ByteArray(0)

    private var runtime: VipsRuntime? = null
    private var createImageFn: ((ByteArray) -> VipsImage)? = null

    @Setup(Level.Trial)
    fun setup() {
        // 이미지 바이트 사전 준비
        val jpegWriter = JpegWriter(80, false)
        photo4kJpegBytes = BenchmarkImageSets.photo4k.bytes(jpegWriter)
        thumbnailJpegBytes = BenchmarkImageSets.thumbnail.bytes(jpegWriter)

        // macOS에서는 SIP가 DYLD_LIBRARY_PATH를 제거하므로 절대 경로를 시스템 프로퍼티로 설정
        applyMacOsVipsLibraryPaths()

        // vips 런타임 초기화 (리플렉션으로 구현체 탐색)
        vipsAvailable = tryInitVipsRuntime()
        if (vipsAvailable) {
            log.debug { "VipsBenchmarkState: vips 런타임 초기화 성공" }
        } else {
            log.warn { "VipsBenchmarkState: vips 런타임 초기화 실패 — vips 벤치마크를 skip합니다" }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        // VipsRuntime.shutdown()은 되돌릴 수 없으므로 벤치마크에서는 호출하지 않습니다.
        // 프로세스 종료 시 JVM이 정리합니다.
        runtime = null
        createImageFn = null
    }

    /**
     * vips 이미지를 생성합니다.
     *
     * @param bytes 이미지 바이트 배열
     * @return [VipsImage] 인스턴스. 호출자가 [AutoCloseable.close]를 호출해야 합니다.
     * @throws IllegalStateException [vipsAvailable]이 false인 경우
     */
    fun createVipsImage(bytes: ByteArray): VipsImage {
        val fn = requireNotNull(createImageFn) { "vips를 사용할 수 없습니다 — vipsAvailable을 먼저 확인하세요" }
        return fn(bytes)
    }

    /**
     * macOS(Apple Silicon/x86)에서 vips-ffm이 libvips를 찾을 수 있도록
     * Homebrew 설치 경로를 시스템 프로퍼티로 등록합니다.
     *
     * macOS SIP는 서명된 JVM에서 DYLD_LIBRARY_PATH를 제거하므로,
     * [SymbolLookup.libraryLookup] 호출 시 절대 경로가 필요합니다.
     * VipsLibLookup.java의 `vipsffm.libpath.*.override` 프로퍼티로 경로를 주입합니다.
     */
    private fun applyMacOsVipsLibraryPaths() {
        val os = System.getProperty("os.name", "").lowercase()
        if (!os.contains("mac")) return

        listOf(
            PROP_VIPS_PATH to "$HOMEBREW_LIB/libvips.dylib",
            PROP_GLIB_PATH to "$HOMEBREW_LIB/libglib-2.0.dylib",
            PROP_GOBJECT_PATH to "$HOMEBREW_LIB/libgobject-2.0.dylib",
        ).forEach { (prop, path) ->
            if (System.getProperty(prop) == null && Paths.get(path).toFile().exists()) {
                System.setProperty(prop, path)
                log.info { "macOS vips 경로 설정: $prop=$path" }
            }
        }
    }

    private fun tryInitVipsRuntime(): Boolean {
        // java25 (FFM) 먼저 시도 → 실패하면 java21 (JNI) 시도
        return tryInitWithClass(FFM_RUNTIME_CLASS, FFM_IMAGE_SUPPORT_CLASS, "ffmVipsImageOf")
            || tryInitWithClass(JNI_RUNTIME_CLASS, JNI_IMAGE_SUPPORT_CLASS, "vipsImageOf")
    }

    private fun tryInitWithClass(
        runtimeClass: String,
        supportClass: String,
        factoryMethodName: String,
    ): Boolean {
        return try {
            val runtimeKClass = Class.forName(runtimeClass)
            // Kotlin object 싱글턴의 INSTANCE 필드 접근
            val instance = runtimeKClass.getField("INSTANCE").get(null) as VipsRuntime
            instance.init()
            runtime = instance

            val supportKClass = Class.forName(supportClass)
            val method = supportKClass.getMethod(factoryMethodName, ByteArray::class.java)
            createImageFn = { bytes -> method.invoke(null, bytes) as VipsImage }
            true
        } catch (t: Throwable) {
            // UnsatisfiedLinkError, ClassNotFoundException, VipsInitializationException 등
            log.warn(t) { "vips 런타임 초기화 실패 ($runtimeClass): ${t::class.simpleName}: ${t.message}" }
            false
        }
    }
}
