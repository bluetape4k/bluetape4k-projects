package io.bluetape4k.hibernate.mapping.tree

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractJpaTreeEntity
import io.bluetape4k.hibernate.model.LongJpaTreeEntity
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

/**
 * 트리 구조를 가지는 Self-Referencing Entity
 *
 * Java APT 경로는 [LongJpaTreeEntity] 상속 구조의 Q 타입을 생성한다.
 * Kotlin codegen 후보는 fixture별 상속 원인을 확인하기 전에 전역 NPE로 실패해 활성화하지 않는다.
 */
@Entity(name = "tree_treenode")
@Table(indexes = [Index(name = "ix_treenode_parent", columnList = "parent_id")])
@DynamicInsert
@DynamicUpdate
class TreeNode private constructor(
    @field:NotBlank
    var title: String,
): LongJpaTreeEntity<TreeNode>() {

    companion object {
        @JvmStatic
        operator fun invoke(title: String): TreeNode {
            title.requireNotBlank("title")
            return TreeNode(title)
        }
    }

    var description: String? = null

    // equals 계약은 엔티티 동등성 테스트의 대상이며, Q 타입 생성 성공 여부의 원인으로 단정하지 않는다.
    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun equalProperties(other: Any): Boolean {
        return other is TreeNode && title == other.title
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: title.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("title", title)
            .add("description", description)
    }
}
