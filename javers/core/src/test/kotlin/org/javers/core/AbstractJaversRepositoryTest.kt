package org.javers.core

import io.bluetape4k.javers.commit.SnowflakeCommitIdGenerator
import io.bluetape4k.javers.diff.filterByType
import io.bluetape4k.javers.latestSnapshotOrNull
import io.bluetape4k.javers.repository.jql.queryAnyDomainObject
import io.bluetape4k.javers.repository.jql.queryByClass
import io.bluetape4k.javers.repository.jql.queryByInstance
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import io.bluetape4k.javers.repository.jql.queryByValueObject
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.javers.common.date.DateProvider
import org.javers.core.commit.CommitMetadata
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.CollectionChange
import org.javers.core.diff.changetype.container.ValueAdded
import org.javers.core.model.ConcreteWithActualType
import org.javers.core.model.DummyAddress
import org.javers.core.model.DummyUser
import org.javers.core.model.DummyUserDetails
import org.javers.core.model.NewEntityWithTypeAlias
import org.javers.core.model.NewValueObjectWithTypeAlias
import org.javers.core.model.PrimitiveEntity
import org.javers.core.model.SnapshotEntity
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Stream
import kotlin.math.absoluteValue

abstract class AbstractJaversRepositoryTest {

    companion object: KLogging()

    protected lateinit var javers: Javers
    protected lateinit var repository: JaversRepository
    protected lateinit var dateProvider: DateProvider
    protected var commitIdGenerator: SnowflakeCommitIdGenerator? = null

    abstract fun prepareJaversRepository(): JaversRepository
    protected open fun useCustomCommitIdSupplier(): Boolean = true

    @BeforeEach
    open fun beforeEach() {
        buildJaversInstance()
    }

    protected open fun buildJaversInstance() {
        dateProvider = prepareDateProvider()
        repository = prepareJaversRepository()

        val javersBuilder = JaversBuilder.javers()
            .withDateTimeProvider(dateProvider)
            .registerJaversRepository(repository)

        if (useCustomCommitIdSupplier()) {
            commitIdGenerator = SnowflakeCommitIdGenerator()

            // NOTE: withCustomCommitIdGenerator 가 internal 함수라서 이 클래스의 package name 을 동일하게 해주었습니다.
            javersBuilder.withCustomCommitIdGenerator(commitIdGenerator)
        }

        javers = javersBuilder.build()
    }

    protected open fun commitSeq(commit: CommitMetadata): Int = when {
        useCustomCommitIdSupplier() -> commitIdGenerator!!.getSeq(commit.id)
        else -> commit.id.majorId.toInt()
    }

    protected open fun prepareDateProvider(): DateProvider = when {
        useCustomCommitIdSupplier() -> TikDateProvider()
        else -> FakeDateProvider()
    }

    protected open fun setNow(dateTime: ZonedDateTime) {
        when (val provider = this.dateProvider) {
            is TikDateProvider -> provider.set(dateTime)
            is FakeDateProvider -> provider.set(dateTime)
        }
    }

    @Test
    fun `CommitMetadata에 현재 LocalDateTime과 Instant를 사용한다`() {
        // GIVEN
        val now = ZonedDateTime.now()
        setNow(now)

        // WHEN
        javers.commit("author", SnapshotEntity(1))
        val snapshot = javers.latestSnapshotOrNull<SnapshotEntity>(1)

        // THEN
        snapshot.shouldNotBeNull()
        snapshot.commitMetadata.author shouldBeEqualTo "author"
        snapshot.commitMetadata.commitDateInstant shouldBeEqualTo now.toInstant()
        ChronoUnit.MILLIS.between(
            snapshot.commitMetadata.commitDate,
            now.toLocalDateTime()
        ).absoluteValue shouldBeLessOrEqualTo 1
    }


    @Test
    fun `다양한 primitive 수형을 commit 합니다`() {
        // GIVEN
        val s = PrimitiveEntity("1")

        // WHEN
        javers.commit("author", s)

        s.intField = 10
        s.longField = 10L
        s.doubleField = 1.1
        s.floatField = 1.1F
        s.charField = 'c'
        s.byteField = 10.toByte()
        s.shortField = 10.toShort()
        s.booleanField = true
        s.IntegerField = 10
        s.LongField = 10
        s.DoubleField = 1.1
        s.FloatField = 1.1F
        s.CharField = 'c'
        s.ByteField = 10.toByte()
        s.ShortField = 10.toShort()
        s.BooleanField = true

        javers.commit("author", s)

        // THEN
        val changes = javers.findChanges(queryAnyDomainObject())
        changes.size shouldBeEqualTo 18

        val valueChanges = changes.filterByType<ValueChange>()
        valueChanges.size shouldBeEqualTo 17
        valueChanges.forEach { change ->
            log.debug { "old=${change.left}, new=${change.right}" }
        }
    }

