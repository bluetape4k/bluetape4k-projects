package io.bluetape4k.hibernate.querydsl.simple

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractJpaTreeEntity
import io.bluetape4k.hibernate.model.LongJpaTreeEntity
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.validation.constraints.NotBlank

/**
 * 트리 구조를 가지는 Self-Referencing Entity
 *
 * 현재 Java APT 경로는 [LongJpaTreeEntity] 상속 구조의 Q 타입을 생성하고
 * `SimpleQuerydslExamples`에서 self-reference query를 실행한다.
 * `querydsl-kotlin-codegen` 후보는 fixture별 원인을 분리하기 전에
 * `KotlinEntitySerializer` 단계의 전역 NPE로 실패하므로 활성화하지 않는다.
 */
@Entity(name = "querydsl_example_entity")
@Access(AccessType.FIELD)
class ExampleEntity: LongJpaTreeEntity<ExampleEntity>() { // AbstractJpaTreeEntity<ExampleEntity, Long>() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): ExampleEntity {
            name.requireNotEmpty("name")
            return ExampleEntity().apply {
                this.name = name
            }
        }
    }

    @get:NotBlank
    var name: String = ""
        protected set

    override fun equalProperties(other: Any): Boolean {
        return other is ExampleEntity && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
