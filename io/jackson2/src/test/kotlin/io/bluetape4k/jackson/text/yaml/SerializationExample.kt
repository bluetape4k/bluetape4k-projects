package io.bluetape4k.jackson.text.yaml

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.text.DataSource
import io.bluetape4k.jackson.text.Database
import io.bluetape4k.jackson.text.FiveMinuteUser
import io.bluetape4k.jackson.text.Gender
import io.bluetape4k.jackson.text.Name
import io.bluetape4k.jackson.text.Outer
import io.bluetape4k.jackson.text.trimYamlDocMarker
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SerializationExample: AbstractYamlExample() {

    companion object: KLogging()

    @Test
    fun `serialize and deserialize simple POJO`() {
        val input = FiveMinuteUser(
            faker.name().firstName(),
            faker.name().lastName(),
            false,
            Gender.MALE,
            byteArrayOf(1, 2, 3, 4)
        )
        val output = yamlMapper.writeValueAsString(input).trimYamlDocMarker()
        log.debug { "output=$output" }

        val expected = """
            |firstName: "${input.firstName}"
            |lastName: "${input.lastName}"
            |verified: false
            |gender: "MALE"
            |userImage: !!binary |-
            |  AQIDBA==
            """.trimMargin()

        output shouldBeEqualTo expected

        val parsed = yamlMapper.readValue<FiveMinuteUser>(output)
        parsed shouldBeEqualTo input
    }

    @Test
    fun `serialize nested POJO`() {
        val input = Outer(Name("Sunghyouk", "Bae"), 54)
        val output = yamlMapper.writeValueAsString(input).trimYamlDocMarker()

        log.debug { "output=\n$output" }

        val expected = """
            |name:
            |  first: "Sunghyouk"
            |  last: "Bae"
            |age: 54
            """.trimMargin()

        output shouldBeEqualTo expected

        val parsed = yamlMapper.readValue<Outer>(expected)
        parsed shouldBeEqualTo input
    }

    @Test
    fun `serialize deserialize dataSource`() {
        val input = Database(
            DataSource(
                "org.h2.Driver", "jdbc:h2:mem:test", "sa", "",
                setOf(
                    "cachePrepStmts=true",
                    "prepStmtCacheSize=250",
                    "prepStmtCacheSqlLimit=2048",
                    "useServerPrepStmts=true"
                )
            )
        )

        val output = yamlMapper.writeValueAsString(input).trimYamlDocMarker()

        log.debug { "output=\n$output" }

        val expected = """
            |dataSource:
            |  driverClass: "org.h2.Driver"
            |  url: "jdbc:h2:mem:test"
            |  username: "sa"
            |  password: ""
            |  properties:
            |  - "cachePrepStmts=true"
            |  - "prepStmtCacheSize=250"
            |  - "prepStmtCacheSqlLimit=2048"
            |  - "useServerPrepStmts=true"
            """.trimMargin()

        output shouldBeEqualTo expected

        val parsed = yamlMapper.readValue<Database>(output)
        parsed shouldBeEqualTo input
    }
}
