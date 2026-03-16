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
 * ## 동작/계약
 * - ASN/City/Country DB 리더는 lazy 초기화 후 동일 인스턴스를 재사용합니다.
 * - 데이터베이스 리소스 파일이 없으면 생성 시점에 예외가 발생합니다.
 * - 생성된 InputStream은 [ShutdownQueue]에 등록되어 JVM 종료 시 정리됩니다.
 *
 * ```kotlin
 * val cityDb = Geoip.cityDatabase
 * // cityDb != null
 * ```
 *
 * - [GeoIP2 Java API](https://maxmind.github.io/GeoIP2-java/)
 * - [Download GeoIP Databases](https://www.maxmind.com/en/accounts/379741/geoip/downloads)
 */
object Geoip : KLogging() {
    private const val GEO_ASN_DB = "GeoLite2-ASN.mmdb"
    private const val GEO_CITY_DB = "GeoLite2-City.mmdb"
    private const val GEO_COUNTRY_DB = "GeoLite2-Country.mmdb"

    /** ASN 데이터베이스 리더입니다. */
    @JvmStatic
    val asnDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_ASN_DB)
    }

    /** City 데이터베이스 리더입니다. */
    @JvmStatic
    val cityDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_CITY_DB)
    }

    /** Country 데이터베이스 리더입니다. */
    @JvmStatic
    val countryDatabase: DatabaseReader by lazy {
        createDatabaseReader(GEO_COUNTRY_DB)
    }

    private fun createDatabaseReader(
        filename: String,
        locales: List<String> = listOf("en", "ko"),
    ): DatabaseReader {
        log.info { "Load $filename ..." }
        val inputStream = Resourcex.getInputStream(filename).requireNotNull("inputStream")
        return databaseReader(inputStream) {
            locales(locales)
            withCache(CHMCache())

            // 혹시 몰라서 InputStream을 닫도록 한다
            ShutdownQueue.register(inputStream)
        }
    }

    private inline fun databaseReader(
        inputStream: InputStream,
        @BuilderInference builder: DatabaseReader.Builder.() -> Unit,
    ): DatabaseReader = DatabaseReader.Builder(inputStream).apply(builder).build()
}
