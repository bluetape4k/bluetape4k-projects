package io.bluetape4k.geo

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertFalse

class GeoReadmeContractTest {

    @Test
    fun `README examples use current public APIs and consumer coordinates`() {
        readmeTexts().forEach { (filename, text) ->
            forbiddenFragments.forEach { fragment ->
                assertFalse(
                    text.contains(fragment),
                    "$filename must not contain stale or internal reference: $fragment",
                )
            }
        }
    }

    private fun readmeTexts(): List<Pair<String, String>> =
        listOf("README.md", "README.ko.md").map { filename ->
            filename to findReadme(filename).readText()
        }

    private fun findReadme(filename: String): Path {
        val cwd = Path.of("").toAbsolutePath()
        return generateSequence(cwd) { it.parent }
            .flatMap { path ->
                sequenceOf(
                    path.resolve("utils/geo").resolve(filename),
                    path.resolve(filename),
                )
            }
            .firstOrNull(Files::isRegularFile)
            ?: error("Cannot find $filename from $cwd")
    }

    private companion object {
        val forbiddenFragments = listOf(
            "io.bluetape4k.geo.geohash",
            "io.bluetape4k.geo.geocode",
            "io.bluetape4k.geo.geoip2",
            "GoogleGeocoder",
            "GeoIp2Support",
            "Libs.",
        )
    }
}
