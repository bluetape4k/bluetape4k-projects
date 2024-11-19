package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging

class TsvRecordReaderTest: AbstractRecordReaderTest() {

    companion object: KLogging()

    override val reader: RecordReader = TsvRecordReader()

    override val productTypePath: String = "csv/product_type.tsv"
    override val extraWordsPath: String = "csv/extra_words.tsv"
}
