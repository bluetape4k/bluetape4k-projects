package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TreeMultimapSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    data class User(val name: String, val age: Int)

    private val users = fastListOf(
        User("alex", 11),
        User("bob", 22),
        User("sam", 22),
        User("jane", 11),
        User("rex", 44),
    )

    @Test
    fun `Key로 정렬한 TreeMultimap 생성`() {
        val userGroup = users.toTreeMultimap { it.age }

        userGroup.size shouldBeEqualTo 3
        userGroup.valueSize() shouldBeEqualTo users.size
        userGroup.first.map { it.name } shouldBeEqualTo fastListOf("alex", "jane")
        userGroup.last.map { it.name } shouldBeEqualTo fastListOf("rex")
    }

    @Test
    fun `Key 로 역정렬한 TreeMultimap 생성`() {
        val comparator = Comparator.reverseOrder<Int>()
        val userGroup = users.toTreeMultimap(comparator) { it.age }

        userGroup.size shouldBeEqualTo 3
        userGroup.valueSize() shouldBeEqualTo users.size
        userGroup.first.map { it.name } shouldBeEqualTo fastListOf("rex")
        userGroup.last.map { it.name } shouldBeEqualTo fastListOf("alex", "jane")
    }
}
