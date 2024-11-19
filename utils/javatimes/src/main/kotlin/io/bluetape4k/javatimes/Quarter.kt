package io.bluetape4k.javatimes

import io.bluetape4k.support.assertInRange

/**
 * 분기(Quarter) 를 나타내는 enum class 입니다.
 */
enum class Quarter(val number: Int) {

    /**
     * Q1 (1,2,3월)
     */
    Q1(1),

    /**
     * Q2 (4,5,6월)
     *
     */
    Q2(2),

    /**
     * Q3 (7,8,9월)
     */
    Q3(3),

    /**
     * Q4 (10,11,12월)
     */
    Q4(4);


    /**
     * 두 개의 Quarter를 더합니다.
     *
     * @param that Quarter
     * @return Quarter
     */
    operator fun plus(that: Quarter): Quarter = VALS[(ordinal + that.ordinal + 1) % QuartersPerYear]

    /**
     * Quarter를 [quaterCount]만큼 증가시킨 [Quarter]를 반환합니다.
     *
     * ```kotlin
     *  val q3 = Quarter.Q1.increment(2) // return Q3
     *  val q1 = Quarter.Q1.increment(4) // return Q1
     * ```
     * @param quaterCount 증가시킬 quater 수
     * @return Quarter
     */
    fun increment(quaterCount: Int): Quarter {
        var index = (ordinal + quaterCount) % QuartersPerYear
        if (index < 0) index += QuartersPerYear
        return VALS[index]
    }

    /**
     * 지정된 [Quarter]만큼 감소시킵니다.
     *
     * @param that Quarter
     * @return Quarter
     */
    operator fun minus(that: Quarter): Quarter {
        var index = (ordinal - that.ordinal - 1) % QuartersPerYear
        if (index < 0) index += QuartersPerYear
        return VALS[index]
    }

    /**
     * [Quarter]에 [quarterCount]만큼 감소시킵니다.
     *
     * ```kotlin
     *  val q4 = Quarter.Q1.decrement(1) // return Q4
     *  val q1 = Quarter.Q1.decrement(4) // return Q1
     * ```
     *
     * @param quarterCount 감소할 quarter 수
     * @return Quarter
     */
    fun decrement(quarterCount: Int): Quarter {
        var index = (ordinal - quarterCount) % QuartersPerYear
        if (index < 0) index += QuartersPerYear
        return VALS[index]
    }

    /**
     * 지정한 Quarter가 가지는 Month를 반환합니다.
     */
    val months: IntArray
        get() = when (this) {
            Q1 -> Q1Months
            Q2 -> Q2Months
            Q3 -> Q3Months
            Q4 -> Q4Months
        }

    /**
     * 지정한 Quarter의 시작 month
     */
    val startMonth: Int = this.ordinal * MonthsPerQuarter + 1

    /**
     * 지정한 Quarter의 마지막 month
     */
    val endMonth: Int = number * MonthsPerQuarter

    companion object {

        @JvmField
        val VALS: Array<Quarter> = entries.toTypedArray()

        /**
         * [numberOfQuarter]에 해당하는 Quarter를 반환합니다.
         *
         * @param numberOfQuarter Quarter value (1..4)
         * @return Quarter
         */
        @JvmStatic
        fun of(numberOfQuarter: Int): Quarter {
            numberOfQuarter.assertInRange(1, 4, "numberOfQuarter")
            return VALS[numberOfQuarter - 1]
        }

        /**
         * [monthOfYear]이 속한 Quarter를 반환합니다.
         *
         * @param monthOfYear [Quarter]가 속한 month 값 (1..12)
         * @return Quarter
         */
        @JvmStatic
        fun ofMonth(monthOfYear: Int): Quarter {
            monthOfYear.assertInRange(1, 12, "monthOfYear")
            return VALS[(monthOfYear - 1) / MonthsPerQuarter]
        }
    }
}
