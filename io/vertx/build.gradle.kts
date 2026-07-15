configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-jdbc"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Vertx core
    api(bt4k.vertx.core)
    api(libs.vertx.lang.kotlin)
    api(libs.vertx.lang.kotlin.coroutines)
    compileOnly(libs.vertx.web)
    compileOnly(libs.vertx.web.client)
    compileOnly(libs.vertx.junit5)

    // Resilience4j
    api(project(":bluetape4k-resilience4j"))
    compileOnly(libs.resilience4j.reactor)
    compileOnly(libs.resilience4j.micrometer)

    // SqlClient
    api(libs.vertx.sql.client)
    api(libs.vertx.sql.client.templates)
    implementation(libs.vertx.mysql.client)
    implementation(libs.vertx.pg.client)
    compileOnly(libs.vertx.jdbc.client)
    compileOnly(bt4k.agroal.pool)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)
    implementation(bt4k.mybatis.dynamic.sql)

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(bt4k.mysql.connector.j)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mysql)
}
