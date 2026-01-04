plugins {
    kotlin("kapt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-jdbc"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    implementation(project(":bluetape4k-vertx-core"))
    implementation(project(":bluetape4k-vertx-sqlclient"))
    testImplementation(Libs.vertx_junit5)

    // Vertx Kotlin
    implementation(Libs.vertx_core)
    implementation(Libs.vertx_lang_kotlin)
    implementation(Libs.vertx_lang_kotlin_coroutines)

    // Vertx SqlClient
    implementation(Libs.vertx_sql_client)
    implementation(Libs.vertx_sql_client_templates)
    implementation(Libs.vertx_mysql_client)
    implementation(Libs.vertx_pg_client)

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    implementation(Libs.vertx_jdbc_client)
    implementation(Libs.agroal_pool)

    // vertx-sql-cleint-templates 에서 @DataObject, @RowMapped 를 위해 사용
    compileOnly(Libs.vertx_codegen)
    kapt(Libs.vertx_codegen)
    kaptTest(Libs.vertx_codegen)

    // MyBatis
    implementation(Libs.mybatis_dynamic_sql)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.jackson_datatype_jdk8)
    implementation(Libs.jackson_datatype_jsr310)

    testImplementation(Libs.h2)
    testImplementation(Libs.mysql_connector_j)

    // Testcontainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
