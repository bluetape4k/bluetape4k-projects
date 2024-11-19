package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx.SelfNode
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx.buildTrie
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class KoreanPosTest: TestBase() {

    companion object {
        val NOUN_TRIE = KoreanPosTrie(Noun, emptyList(), ending = Noun)
        val NOUN_SELF_NODE = KoreanPosTrie(Noun, listOf(SelfNode), ending = Noun)
        val SUFFIX_NOUN = KoreanPosTrie(Suffix, emptyList(), ending = Noun)
    }

    @Test
    fun `should build Trie for initial optionals with final non-optionals`() {
        // 0 -> 1
        var actual = buildTrie("m0N1", Noun)
        var expected = listOf(
            KoreanPosTrie(Modifier, listOf(NOUN_TRIE), ending = null),
            NOUN_TRIE
        )

        log.debug { "0 -> 1 : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected

        // * -> +
        actual = buildTrie("m*N+", Noun)
        expected = listOf(
            KoreanPosTrie(
                Modifier,
                listOf(SelfNode, NOUN_SELF_NODE),
                ending = null
            ),
            NOUN_SELF_NODE
        )

        log.debug { "* -> + : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected
    }

    @Test
    fun `should build Trie for initial optionals with multiple non-optionals`() {
        // 0 -> 0 -> 1
        val actual = buildTrie("m0N0s1", Noun)
        val expected = listOf(
            KoreanPosTrie(
                Modifier,
                listOf(KoreanPosTrie(Noun, listOf(SUFFIX_NOUN), ending = null), SUFFIX_NOUN),
                ending = null
            ),
            KoreanPosTrie(Noun, listOf(SUFFIX_NOUN), ending = null),
            KoreanPosTrie(Suffix, emptyList(), ending = Noun)
        )

        log.debug { "0 -> 0 -> 1 : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected
    }

    @Test
    fun `should build Trie for initial non-optionals with final non-optionals`() {
        // 1 -> +
        var actual = buildTrie("m1N+", Noun)
        var expected = listOf(KoreanPosTrie(Modifier, listOf(NOUN_SELF_NODE), ending = null))
        log.debug { "1 -> + : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected

        // + -> 1
        actual = buildTrie("N+s1", Noun)
        expected =
            listOf(KoreanPosTrie(Noun, listOf(SelfNode, KoreanPosTrie(Suffix, emptyList(), Noun)), ending = null))

        log.debug { "* -> 1 : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected
    }

    @Test
    fun `should build Trie for initial non-optionals with final optionals`() {
        // 1 -> *
        var actual = buildTrie("m1N*", Noun)
        var expected = listOf(KoreanPosTrie(Modifier, listOf(NOUN_SELF_NODE), ending = Noun))
        log.debug { "1 -> * : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected

        // + -> 0
        actual = buildTrie("N+s0", Noun)
        expected = listOf(KoreanPosTrie(Noun, listOf(SelfNode, SUFFIX_NOUN), ending = Noun))
        log.debug { "+ -> 0 : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected
    }

    @Test
    fun `should build Trie for initial non-optionals with multiple non-optionals`() {
        // + -> + -> 0
        val actual = buildTrie("A+V+A0", Verb)
        val expected = listOf(
            KoreanPosTrie(
                Adverb,
                listOf(
                    SelfNode,
                    KoreanPosTrie(
                        Verb,
                        listOf(SelfNode, KoreanPosTrie(Adverb, emptyList(), Verb)),
                        Verb
                    )
                ),
                ending = null
            )
        )
        log.debug { "+ -> + -> 0 : \nactual=$actual\nexpected=$expected" }
        actual shouldContainSame expected
    }

    @Test
    fun `buildTrie should build Trie correctly for initial optionals`() {
        val expected = listOf(KoreanPosTrie(Adverb, emptyList(), ending = Adverb))
        val actual = buildTrie("A1", Adverb)
        actual shouldContainSame expected
    }

    @Test
    fun `buildTrie should build Trie correctly for initial non-optionals`() {
        val expected = listOf(KoreanPosTrie(Exclamation, listOf(SelfNode), ending = Exclamation))
        val actual = buildTrie("E+", Exclamation)
        actual shouldContainSame expected
    }
}
