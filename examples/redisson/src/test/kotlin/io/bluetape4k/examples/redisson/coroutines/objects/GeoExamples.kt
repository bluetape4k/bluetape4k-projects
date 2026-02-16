package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RGeo
import org.redisson.api.geo.GeoEntry
import org.redisson.api.geo.GeoPosition
import org.redisson.api.geo.GeoSearchArgs
import org.redisson.api.geo.GeoUnit


/**
 * [RGeo] examples
 *
 * Java implementation of Redis based RGeo object is a holder for geospatial items.
 *
 * 참고: [Geospatial Holder](https://github.com/redisson/redisson/wiki/6.-distributed-objects/#63-geospatial-holder)
 */
class GeoExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `RGeo example`() = runSuspendIO {

        val geo: RGeo<String> = redisson.getGeo(randomName())

        val palermo = GeoEntry(13.361389, 38.115556, "Palermo")
        val catania = GeoEntry(15.087269, 37.502669, "Catania")

        geo.addAsync(palermo, catania).awaitSuspending() shouldBeEqualTo 2L

        val dist = geo.distAsync("Palermo", "Catania", GeoUnit.METERS).awaitSuspending()
        val pos = geo.posAsync("Palermo", "Catania").awaitSuspending()

        log.debug { "distance=$dist, pos=$pos" }

        // 중심점으로부터 반경 200 km 내의 도시 찾기
        val fromLocation = GeoSearchArgs.from(15.0, 37.0).radius(200.0, GeoUnit.KILOMETERS)
        val cities = geo.searchAsync(fromLocation).awaitSuspending()
        cities shouldBeEqualTo listOf("Palermo", "Catania")

        // Palermo 시를 중심으로 반경 10 km 내의 도시 찾기
        val fromPalermo = GeoSearchArgs.from("Palermo").radius(10.0, GeoUnit.KILOMETERS)
        val allNearCities = geo.searchAsync(fromPalermo).awaitSuspending()
        allNearCities shouldBeEqualTo listOf("Palermo")

        val searchArgsFromLocation = GeoSearchArgs.from(15.0, 37.0).radius(200.0, GeoUnit.KILOMETERS)
        val searchArgsFromPalermo = GeoSearchArgs.from("Palermo").radius(200.0, GeoUnit.KILOMETERS)

        val citiesWithDistance: MutableMap<String, Double> =
            geo.searchWithDistanceAsync(searchArgsFromLocation).awaitSuspending()
        citiesWithDistance.forEach { (city, distance) ->
            log.debug { "city=$city, distance from (15.0, 37.0)=$distance km" }
        }

        val allNearCitiesDistance: MutableMap<String, Double> =
            geo.searchWithDistanceAsync(searchArgsFromPalermo).awaitSuspending()
        allNearCitiesDistance.forEach { (city, distance) ->
            log.debug { "city=$city, distance from Palermo=$distance km" }
        }

        val citiesWithPosition: MutableMap<String, GeoPosition> =
            geo.searchWithPositionAsync(searchArgsFromLocation).awaitSuspending()
        citiesWithPosition.forEach { (city, position) ->
            log.debug { "city=$city, position=$position" }
        }


        val allNearCitiesPosition: MutableMap<String, GeoPosition> =
            geo.searchWithPositionAsync(searchArgsFromPalermo).awaitSuspending()
        allNearCitiesPosition.forEach { (city, position) ->
            log.debug { "city=$city, position=$position" }
        }

        geo.deleteAsync().awaitSuspending().shouldBeTrue()
    }
}
