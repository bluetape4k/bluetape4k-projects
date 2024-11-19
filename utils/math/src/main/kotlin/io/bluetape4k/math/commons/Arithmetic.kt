package io.bluetape4k.math.commons

/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("plusOfDoubleSequence")
operator fun Sequence<Double>.plus(right: Sequence<Double>): Sequence<Double> = sequence {
    val lhs = this@plus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() + rhs.next())
    }
}

@JvmName("plusOfDoubleIterable")
operator fun Iterable<Double>.plus(right: Iterable<Double>): Iterable<Double> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("minusOfDoubleSequence")
operator fun Sequence<Double>.minus(right: Sequence<Double>): Sequence<Double> = sequence {
    val lhs = this@minus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() - rhs.next())
    }
}

@JvmName("minusOfDoubleIterable")
operator fun Iterable<Double>.minus(right: Iterable<Double>): Iterable<Double> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("timesOfDoubleSequence")
operator fun Sequence<Double>.times(right: Sequence<Double>): Sequence<Double> = sequence {
    val lhs = this@times.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() * rhs.next())
    }
}

@JvmName("timesOfDoubleIterable")
operator fun Iterable<Double>.times(right: Iterable<Double>): Iterable<Double> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("divOfDoubleSequence")
operator fun Sequence<Double>.div(right: Sequence<Double>): Sequence<Double> = sequence {
    val lhs = this@div.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() / rhs.next())
    }
}

@JvmName("divOfDoubleIterable")
operator fun Iterable<Double>.div(right: Iterable<Double>): Iterable<Double> =
    asSequence().div(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("plusOfFloatSequence")
operator fun Sequence<Float>.plus(right: Sequence<Float>): Sequence<Float> = sequence {
    val lhs = this@plus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() + rhs.next())
    }
}

@JvmName("plusOfFloatIterable")
operator fun Iterable<Float>.plus(right: Iterable<Float>): Iterable<Float> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("minusOfFloatSequence")
operator fun Sequence<Float>.minus(right: Sequence<Float>): Sequence<Float> = sequence {
    val lhs = this@minus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() - rhs.next())
    }
}

@JvmName("minusOfFloatIterable")
operator fun Iterable<Float>.minus(right: Iterable<Float>): Iterable<Float> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("timesOfFloatSequence")
operator fun Sequence<Float>.times(right: Sequence<Float>): Sequence<Float> = sequence {
    val lhs = this@times.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() * rhs.next())
    }
}

@JvmName("timesOfFloatIterable")
operator fun Iterable<Float>.times(right: Iterable<Float>): Iterable<Float> =
    asSequence().times(right.asSequence()).asIterable()


/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("divOfFloatSequence")
operator fun Sequence<Float>.div(right: Sequence<Float>): Sequence<Float> = sequence {
    val lhs = this@div.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() / rhs.next())
    }
}

@JvmName("divOfFloatIterable")
operator fun Iterable<Float>.div(right: Iterable<Float>): Iterable<Float> =
    asSequence().div(right.asSequence()).asIterable()


/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("plusOfLongSequence")
operator fun Sequence<Long>.plus(right: Sequence<Long>): Sequence<Long> = sequence {
    val lhs = this@plus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() + rhs.next())
    }
}

@JvmName("plusOfLongIterable")
operator fun Iterable<Long>.plus(right: Iterable<Long>): Iterable<Long> =
    asSequence().plus(right.asSequence()).asIterable()


/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("minusOfLongSequence")
operator fun Sequence<Long>.minus(right: Sequence<Long>): Sequence<Long> = sequence {
    val lhs = this@minus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() - rhs.next())
    }
}

@JvmName("minusOfLongIterable")
operator fun Iterable<Long>.minus(right: Iterable<Long>): Iterable<Long> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("timesOfLongSequence")
operator fun Sequence<Long>.times(right: Sequence<Long>): Sequence<Long> = sequence {
    val lhs = this@times.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() * rhs.next())
    }
}

@JvmName("timesOfLongIterable")
operator fun Iterable<Long>.times(right: Iterable<Long>): Iterable<Long> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("divOfLongSequence")
operator fun Sequence<Long>.div(right: Sequence<Long>): Sequence<Long> = sequence {
    val lhs = this@div.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() / rhs.next())
    }
}

@JvmName("divOfLongIterable")
operator fun Iterable<Long>.div(right: Iterable<Long>): Iterable<Long> =
    asSequence().div(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("plusOfIntSequence")
operator fun Sequence<Int>.plus(right: Sequence<Int>): Sequence<Int> = sequence {
    val lhs = this@plus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() + rhs.next())
    }
}

@JvmName("plusOfIntIterable")
operator fun Iterable<Int>.plus(right: Iterable<Int>): Iterable<Int> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("minusOfIntSequence")
operator fun Sequence<Int>.minus(right: Sequence<Int>): Sequence<Int> = sequence {
    val lhs = this@minus.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() - rhs.next())
    }
}

@JvmName("minusOfIntIterable")
operator fun Iterable<Int>.minus(right: Iterable<Int>): Iterable<Int> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("timesOfIntSequence")
operator fun Sequence<Int>.times(right: Sequence<Int>): Sequence<Int> = sequence {
    val lhs = this@times.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() * rhs.next())
    }
}

@JvmName("timesOfIntIterable")
operator fun Iterable<Int>.times(right: Iterable<Int>): Iterable<Int> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * @param right right side collection
 * @return
 */
@JvmName("divOfIntSequence")
operator fun Sequence<Int>.div(right: Sequence<Int>): Sequence<Int> = sequence {
    val lhs = this@div.iterator()
    val rhs = right.iterator()

    while (lhs.hasNext() && rhs.hasNext()) {
        yield(lhs.next() / rhs.next())
    }
}

@JvmName("divOfIntIterable")
operator fun Iterable<Int>.div(right: Iterable<Int>): Iterable<Int> =
    asSequence().div(right.asSequence()).asIterable()
