package io.bluetape4k.csv.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel

class CoTsvRecordReaderTest: AbstractCoRecordReaderTest() {

    companion object: KLoggingChannel()

    override val reader = CoTsvRecordReader()

    override val productTypePath: String = "csv/product_type.tsv"
    override val extraWordsPath: String = "csv/extra_words.tsv"

}
