package io.bluetape4k.hibernate.mapping.associations.unidirection

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class UnidirectionTest(
    @param:Autowired private val cloudRepo: CloudRepository,
    @param:Autowired private val snowflakeRepo: SnowflakeRepository,
): AbstractHibernateTest() {

    companion object: KLogging() {
        fun newCloud(): Cloud {
            return Cloud(
                faker.name().name(),
                faker.random().nextDouble(0.0, 40.0)
            )
        }

        fun newSnowflake(): Snowflake {
            return Snowflake(
                faker.name().name(),
                faker.lorem().characters(16, 255, true)
            )
        }
    }

    @Test
    fun `one-to-many unidirectional association`() {
        val sf1 = newSnowflake()
        val sf2 = newSnowflake()

        val cloud = newCloud()
        cloud.producedSnowflakes.add(sf1)
        cloud.producedSnowflakes.add(sf2)

        cloudRepo.save(cloud)
        flushAndClear()

        val loaded = cloudRepo.findByIdOrNull(cloud.id!!)!!

        loaded shouldBeEqualTo cloud
        loaded.producedSnowflakes shouldContainSame cloud.producedSnowflakes

        // 삭제할 snowflake
        val sfToRemove = loaded.producedSnowflakes.first()
        val sf3 = newSnowflake()
        loaded.producedSnowflakes.remove(sfToRemove)
        // 남은 snowflake
        val sfToRemain = loaded.producedSnowflakes.first()
        loaded.producedSnowflakes.add(sf3)

        cloudRepo.save(loaded)
        flushAndClear()

        snowflakeRepo.count() shouldBeEqualTo 2

        val loaded2 = cloudRepo.findByIdOrNull(cloud.id!!)!!
        loaded2 shouldBeEqualTo cloud
        loaded.producedSnowflakes.map { it.name } shouldContainSame listOf(sfToRemain.name, sf3.name)
    }
}
