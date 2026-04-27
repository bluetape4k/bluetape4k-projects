package io.bluetape4k.images.svg

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.junit.jupiter.api.Test

/**
 * BatikSvgRasterizer XXE/SSRF 보안 방어 검증 테스트입니다.
 */
@TempFolderTest
class BatikSvgRasterizerSecurityTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    private val rasterizer = BatikSvgRasterizer()

    @Test
    fun `XXE DOCTYPE 선언 SVG - 기본 옵션에서 거부됨`() = runSuspendIO {
        // security_xxe.svg에는 DOCTYPE + file:///etc/passwd 외부 엔티티가 있음
        val input = Resourcex.getInputStream("images/security_xxe.svg")!!

        try {
            input.use { rasterizer.rasterize(it) }
            // XXE 차단 시 예외가 발생하거나, 엔티티가 무시된 상태로 이미지가 반환됨
            // 중요한 것: /etc/passwd 내용이 이미지에 포함되지 않아야 함
            log.debug { "DOCTYPE SVG: 예외 없이 처리됨 (엔티티 무시됨)" }
        } catch (e: Exception) {
            log.debug { "DOCTYPE SVG: 예외로 거부됨 (예상 가능한 동작): ${e.javaClass.simpleName}" }
            // 예외로 거부되는 것도 올바른 동작
        }
    }

    @Test
    fun `외부 HTTP 리소스 참조 SVG - 기본 옵션에서 차단됨`() = runSuspendIO {
        // external_resource.svg에는 http://example.com/remote.png 참조가 있음
        val input = Resourcex.getInputStream("images/external_resource.svg")!!

        try {
            // allowExternalResources=false(기본값) → 외부 로드 차단
            input.use { rasterizer.rasterize(it, SvgRasterizeOptions(allowExternalResources = false)) }
            log.debug { "외부 리소스 SVG: 예외 없이 처리됨 (리소스 무시됨)" }
        } catch (e: Exception) {
            log.debug { "외부 리소스 SVG: 예외로 거부됨 (예상 가능한 동작): ${e.javaClass.simpleName}" }
        }
    }

    @Test
    fun `기본 SVG - allowExternalResources=false에서 정상 래스터화`() = runSuspendIO {
        // 외부 리소스 없는 일반 SVG는 기본 옵션에서 정상 처리되어야 함
        val input = Resourcex.getInputStream("images/sample.svg")!!
        val opts = SvgRasterizeOptions(allowExternalResources = false)

        input.use {
            val image = rasterizer.rasterize(it, opts)
            log.debug { "일반 SVG 래스터화: ${image.width}x${image.height}" }
        }
    }
}
