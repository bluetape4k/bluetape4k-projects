# Module Examples - JPA & Querydsl

English | [한국어](./README.ko.md)

A collection of examples for learning database query patterns using JPA and Querydsl.

## Examples

### Querydsl Basics (examples/)

| Example File          | Description                   |
|-----------------------|-------------------------------|
| `QuerydslExamples.kt` | Basic Querydsl query patterns |

### Common Query Patterns

#### Basic Queries

```kotlin
// Single record lookup
val member = queryFactory
    .selectFrom(qmember)
    .where(qmember.name.eq("member-1"))
    .fetchOne()

// Multiple conditions (AND)
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
// Using JPQL directly
val member = entityManager
    .createQuery("select m from Member m where m.name = :name", Member::class.java)
    .setParameter("name", "member-1")
    .singleResult

// Using Querydsl
val member = queryFactory
    .selectFrom(qmember)
    .where(qmember.name.eq("member-1"))
    .fetchOne()
```

#### Projections (DTO Queries)

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

#### Dynamic Queries

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

#### Subqueries

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

## Domain Model

### Entities (domain/model/)

| Entity   | Description                     |
|----------|---------------------------------|
| `Member` | Member entity (name, age, team) |
| `Team`   | Team entity (name, members)     |

### DTOs (domain/dto/)

| DTO                     | Description             |
|-------------------------|-------------------------|
| `MemberDto`             | Member DTO              |
| `TeamDto`               | Team DTO                |
| `MemberTeamDto`         | Member + Team join DTO  |
| `MemberSearchCondition` | Search condition object |

### Repositories (domain/repository/)

| Repository               | Description             |
|--------------------------|-------------------------|
| `MemberRepository`       | Member repository       |
| `TeamRepository`         | Team repository         |
| `MemberRepositoryCustom` | Custom query interface  |
| `MemberRepositoryImpl`   | Querydsl implementation |

## How to Run

```bash
# Run all examples
./gradlew :examples:jpa-querydsl:test

# Run only Querydsl examples
./gradlew :examples:jpa-querydsl:test --tests "*QuerydslExamples*"
```

## References

- [Querydsl Reference](http://querydsl.com/static/querydsl/latest/reference/html_single/)
- [Spring Data JPA + Querydsl](https://spring.io/projects/spring-data-jpa)
