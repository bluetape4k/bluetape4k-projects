configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))

    api(libs.exposed.core)
    api(libs.exposed.r2dbc)
    compileOnly(libs.exposed.java.time)
    compileOnly(libs.exposed.kotlin.datetime)

    api(project(":bluetape4k-exposed-r2dbc"))
    api(project(":bluetape4k-exposed-cache"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))

    // Redisson
    api(project(":bluetape4k-redisson"))
    api(libs.redisson)

    // Codecs
    api(project(":bluetape4k-io"))

    // Serializers
    runtimeOnly(libs.kryo5)
    runtimeOnly(libs.fory.kotlin)  // new Apache Fory

    // Compressor
    runtimeOnly(libs.lz4.java)
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.zstd.jni)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(project(":bluetape4k-idgenerators"))

    // R2DBC
    api(libs.r2dbc.spi)
    api(libs.r2dbc.pool)
    testRuntimeOnly(libs.r2dbc.h2)
    testRuntimeOnly(libs.r2dbc.mariadb)
    testRuntimeOnly(libs.r2dbc.mysql)
    testRuntimeOnly(libs.r2dbc.postgresql)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // Database Drivers for Testcontainers Database
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)

}
