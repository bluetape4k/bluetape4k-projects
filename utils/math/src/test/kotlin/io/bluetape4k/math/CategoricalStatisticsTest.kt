package io.bluetape4k.math

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.Serializable

class CategoricalStatisticsTest {

    companion object: KLogging()

    @Test
    fun `mode for Ints`() {
        val ints = intArrayOf(2, 54, 67, 3, 4, 5, 2, 2)

        // 요소 중 가장 빈도 수가 높은 놈들을 가져옵니다.
        // {2=3, 54=1, 67=1, 3=1, 4=1, 5=1}
        val maxFreqs = ints.mode()

        maxFreqs.toFastList() shouldBeEqualTo listOf(2)
    }

    @Test
    fun `mode for Ints has top is same`() {
        val ints = intArrayOf(2, 2, 2, 3, 3, 3, 4, 4)

        // 요소 중 가장 빈도 수가 높은 놈들을 가져옵니다.
        // {2=3, 3=3, 4=2}
        val maxFreqs = ints.mode().toFastList()

        maxFreqs shouldBeEqualTo listOf(2, 3)
    }

    data class Product(
        val id: Int,
        val name: String,
        val category: String,
        val section: Int,
        val defectRate: Double,
    ): Serializable

    data class CategoryAndSection(val category: String, val section: Int)

    @Test
    fun `multiple keys`() {
        val products = listOf(
            Product(1, "Rayzeon", "ABR", 3, 1.1),
            Product(2, "ZenFire", "ABZ", 4, 0.7),
            Product(3, "HydroFlux", "ABR", 3, 1.9),
            Product(4, "IceFlyer", "ZBN", 1, 2.4),
            Product(5, "FireCoyote", "ABZ", 4, 3.2),
            Product(6, "LightFiber", "ABZ", 2, 5.1),
            Product(7, "PyroKit", "ABR", 3, 1.4),
            Product(8, "BladeKit", "ZBN", 1, 0.5),
            Product(9, "NightHawk", "ZBN", 1, 3.5),
            Product(10, "NoctoSquirrel", "ABR", 2, 1.1),
            Product(11, "WolverinePack", "ABR", 3, 1.2)
        )

        val countByCategoryAndSection = products.countBy {
            CategoryAndSection(it.category, it.section)
        }
        log.debug { "Count by Category and Section" }
        countByCategoryAndSection.forEach {
            log.debug { "\t$it" }
        }

        val averageDefectByCategoryAndSection =
            products.averageBy(
                keySelector = { CategoryAndSection(it.category, it.section) },
                valueMapper = { it.defectRate })

        log.debug { "Average defect rate by Category and Section" }
        averageDefectByCategoryAndSection.forEach {
            log.debug { "\t$it" }
        }
    }
}