    @Test
    fun `ValueObject를 소유한 Entity의 snapshot으로부터 ValueObject 변경을 조회`() {
        // GIVEN
        val data = listOf(
            DummyUserDetails(1, DummyAddress(city = "London")),
            DummyUserDetails(1, DummyAddress(city = "Paris")),
            SnapshotEntity(1, valueObjectRef = DummyAddress("London")),
            SnapshotEntity(1, valueObjectRef = DummyAddress("Paris")),
            SnapshotEntity(2, valueObjectRef = DummyAddress("Rome")),
            SnapshotEntity(2, valueObjectRef = DummyAddress("Paris")),
            // Noise
            SnapshotEntity(2, valueObjectRef = DummyAddress("Paris")).apply {
                arrayOfValueObjects = arrayOf(DummyAddress("Luton"))
            }
        )

        data.forEach { javers.commit("author", it) }

        // WHEN
        val changes = javers.findChanges(queryByValueObject<SnapshotEntity>("valueObjectRef"))

        // THEN
        log.debug { changes.prettyPrint() }
        changes.forEach {
            log.debug { "change=$it" }
            it.affectedGlobalId.typeName shouldBeEqualTo DummyAddress::class.java.name
        }
        changes.size shouldBeEqualTo 4
        commitSeq(changes[0].commitMetadata.get()) shouldBeEqualTo 6
    }

    @Test
    fun `JaversRepository로 Reference Object 변화 이력을 저장합니다`() {
        // GIVEN
        val ref = SnapshotEntity(id = 2)
        val cdo = SnapshotEntity(id = 1).apply {
            entityRef = ref
            arrayOfIntegers = intArrayOf(1, 2)
            listOfDates = mutableListOf(
                LocalDate.of(2001, 1, 1),
                LocalDate.of(2001, 1, 2)
            )
            mapOfValues[LocalDate.of(2001, 1, 1)] = BigDecimal(1.1)
            mapOfGenericValues["enumSet"] = EnumSet.of(SnapshotEntity.DummyEnum.VAL1, SnapshotEntity.DummyEnum.VAL2)
        }

        javers.commit("author", cdo)    // v. 1
        cdo.intProperty = 5
        javers.commit("author2", cdo)   // v. 2

        // WHEN
        val snapshots = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1))

        // THEN
