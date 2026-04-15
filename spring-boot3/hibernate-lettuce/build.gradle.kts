plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot autoconfigure (HibernatePropertiesCustomizer 포함) — compileOnly (transitive 오염 방지)
    compileOnly(Libs.springBoot("autoconfigure"))

    // Optional 의존성 (사용자 프로젝트에서 선택적 활성화)
    compileOnly(Libs.springBootStarter("data-jpa"))
    compileOnly(Libs.hibernate_core)
    compileOnly(Libs.micrometer_core)
    compileOnly(Libs.springBootStarter("actuator"))

    // 직렬화/압축 런타임
    implementation(Libs.fory_kotlin)
    implementation(Libs.zstd_jni)

    // Test
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.springBootStarter("data-jpa"))
    testImplementation(Libs.springBootStarter("actuator"))
    testImplementation(Libs.micrometer_core)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
}
