plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

// 전역 dependencyManagement가 spring_boot3_dependencies BOM을 임포트하여 Spring Boot 4 / Spring Framework 7 /
// Hibernate 7 / Jakarta EE 11 아티팩트를 구버전으로 다운그레이드한다.
// 테스트 설정에서 SB4 호환 버전을 강제하여 통합 테스트가 올바른 classpath로 실행되게 한다.
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group) {
            "org.springframework.boot" -> {
                useVersion("4.0.3")
                because("Spring Boot 4 통합 테스트: global SB3 BOM 다운그레이드 방지")
            }
            "org.springframework" -> {
                useVersion("7.0.5")
                because("Spring Framework 7: SB4 4.0.3 호환 버전 강제")
            }
            "org.hibernate.orm" -> {
                useVersion("7.2.4.Final")
                because("Hibernate 7: SB4 4.0.3 호환 버전 강제")
            }
            "jakarta.persistence" -> {
                useVersion("3.2.0")
                because("Jakarta Persistence 3.2: Hibernate 7 / SB4 호환 버전 강제")
            }
            "org.springframework.data" -> {
                useVersion("4.0.3")
                because("Spring Data 4.0.3: SB4 4.0.3 호환 버전 강제 (ListenableFuture 제거 버전)")
            }
        }
    }
}

dependencies {
    // Spring Boot 4 BOM: platform() 방식 필수 (dependencyManagement 사용 금지 - KGP 2.3 충돌)
    implementation(platform(Libs.spring_boot4_dependencies))

    // 핵심: Hibernate 2nd Level Cache Lettuce 구현체
    api(project(":bluetape4k-hibernate-cache-lettuce"))

    // Spring Boot 4: HibernatePropertiesCustomizer가 spring-boot-hibernate 모듈로 이동 — compileOnly
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("hibernate"))

    // Optional 의존성
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
