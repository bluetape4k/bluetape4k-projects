package io.bluetape4k.csv.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendTsvRecordReaderTest: AbstractSuspendRecordReaderTest() {

    companion object: KLoggingChannel()

    override val reader = SuspendTsvRecordReader()

    override val productTypePath: String = "csv/product_type.tsv"
    override val extraWordsPath: String = "csv/extra_words.tsv"

}
