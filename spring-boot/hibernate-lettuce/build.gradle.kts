plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
    implementation(platform(bt4k.spring.boot4.dependencies))

    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot 4: HibernatePropertiesCustomizer가 spring-boot-hibernate 모듈로 이동 — compileOnly
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-hibernate")

    // Optional 의존성
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly(bt4k.hibernate.core)
    compileOnly(libs.micrometer.core)
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.springframework.boot:spring-boot-micrometer-metrics")

    // 직렬화/압축 런타임
    implementation(bt4k.fory.kotlin)
    implementation(bt4k.zstd.jni)

    // Test
    testImplementation(project(":bluetape4k-spring-boot-core"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation(libs.micrometer.core)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(bt4k.h2.v2)
    testImplementation(bt4k.hikaricp)
}
