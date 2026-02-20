# Examples - JPA & Querydsl

JPA와 Querydsl을 사용한 데이터베이스 쿼리 패턴을 학습하는 예제 모음입니다.

## 예제 목록

### Querydsl 기본 (examples/)

| 예제 파일                 | 설명                |
|-----------------------|-------------------|
| `QuerydslExamples.kt` | Querydsl 기본 쿼리 패턴 |

### 주요 쿼리 패턴

#### 기본 조회

```kotlin
// 단건 조회
val member = queryFactory
    .selectFrom(qmember)
    .where(qmember.name.eq("member-1"))
    .fetchOne()

// 복수 조건 (AND)
val members = queryFactory
    .selectFrom(qmember)
    .where(
        qmember.name.eq("member-1"),
        qmember.age.inValues(10, 20, 30)
    )
    .fetch()
```

#### JPQL vs Querydsl

```kotlin
// JPQL 직접 사용
val member = entityManager
    .createQuery("select m from Member m where m.name = :name", Member::class.java)
    .setParameter("name", "member-1")
    .singleResult

// Querydsl 사용
val member = queryFactory
    .selectFrom(qmember)
    .where(qmember.name.eq("member-1"))
    .fetchOne()
```

#### 프로젝션 (DTO 조회)

```kotlin
// Projections.bean
val dto = queryFactory
    .select(Projections.bean(MemberDto::class.java, qmember.name, qmember.age))
    .from(qmember)
    .fetch()

// Projections.constructor
val dto = queryFactory
    .select(Projections.constructor(MemberDto::class.java, qmember.name, qmember.age))
    .from(qmember)
    .fetch()

// @QueryProjection (Q-Type)
val dto = queryFactory
    .select(QMemberDto(qmember.name, qmember.age))
    .from(qmember)
    .fetch()
```

#### 동적 쿼리

```kotlin
// BooleanBuilder
val builder = BooleanBuilder()
if (name != null) builder.and(qmember.name.eq(name))
if (age != null) builder.and(qmember.age.gt(age))

val members = queryFactory
    .selectFrom(qmember)
    .where(builder)
    .fetch()
```

#### 서브쿼리

```kotlin
// JPAExpressions
val maxAge = JPAExpressions
    .select(qmember.age.max())
    .from(qmember)

val members = queryFactory
    .selectFrom(qmember)
    .where(qmember.age.eq(maxAge))
    .fetch()
```

## 도메인 모델

### Entity (domain/model/)

| 엔티티      | 설명                       |
|----------|--------------------------|
| `Member` | 회원 엔티티 (name, age, team) |
| `Team`   | 팀 엔티티 (name, members)    |

### DTO (domain/dto/)

| DTO                     | 설명          |
|-------------------------|-------------|
| `MemberDto`             | 회원 DTO      |
| `TeamDto`               | 팀 DTO       |
| `MemberTeamDto`         | 회원+팀 조인 DTO |
| `MemberSearchCondition` | 검색 조건       |

### Repository (domain/repository/)

| Repository               | 설명           |
|--------------------------|--------------|
| `MemberRepository`       | 회원 리포지토리     |
| `TeamRepository`         | 팀 리포지토리      |
| `MemberRepositoryCustom` | 커스텀 쿼리 인터페이스 |
| `MemberRepositoryImpl`   | Querydsl 구현체 |

## 실행 방법

```bash
# 모든 예제 실행
./gradlew :examples:jpa-querydsl:test

# Querydsl 예제만 실행
./gradlew :examples:jpa-querydsl:test --tests "*QuerydslExamples*"
```

## 참고

- [Querydsl Reference](http://querydsl.com/static/querydsl/latest/reference/html_single/)
- [Spring Data JPA + Querydsl](https://spring.io/projects/spring-data-jpa)
