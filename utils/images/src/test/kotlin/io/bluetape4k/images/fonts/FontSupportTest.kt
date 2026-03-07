package io.bluetape4k.images.fonts

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.awt.Font

class FontSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `기본 폰트를 생성한다`() {
        val font = fontOf()

        font.shouldNotBeNull()
        font.family shouldBeEqualTo Font.SANS_SERIF
        font.style shouldBeEqualTo Font.PLAIN
        font.size shouldBeEqualTo DEFAULT_FONT_SIZE
    }

    @Test
    fun `스타일과 크기를 지정하여 폰트를 생성한다`() {
        val font = fontOf(style = Font.BOLD, size = 24)

        font.shouldNotBeNull()
        font.style shouldBeEqualTo Font.BOLD
        font.size shouldBeEqualTo 24
    }

    @Test
    fun `ITALIC 스타일 폰트를 생성한다`() {
        val font = fontOf(style = Font.ITALIC, size = 16)

        font.style shouldBeEqualTo Font.ITALIC
        font.size shouldBeEqualTo 16
    }

    @Test
    fun `DEFAULT_FONT가 올바르게 초기화 된다`() {
        DEFAULT_FONT.shouldNotBeNull()
        DEFAULT_FONT.family shouldBeEqualTo Font.SANS_SERIF
        DEFAULT_FONT.style shouldBeEqualTo Font.PLAIN
        DEFAULT_FONT.size shouldBeEqualTo DEFAULT_FONT_SIZE
    }
}
