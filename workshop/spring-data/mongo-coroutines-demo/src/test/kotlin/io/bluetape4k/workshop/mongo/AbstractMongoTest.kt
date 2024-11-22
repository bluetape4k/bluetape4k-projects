package io.bluetape4k.workshop.mongo

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.mongo.domain.Person
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
abstract class AbstractMongoTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        fun newPerson(): Person =
            Person(
                faker.name().firstName(),
                faker.name().lastName(),
                faker.random().nextInt(10, 80)
            )
    }

}
