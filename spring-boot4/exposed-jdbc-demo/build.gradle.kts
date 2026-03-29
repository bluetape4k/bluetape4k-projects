plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-exposed-jdbc"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("jdbc"))
    implementation(Libs.jackson_module_kotlin)
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_dao)
    implementation(Libs.exposed_migration_jdbc)
    implementation(Libs.exposed_java_time)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
}
