package io.bluetape4k.spring.cassandra.convert.converter

import org.springframework.core.convert.converter.Converter
import java.util.*

enum class CurrencyConverter: Converter<Currency, String> {

    INSTANCE;

    override fun convert(source: Currency): String? {
        return source.getDisplayName(Locale.KOREA)
    }
}
