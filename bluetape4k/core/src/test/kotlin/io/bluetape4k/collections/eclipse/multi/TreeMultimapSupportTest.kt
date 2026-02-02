package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.Serializable

class TreeMultimapSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    data class User(
        val name: String,
        val age: Int,
    ): Comparable<User>, Serializable {
        override fun compareTo(other: User): Int = name.compareTo(other.name)
    }

    private val users = fastListOf(
        User("alex", 11),
        User("bob", 22),
        User("sam", 22),
        User("jane", 11),
        User("rex", 44),
    )

    @Test
    fun `Key로 정렬한 TreeMultimap 생성`() {
        val userGroup: TreeMultimap<Int, User> = users.toTreeMultimap { it.age }

        userGroup.valuesView().forEach {
            log.debug { "User: $it" }
        }

        userGroup.keysView().size() shouldBeEqualTo 3
        userGroup.valuesView().size() shouldBeEqualTo users.size
        userGroup[11].map { it.name } shouldBeEqualTo listOf("alex", "jane")
        userGroup[22].map { it.name } shouldBeEqualTo listOf("bob", "sam")
        userGroup[44].map { it.name } shouldBeEqualTo listOf("rex")
    }

    @Test
    fun `Key 로 역정렬한 TreeMultimap 생성`() {
        val comparator = Comparator.reverseOrder<User>()
        val userGroup = users.toTreeMultimap(comparator) { it.age }

        userGroup.keysView().size() shouldBeEqualTo 3
        userGroup.valuesView().size() shouldBeEqualTo users.size
        userGroup[11].map { it.name } shouldBeEqualTo listOf("jane", "alex")
        userGroup[22].map { it.name } shouldBeEqualTo listOf("sam", "bob")
        userGroup[44].map { it.name } shouldBeEqualTo listOf("rex")
    }
}
