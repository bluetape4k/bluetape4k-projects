package io.bluetape4k.geohash

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug

abstract class AbstractGeoHashTest {

    companion object: KLogging() {
        const val DELTA: Double = 1e-12
    }

    protected fun GeoHash.printBoundingBox() {
        log.debug {
            """
            Bounding Box:
            center=$boundingBoxCenter
            corners=$boundingBox
            """.trimIndent()
        }
    }
}
