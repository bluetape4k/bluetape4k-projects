package io.bluetape4k.images

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class ImageFormatTest {

    @Test
    fun `parse should ignore case and trim`() {
        ImageFormat.parse(" jpg ") shouldBeEqualTo ImageFormat.JPG
        ImageFormat.parse("Png") shouldBeEqualTo ImageFormat.PNG
    }

    @Test
    fun `parse should return null for blank or unknown`() {
        ImageFormat.parse("").shouldBeNull()
        ImageFormat.parse("  ").shouldBeNull()
        ImageFormat.parse("unknown").shouldBeNull()
    }
}