//        val refId = GlobalIdTestBuilder.instanceId(2, SnapshotEntity::class)
//        log.trace { "refId=$refId" }
//
//        val snapshot = snapshots.find { (it.globalId as InstanceId).cdoId == 1 }
//        log.trace { "majorId = ${snapshot!!.commitMetadata.id.majorId}" }
//        commitSeq(snapshot!!.commitMetadata) shouldBeEqualTo 2
//
//        snapshot.getPropertyValue("id") shouldBeEqualTo 1
//        snapshot.getPropertyValue("entityRef") shouldBeEqualTo refId
//        (snapshot.getPropertyValue("arrayOfIntegers") as IntArray) shouldContainSame intArrayOf(1, 2)
//
//        with(snapshots[0]) {
//            commitSeq(commitMetadata) shouldBeEqualTo 2
//            commitMetadata.author shouldBeEqualTo "author2"
//            changed.size shouldBeEqualTo 1
//            changed[0] shouldBeEqualTo "intProperty"
//            isInitial.shouldBeFalse()
//
//            log.debug { "Snapshot commitId: ${this.commitId}" }
//            state.forEachProperty { name, value ->
//                log.debug { "property name=$name, value=$value" }
//            }
//            log.debug { "Json= ${javers.jsonConverter.toJson(this)}" }
//        }
//
//        with(snapshots[1]) {
//            commitSeq(commitMetadata) shouldBeEqualTo 1
//            commitMetadata.author shouldBeEqualTo "author"
//            getPropertyValue("intProperty").shouldBeNull()
//            isInitial.shouldBeTrue()
//
//            log.debug { "Snapshot commitId: ${this.commitId}" }
//            state.forEachProperty { name, value ->
//                log.debug { "property name=$name, value=$value" }
//            }
//
//            log.debug { "Json= ${javers.jsonConverter.toJson(this)}" }
//        }
//
//        val changes = javers.findChanges(queryByInstanceId<SnapshotEntity>(1))
//
//        log.debug { "Changes=${changes.prettyPrint()}" }
//        log.debug { "Changes Json = ${javers.jsonConverter.toJson(changes)}" }
    }

    @Test
    fun `엔티티 속성을 Repository의 가장 최신 것과 비교하기`() {
        //GIVEN
        val user = DummyUser(name = "John").apply { age = 18 }
        javers.commit("login", user)

        // WHEN
        user.age = 19
        javers.commit("login", user)

        val history = javers.findChanges(queryByInstanceId<DummyUser>("John"))

        // THEN
        with(history[0]) {

            log.trace { "change=${javers.jsonConverter.toJson(this)}" }

            this shouldBeInstanceOf ValueChange::class.java
//            affectedGlobalId shouldBeEqualTo GlobalIdTestBuilder.instanceId("John", DummyUser::class)
//
//            with(this as ValueChange) {
//                propertyName shouldBeEqualTo "age"
//                left shouldBeEqualTo 18
//                right shouldBeEqualTo 19
//            }
        }
    }

    @Test
    fun `Repository로부터 온전한 엔티티인 Shadow를 가져온다`() {
        //GIVEN
        val user = DummyUser(name = "John").apply { age = 18 }
        javers.commit("login", user)

        // WHEN
        user.age = 19
        javers.commit("login", user)

        // Shadows는 저장된 Snapshot 변경이력으로부터, 원하는 객체 재구성해준다
        val shadows = javers.findShadows<DummyUser>(queryByInstance(user))

        // THEN
        shadows.size shouldBeEqualTo 2

        val newUser = shadows[0].get()
        val oldUser = shadows[1].get()

        newUser.age shouldBeEqualTo 19
        oldUser.age shouldBeEqualTo 18

        shadows[0].commitMetadata.id.majorId shouldBeGreaterThan shadows[1].commitMetadata.id.majorId
    }

    @Test
    fun `Repository에 복수의 snapshots을 연속으로 commit하고 read하기`() {
        val cdo = SnapshotEntity(id = 1)

        (1..25).forEach {
            cdo.intProperty = it
            javers.commit("login", cdo)

            val snapshot = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1)).first()
            snapshot.getPropertyValue("intProperty") shouldBeEqualTo it
        }
    }

    @Test
    fun `부모클래스의 Generic 필드에 대한 변화를 Commit하기`() {
        // GIVEN
        javers.commit("author", ConcreteWithActualType("a", listOf("1")))
        javers.commit("author", ConcreteWithActualType("a", listOf("1", "2")))

        // WHEN
        val changes = javers.findChanges(queryByClass<ConcreteWithActualType>())

        // THEN
        val change = changes[0]

        Assumptions.assumeTrue { change is CollectionChange<*> }
        change shouldBeInstanceOf CollectionChange::class.java
        if (change is CollectionChange<*>) {
            val elementChange = change.changes[0]
            elementChange shouldBeInstanceOf ValueAdded::class.java

            if (elementChange is ValueAdded) {
                elementChange.index shouldBeEqualTo 1
                elementChange.addedValue shouldBeInstanceOf String::class
                elementChange.addedValue shouldBeEqualTo "2"
            }
        }
    }

    @Test
    fun `ValueObject와 소유권자인 Entity 를 ValueObject 쿼리로 조회하기`() {
        // GIVEN
        javers.commit(
            "author",
            NewEntityWithTypeAlias(1.toBigDecimal()).apply { valueObject = NewValueObjectWithTypeAlias(5) })
        javers.commit(
            "author",
            NewEntityWithTypeAlias(1.toBigDecimal()).apply { valueObject = NewValueObjectWithTypeAlias(6) })

        // WHEN
        val changes = javers.findChanges(queryByValueObject<NewEntityWithTypeAlias>("valueObject"))

        // THEN
        changes.size shouldBeEqualTo 2

        val valueChange = changes.find { it is ValueChange && it.propertyName == "some" } as ValueChange

        valueChange.left shouldBeEqualTo 5
        valueChange.right shouldBeEqualTo 6
    }

    @ParameterizedTest
    @MethodSource("getTimeRangeQuery")
    fun `Entity Snapshot을 기간으로 검색하기`(query: JqlQuery, expectedCommitDates: List<LocalDateTime>) {
        // GIVEN:
        repeat(5) {
            val index = it + 1
            val entity = SnapshotEntity(1).apply { intProperty = index }
            val now = ZonedDateTime.of(2015, 1, 1, index, 0, 0, 0, ZoneOffset.UTC)
            setNow(now)
            javers.commit("author", entity)
        }

        // WHEN
        val snapshots = javers.findSnapshots(query)
        val commitDates = snapshots.map { it.commitMetadata.commitDate }

        // THEN
        commitDates shouldContainSame expectedCommitDates
    }

    private fun getTimeRangeQuery(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                QueryBuilder
                    .byInstanceId(1, SnapshotEntity::class.java)
                    .from(LocalDateTime.of(2015, 1, 1, 3, 0))
                    .build(),
                (5 downTo 3).map { LocalDateTime.of(2015, 1, 1, it, 0) }
            ),
            Arguments.of(
                QueryBuilder
                    .byInstanceId(1, SnapshotEntity::class.java)
                    .to(LocalDateTime.of(2015, 1, 1, 3, 0))
                    .build(),
                (3 downTo 1).map { LocalDateTime.of(2015, 1, 1, it, 0) }
            ),

            Arguments.of(
                QueryBuilder
                    .byInstanceId(1, SnapshotEntity::class.java)
                    .from(LocalDateTime.of(2015, 1, 1, 2, 0))
                    .to(LocalDateTime.of(2015, 1, 1, 4, 0))
                    .build(),
                (4 downTo 2).map { LocalDateTime.of(2015, 1, 1, it, 0) }
            )
        )

    @Test
    fun `Entity snapshot version 은 증가됩니다`() {
        // WHEN
        val entity = SnapshotEntity(id = 1).apply { intProperty = 11 }
        javers.commit("author", entity)
        javers.commit("author", entity.apply { intProperty = 22 })
        javers.commit("author", entity.apply { intProperty = 33 })
        javers.commit("author", entity.apply { intProperty = 44 })

        // THEN
        val snapshots = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1))
        snapshots.size shouldBeEqualTo 4
        snapshots.forEach {
            log.debug { "version=${it.version}" }
        }
        snapshots[0].version shouldBeEqualTo 4
        snapshots[1].version shouldBeEqualTo 3
        snapshots[2].version shouldBeEqualTo 2
        snapshots[3].version shouldBeEqualTo 1
    }


    @RepeatedTest(3)
    fun `200개의 다른 snapshot들을 조회한다`() {
        // GIVEN:
        repeat(200) {
            javers.commit("author", SnapshotEntity(id = 1).apply { intProperty = it + 1 })
        }

        // val instanceId = JaversTestBuilder.javersTestAssembly().instanceId(SnapshotEntity(id = 1))
        // val snapshotIdentifiers = List(200) { SnapshotIdentifier(instanceId, it + 1L) }

        // WHEN:
        // val snapshots = repository.getSnapshots(snapshotIdentifiers)

        // THEN:
        // snapshots.size shouldBeEqualTo snapshotIdentifiers.size
    }

    @Test
    fun `Commited properties 를 commit 하고 read 하기`() {
        val commitProperties = mapOf(
            "tenant" to "ACME",
            "sessionId" to "1234567890",
            "device" to "smartwatch",
            "yet another property name" to "yet another property value"
        )
        javers.commit("author", SnapshotEntity(id = 1), commitProperties)

        // WHEN
        val snapshot = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1)).first()

        // THEN
        snapshot.commitMetadata.properties shouldContainSame commitProperties
    }

    @Test
    fun `cluster-friendly commitId generator 지원하기`() {
        val threads = 10
        val javersRepo = prepareJaversRepository()

        List(threads) { it }
            .parallelStream()
            .map {
                it to JaversBuilder.javers()
                    .registerJaversRepository(javersRepo)
                    .withCommitIdGenerator(CommitIdGenerator.RANDOM)
                    .build()
            }.forEach {
                val commit = it.second.commit("author", SnapshotEntity(id = it.first))
                log.debug { "Commit entity. commit=$commit" }
            }

        val javers = JaversBuilder.javers().registerJaversRepository(javersRepo).build()
        val snapshots = javers.findSnapshots(queryAnyDomainObject())

        snapshots.map { it.commitId }.toSet() shouldHaveSize threads
    }

    @Test
    fun `변경이 없는 zero snapshots 은 commit 되지 않습니다`() {
        // GIVEN
        val anEntity = SnapshotEntity(1).apply { intProperty = 100 }

        // WHEN
        val commit = javers.commit("author", anEntity)
        val snapshots = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1))

        // THEN
        snapshots.size shouldBeEqualTo 1
        repository.headId shouldBeEqualTo commit.id

        // WHEN: 변경이 없는 엔티티는 저장되면 안됩니다
        javers.commit("author", anEntity)

        // THEN: 저장되지 않았으므로 headId의 변화가 없다
        repository.headId shouldBeEqualTo commit.id
    }
}
