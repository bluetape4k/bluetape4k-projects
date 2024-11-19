package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging

class CsvRecordReaderTest: AbstractRecordReaderTest() {

    companion object: KLogging()

    override val reader = CsvRecordReader()

    override val productTypePath: String = "csv/product_type.csv"
    override val extraWordsPath: String = "csv/extra_words.csv"

}
