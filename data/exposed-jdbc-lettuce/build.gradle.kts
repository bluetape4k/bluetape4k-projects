configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-cache"))
    api(project(":bluetape4k-resilience4j"))
    api(libs.resilience4j.retry)

    // Exposed
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    // Lettuce
    api(libs.lettuce.core)

    // Serializer (LettuceLoadedMap에서 사용하는 codec용)
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.snappy.java)
    compileOnly(libs.lz4.java)
    compileOnly(libs.zstd.jni)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(testFixtures(project(":bluetape4k-exposed-cache")))

    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.mariadb.java.client)
    testImplementation(libs.mysql.connector.j)
    testImplementation(libs.postgresql.driver)
    testImplementation(libs.pgjdbc.ng)
}
