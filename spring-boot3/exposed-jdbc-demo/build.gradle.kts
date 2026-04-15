plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    implementation(project(":bluetape4k-spring-boot3-exposed-jdbc"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.exposed_spring_boot_starter)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_java_time)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
}
