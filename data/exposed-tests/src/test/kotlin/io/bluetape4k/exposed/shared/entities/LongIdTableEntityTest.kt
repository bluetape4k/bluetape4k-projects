package io.bluetape4k.exposed.shared.entities

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.shared.entities.LongIdTables.Cities
import io.bluetape4k.exposed.shared.entities.LongIdTables.City
import io.bluetape4k.exposed.shared.entities.LongIdTables.People
import io.bluetape4k.exposed.shared.entities.LongIdTables.Person
import io.bluetape4k.exposed.shared.entities.LongIdTables.Town
import io.bluetape4k.exposed.shared.entities.LongIdTables.Towns
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

object LongIdTables {

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS cities (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Cities: LongIdTable() {
        val name = varchar("name", 50)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS people (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(80) NOT NULL,
     *      city_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_people_city_id__id FOREIGN KEY (city_id) REFERENCES cities(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object People: LongIdTable() {
        val name = varchar("name", 80)
        val cityId = reference("city_id", Cities)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS towns (
     *      id BIGSERIAL PRIMARY KEY,
     *      city_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_towns_city_id__id FOREIGN KEY (city_id) REFERENCES cities(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Towns: LongIdTable("towns") {
        val cityId = long("city_id").references(Cities.id)
    }

    class City(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<City>(Cities)

        var name: String by Cities.name
        val towns: SizedIterable<Town> by Town referrersOn Towns.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Person(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Person>(People)

        var name by People.name
        var city by City referencedOn People.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("city", city.name)
            .toString()
    }


    class Town(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Town>(Towns)

        var city by City referencedOn Towns.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("city", city.name)
            .toString()
    }
}

class LongIdTableEntityTest: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create tables`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            Cities.exists().shouldBeTrue()
            People.exists().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val mumbai = City.new { name = "Mumbai" }
            val pune = City.new { name = "Pune" }

            Person.new {
                name = "David D'souza"
                city = mumbai
            }
            Person.new {
                name = "Tushar Mumbaikar"
                city = mumbai
            }
            Person.new {
                name = "Tanu Arora"
                city = pune
            }

            val allCities = City.all().map { it.name }
            allCities shouldContainSame listOf("Mumbai", "Pune")

            val allPeople = Person.all().map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "David D'souza" to "Mumbai",
                "Tushar Mumbaikar" to "Mumbai",
                "Tanu Arora" to "Pune"
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update and delete records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val mumbai = City.new { name = "Mumbai" }
            val pune = City.new { name = "Pune" }

            Person.new {
                name = "David D'souza"
                city = mumbai
            }
            Person.new {
                name = "Tushar Mumbaikar"
                city = mumbai
            }
            val tanu = Person.new {
                name = "Tanu Arora"
                city = pune
            }

            tanu.delete()
            pune.delete()

            val allCities = City.all().map { it.name }
            allCities shouldContainSame listOf("Mumbai")

            val allPeople = Person.all().map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "David D'souza" to "Mumbai",
                "Tushar Mumbaikar" to "Mumbai"
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `foreign key between long and EntityID columns`(testDB: TestDB) {
        withTables(testDB, Cities, Towns) {
            val cId = Cities.insertAndGetId {
                it[name] = "City A"
            }
            val tId = Towns.insertAndGetId {
                it[cityId] = cId.value
            }
            val tId2 = Towns.insertAndGetId {
                it[cityId] = cId.value
            }

            /**
             * lazy loaded referencedOn
             *
             * ```sql
             * SELECT towns.id, towns.city_id FROM towns
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val town1 = Town.all().first()
            town1.city.id shouldBeEqualTo cId

            /**
             * eager loaded referrersOn (`with(Town::city)`)
             *
             * ```sql
             * SELECT towns.id, towns.city_id FROM towns
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val town1WithCity = Town.all().with(Town::city).first()
            town1WithCity.city.id shouldBeEqualTo cId

            /**
             * lazy loaded referrersOn
             *
             * ```sql
             * SELECT cities.id, cities."name" FROM cities
             * SELECT towns.id, towns.city_id FROM towns WHERE towns.city_id = 1
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val city1 = City.all().single()
            val towns = city1.towns
            towns.first().city.id shouldBeEqualTo cId

            /**
             * eager loaded referrersOn (`with(City::towns)`)
             *
             * ```sql
             * SELECT cities.id, cities."name"
             *   FROM cities;
             *
             * SELECT towns.id, towns.city_id, cities.id
             *   FROM towns INNER JOIN cities ON towns.city_id = cities.id
             *  WHERE towns.city_id = 1;
             *
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val city1WithTowns = City.all().with(City::towns).single()

            city1WithTowns.towns.first().city.id shouldBeEqualTo cId
        }
    }
}
