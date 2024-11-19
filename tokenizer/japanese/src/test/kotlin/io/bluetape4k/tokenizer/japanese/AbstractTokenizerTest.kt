package io.bluetape4k.tokenizer.japanese

import com.atilika.kuromoji.ipadic.Tokenizer
import io.bluetape4k.logging.KLogging

abstract class AbstractTokenizerTest {

    companion object: KLogging()

    protected val tokenizer: Tokenizer = Tokenizer.Builder().build()
}
