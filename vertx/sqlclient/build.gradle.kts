configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-jdbc"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))

    // Vertx
    api(project(":bluetape4k-vertx-core"))
    compileOnly(Libs.vertx_junit5)

    // Vertx Kotlin
    api(Libs.vertx_core)
    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)

    // Vertx SqlClient
    api(Libs.vertx_sql_client)
    api(Libs.vertx_sql_client_templates)
    implementation(Libs.vertx_mysql_client)
    implementation(Libs.vertx_pg_client)

    // Vertx Jdbc (MySQL, Postgres 를 제외한 H2 같은 것은 기존 JDBC 를 Wrapping한 것을 사용합니다)
    compileOnly(Libs.vertx_jdbc_client)
    compileOnly(Libs.agroal_pool)

    // MyBatis
    implementation(Libs.mybatis_dynamic_sql)

    // Vetx SqlClient Templates 에서 Jackson Databind 를 이용한 매핑을 사용한다
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mysql_connector_j)

    // Testcontainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
