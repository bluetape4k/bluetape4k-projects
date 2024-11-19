package io.bluetape4k.tokenizer.japanese.examples

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import org.junit.jupiter.api.Test

class KuromojiTokenizerExample: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `Kuromoji Tokenizer Example`() {
        val tokens: MutableList<Token> = tokenizer.tokenize("お寿司が食べたい。")
        tokens.forEach { token ->
            log.debug { "token=${token.surface}: ${token.allFeatures}, ${token.position}" }
        }
    }
}
