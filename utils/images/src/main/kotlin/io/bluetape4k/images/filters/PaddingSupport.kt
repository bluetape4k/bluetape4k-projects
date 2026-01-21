package io.bluetape4k.images.filters

import com.sksamuel.scrimage.filter.Padding

/**
 * [Padding] 생성자
 */
fun paddingOf(constant: Int): Padding = Padding(constant)

/**
 * [Padding] 생성자
 */
fun paddingOf(
    top: Int,
    right: Int,
    bottom: Int,
    left: Int,
): Padding =
    Padding(top, right, bottom, left)
