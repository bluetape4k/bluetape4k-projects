package io.bluetape4k.hibernate.mapping.tree

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class TreeNodeTest(
    @param:Autowired private val repository: TreeNodeRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `build tree nodes`() {
        val root = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")

        root.addChildren(child1, child2)

        val child11 = TreeNode("child11")
        val child12 = TreeNode("child12")
        child1.addChildren(child11, child12)

        // cascade 를 이용하여 children도 모두 저장되어야 한다
        repository.save(root)
        flushAndClear()

        val loaded = repository.findByIdOrNull(child1.id!!)

        loaded.shouldNotBeNull()
        loaded shouldBeEqualTo child1
        loaded.parent shouldBeEqualTo root
        loaded.children shouldBeEqualTo setOf(child11, child12)

        // 모든 root 를 조회한다
        val roots = repository.findRoots()
        roots shouldBeEqualTo listOf(root)

        // child1 삭제
        repository.delete(loaded)
        flushAndClear()

        val rootNode = repository.findRoots().single()
        rootNode.children shouldBeEqualTo setOf(child2)

        // child1 삭제 시 자손은 모두 삭제된다.
        repository.existsById(child11.id!!).shouldBeFalse()

        // child2는 자손이 없었다
        repository.existsById(child12.id!!).shouldBeFalse()
    }
}
