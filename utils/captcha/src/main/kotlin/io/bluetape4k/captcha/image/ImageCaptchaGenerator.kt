package io.bluetape4k.captcha.image

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.captcha.Captcha
import io.bluetape4k.captcha.CaptchaCodeGenerator
import io.bluetape4k.captcha.CaptchaGenerator
import io.bluetape4k.captcha.config.CaptchaConfig
import io.bluetape4k.captcha.exceptions.FontLoadException
import io.bluetape4k.captcha.utils.FontProvider
import io.bluetape4k.images.useGraphics
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.random.Random

class ImageCaptchaGenerator(
    override val config: CaptchaConfig = CaptchaConfig.DEFAULT,
    override val codeGenerator: CaptchaCodeGenerator = CaptchaCodeGenerator.DEFAULT,
): CaptchaGenerator<ImmutableImage> {

    companion object: KLogging()

    override fun generate(): Captcha<ImmutableImage> {
        val code = codeGenerator.next(config.length.coerceAtLeast(4))
        val image = ImmutableImage.create(config.width, config.height, BufferedImage.TYPE_INT_RGB)

        image.useGraphics { graphics ->
            drawCode(graphics, code)
            if (config.noiseCount > 0) {
                drawNoise(graphics)
            }
        }

        return ImageCaptcha(code, image)
    }

    private fun drawCode(graphics: Graphics2D, code: String) {
        val prevColor = graphics.color
        val prevFont = graphics.font

        graphics.color = config.backgroundColor
        graphics.fillRect(0, 0, config.width, config.height)

        log.debug { "Draw code. code=$code, width=${config.width}, height=${config.height}" }

        try {
            val fontMetrics = graphics.fontMetrics
            val totalWidth = fontMetrics.stringWidth(code)  // 지정한 문자열을 모두 그렸을 때의 전체 너비
            val charGap = (config.width - totalWidth) / (code.length + 2) - 4  // 문자열 간격
            var x = charGap / 4
            val y = (config.height - fontMetrics.height) / 2 + fontMetrics.ascent

            code.forEach { c ->
                graphics.font = getRandomFont()
                graphics.color = getRandomColor()

                val charWidth = graphics.fontMetrics.charWidth(c.code)
                val charX = x + charWidth / 2
                val angle = Random.nextInt(-20, 20)

                log.trace { "draw char at ($charX, $y), angle=$angle, charGap=$charGap, x=$x, y=$y" }
                try {
                    graphics.rotate(Math.toRadians(angle.toDouble()), charX.toDouble(), y.toDouble())
                    graphics.drawString(c.toString(), charX, y)
                } finally {
                    // rotate 는 reset 해줘야, 다음 문자가 제대로 그려짐
                    graphics.rotate(Math.toRadians(-angle.toDouble()), charX.toDouble(), y.toDouble())
                }
                x += charWidth + charGap
            }
        } finally {
            graphics.color = prevColor
            graphics.font = prevFont
        }
    }

    private fun drawNoise(graphics: Graphics) {
        val graphicColor = graphics.color
        try {
            repeat(config.noiseCount) {
                val x1 = Random.nextInt(config.width)
                val y1 = Random.nextInt(config.height)
                val x2 = Random.nextInt(config.width)
                val y2 = Random.nextInt(config.height)

                graphics.color = getRandomColor()
                graphics.drawLine(x1, y1, x2, y2)
            }
        } finally {
            graphics.color = graphicColor
        }
    }

    private val fonts: Array<Font> by lazy {
        if (config.fontPaths.isNotEmpty()) {
            loadCustomFonts().toTypedArray()
        } else {
            FontProvider.loadAllFontsFromResource(CaptchaConfig.DEFAULT_FONTS_IN_RESOURCE).toTypedArray()
        }
    }

    private fun loadCustomFonts(): List<Font> {
        return config.fontPaths.mapNotNull { path ->
            try {
                val fontFile = File(path)
                if (fontFile.exists()) {
                    Font.createFont(Font.TRUETYPE_FONT, fontFile)
                } else {
                    log.warn { "Font file not found: $path" }
                    null
                }
            } catch (e: Exception) {
                val fe = FontLoadException(e)
                log.warn(fe) { "Failed to load font from $path" }
                null
            }
        }
    }

    private fun getRandomFont(): Font {
        val style = config.fontStyles.random()
        val size = config.height.toFloat() / 2F - Random.nextInt(10, 15).toFloat()
        return fonts.random().deriveFont(style, size)
    }

    private fun getRandomColor(): Color = config.themePalette.random()
}
