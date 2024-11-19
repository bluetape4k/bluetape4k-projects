package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.round

class TrimTest {

    companion object: KLogging()

    @Nested
    inner class Round {
        @Test
        fun `round for special values`() {
            Double.NaN.roundDigits() shouldBeEqualTo round(Double.NaN) shouldBeEqualTo Double.NaN
            Double.POSITIVE_INFINITY.roundDigits() shouldBeEqualTo round(Double.POSITIVE_INFINITY) shouldBeEqualTo Double.POSITIVE_INFINITY
            Double.NEGATIVE_INFINITY.roundDigits() shouldBeEqualTo round(Double.NEGATIVE_INFINITY) shouldBeEqualTo Double.NEGATIVE_INFINITY
            Double.MAX_VALUE.roundDigits() shouldBeEqualTo Double.MAX_VALUE
            Double.MIN_VALUE.roundDigits() shouldBeEqualTo 0.0
        }

        @Test
        fun `round for no digits`() {
            0.0.roundDigits() shouldBeEqualTo 0.0
            0.1.roundDigits() shouldBeEqualTo 0.0

            0.49.roundDigits() shouldBeEqualTo 0.0
            0.49999.roundDigits() shouldBeEqualTo 0.0
            0.5.roundDigits() shouldBeEqualTo 0.0
            0.50.roundDigits() shouldBeEqualTo 0.0
            0.51.roundDigits() shouldBeEqualTo 0.0
            0.5000001.roundDigits() shouldBeEqualTo 0.0

            1.11.roundDigits() shouldBeEqualTo 1.0
            1.49999.roundDigits() shouldBeEqualTo 1.0
            1.50000.roundDigits() shouldBeEqualTo 2.0
            1.50001.roundDigits() shouldBeEqualTo 2.0

            42.4999.roundDigits() shouldBeEqualTo 42.0
            42.5000.roundDigits() shouldBeEqualTo 42.0
            42.5001.roundDigits() shouldBeEqualTo 42.0
            42.5001.roundDigits(1) shouldBeEqualTo 42.5

            (-0.0).roundDigits() shouldBeEqualTo 0.0
            (-0.45).roundDigits() shouldBeEqualTo -0.0
            (-0.50).roundDigits() shouldBeEqualTo -0.0
            (-0.51).roundDigits() shouldBeEqualTo -0.0
            (-0.500001).roundDigits() shouldBeEqualTo -0.0

            (-42.4999).roundDigits() shouldBeEqualTo -42.0
            (-42.5).roundDigits() shouldBeEqualTo -42.0
            (-42.5001).roundDigits() shouldBeEqualTo -42.0
            (-42.9001).roundDigits() shouldBeEqualTo -43.0
        }

        @Test
        fun `round for positive digits`() {
            0.10.roundDigits(1) shouldBeEqualTo 0.1
            0.111.roundDigits(1) shouldBeEqualTo 0.1

            0.149999.roundDigits(1) shouldBeEqualTo 0.1
            0.150000.roundDigits(1) shouldBeEqualTo 0.2
            0.150001.roundDigits(1) shouldBeEqualTo 0.2

            1.149999.roundDigits(1) shouldBeEqualTo 1.1
            1.150000.roundDigits(1) shouldBeEqualTo 1.2
            1.150001.roundDigits(1) shouldBeEqualTo 1.2

            1.145000.roundDigits(2) shouldBeEqualTo 1.140
            1.145000.roundDigits(3) shouldBeEqualTo 1.145
            1.149999.roundDigits(3) shouldBeEqualTo 1.15

            (-0.149999).roundDigits(1) shouldBeEqualTo -0.1
            (-0.150000).roundDigits(1) shouldBeEqualTo -0.2
            (-0.150001).roundDigits(1) shouldBeEqualTo -0.2

            (-1.149999).roundDigits(1) shouldBeEqualTo -1.1
            (-1.150000).roundDigits(1) shouldBeEqualTo -1.2
            (-1.150001).roundDigits(1) shouldBeEqualTo -1.2
        }

        @Test
        fun `round for negative digits`() {
            44.10.roundDigits(-1) shouldBeEqualTo 40.0
            45.111.roundDigits(-1) shouldBeEqualTo 40.0
            46.111.roundDigits(-1) shouldBeEqualTo 50.0

            14.999999.roundDigits(-1) shouldBeEqualTo 10.0
            15.000000.roundDigits(-1) shouldBeEqualTo 20.0
            15.000001.roundDigits(-1) shouldBeEqualTo 20.0

            44.999999.roundDigits(-1) shouldBeEqualTo 40.0
            45.000000.roundDigits(-1) shouldBeEqualTo 40.0
            45.000001.roundDigits(-1) shouldBeEqualTo 40.0

            (-14.999999).roundDigits(-1) shouldBeEqualTo -10.0
            (-15.000000).roundDigits(-1) shouldBeEqualTo -20.0
            (-15.000001).roundDigits(-1) shouldBeEqualTo -20.0

            (-44.999999).roundDigits(-1) shouldBeEqualTo -40.0
            (-45.000000).roundDigits(-1) shouldBeEqualTo -40.0
            (-45.000001).roundDigits(-1) shouldBeEqualTo -40.0

            12.4525.roundDigits(3) shouldBeEqualTo 12.452
            3456.0.roundDigits(-2) shouldBeEqualTo 3400.0
            3556.0.roundDigits(-2) shouldBeEqualTo 3600.0
        }
    }

