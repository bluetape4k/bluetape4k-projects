package io.bluetape4k.csv.coroutines

import io.bluetape4k.logging.KLogging

class CoCsvRecordReaderTest: AbstractCoRecordReaderTest() {

    companion object: KLogging()

    override val reader = CoCsvRecordReader()

    override val productTypePath: String = "csv/product_type.csv"
    override val extraWordsPath: String = "csv/extra_words.csv"

}
