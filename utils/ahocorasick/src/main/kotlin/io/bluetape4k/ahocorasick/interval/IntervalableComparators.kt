package io.bluetape4k.ahocorasick.interval

object IntervalableComparators {

    val SizeComparator: Comparator<Intervalable> = Comparator { o1, o2 ->
        var comparison = o1.size - o2.size
        if (comparison == 0) {
            comparison = o1.start - o2.start
        }
        comparison
    }

    val ReverseSizeComparator: Comparator<Intervalable> = Comparator { o1, o2 ->
        var comparison = o2.size - o1.size
        if (comparison == 0) {
            comparison = o1.start - o2.start
        }
        comparison
    }

    val PositionComparator: Comparator<Intervalable> = Comparator { o1, o2 ->
        o1.start - o2.start
    }
}