    @Nested
    inner class Ceil {

        @Test
        fun `ceil digits for special values`() {
            Double.NaN.ceilDigits() shouldBeEqualTo Double.NaN
            Double.POSITIVE_INFINITY.ceilDigits() shouldBeEqualTo Double.POSITIVE_INFINITY
            Double.NEGATIVE_INFINITY.ceilDigits() shouldBeEqualTo Double.NEGATIVE_INFINITY
            Double.MAX_VALUE.ceilDigits() shouldBeEqualTo Double.MAX_VALUE
            Double.MIN_VALUE.ceilDigits() shouldBeEqualTo 1.0
        }

        @Test
        fun `ceil digits with no digits`() {
            0.0.ceilDigits() shouldBeEqualTo 0.0
            0.1.ceilDigits() shouldBeEqualTo 1.0
            0.0001.ceilDigits() shouldBeEqualTo 0.0

            17.000001.ceilDigits() shouldBeEqualTo 17.0
            17.01.ceilDigits() shouldBeEqualTo 17.0
            17.10.ceilDigits() shouldBeEqualTo 18.0

            4.101.ceilDigits() shouldBeEqualTo 5.0
            (-4.101).ceilDigits() shouldBeEqualTo -4.0
        }

        @Test
        fun `ceil digits with positive digits`() {
            0.0.ceilDigits(1) shouldBeEqualTo 0.0
            0.1.ceilDigits(1) shouldBeEqualTo 0.1
            0.101.ceilDigits(1) shouldBeEqualTo 0.1
            0.10001.ceilDigits(1) shouldBeEqualTo 0.1
            0.11.ceilDigits(1) shouldBeEqualTo 0.2

            0.1100001.ceilDigits(2) shouldBeEqualTo 0.11

            4.201.ceilDigits(2) shouldBeEqualTo 4.21
            4.2001.ceilDigits(3) shouldBeEqualTo 4.201

            (-4.201).ceilDigits(2) shouldBeEqualTo -4.2
            (-4.2001).ceilDigits(3) shouldBeEqualTo -4.20
        }

        @Test
        fun `ceil digits with negative digits`() {
            4.201.ceilDigits() shouldBeEqualTo 5.0
            42.01.ceilDigits(-1) shouldBeEqualTo 50.0

            (-4.201).ceilDigits() shouldBeEqualTo -4.0
            (-42.01).ceilDigits(-1) shouldBeEqualTo -40.0
        }
    }

    @Nested
    inner class Floor {

        @Test
        fun `floor digits for special values`() {
            Double.NaN.floorDigits() shouldBeEqualTo Double.NaN
            Double.POSITIVE_INFINITY.floorDigits() shouldBeEqualTo Double.POSITIVE_INFINITY
            Double.NEGATIVE_INFINITY.floorDigits() shouldBeEqualTo Double.NEGATIVE_INFINITY
            Double.MAX_VALUE.floorDigits() shouldBeEqualTo Double.MAX_VALUE
            Double.MIN_VALUE.floorDigits() shouldBeEqualTo 0.0
        }

        @Test
        fun `floor digits with no digits`() {
            0.0.floorDigits() shouldBeEqualTo 0.0
            0.1.floorDigits() shouldBeEqualTo 0.0
            0.0001.floorDigits() shouldBeEqualTo 0.0

            17.000001.floorDigits() shouldBeEqualTo 17.0
            17.01.floorDigits() shouldBeEqualTo 17.0

            4.201.floorDigits() shouldBeEqualTo 4.0
            (-4.201).floorDigits() shouldBeEqualTo -5.0
        }

        @Test
        fun `floor digits with positive digits`() {
            0.0.floorDigits(1) shouldBeEqualTo 0.0
            0.1.floorDigits(1) shouldBeEqualTo 0.1
            0.101.floorDigits(1) shouldBeEqualTo 0.1
            0.10001.floorDigits(1) shouldBeEqualTo 0.1
            0.11.floorDigits(1) shouldBeEqualTo 0.10

            0.1100001.floorDigits(2) shouldBeEqualTo 0.11

            4.201.floorDigits(2) shouldBeEqualTo 4.20
            4.2001.floorDigits(3) shouldBeEqualTo 4.200

            (-4.201).floorDigits(2) shouldBeEqualTo -4.21
            (-4.2001).floorDigits(3) shouldBeEqualTo -4.201
        }

        @Test
        fun `floor digits with negative digits`() {
            4.201.floorDigits() shouldBeEqualTo 4.0
            42.01.floorDigits(-1) shouldBeEqualTo 40.0

            (-4.201).floorDigits() shouldBeEqualTo -5.0
            (-42.01).floorDigits(-1) shouldBeEqualTo -50.0
        }
    }
}
