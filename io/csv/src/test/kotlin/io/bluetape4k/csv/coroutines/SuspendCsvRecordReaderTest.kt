package io.bluetape4k.csv.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendCsvRecordReaderTest: AbstractSuspendRecordReaderTest() {

    companion object: KLoggingChannel()

    override val reader = SuspendCsvRecordReader()

    override val productTypePath: String = "csv/product_type.csv"
    override val extraWordsPath: String = "csv/extra_words.csv"

}
