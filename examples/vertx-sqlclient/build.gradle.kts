plugins {
    kotlin("kapt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-jdbc"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    testImplementation(project(":bluetape4k-vertx-core"))
    testImplementation(project(":bluetape4k-vertx-sqlclient"))
    testImplementation(Libs.vertx_junit5)

    // Vertx Kotlin
    testImplementation(Libs.vertx_core)
    testImplementation(Libs.vertx_lang_kotlin)
    testImplementation(Libs.vertx_lang_kotlin_coroutines)

    // Vertx SqlClient
    testImplementation(Libs.vertx_sql_client)
    testImplementation(Libs.vertx_sql_client_templates)
    testImplementation(Libs.vertx_mysql_client)
    testImplementation(Libs.vertx_pg_client)

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    testImplementation(Libs.vertx_jdbc_client)
    testImplementation(Libs.agroal_pool)

    // vertx-sql-cleint-templates 에서 @DataObject, @RowMapped 를 위해 사용
    compileOnly(Libs.vertx_codegen)
    kapt(Libs.vertx_codegen)
    kaptTest(Libs.vertx_codegen)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_module_blackbird)
    implementation(Libs.jackson_datatype_jsr310)

    testRuntimeOnly(Libs.h2)
    testRuntimeOnly(Libs.mysql_connector_j)

    // Testcontainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)

    // Coroutines
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
