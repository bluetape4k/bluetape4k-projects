package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.filter.BrightnessFilter
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ImageFilterChainTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    @Test
    fun `empty ops - build returns empty list`() {
        val chain = ImageFilterChain()
        val ops = chain.build()
        ops.size shouldBeEqualTo 0
    }

    @Test
    fun `raw filter - build returns single Op Native`() {
        val chain = ImageFilterChain()
        chain.raw(BrightnessFilter(1.2f))

        val ops = chain.build()
        ops.size shouldBeEqualTo 1
        (ops[0] is ImageFilterChain.Op.Native).shouldBeTrue()
    }

    @Test
    fun `pixel transform - build returns single Op Pixel`() {
        val chain = ImageFilterChain()
        chain.pixel { it }

        val ops = chain.build()
        ops.size shouldBeEqualTo 1
        (ops[0] is ImageFilterChain.Op.Pixel).shouldBeTrue()
    }

    @Test
    fun `raw then pixel - build preserves insertion order`() {
        val chain = ImageFilterChain()
        chain.raw(BrightnessFilter(1.2f))
        chain.pixel { it }

        val ops = chain.build()
        ops.size shouldBeEqualTo 2
        (ops[0] is ImageFilterChain.Op.Native).shouldBeTrue()
        (ops[1] is ImageFilterChain.Op.Pixel).shouldBeTrue()
    }

    @Test
    fun `brightness contrast sepia - build returns three Native ops in order`() {
        val chain = ImageFilterChain()
        chain.brightness(1.2f)
        chain.contrast(1.1)
        chain.sepia()

        val ops = chain.build()
        ops.size shouldBeEqualTo 3
        (ops[0] is ImageFilterChain.Op.Native).shouldBeTrue()
        (ops[1] is ImageFilterChain.Op.Native).shouldBeTrue()
        (ops[2] is ImageFilterChain.Op.Native).shouldBeTrue()
    }
}
