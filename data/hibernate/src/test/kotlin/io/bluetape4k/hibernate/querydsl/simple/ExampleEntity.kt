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
 * [AbstractJpaTreeEntity]를 직접 상속해서 써야 QueryDSL에서 문제가 생기지 않는다.
 * `LongJpaTreeEntity`에서 상속받게 되면 kapt 작업 시 예외가 발생한다.
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
