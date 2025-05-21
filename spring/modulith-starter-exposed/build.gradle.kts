plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_modulith_bom))
    implementation(platform(Libs.exposed_bom))

    // Spring Modulith
    api(project(":bluetape4k-spring-modulith-events-exposed"))
    api(Libs.spring_modulith_starter_core)
    api(Libs.spring_modulith_events_api)
    compileOnly(Libs.spring_modulith_events_core)
    compileOnly(Libs.spring_modulith_events_jackson)


}
