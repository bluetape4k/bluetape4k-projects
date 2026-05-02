plugins {
    kotlin("plugin.spring")
}

// Hibernate ORM 7.x requires Jakarta Persistence 3.2.0.
// Spring Boot 3.x BOM constrains jakarta.persistence to 3.1.0; override it here.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "jakarta.persistence") {
            useVersion("3.2.0")
            because("Hibernate ORM 7.x requires Jakarta Persistence 3.2.0")
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot autoconfigure (HibernatePropertiesCustomizer 포함) — compileOnly (transitive 오염 방지)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Optional 의존성 (사용자 프로젝트에서 선택적 활성화)
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly(libs.hibernate.core)
    compileOnly(libs.micrometer.core)
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")

    // 직렬화/압축 런타임
    implementation(libs.fory.kotlin)
    implementation(libs.zstd.jni)

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation(libs.micrometer.core)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)
}
