package io.bluetape4k.examples.jpa.querydsl.examples

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import io.bluetape4k.examples.jpa.querydsl.AbstractQuerydslTest
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberDto
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberTeamDto
import io.bluetape4k.examples.jpa.querydsl.domain.dto.MemberVo
import io.bluetape4k.examples.jpa.querydsl.domain.dto.QMemberTeamVo
import io.bluetape4k.examples.jpa.querydsl.domain.dto.QMemberVo
import io.bluetape4k.examples.jpa.querydsl.domain.dto.QTeamVo
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import io.bluetape4k.examples.jpa.querydsl.domain.model.QMember
import io.bluetape4k.examples.jpa.querydsl.domain.model.QTeam
import io.bluetape4k.examples.jpa.querydsl.domain.model.Team
import io.bluetape4k.hibernate.isLoaded
import io.bluetape4k.hibernate.querydsl.core.inValues
import io.bluetape4k.hibernate.querydsl.core.minus
import io.bluetape4k.hibernate.querydsl.core.numberPathOf
import io.bluetape4k.hibernate.querydsl.core.plus
import io.bluetape4k.hibernate.querydsl.core.simplePathOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class QuerydslExamples: AbstractQuerydslTest() {

    companion object: KLogging() {
        private const val MEMBER_COUNT = 4
    }

    private lateinit var queryFactory: JPAQueryFactory

    private var testId: Long = 0L

    private val qmember = QMember.member
    private val qteam = QTeam.team

    // @QueryDelegate 를 사용하는 게 좋겠다 (https://ocwokocw.tistory.com/181)
    private val QMember.birthDay
        get() = Expressions.constant(LocalDateTime.now().year).minus(age)

    @BeforeEach
    fun setup() {
        queryFactory = JPAQueryFactory { tem.entityManager }

        queryFactory.delete(qmember).execute()
        queryFactory.delete(qteam).execute()

        log.debug { "Add Sample Team and Member entity ..." }

        val teamA = Team("teamA")
        val teamB = Team("teamB")
        tem.persist(teamA)
        tem.persist(teamB)
        tem.flush()

        val members = List(MEMBER_COUNT) {
            val i = it + 1
            val selectedTeam = if (i <= MEMBER_COUNT / 2) teamA else teamB
            val member = Member("member-$i", i * 10, selectedTeam)
            tem.persist(member)
        }
        testId = members.first().id!!

        flushAndClear()
    }

    @Test
    fun `distinct 사용 예`() {
        val teamA = queryFactory.selectFrom(qteam)
            .where(qteam.name.eq("teamA"))
            .fetchOne()

        // 기존 member2 와 같은 속성의 값을 가진 것을 추가한다. (id 만 다를 뿐 ...)
        val member2 = Member("member-2", 20, teamA)
        tem.persist(member2)

        val count = queryFactory.select(qmember).from(qmember).fetchCount()
        log.debug { "Member count=$count" }
        count.toInt() shouldBeEqualTo MEMBER_COUNT + 1

        val results = queryFactory.select(qmember.name, qmember.age)
            .distinct()
            .from(qmember)
            .orderBy(qmember.name.asc())
            .fetch()

        // distinct 되므로 중복된 member-2 가 제외된다.
        results shouldHaveSize MEMBER_COUNT
        results.forEach {
            log.debug { "member=$it" }
        }
    }

    @Test
    fun `JPQL 을 직접 사용하는 예`() {
        val member = tem.entityManager
            .createQuery("select m from Member m where m.name = :name", Member::class.java)
            .setParameter("name", "member-1")
            .singleResult

        member.name shouldBeEqualTo "member-1"
    }

    @Test
    fun `조회 쿼리 중복 호출 시 매번 실행한다`() {
        val member = queryFactory
            .select(qmember)
            .from(qmember)
            .where(qmember.id.eq(testId))
            .fetchOne()
        log.debug { "member=$member" }

        val member2 = queryFactory
            .select(qmember)
            .from(qmember)
            .where(qmember.id.eq(testId))
            .fetchOne()
        log.debug { "member2=$member2" }
    }

    @Test
    fun `복수의 검색 조건들은 and 로 적용`() {
        val member = queryFactory
            .selectFrom(qmember)
            .where(
                qmember.name.eq("member-1"),
                null,       // Predicate 가 null 인 경우는 단순 무시한다
                qmember.age.inValues(10, 20, 30, 40)
            )
            .fetchOne()!!

        member.name shouldBeEqualTo "member-1"
        log.debug { "member=$member" }
    }

    @Test
    fun `조건절의 Predicate가 모두 null 인 경우는 where 조건이 없다`() {
        val member = queryFactory
            .selectFrom(qmember)
            .where(
                null,
                null,
                null,
            )
            .orderBy(qmember.id.asc())
            .fetchFirst()!!

        member.name shouldBeEqualTo "member-1"
        log.debug { "member=$member" }
    }

    /**
     * 페이징 적용 시 - `fetchResults()` 사용
     *
     * `fetchResults()` 는 deprecated 되었고 (`fetchCount()` 때문),
     * [Blaze-Persistence](https://persistence.blazebit.com/documentation/1.5/core/manual/en_US/index.html#querydsl-integration)
     * 를 사용하는 걸 추천하네요
     */
    @Test
    fun `페이징 적용 시 - fetchResults 사용`() {

        val queryResults = queryFactory
            .selectFrom(qmember)
            .offset(1)
            .limit(3)
            .fetchResults()

        val total = queryResults.total
        val offset = queryResults.offset
        val limit = queryResults.limit
        val members = queryResults.results

        members shouldHaveSize 3

        val memberCount = queryFactory
            .selectFrom(qmember)
            .fetchCount()

        total shouldBeEqualTo memberCount
    }

    @Test
    fun `정렬 방식 - nullsLast`() {
        tem.persist(Member("sort-member-1", null))
        tem.persist(Member("sort-member-2", null))
        tem.persist(Member("sort-member-3", 99))
        tem.persist(Member("sort-member-4", 100))
        tem.persist(Member("sort-member-5", 99))
        tem.persist(Member("sort-member-6", 100))
        tem.flush()

        val members = queryFactory
            .selectFrom(qmember)
            .where(qmember.name.startsWith("sort-member"))
            .orderBy(qmember.age.desc().nullsLast(), qmember.name.asc())
            .fetch()

        members.last().age.shouldBeNull()
        members.first().name shouldBeEqualTo "sort-member-4"
        members.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `aggregation query`() {
        val result = queryFactory
            .select(
                qmember.count(),
                qmember.age.sum(),
                qmember.age.avg(),
                qmember.age.max(),
                qmember.age.min()
            )
            .from(qmember)
            .fetchOne()!!

        result[qmember.count()] shouldBeEqualTo 4
        result[qmember.age.sum()] shouldBeEqualTo 100
        result[qmember.age.avg()] shouldBeEqualTo 25.0
        result[qmember.age.max()] shouldBeEqualTo 40
        result[qmember.age.min()] shouldBeEqualTo 10
    }

    @Test
    fun `팀별로 멤버의 나이 평균 구하기 - groupBy`() {

        // alias 사용 시, path 로 지정하면 재사용 할 수 있다
        val avgAge = simplePathOf<Double>("avgAge")

        val results = queryFactory
            .select(qteam.name, qmember.age.avg().`as`(avgAge))
            .from(qmember)
            .join(qmember.team(), qteam)
            .groupBy(qteam.name)
            .fetch()

        val teamA = results[0]
        val teamB = results[1]

        teamA[qteam.name] shouldBeEqualTo "teamA"
        teamA[avgAge] shouldBeEqualTo 15.0

        teamB[qteam.name] shouldBeEqualTo "teamB"
        teamB[avgAge] shouldBeEqualTo 35.0
    }

    @Test
    fun `groupBy and having`() {
        // alias 사용 시, path 로 지정하면 재사용 할 수 있다
        val avgAge = numberPathOf<Double>("avgAge")
        val avgAgeExpr = qmember.age.avg()

        val results = queryFactory
            .select(qteam.name, avgAgeExpr.`as`(avgAge))
            .from(qmember)
            .join(qmember.team(), qteam)
            .groupBy(qteam.name)
            .having(avgAgeExpr.gt(20))
            .fetch()

        val team = results.first()

        team.get(qteam.name) shouldBeEqualTo "teamB"
        team.get(avgAge) shouldBeEqualTo 35.0
    }

    @Test
    fun `right outer join`() {
        val members = queryFactory
            .selectFrom(qmember)
            .rightJoin(qmember.team(), qteam)
            .where(qteam.name.eq("teamA"))
            .fetch()

        members shouldHaveSize 2
        members.map { it.team!!.name }.distinct() shouldContainSame listOf("teamA")
    }

    @Test
    fun `join on filtering`() {
        /**
         * ```sql
         * select
         *     member0_.id as id1_0_0_,
         *     team1_.id as id1_1_1_,
         *     member0_.age as age2_0_0_,
         *     member0_.name as name3_0_0_,
         *     member0_.team_id as team_id4_0_0_,
         *     team1_.name as name2_1_1_
         * from
         *     member member0_
         * inner join
         *     team team1_
         *         on member0_.team_id=team1_.id
         *         and (
         *             team1_.name=?
         *         )
         *  ```
         */
        val tuples = queryFactory
            .select(qmember, qteam)
            .from(qmember)
            .join(qmember.team(), qteam).on(qteam.name.eq("teamA"))
            .fetch()

        tuples.forEach {
            log.debug { "tuple=$it" }
        }
        tuples.map { it.get(qteam)!!.name }.distinct() shouldContainSame listOf("teamA")

        val members = tuples.map { it.get(qmember) }
        val teams = tuples.map { it.get(qteam) }
    }

    /**
     * `EntityPath` 를 이용하여 간단하게 Projection 하기
     *  NOTE: 이건 정말 예시로 보여주는 것이지, 이렇게 하면 Projections 의 잇점이 하나도 없다 (N+1 문제가 생긴다)
     */
    @Test
    fun `projections by constructor with EntityPath`() {
        val projections = Projections.constructor(MemberDto::class.java, qmember)
        val memberDtos = queryFactory
            .select(projections)
            .from(qmember)
            .fetch()

        memberDtos shouldHaveSize MEMBER_COUNT
    }

    /**
     * `EntityPath` 를 이용하여 간단하게 Projection 하기
     */
    @Test
    fun `projections by constructor with Two EntityPath`() {
        val projections = Projections.constructor(MemberTeamDto::class.java, qmember, qteam)
        val memberDtos = queryFactory
            .select(projections)
            .from(qmember)
            .join(qmember.team(), qteam)
            .fetch()

        memberDtos shouldHaveSize MEMBER_COUNT
    }

    @Test
    fun `one-to-many 의 lazy loading 시 proxy 로 제공`() {
        val teamA = queryFactory
            .selectFrom(qteam)
            .where(qteam.name.eq("teamA"))
            .fetchOne()!!

        // team.members 는 loading 되지 않았다
        val loaded = em.isLoaded(teamA.members) // emf.persistenceUnitUtil.isLoaded(teamA, "members")
        loaded.shouldBeFalse()

        // team.members 를 여기서 가져온다
        teamA.members shouldHaveSize 2
    }

    @Test
    fun `one-to-many 의 lazy loading 시 fetchJoin 적용하기`() {
        val teamA = queryFactory
            .selectFrom(qteam)
            .join(qteam.members, qmember).fetchJoin()
            .where(qteam.name.eq("teamA"))
            .fetchOne()!!

        // team.members 는 loading 되어 있다
        val loaded = em.isLoaded(teamA.members) // emf.persistenceUnitUtil.isLoaded(teamA, "members")
        loaded.shouldBeTrue()

        teamA.members shouldHaveSize 2
    }

    @Test
    fun `many-to-one lazy loading 시 proxy로 제공`() {
        val member = queryFactory
            .select(qmember)
            .from(qmember)
            .where(qmember.name.eq("member-1"))
            .fetchOne()!!

        log.debug { "member name=${member.name}" }

        val loaded = em.isLoaded(member.team)  // emf.persistenceUnitUtil.isLoaded(member.team)
        loaded.shouldBeFalse()
    }

    @Test
    fun `many-to-one lazy loading + fetchJoin 시 fetch join 수행`() {
        val member = queryFactory
            .selectFrom(qmember)
            .join(qmember.team(), qteam).fetchJoin()
            .where(qmember.name.eq("member-1"))
            .fetchOne()!!

        log.debug { "member name=${member.name}" }

        val loaded = em.isLoaded(member.team) // emf.persistenceUnitUtil.isLoaded(member.team)
        loaded.shouldBeTrue()
    }

    @Test
    fun `subquery - 나이가 가장 많은 회원 조회`() {
        val qmemberSub = QMember("memberSub")
        val subquery = JPAExpressions.select(qmemberSub.age.max()).from(qmemberSub)

        val member = queryFactory
            .selectFrom(qmember)
            .where(qmember.age.eq(subquery))
            .fetchFirst()!!

        member.age shouldBeEqualTo 40
    }

    @Test
    fun `subquery - 나이가 모든 멤버의 평균 이상인 회원 조회`() {
        val qmemberSub = QMember("memberSub")
        val subquery = JPAExpressions.select(qmemberSub.age.avg()).from(qmemberSub)

        val members = queryFactory
            .selectFrom(qmember)
            .where(qmember.age.goe(subquery))
            .fetch()

        members shouldHaveSize 2
        members.map { it.age } shouldContainSame listOf(30, 40)
    }

    @Test
    fun `subquery - 나이가 10상 초과인 회원 조회`() {
        val qmemberSub = QMember("memberSub")
        val subquery = JPAExpressions
            .select(qmemberSub.id)
            .from(qmemberSub)
            .where(qmemberSub.age.gt(10))

        val members = queryFactory
            .selectFrom(qmember)
            .where(qmember.id.`in`(subquery))
            .fetch()

        members shouldHaveSize 3
        members.map { it.age } shouldContainSame listOf(20, 30, 40)
    }

    /**
     * 성능이 느리므로 조심해서 사용해야 한다
     */
    @Test
    fun `select 절에서 subquery 사용`() {
        val qmemberSub = QMember("memberSub")
        val subquery = JPAExpressions
            .select(qmemberSub.age.avg())
            .from(qmemberSub)
            .where(qmemberSub.age.goe(qmember.age))

        val results = queryFactory
            .select(qmember.name, subquery)
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `간단한 case 구문 예`() {
        val results = queryFactory
            .select(
                qmember.name,
                qmember.age
                    .`when`(10).then("teenager")
                    .`when`(20).then("youngman")
                    .otherwise("oldman")
            )
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `복잡한 case 구문 예`() {
        // case 값으로 뭔가 집계나 join 이 필요한 경우 빼고는
        // 이런 복잡한 case 구문은 차라리 property 로 표현하는 게 낫다
        val caseColumn = CaseBuilder()
            .`when`(qmember.age.between(0, 20)).then("young")
            .`when`(qmember.age.between(21, 30)).then("middle")
            .otherwise("old")

        val results = queryFactory
            .select(caseColumn)
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `case 구분으로 정렬하기`() {
        val rankPath = CaseBuilder()
            .`when`(qmember.age.notBetween(0, 30)).then(1)
            .`when`(qmember.age.between(0, 20)).then(2)
            .otherwise(3)

        val results = queryFactory
            .select(qmember.name, qmember.age, rankPath)
            .from(qmember)
            .orderBy(rankPath.asc())
            .fetch()

        results.forEach { tuple ->
            val name = tuple[qmember.name]
            val age = tuple[qmember.age]
            val rank = tuple[rankPath]

            log.debug { "name=$name, age=$age, rank=$rank" }
        }
    }

    @Test
    fun `use constant expression`() {

        // Constant expression 은 아예 Query 문에서는 제공하지 않네요???
        val constantExpr = Expressions.constant("A")

        /**
         * ```
         * select
         *     member0_.name as col_0_0_
         * from
         *     member member0_
         * where
         *     member0_.name=?
         * ```
         */
        val result = queryFactory
            .select(qmember.name, constantExpr)
            .from(qmember)
            .where(qmember.name.eq("member-1"))
            .fetchOne()!!

        log.debug { "result=$result" }
        result[qmember.name] shouldBeEqualTo "member-1"
        result[constantExpr] shouldBeEqualTo constantExpr.toString()
    }

    @Test
    fun `SQL 함수 사용하기 - concat`() {
        val results = queryFactory
            .select(qmember.name + "_" + qmember.age.stringValue())
            .from(qmember)
            .where(qmember.name.eq("member1"))
            .fetch()
            .map { it.toString() }

        results.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `하나의 컬럼만 조회`() {
        val results: MutableList<String> = queryFactory
            .select(qmember.name)
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `projection by bean`() {
        val memberVos = queryFactory
            .select(
                Projections.bean(MemberVo::class.java, qmember.name.`as`("name"))
            )
            .from(qmember)
            .fetch()

        memberVos.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `projection by field`() {
        val memberVos = queryFactory
            .select(
                Projections.fields(MemberVo::class.java, qmember.name.`as`("name"))
            )
            .from(qmember)
            .fetch()

        memberVos.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `projection by field with ExpressionUtils`() {
        val memberSub = QMember("memberSub")

        val memberVos = queryFactory
            .select(
                Projections.fields(
                    MemberVo::class.java,
                    qmember.name.`as`("name"),   // MemberVo 의 field 명을 지정
                    ExpressionUtils.`as`(JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")
                )
            )
            .from(qmember)
            .fetch()

        memberVos.forEach {
            log.debug { it }
        }
        memberVos.all { it.age == 40 }.shouldBeTrue()
    }

    @Test
    fun `projection by constructor`() {
        val memberDtos = queryFactory
            .select(
                Projections.constructor(
                    MemberDto::class.java,
                    qmember.id,
                    qmember.name,
                    qmember.age
                )
            )
            .from(qmember)
            .fetch()

        memberDtos.forEach {
            log.debug { it }
        }
    }

    /**
     * `@QueryProjection` 을 적용한 DTO 에 대해서 직접 생성할 수 있습니다.
     * 다만 이렇게 하면 DTO 가 querydsl 의존성이 생깁니다.
     */
    @Test
    fun `projection by @QueryProjection`() {
        val memberVos = queryFactory
            .select(QMemberVo(qmember.id, qmember.name, qmember.age))
            .from(qmember)
            .fetch()

        memberVos.forEach {
            log.debug { it }
        }
    }

    /**
     * `@QueryProjection` 을 적용한 DTO 에 대해서 직접 생성할 수 있습니다.
     * 다만 이렇게 하면 DTO 가 querydsl 의존성이 생깁니다.
     */
    @Test
    fun `projection by @QueryProjection composite dto`() {
        val memberTeamVos = queryFactory
            .select(
                QMemberTeamVo(
                    QMemberVo(qmember.id, qmember.name, qmember.age),
                    QTeamVo(qteam.id, qteam.name)
                )
            )
            .from(qmember)
            .join(qmember.team(), qteam)
            .fetch()

        memberTeamVos.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `make predicate with BooleanExpression`() {
        val members = queryFactory
            .selectFrom(qmember)
            .where(searchCondition1("member-1"))
            .fetch()

        members.forEach {
            log.debug { it }
        }
        members shouldHaveSize 1
    }

    private fun searchCondition1(name: String? = null, age: Int? = null): Predicate {
        // BooleanExpression 을 사용할 수도 있다.
        return BooleanBuilder().also { builder ->
            name?.let { builder.and(qmember.name.eq(it)) }
            age?.let { builder.and(qmember.age.eq(it)) }
        }
    }

    @Test
    fun `execute update`() {
        // Update 작업은 EntityManager를 거치지 않고 실행된다.
        val affected = queryFactory
            .update(qmember)
            .set(qmember.name, qmember.name + "-not")
            .where(qmember.age.lt(28))
            .execute()
            .toInt()

        affected shouldBeEqualTo 2

        val updated = queryFactory
            .selectFrom(qmember)
            .fetch()

        updated.forEach {
            log.debug { it }
        }
        updated.count { it.name.endsWith("-not") } shouldBeEqualTo affected
    }

    @Test
    fun `update column with numeric operation`() {
        // Update 작업은 EntityManager를 거치지 않고 실행된다.
        val affected = queryFactory
            .update(qmember)
            .set(qmember.age, qmember.age.add(1))
            .execute()
            .toInt()

        affected shouldBeEqualTo MEMBER_COUNT

        val affected2 = queryFactory
            .update(qmember)
            .set(qmember.age, qmember.age.subtract(1))
            .execute()
            .toInt()

        affected2 shouldBeEqualTo MEMBER_COUNT

        val affected3 = queryFactory
            .update(qmember)
            .set(qmember.age, qmember.age.multiply(2.0))
            .execute()
            .toInt()

        affected3 shouldBeEqualTo MEMBER_COUNT
    }

    @Test
    fun `delete operations`() {
        val affected = queryFactory
            .delete(qmember)
            .where(qmember.age.gt(18))
            .execute()
            .toInt()

        affected shouldBeEqualTo 3
    }

    @Disabled("단순 entity insert 는 불가하다")
    @Test
    fun `insert operations`() {
        val affected = queryFactory
            .insert(qteam)
            .columns(qteam.name)
            .values("TEAM-100")
            .execute()
            .toInt()

        affected shouldBeEqualTo 1

        val affected2 = queryFactory
            .insert(qmember)
            .set(qmember.name, "xxxx")
            .set(qmember.age, 99)
            .execute()
            .toInt()

        affected2 shouldBeEqualTo 1
    }

    @Test
    fun `SQL function 사용하기`() {
        val results = queryFactory
            .select(
                Expressions.stringTemplate("function('upper', {0})", qmember.name)
            )
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
        results.all { it.startsWith("MEMBER-") }.shouldBeTrue()
    }

    @Test
    fun `SQL function 을 expression 함수로 표현하기`() {
        val results = queryFactory
            .select(qmember.name.upper())
            .from(qmember)
            .fetch()

        results.forEach {
            log.debug { it }
        }
        results.all { it.startsWith("MEMBER-") }.shouldBeTrue()
    }

}
