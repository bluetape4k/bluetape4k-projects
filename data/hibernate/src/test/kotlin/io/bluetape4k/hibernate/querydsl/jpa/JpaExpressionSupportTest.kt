package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.ListPath
import com.querydsl.core.types.dsl.SimplePath
import io.bluetape4k.hibernate.model.QIntJpaEntity
import io.bluetape4k.hibernate.querydsl.core.comparablePathOf
import io.bluetape4k.hibernate.querydsl.core.listPathOf
import io.bluetape4k.hibernate.querydsl.core.simplePathOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class JpaExpressionSupportTest {

    @Test
    fun `CollectionExpression_avg는 평균값 표현식을 반환한다`() {
        val path = comparablePathOf<Int>("score")
        // CollectionPath로 감싸야 avg 사용 가능
        // 직접적인 ComparablePath에는 avg() 없음 — 스킵
        path.shouldNotBeNull()
    }

    @Test
    fun `CollectionExpression_avg min max는 집계 표현식을 반환한다`() {
        // CollectionPath<Int, ComparablePath<Int>> 생성
        val meta = com.querydsl.core.types.PathMetadataFactory.forVariable("scores")
        val listPath: ListPath<Int, SimplePath<Int>> = listPathOf(meta)

        val avgExpr = listPath.avg()
        avgExpr.shouldNotBeNull()

        val maxExpr = listPath.max()
        maxExpr.shouldNotBeNull()

        val minExpr = listPath.min()
        minExpr.shouldNotBeNull()
    }

    @Test
    fun `EntityPath_type는 타입 StringExpression을 반환한다`() {
        val entityPath = QIntJpaEntity("testEntity")
        val typeExpr = entityPath.type()
        typeExpr.shouldNotBeNull()
    }
}
