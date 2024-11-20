package io.bluetape4k.geoip2

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.utils.Resourcex
import io.bluetape4k.utils.ShutdownQueue
import java.io.InputStream

/**
 * Maxmind GeoIP2 Database 를 제공합니다.
 *
 * - [GeoIP2 Java API](https://maxmind.github.io/GeoIP2-java/)
 * - [Download GeoIP Databases](https://www.maxmind.com/en/accounts/379741/geoip/downloads)
 */
object Geoip: KLogging() {

    private const val GEO_ASN_DB = "GeoLite2-ASN.mmdb"
    private const val GEO_CITY_DB = "GeoLite2-City.mmdb"
    private const val GEO_COUNTRY_DB = "GeoLite2-Country.mmdb"

    /**
     * ASN Database
     */
    @JvmStatic
    val asnDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_ASN_DB)
    }

    /**
     * City Database
     */
    @JvmStatic
    val cityDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_CITY_DB)
    }

    /**
     * Country Database
     */
    @JvmStatic
    val countryDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_COUNTRY_DB)
    }

    private fun createDatabaseReader(
        filename: String,
        locales: List<String> = listOf("en", "ko"),
    ): DatabaseReader {
        log.info { "Load $filename ..." }
        val inputStream = Resourcex.getInputStream(filename)!!
        inputStream.requireNotNull("inputStream")
        return databaseReader(inputStream) {
            locales(locales)
            withCache(CHMCache())

            // 혹시 몰라서 InputStream을 닫도록 한다
            ShutdownQueue.register(inputStream)
        }
    }

    private inline fun databaseReader(
        inputStream: InputStream,
        initializer: DatabaseReader.Builder.() -> Unit,
    ): DatabaseReader {
        return DatabaseReader.Builder(inputStream).apply(initializer).build()
    }
}
