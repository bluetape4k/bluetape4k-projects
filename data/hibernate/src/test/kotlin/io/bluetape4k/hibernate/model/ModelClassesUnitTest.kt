package io.bluetape4k.hibernate.model

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ModelClassesUnitTest {

    // TreeNodePosition - data class
    @Test
    fun `TreeNodePosition 기본값으로 생성된다`() {
        val pos = TreeNodePosition()
        pos.nodeLevel shouldBeEqualTo 0
        pos.nodeOrder shouldBeEqualTo 0
    }

    @Test
    fun `TreeNodePosition 값을 지정하여 생성된다`() {
        val pos = TreeNodePosition(nodeLevel = 3, nodeOrder = 5)
        pos.nodeLevel shouldBeEqualTo 3
        pos.nodeOrder shouldBeEqualTo 5
    }

    @Test
    fun `TreeNodePosition equals와 hashCode가 동작한다`() {
        val pos1 = TreeNodePosition(1, 2)
        val pos2 = TreeNodePosition(1, 2)
        (pos1 == pos2).shouldBeTrue()
        pos1.hashCode() shouldBeEqualTo pos2.hashCode()
    }

    // JpaTreeEntity - interface with default methods
    @Test
    fun `JpaTreeEntity addChildren은 부모를 설정한다`() {
        val parent = ConcreteTreeNode("parent")
        val child = ConcreteTreeNode("child")
        parent.addChildren(child)

        parent.children.size shouldBeEqualTo 1
        child.parent shouldBeEqualTo parent
    }

    @Test
    fun `JpaTreeEntity removeChildren은 부모를 null로 설정한다`() {
        val parent = ConcreteTreeNode("parent")
        val child = ConcreteTreeNode("child")
        parent.addChildren(child)
        parent.removeChildren(child)

        parent.children.size shouldBeEqualTo 0
        child.parent.shouldBeNull()
    }

    @Test
    fun `JpaTreeEntity addChildren은 중복을 추가하지 않는다`() {
        val parent = ConcreteTreeNode("parent")
        val child = ConcreteTreeNode("child")
        parent.addChildren(child)
        parent.addChildren(child) // 중복 추가

        parent.children.size shouldBeEqualTo 1
    }

    // JpaLocalizedEntity - interface with default methods
    @Test
    fun `JpaLocalizedEntity getLocalizedValue는 기본값을 반환한다`() {
        val entity = ConcreteLocalizedEntity()
        val value = entity.getLocalizedValue(java.util.Locale.ENGLISH)
        value.shouldNotBeNull()
    }

    @Test
    fun `JpaLocalizedEntity getCurrentLocalizedValue는 현재 locale 값을 반환한다`() {
        val entity = ConcreteLocalizedEntity()
        val value = entity.getCurrentLocalizedValue()
        value.shouldNotBeNull()
    }

    @Test
    fun `JpaLocalizedEntity getLocalizedValueOrDefault는 localeMap에 값이 있으면 반환한다`() {
        val entity = ConcreteLocalizedEntity()
        val locale = java.util.Locale.ENGLISH
        val expected = ConcreteLocalizedEntity.Value("hello")
        entity.localeMap[locale] = expected
        val value = entity.getLocalizedValueOrDefault(locale)
        value shouldBeEqualTo expected
    }

    // UuidJpaEntity - abstract class with default id
    @Test
    fun `UuidJpaEntity는 기본적으로 UUID id를 생성한다`() {
        val entity = ConcreteUuidEntity("uuid-test")
        entity.id.shouldNotBeNull()
    }

    // IntJpaEntity - abstract class
    @Test
    fun `IntJpaEntity 구현체는 Int id를 가진다`() {
        val entity = ConcreteIntEntity("int-test")
        entity.id.shouldBeNull() // 영속화 전에는 null
    }

    // IntJpaTreeEntity - abstract class with tree support
    @Test
    fun `IntJpaTreeEntity addChildren은 부모를 설정한다`() {
        val parent = ConcreteIntTreeNode("int-parent")
        val child = ConcreteIntTreeNode("int-child")
        parent.addChildren(child)
        parent.children.size shouldBeEqualTo 1
        child.parent shouldBeEqualTo parent
    }

    // AbstractJpaTreeEntity - direct subclass
    @Test
    fun `AbstractJpaTreeEntity subclass의 parent는 null로 초기화된다`() {
        val node = DirectTreeNode("direct-node")
        node.parent.shouldBeNull()
        node.children.size shouldBeEqualTo 0
    }

    // AbstractPersistenceObject - abstract
    @Test
    fun `AbstractPersistenceObject isPersisted는 false이다`() {
        val obj = ConcretePersistenceObject()
        obj.isPersisted.shouldBeFalse()
    }
}

// Concrete implementations for testing

private class ConcreteTreeNode(val name: String) : LongJpaTreeEntity<ConcreteTreeNode>() {
    override fun equalProperties(other: Any) = other is ConcreteTreeNode && name == other.name
    override fun hashCode(): Int = name.hashCode()
}

private class ConcreteLocalizedEntity : JpaLocalizedEntity<ConcreteLocalizedEntity.Value> {
    data class Value(val text: String = "default") : JpaLocalizedEntity.LocalizedValue
    override val localeMap: MutableMap<java.util.Locale, Value> = mutableMapOf()
    override val isPersisted: Boolean = false
    override fun createDefaultLocalizedValue() = Value()
}

private class ConcreteUuidEntity(val name: String) : UuidJpaEntity() {
    override fun equalProperties(other: Any) = other is ConcreteUuidEntity && name == other.name
    override fun hashCode(): Int = name.hashCode()
}

private class ConcretePersistenceObject : AbstractPersistenceObject() {
    override fun equalProperties(other: Any) = other is ConcretePersistenceObject
    override fun hashCode(): Int = 42
}

private class ConcreteIntEntity(val name: String) : IntJpaEntity() {
    override fun equalProperties(other: Any) = other is ConcreteIntEntity && name == other.name
    override fun hashCode(): Int = name.hashCode()
}

private class ConcreteIntTreeNode(val name: String) : IntJpaTreeEntity<ConcreteIntTreeNode>() {
    override fun equalProperties(other: Any) = other is ConcreteIntTreeNode && name == other.name
    override fun hashCode(): Int = name.hashCode()
}

private class DirectTreeNode(val name: String) : AbstractJpaTreeEntity<DirectTreeNode, Long>() {
    override var id: Long? = null
    override fun equalProperties(other: Any) = other is DirectTreeNode && name == other.name
    override fun hashCode(): Int = name.hashCode()
}
