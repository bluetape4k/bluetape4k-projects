package io.bluetape4k.examples.cassandra.convert

import com.datastax.oss.driver.api.core.cql.Row
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.examples.cassandra.AbstractReactiveCassandraTestConfiguration
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.jackson.writeAsString
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions
import java.util.*

@EntityScan(basePackageClasses = [Addressbook::class])
class ConversionTestConfiguration: AbstractReactiveCassandraTestConfiguration() {

    @Bean
    override fun customConversions(): CassandraCustomConversions {
        val converters: MutableList<Converter<*, *>> = fastListOf(
            ContactWriteConverter(),
            ContactReadConverter(),
            CustomAddressbookReadConverter(),
            CurrencyToStringConverter.INSTANCE,
            StringToCurrencyConverter.INSTANCE
        )

        return CassandraCustomConversions(converters)
    }

    // @WritingConverter, @ReadingConverter 를 지정하면 따로 등록할 필요가 없습니다.
    // @WritingConverter
    class ContactWriteConverter: Converter<Contact, String> {
        companion object {
            private val mapper = Jackson.defaultJsonMapper
        }

        override fun convert(source: Contact): String? {
            return mapper.writeAsString(source)
        }
    }

    // @WritingConverter, @ReadingConverter 를 지정하면 따로 등록할 필요가 없습니다.
    // @ReadingConverter
    class ContactReadConverter: Converter<String, Contact> {
        companion object {
            private val mapper = Jackson.defaultJsonMapper
        }

        override fun convert(source: String): Contact? {
            return mapper.readValueOrNull<Contact>(source)
        }
    }

    class CustomAddressbookReadConverter: Converter<Row, CustomAddressbook> {
        override fun convert(source: Row): CustomAddressbook {
            return CustomAddressbook(source.getString("id"), source.getString("me"))
        }
    }

    enum class StringToCurrencyConverter: Converter<String, Currency> {
        INSTANCE {
            override fun convert(source: String): Currency? {
                return runCatching { Currency.getInstance(source) }.getOrNull()
            }
        }
    }

    enum class CurrencyToStringConverter: Converter<Currency, String> {
        INSTANCE {
            override fun convert(source: Currency): String? {
                return source.currencyCode
            }
        }
    }
}
