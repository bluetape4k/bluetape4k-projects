plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    implementation(project(":bluetape4k-spring-boot3-exposed-r2dbc"))

    implementation(Libs.exposed_r2dbc)
    implementation(Libs.exposed_java_time)

    runtimeOnly(Libs.r2dbc_h2)
    runtimeOnly(Libs.h2_v2)   // JDBC DataSource (DataInitializer + SchemaUtils에 필요)

    implementation(Libs.springBootStarter("webflux"))
    implementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.springBootStarter("test"))

    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(project(":bluetape4k-junit5"))
}
