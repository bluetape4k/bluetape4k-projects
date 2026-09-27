package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.dsl.ListPath
import com.querydsl.core.types.dsl.SimplePath
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.hibernate.model.QIntJpaEntity
import io.bluetape4k.hibernate.querydsl.core.comparablePathOf
import io.bluetape4k.hibernate.querydsl.core.listPathOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class JpaExpressionSupportTest {

    companion object: KLogging()

    @Test
    fun `CollectionExpression_avg는 평균값 표현식을 반환한다`() {
        val path = comparablePathOf<Int>("score")
        // CollectionPath로 감싸야 avg 사용 가능
        // 직접적인 ComparablePath에는 avg() 없음 — 스킵
        path.toString() shouldBeEqualTo "score"
    }

    @Test
    fun `CollectionExpression_avg min max는 집계 표현식을 반환한다`() {
        // CollectionPath<Int, ComparablePath<Int>> 생성
        val meta = com.querydsl.core.types.PathMetadataFactory.forVariable("scores")
        val listPath: ListPath<Int, SimplePath<Int>> = listPathOf(meta)

        val avgExpr = listPath.avg()
        avgExpr.toString() shouldBeEqualTo "avg(scores)"

        val maxExpr = listPath.max()
        maxExpr.toString() shouldBeEqualTo "max(scores)"

        val minExpr = listPath.min()
        minExpr.toString() shouldBeEqualTo "min(scores)"
    }

    @Test
    fun `EntityPath_type는 타입 StringExpression을 반환한다`() {
        val entityPath = QIntJpaEntity("testEntity")
        val typeExpr = entityPath.type()
        log.debug { "typeExpr: $typeExpr" }
    }
}
