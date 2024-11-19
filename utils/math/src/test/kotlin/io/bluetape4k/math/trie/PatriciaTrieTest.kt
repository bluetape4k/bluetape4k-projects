package io.bluetape4k.math.trie

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.commons.collections4.trie.PatriciaTrie
import org.junit.jupiter.api.Test

class PatriciaTrieTest {

    @Test
    fun `prefix map`() {
        val trie = PatriciaTrie<String>()

        val keys = listOf(
            "",
            "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
            "Alberts", "Allie", "Alliese", "Alabama", "Banane",
            "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
            "Amma"
        )

        keys.forEach {
            trie[it] = it
        }

        val map = trie.prefixMap("Al")
        map.size shouldBeEqualTo 8
        map.firstKey() shouldBeEqualTo "Alabama"
        map.lastKey() shouldBeEqualTo "Alliese"
        map["Albertoo"] shouldBeEqualTo "Albertoo"

        trie["Xavier"].shouldNotBeNull()
        map["Xavier"].shouldBeNull()
    }

}
