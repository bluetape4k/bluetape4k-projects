package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.ValueObject

interface Intervalable: Comparable<Intervalable>, ValueObject {

    val start: Int
    val end: Int

    val size: Int get() = end - start + 1
}
