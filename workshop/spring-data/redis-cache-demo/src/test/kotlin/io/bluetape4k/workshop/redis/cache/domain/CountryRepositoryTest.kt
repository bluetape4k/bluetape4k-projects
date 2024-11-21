package io.bluetape4k.workshop.redis.cache.domain

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.workshop.redis.cache.AbstractRedisCacheTest
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

@Profile("app")
class CountryRepositoryTest(
    @Autowired private val countryRepo: CountryRepository,
): AbstractRedisCacheTest() {

    companion object: KLogging() {
        internal const val KR = "KR"
        internal const val US = "US"
        internal const val EXPECTED_MILLIS = 400L
    }

    @BeforeEach
    fun beforeEach() {
        countryRepo.evictCache(KR)
    }

    @Test
    fun `get country at first`() {
        countryRepo.evictCache(KR)

        val kr = measureTimeMillis {
            countryRepo.findByCode(KR)
        }
        kr shouldBeGreaterThan EXPECTED_MILLIS

        val kr2 = measureTimeMillis {
            countryRepo.findByCode(KR)
        }
        kr2 shouldBeLessThan EXPECTED_MILLIS

        log.debug { "kr=$kr msec, kr2=$kr2 msec" }
    }

    @Test
    fun `evict cached country`() {
        countryRepo.evictCache(US)

        val us = measureTimeMillis {
            countryRepo.findByCode(US)
        }
        us shouldBeGreaterThan EXPECTED_MILLIS

        val usCached = measureTimeMillis {
            countryRepo.findByCode(US)
        }
        usCached shouldBeLessThan EXPECTED_MILLIS

        countryRepo.evictCache(US)

        val usEvicted = measureTimeMillis {
            countryRepo.findByCode(US)
        }
        usEvicted shouldBeGreaterThan EXPECTED_MILLIS
    }

    @Test
    fun `get random countries`() {
        val codeMap = ConcurrentHashMap<String, Country>()

        measureTimeMillis {
            MultithreadingTester()
                .numThreads(4)
                .roundsPerThread(8)
                .add {
                    val country = retreiveCountry()
                    codeMap[country.code] = country
                }
                .run()
        } shouldBeLessThan 4 * 8 * EXPECTED_MILLIS

        codeMap.size shouldBeLessOrEqualTo CountryRepository.SAMPLE_COUNTRY_CODES.size
    }

    private fun retreiveCountry(): Country {
        val code = CountryRepository.SAMPLE_COUNTRY_CODES.random()
        log.info { "Looking for country with code [$code]" }
        return countryRepo.findByCode(code)
    }
}
