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
    api(Libs.vertx_core)
    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)
    compileOnly(Libs.vertx_web)
    compileOnly(Libs.vertx_web_client)
    compileOnly(Libs.vertx_junit5)

    // Resilience4j
    api(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_reactor)
    compileOnly(Libs.resilience4j_micrometer)

    // SqlClient
    api(Libs.vertx_sql_client)
    api(Libs.vertx_sql_client_templates)
    implementation(Libs.vertx_mysql_client)
    implementation(Libs.vertx_pg_client)
    compileOnly(Libs.vertx_jdbc_client)
    compileOnly(Libs.agroal_pool)
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)
    implementation(Libs.mybatis_dynamic_sql)

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mysql_connector_j)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
}
