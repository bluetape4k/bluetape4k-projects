package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex

abstract class AbstractFilterTest: AbstractImageTest() {

    companion object: KLogging() {
        const val FILTERS_DIR = "/images/filters/"
    }

    protected fun loadResourceImageBytes(imageName: String): ByteArray {
        return Resourcex.getBytes("$FILTERS_DIR/$imageName")
    }

    protected fun loadResourceImage(imageName: String): ImmutableImage {
        return immutableImageOf(loadResourceImageBytes(imageName))
    }
}
