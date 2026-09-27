package io.bluetape4k.hibernate.querydsl.codegen

import com.querydsl.jpa.impl.JPAQuery
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.mapping.associations.join.AddressEntity
import io.bluetape4k.hibernate.mapping.associations.join.JoinUser
import io.bluetape4k.hibernate.mapping.associations.join.JoinUserRepository
import io.bluetape4k.hibernate.mapping.associations.join.QAddressEntity
import io.bluetape4k.hibernate.mapping.associations.join.QJoinUser
import io.bluetape4k.hibernate.mapping.tree.QTreeNode
import io.bluetape4k.hibernate.mapping.tree.TreeNode
import io.bluetape4k.hibernate.mapping.tree.TreeNodeRepository
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path

class QuerydslCodegenCompatibilityTest(
    @param:Autowired private val userRepository: JoinUserRepository,
    @param:Autowired private val treeNodeRepository: TreeNodeRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `Java APT generates Q types for association and tree fixtures`() {
        val generatedSourceRoot = Path.of("build/generated/source/kapt/test")
        listOf(
            "io/bluetape4k/hibernate/mapping/associations/join/QJoinUser.java",
            "io/bluetape4k/hibernate/mapping/associations/join/QAddressEntity.java",
            "io/bluetape4k/hibernate/mapping/tree/QTreeNode.java",
            "io/bluetape4k/hibernate/querydsl/simple/QExampleEntity.java",
            "io/bluetape4k/hibernate/querydsl/simple/QExampleDto.java",
        ).forEach { relativePath ->
            generatedSourceRoot.resolve(relativePath).toFile().exists().shouldBeTrue()
        }
    }

    @Test
    fun `generated tree Q type resolves self-reference query`() {
        val rootTitle = "querydsl-tree-root"
        val childTitle = "querydsl-tree-child"
        val root = TreeNode(rootTitle)
        root.addChildren(TreeNode(childTitle))
        treeNodeRepository.saveAndFlush(root)
        flushAndClear()

        val node = QTreeNode.treeNode
        val parent = QTreeNode("parent")
        val result = JPAQuery<TreeNode>(em)
            .select(node)
            .from(node)
            .innerJoin(node.parent(), parent)
            .where(
                node.title.eq(childTitle),
                parent.title.eq(rootTitle),
            )
            .fetch()

        result shouldHaveSize 1
        result.first().title shouldBeEqualTo childTitle
        result.first().parent.shouldNotBeNull().title shouldBeEqualTo rootTitle
    }

    @Test
    fun `generated Q paths resolve repository entity and association query`() {

        val userName = "querydsl-user"

        val saved = userRepository.saveAndFlush(
            JoinUser(userName).apply {
                addresses["home"] = AddressEntity(
                    street = "Main Street",
                    city = "Seoul",
                    zipcode = "04524"
                )
            }
        )
        flushAndClear()

        val user = QJoinUser.joinUser
        val address = QAddressEntity("address")
        val result = JPAQuery<JoinUser>(em)
            .select(user)
            .from(user)
            .innerJoin(user.addresses, address)
            .where(
                user.name.eq(userName),
                address.city.eq("Seoul")
            )
            .fetch()

        result shouldHaveSize 1
        result.first().name shouldBeEqualTo userName

        val loaded = userRepository.findById(saved.id!!).orElseThrow().shouldNotBeNull()
        loaded.name shouldBeEqualTo userName
    }
}
