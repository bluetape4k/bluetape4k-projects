# Module bluetape4k-hibernate

**[hibernate-orm](https://hibernate.org/orm/)를 Kotlin에서 사용할 때 유용한 기능을 제공합니다.**

## 개요

JPA 사용 시 편의를 도와주는 Entity class, Converter, Event Listener, Hibernate Stateless Session 등에 대한 다양한 기능을 제공합니다.

## Setup

우선 Kotlin 언어에서는 `final` 이 기본이므로, Hibernate proxy를 사용하려면 `open` 을 모두 붙여줘야 하지만, plugin 중에 `kotlin("plugin.jpa")` 을 추가하면
자동으로 `open` 으로 만들어줍니다.

`kotlin("plugin.spring")` 은 Spring Framework 의 `@Component`, `@Service`, `@Repository` 같은 annotation이 적용된 클래스와 public
method 들을 모두 `open` 으로 만들어줍니다.

마지막으로 QueryDSL과 같이 annotation processor를 이용하여 java code 를 동적으로 생성해서 사용하고자 할 때에는 `kotlin("kapt")` 를 추가하면됩니다. 실제 사용 시 다음과
같이 `kapt` 를 사용할 모듈을 추가하면 됩니다.

```kotlin
dependencies {
    kapt(Libs.querydsl_apt + ":jpa")
}
```

```kotlin
plugins {
    idea
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation("io.bluetape4k:bluetape4k-hibernate:${VERSION}")

    // Hibernate
    implementation(Libraries.hibernate_jpa_2_1_api)
    implementation(Libraries.hibernate_core)
    testImplementation(Libraries.hibernate_testing)

    // Validator
    implementation(Libraries.javax_el_api)
    implementation(Libraries.validation_api)
    implementation(Libraries.hibernate_validator)

    // NOTE: Java 9+ 환경에서 kapt가 제대로 동작하려면 javax.annotation-api 를 참조해야 합니다.
    // Use QueryDSL
    implementation(Libraries.jakarta_annotation_api)
    implementation(Libraries.querydsl_jpa)

    kapt(Libraries.querydsl_apt + ":jpa")
    kaptTest(Libraries.querydsl_apt + ":jpa")

    // Use spring-data-jpa & spring-boot
    implementation("org.springframework.data:spring-data-jpa")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

idea {
    module {
        val kaptMain = file("$buildDir/generated/source/kapt/main")
        sourceDirs.plus(kaptMain)
        generatedSourceDirs.plus(kaptMain)

        val kaptTest = file("$buildDir/generated/source/kapt/test")
        testSourceDirs.plus(kaptTest)
    }
}
```

## 기능

### JPA 기본 Entities

보통 JPA를 사용할 때에는 Identifier 가 가장 중요한 속성이므로, 이를 활용하여 공통적인 코드를 묶을 수 있습니다.
다음은 JPA에서 사용할 수 있는 기본 Entity Class 들의 상속 체계입니다.

![JPA Entity Diagram](doc/JPA_Entity_Diagram.png)

대부분의 평범한 엔티티는 `IntJpaEntity` 나 `LongJpaEntity` 를 상속받으면 되고, Identifier가 Int 나 Long 수형이 아닌 경우에는 `AbstractJapEntity` 를 상속받아
사용하면 됩니다.

#### IntJpaEntity, LongJpaEntity 사용

엔티티의 Identifier로 가장 자주 사용하는 `Int`, `Long` 을 Id 수형으로 사용하는 Entity 입니다.

* [파일: IntJpaEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/IntJpaEntity.kt)
* [파일: LongJpaEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/LongJpaEntity.kt)

`LongJpaEntity` 를 상속한 엔티티의 예는 다음과 같습니다.

```kotlin
@Entity
class Cavalier(
    val name: String,
    @OneToOne(cascade = [ALL])
    @JoinColumn(name = "horse_id")
    var horse: Horse? = null
): LongJpaEntity() {

    // Business Unique에 해당하는 속성으로만 equals 를 비교하도록 합니다.
    override fun equalProperties(other: Any): Boolean =
        other is Cavalier && name == other.name

    // Entity의 toString을 손쉽게 만들어줍니다. 로그에 출력할 속성 정보만 제공하도록 합니다.
    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
```

#### JpaTreeEntity

트리처럼 `parent` 와 `children` 이 있는 엔티티를 표현합니다. DB에서는 `self-reference` model 입니다.

[JpaTreeEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/JpaTreeEntity.kt)

Identifier 가 Int, Long 수형인 `JpaTreeEntity` 도 제공됩니다.

* [IntJpaTreeEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/IntJpaTreeEntity.kt)
* [LongJpaTreeEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/LongJpaTreeEntity.kt)

`JpaTreeEntity` 를 상속받아 트리구조를 가진 Entity를 정의하면 다음과 같습니다.

```kotlin
@Entity(name = "tree_treenode")
@Table(indexes = [Index(name = "ix_tree_treenode_parent", columnList = "parent_id")])
@DynamicInsert
@DynamicUpdate
class TreeNode: IntJpaTreeEntity<TreeNode>() {

    var title: String? = null

    var description: String? = null

    override fun equalProperties(other: Any): Boolean {
        return other is TreeNode && title == other.title
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("title", title)
            .add("description", description)
    }
}
```

`TreeNode` 엔티티를 DB에 저장하는 예는 다음과 같습니다. `addChildren` 메소드를 통해 `parent` 와 `child` 관계를 지정하면 cascade save 를 수행할 수 있습니다.

```kotlin
@Test
fun `build tree nodes`() {
    val root = TreeNode().apply { title = "root" }
    val child1 = TreeNode().apply { title = "child1" }
    val child2 = TreeNode().apply { title = "child2" }

    root.addChildren(child1, child2)

    val child11 = TreeNode().apply { title = "child11" }
    val child12 = TreeNode().apply { title = "child12" }
    child1.addChildren(child11, child12)

    // cascade 를 이용하여 children도 모두 저장되어야 한다
    repository.save(root)
}
```

#### JpaLocalizedEntity

다국어를 지원해야 하는 Entity의 경우에는 `JpaLocalizedEntity` 를 구현하고, 다국어 정보를 매핑하면 됩니다.

NOTE: 현재 Refactoring 중입니다.
향후 Extension methods 로 변경할 예정입니다.

[JpaLocalizedEntity.kt](src/main/kotlin/io/bluetape4k/hibernate/model/JpaLocalizedEntity.kt)

### Converters

Hibernate 에는 `UserType` 이라는 것이 있어 프로그래밍언어에서 정의한 수형과 Database 가 지원하는 Column 수형 간의 변환을 수행해 줍니다.
JPA 에는 `Converter` 가 이러한 수형 변환 기능을 제공합니다.
단, JPA 는 1:1 매핑만 가능합니다. 만약 1:N 방식으로 변환을 원할 경우에는 Hibernate의 `CompositeUserType` 을 사용해야 합니다.

[CompressedStringConverter.kt](src/main/kotlin/io/bluetape4k/hibernate/converters/CompressedStringConverter.kt)

```kotlin
// Locale 수형을 문자열로 저장
@Convert(converter = LocaleAsStringConverter::class)
var locale: Locale = Locale.getDefault()

// java.time.Duration을 Timestamp 로 저장
@Convert(converter = DurationAsTimestampConverter::class)
var duration: Duration? = null

// 문자열을 RC2 알고리즘으로 암호화해서 저장
@Convert(converter = RC2StringConverter::class)
var password: String? = null

// 객체를 직렬화해서 BLOB로 저장 
@Convert(converter = SnappyFstObjectAsByteArrayConverter::class)
@Basic(fetch = FetchType.LAZY)
val component: Component? = Component("test data")
```

### Listeners

`EntityManager` 가 Entity 를 처리하는 과정에서, Entity의 변화를 추적할 수 있는 listener 가능을 제공합니다.

[JpaEntityEventLogger.kt](src/main/kotlin/io/bluetape4k/hibernate/listeners/JpaEntityEventLogger.kt)

```kotlin
@EntityListeners(JpaEntityEventLogger::class)       // 해당 엔티티에 변화가 생길 때, `JpaEntityListener` 의 event handler를 호출합니다.
@MappedSuperclass
abstract class AbstractJpaEntity<TId: Serializable>: AbstractPersistenceObject(), JpaEntity<TId> {
    // Some codes ...
}
```

### EntityManager's Extension methods

JPA `EntityManager` 에 다양한 Extension method를 제공합니다.

```kotlin
fun EntityManager.isLoaded(entity: Any?): Boolean =
    entity?.run { entityManagerFactory.persistenceUnitUtil.isLoaded(this) } ?: false

fun <T: JpaEntity<*>> EntityManager.save(entity: T): T {
    return if (entity.persisted && !contains(entity)) {
        merge(entity)
    } else {
        persist(entity)
        entity
    }
}

fun <T: JpaEntity<*>> EntityManager.delete(entity: T) {
    if (entity.persisted) {
        if (!contains(entity)) {
            remove(merge(entity))
        } else {
            remove(entity)
        }
    }
}

inline fun <reified T: JpaEntity<TId>, TId> EntityManager.deleteById(id: TId) {
    val entity = this.getReference(T::class.java, id)
    remove(entity)
}

inline fun <reified T> EntityManager.findOne(id: Serializable): T? =
    find(T::class.java, id)

fun <T> EntityManager.findOne(clazz: Class<T>, id: Serializable): T? =
    find(clazz, id)

fun <T> EntityManager.exists(entityClass: Class<T>, id: Serializable): Boolean =
    find(entityClass, id) != null

fun <T> EntityManager.findAll(clazz: Class<T>): List<T> {
    return newQuery(clazz, null, null as? Sort?).resultList
}

fun <T> EntityManager.findAll(clazz: Class<T>, ids: Iterable<Serializable>): List<T> {
    if (ids.count() == 0) {
        return emptyList()
    }

    val spec = Specification<T> { root, _, cb ->
        val path = root.get(entityInfo(clazz).idAttribute)
        path.`in`(cb.parameter(Iterable::class.java), "ids")
    }

    return newQuery(clazz, spec, null as? Sort?)
        .setParameter("ids", ids)
        .resultList
}
```

`EntityManager` 의 현재 물리적인 Jdbc Connection 을 얻는 함수.
이 함수는 `StatelessSession` 을 사용하고자 할 때 필요한 함수입니다.

```kotlin
/**
 * 현 [EntityManager] 가 사용하는 [Connection] 을 가져옵니다.
 */
fun <T> EntityManager.currentConnection(): Connection {
    return currentSessionImpl()
        .jdbcCoordinator
        .logicalConnection
        .physicalConnection
}
```

### Stateless Session

Hibernate 는 기본적으로 `Session` (Current Thread Context에 제한된) 기반으로, 관리하는 Entity를 메모리에 저장하여, 엔티티의 생성,변경 등을 추적 관리합니다.
JPA 에서는 `EntityManager` 가 이 역할을 수행합니다.
이러한 역할은 ORM 에서 필수적인 요소이므로 JPA 에서는 필수적으로 사용합니다.
다만, 엔티티의 상태를 관리하는 것은 많은 비용이 들어가서, 성능 면에서는 Jdbc를 직접 사용하는 것보다 불리합니다.

Hibernate 에서는 이러한 성능 상에 불리한 경우를 우회하기 위한 대안도 제시하는데, `Stateless Session` 이라는 개념입니다.
이는 상태관리를 할 필요없는 Session 으로 DB에 직접적으로 명령을 수행할 수 있도록 해주는 기능입니다.
이는 Jdbc 를 직접 사용하는 것과 같이 상태 관리 없이 직접 명령을 수행할 수 있게 해주빈다.

대량 레코드 추가,수정,삭제 시에는 `Stateless Session` 사용을 적극 고려할 것을 추천합니다.

[StatelessSessionSupport.kt](src/main/kotlin/io/bluetape4k/hibernate/stateless/StatelessSessionSupport.kt)

```kotlin
@RepeatedTest(5)
fun `one-to-many entity with stateless`() {
    val elapsed = measureTimeMillis {
        tem.entityManager.withStateless { stateless ->
            repeat(COUNT) {
                val master = createMaster("master-$it")
                stateless.insert(master)
                master.details.forEach { detail ->
                    stateless.insert(detail)
                }
            }
        }
    }
    log.debug { "Stateless save: $elapsed msec" }
}
```
