package io.bluetape4k.ahocorasick.interval

/**
 * Intervalable 객체를 비교하기 위한 Comparator 모음.
 *
 * 이 객체는 Intervalable 객체들을 다양한 기준으로 정렬할 수 있는 Comparator를 제공합니다.
 */
object IntervalableComparators {
    /**
     * Interval의 크기(길이)를 기준으로 오름차순 정렬하는 Comparator.
     *
     * 크기가 같으면 시작 위치를 기준으로 정렬합니다.
     */
    val SizeComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            var comparison = o1.size - o2.size
            if (comparison == 0) {
                comparison = o1.start - o2.start
            }
            comparison
        }

    /**
     * Interval의 크기(길이)를 기준으로 내림차순 정렬하는 Comparator.
     *
     * 크기가 같으면 시작 위치를 기준으로 정렬합니다.
     */
    val ReverseSizeComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            var comparison = o2.size - o1.size
            if (comparison == 0) {
                comparison = o1.start - o2.start
            }
            comparison
        }

    /**
     * Interval의 시작 위치를 기준으로 정렬하는 Comparator.
     */
    val PositionComparator: Comparator<Intervalable> =
        Comparator { o1, o2 ->
            o1.start - o2.start
        }
}
