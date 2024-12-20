@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(Libs.exposed_core)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_kotlin_datetime)
    implementation(Libs.exposed_spring_boot_starter)
    implementation(Libs.exposed_spring_transaction)

    // Entity ID generators
    api(project(":bluetape4k-idgenerators"))
    api(Libs.java_uuid_generator)

    // Bluetape4k
    implementation(project(":bluetape4k-jdbc"))
    implementation(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_postgresql)

    // Database Drivers
    compileOnly(Libs.hikaricp)

    // H2
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.mysql_connector_j)

    // Spring Boot
    testImplementation(Libs.springBootStarter("jdbc"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)
}
