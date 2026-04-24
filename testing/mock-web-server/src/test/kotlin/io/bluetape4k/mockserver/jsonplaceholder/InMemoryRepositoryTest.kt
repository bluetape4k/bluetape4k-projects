package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [InMemoryRepository]에 대한 단위 테스트.
 *
 * CRUD 동작, ID 시퀀스, loadAll 교체 동작을 검증한다.
 */
class InMemoryRepositoryTest {

    companion object : KLogging()

    data class Item(val id: Long = 0L, val name: String = "")

    private lateinit var repo: InMemoryRepository<Item>

    @BeforeEach
    fun setup() {
        repo = InMemoryRepository(
            idExtractor = { it.id },
            withId = { item, id -> item.copy(id = id) }
        )
    }

    @Test
    fun `add assigns new incremental id`() {
        val first = repo.add(Item(name = "first"))
        val second = repo.add(Item(name = "second"))

        first.id shouldBeEqualTo 1L
        second.id shouldBeEqualTo 2L
        repo.count() shouldBeEqualTo 2
    }

    @Test
    fun `find returns item by id`() {
        val saved = repo.add(Item(name = "target"))
        val found = repo.find(saved.id)

        found.shouldNotBeNull()
        found.name shouldBeEqualTo "target"
    }

    @Test
    fun `find returns null for non-existent id`() {
        repo.find(999L).shouldBeNull()
    }

    @Test
    fun `all returns all stored items`() {
        repo.add(Item(name = "a"))
        repo.add(Item(name = "b"))
        repo.add(Item(name = "c"))

        repo.all().size shouldBeEqualTo 3
    }

    @Test
    fun `update replaces existing item`() {
        val saved = repo.add(Item(name = "original"))
        val updated = repo.update(saved.id, Item(name = "modified"))

        updated.shouldNotBeNull()
        updated.id shouldBeEqualTo saved.id
        updated.name shouldBeEqualTo "modified"

        // 저장소에서도 변경 확인
        repo.find(saved.id)!!.name shouldBeEqualTo "modified"
    }

    @Test
    fun `update returns null for non-existent id`() {
        repo.update(999L, Item(name = "ghost")).shouldBeNull()
    }

    @Test
    fun `delete removes item and returns true`() {
        val saved = repo.add(Item(name = "to-delete"))
        repo.delete(saved.id).shouldBeTrue()
        repo.find(saved.id).shouldBeNull()
        repo.count() shouldBeEqualTo 0
    }

    @Test
    fun `delete returns false for non-existent id`() {
        repo.delete(999L).shouldBeFalse()
    }

    @Test
    fun `loadAll replaces all data and resets sequence`() {
        repo.add(Item(name = "old1"))
        repo.add(Item(name = "old2"))

        val newItems = listOf(
            Item(id = 10L, name = "new10"),
            Item(id = 20L, name = "new20"),
        )
        repo.loadAll(newItems)

        repo.count() shouldBeEqualTo 2
        repo.find(10L).shouldNotBeNull()
        repo.find(20L).shouldNotBeNull()
        repo.find(1L).shouldBeNull() // old items gone

        // 새 add는 max(10,20)+1 = 21부터 시작
        val added = repo.add(Item(name = "after-reload"))
        added.id shouldBeEqualTo 21L
    }

    @Test
    fun `loadAll with empty list clears all data`() {
        repo.add(Item(name = "a"))
        repo.add(Item(name = "b"))

        repo.loadAll(emptyList())

        repo.count() shouldBeEqualTo 0
        repo.all().size shouldBeEqualTo 0
    }
}
