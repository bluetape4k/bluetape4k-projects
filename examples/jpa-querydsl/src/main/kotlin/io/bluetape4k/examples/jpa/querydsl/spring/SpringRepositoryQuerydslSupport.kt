package io.bluetape4k.examples.jpa.querydsl.spring

import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import io.bluetape4k.support.assertNotNull
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport
import org.springframework.data.jpa.repository.support.Querydsl
import org.springframework.data.querydsl.SimpleEntityPathResolver
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class SpringRepositoryQuerydslSupport(private val entityClass: Class<*>) {

    private var entityManager: EntityManager? = null
    private var querydsl: Querydsl? = null
    private var queryFactory: JPAQueryFactory? = null

    @PersistenceContext
    fun setEntityManager(entityManager: EntityManager) {
        entityManager.assertNotNull("entityManager")

        val entityInfo = JpaEntityInformationSupport.getEntityInformation(entityClass, entityManager)
        val resolver = SimpleEntityPathResolver.INSTANCE
        val path = resolver.createPath(entityInfo.javaType)
        this.entityManager = entityManager
        this.querydsl = Querydsl(entityManager, PathBuilder(path.type, path.metadata))
        this.queryFactory = JPAQueryFactory(entityManager)
    }

    @PostConstruct
    fun assertProperProperty() {
        entityManager.assertNotNull("entityManager")
        querydsl.assertNotNull("querydsl")
        queryFactory.assertNotNull("queryFactory")
    }

    protected fun getEntityManager(): EntityManager = entityManager!!
    protected fun getQuerydsl(): Querydsl = querydsl!!
    protected fun getQueryFactory(): JPAQueryFactory = queryFactory!!

    @Suppress("DEPRECATION")
    protected fun <T> withPaging(pageable: Pageable, queryAction: (JPAQueryFactory) -> JPAQuery<T>): Page<T> {
        val contentQuery = queryAction(getQueryFactory())
        val content = getQuerydsl().applyPagination(pageable, contentQuery).fetch()

        // NOTE: fetchCount 를 쓰지말고 Blaze-Persistence for Querydsl 를 쓰라고 하네요.
        // Blaze Persistence 예제는 quarkus-workshop 에 있습니다.
        // 참고 : https://persistence.blazebit.com/documentation/1.5/core/manual/en_US/index.html#querydsl-integration
        return PageableExecutionUtils.getPage(content, pageable) { contentQuery.fetchCount() }
    }

    @Suppress("DEPRECATION")
    protected fun <T> withPaging(
        pageable: Pageable,
        contentQueryAction: (JPAQueryFactory) -> JPAQuery<T>,
        countQueryAction: (JPAQueryFactory) -> JPAQuery<T>,
    ): Page<T> {
        val contentQuery = contentQueryAction(getQueryFactory())
        val content = getQuerydsl().applyPagination(pageable, contentQuery).fetch()

        val countQuery = countQueryAction(getQueryFactory())

        // NOTE: fetchCount 를 쓰지말고 Blaze-Persistence for Querydsl 를 쓰라고 하네요.
        // Blaze Persistence 예제는 quarkus-workshop 에 있습니다.
        // 참고 : https://persistence.blazebit.com/documentation/1.5/core/manual/en_US/index.html#querydsl-integration
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchCount() }
    }
}
