package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.eclipse.collections.api.multimap.Multimap
import org.junit.jupiter.api.Test
import java.io.Serializable

class MultimapSupportTest: AbstractCollectionTest() {

    companion object: KLogging() {
        private val ones = fastListOf("1", "one", "하나")
        private val twos = fastListOf("2", "둘")
    }

    data class User(val name: String, val age: Int): Serializable

    private fun Multimap<Int, String>.verify() {
        size() shouldBeEqualTo 5
        keysView().size() shouldBeEqualTo 2
        this[1] shouldContainSame ones
        this[2] shouldContainSame twos
    }

    @Test
    fun `List Multimap 생성`() {
        val mmap = listMultimapOf<Int, String>()
            .apply {
                putAll(1, ones)
                putAll(2, twos)
            }
        mmap.verify()

        // 새로운 요소 추가
        mmap.put(3, "3")
        mmap.put(3, "셋")
        mmap[3] shouldBeEqualTo listOf("3", "셋")
    }

    @Test
    fun `List 을 List Multimap 로 변환`() {
        val map = mapOf(1 to ones, 2 to twos)
            .flatMap { (k, vs) ->
                vs.map { k to it }
            }

        val mmap = map.toListMultimap()
        mmap.verify()

        val mmap2 = map.toListMultimap { it }
        mmap2.verify()
    }

    @Test
    fun `Set Multimap 생성`() {
        val smap = setMultimapOf<Int, String>()
            .apply {
                putAll(1, ones)
                putAll(2, twos)
            }
        smap.verify()

        // 중복되는 요소는 추가되지 않는다.

        smap.put(2, "둘")
        smap.verify()

        // 새로운 요소 추가
        smap.put(3, "3")
        smap.put(3, "3")
        smap.put(3, "셋")
        smap[3] shouldBeEqualTo setOf("3", "셋")
    }

    @Test
    fun `Map 을 Set Multimap 으로 변환`() {
        val map = mapOf(1 to ones, 2 to twos)
            .flatMap { (k, vs) ->
                vs.map { k to it }
            }

        val smap = map.toSetMultimap()
        smap.verify()
        smap.put(1, "하나")  // 중복된 요소는 추가되지 않음
        smap.verify()

        val smap2 = map.toSetMultimap { it }
        smap2.verify()
    }
}
