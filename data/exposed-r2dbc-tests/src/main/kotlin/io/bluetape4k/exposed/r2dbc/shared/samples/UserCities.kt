package io.bluetape4k.exposed.r2dbc.shared.samples

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/**
 * ```sql
 * -- Postgres
 * CREATE TABLE IF NOT EXISTS country (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL
 * );
 *
 * ALTER TABLE country ADD CONSTRAINT country_name_unique UNIQUE ("name")
 * ```
 */
object CountryTable: IntIdTable() {
    val name = varchar("name", 50).uniqueIndex()
}

/**
 * ```sql
 * -- Postgres
 * CREATE TABLE IF NOT EXISTS city (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      country_id INT NOT NULL,
 *
 *      CONSTRAINT fk_city_country_id__id FOREIGN KEY (country_id) REFERENCES country(id)
 *          ON DELETE RESTRICT ON UPDATE RESTRICT
 * );
 *
 * CREATE INDEX city_name ON city ("name");
 * ALTER TABLE city ADD CONSTRAINT city_country_id_name_unique UNIQUE (country_id, "name");
 * ```
 */
object CityTable: IntIdTable() {
    val name = varchar("name", 50).index()
    val countryId = reference("country_id", CountryTable)  // many-to-one

    init {
        uniqueIndex(countryId, name)
    }
}

/**
 * ```sql
 * -- Postgres
 * CREATE TABLE IF NOT EXISTS "User" (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      age INT NOT NULL
 * );
 *
 * CREATE INDEX user_name ON "User" ("name");
 * ```
 */
object UserTable: IntIdTable() {
    val name = varchar("name", 50).index()
    val age = integer("age")
}


/**
 * City - User  Many-to-many relationship table
 *
 * ```sql
 * CREATE TABLE IF NOT EXISTS usertocity (
 *      user_id INT NOT NULL,
 *      city_id INT NOT NULL,
 *
 *      CONSTRAINT fk_usertocity_user_id__id FOREIGN KEY (user_id) REFERENCES "User"(id)
 *          ON DELETE CASCADE ON UPDATE RESTRICT,
 *
 *      CONSTRAINT fk_usertocity_city_id__id FOREIGN KEY (city_id) REFERENCES city(id)
 *          ON DELETE CASCADE ON UPDATE RESTRICT
 * );
 * ```
 */
object UserToCityTable: Table() {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val cityId = reference("city_id", CityTable, onDelete = ReferenceOption.CASCADE)
}
