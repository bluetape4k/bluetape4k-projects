package io.bluetape4k.hibernate.querydsl.simple

import com.querydsl.core.types.Projections
import com.querydsl.jpa.HQLTemplates
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.querydsl.core.inValues
import io.bluetape4k.hibernate.querydsl.core.stringExpressionOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SimpleQuerydslExamples: AbstractHibernateTest() {

    companion object: KLogging()

    @BeforeEach
    fun setup() {
        val parent1 = ExampleEntity("example-1")
        val child1 = ExampleEntity("child-1").apply { parent = parent1 }
        parent1.children.add(child1)
        val parent2 = ExampleEntity("example-2")

        listOf(parent1, parent2).forEach { tem.persist(it) }
        flushAndClear()
    }

    @Test
    fun `basic meta model expression`() {
        val self = QExampleEntity.exampleEntity
        val child = QExampleEntity("child")

        val query = JPAQuery<ExampleEntity>(em)
            .from(self)
            .innerJoin(self.children, child)
            .select(self.name, child.name)
            .where(self.name.eq("example-1"))

        log.debug { "query=$query" }

        val results = query.fetch()
        results.shouldNotBeEmpty()
        results shouldHaveSize 1
        val tuple = results.first()
        tuple.get(0, String::class.java) shouldBeEqualTo "example-1"
        tuple.get(1, String::class.java) shouldBeEqualTo "child-1"
    }

    @Test
    fun `using JPAQueryFactory`() {
        val queryFactory = JPAQueryFactory(HQLTemplates.DEFAULT, em)
        val self = QExampleEntity.exampleEntity
        val child = QExampleEntity("child")

        val query = queryFactory.selectFrom(self)
            .innerJoin(self.children, child)
            .select(self.name, child.name)
            .where(self.name.eq(stringExpressionOf("example-1")))

        log.debug { "query=$query" }

        val results = query.fetch()
        results.shouldNotBeEmpty()
        results shouldHaveSize 1
        val tuple = results.first()
        tuple.get(0, String::class.java) shouldBeEqualTo "example-1"
        tuple.get(1, String::class.java) shouldBeEqualTo "child-1"
    }

    @Test
    fun `using covering index`() {
        val queryFactory = JPAQueryFactory(HQLTemplates.DEFAULT, em)

        val self = QExampleEntity.exampleEntity
        val sub = QExampleEntity("sub")

        val subQuery = queryFactory
            .select(sub.id)
            .from(sub)
            .where(sub.name.like("example%"))

        val query = queryFactory
            .select(self.name)
            .from(self)
            .where(self.id.inValues(subQuery))

        log.debug { "query=$query" }

        val results = query.fetch()
        results shouldHaveSize 2
        results.toSet() shouldBeEqualTo setOf("example-1", "example-2")
    }

    @Test
    fun `projections by constructor`() {
        val queryFactory = JPAQueryFactory(HQLTemplates.DEFAULT, em)
        val example = QExampleEntity.exampleEntity

        val constructor = Projections.constructor(ExampleDto::class.java, example.id, example.name)
        val dtos = queryFactory.select(constructor)
            .from(example)
            .fetch()

        dtos.forEach {
            log.trace { it }
        }
        dtos shouldHaveSize 3
    }

    @Test
    fun `use QueryProjection annotation`() {
        val queryFactory = JPAQueryFactory(HQLTemplates.DEFAULT, em)
        val example = QExampleEntity.exampleEntity

        val dtos = queryFactory
            .select(QExampleDto(example.id, example.name))
            .from(example)
            .fetch()

        dtos.forEach {
            log.trace { it }
        }
        dtos shouldHaveSize 3
    }
}
