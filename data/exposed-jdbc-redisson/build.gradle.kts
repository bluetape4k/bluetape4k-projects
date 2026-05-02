configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(libs.exposed.bom))

    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.kotlin.datetime)

    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-cache"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))

    // Redisson
    api(project(":bluetape4k-redisson"))
    api(libs.redisson)


    testImplementation(project(":bluetape4k-io"))

    // Codecs
    compileOnly(libs.kryo5)
    compileOnly(libs.fory.kotlin)  // new Apache Fory

    // Compressor
    compileOnly(libs.snappy.java)
    compileOnly(libs.lz4.java)
    compileOnly(libs.zstd.jni)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Bluetape4k Modules for Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    testImplementation(project(":bluetape4k-idgenerators"))

    // Database Drivers
    testImplementation(libs.hikaricp)
    testImplementation(libs.h2.v2)
    testImplementation(libs.mariadb.java.client)
    testImplementation(libs.mysql.connector.j)
    testImplementation(libs.postgresql.driver)
    testImplementation(libs.pgjdbc.ng)

}
