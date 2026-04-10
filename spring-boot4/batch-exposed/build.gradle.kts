plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

// 루트 build.gradle.kts의 dependencyManagement 플러그인이 spring_boot3_dependencies BOM을 전체 subproject에
// 적용하므로 spring-boot 4.x 아티팩트가 3.x 로 다운그레이드된다.
// configurations.all 은 Kotlin 내부 설정(kotlinBuildToolsApiClasspath 등)에도 영향을 주어 컴파일 오류를 유발하므로,
// 실제 컴파일/런타임 classpath 설정만 대상으로 버전을 강제한다.
listOf(
    "compileClasspath", "runtimeClasspath",
    "testCompileClasspath", "testRuntimeClasspath",
).forEach { configName ->
    configurations.matching { it.name == configName }.configureEach {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.springframework.boot" -> {
                    useVersion("4.0.5")
                    because("spring-boot4 모듈: root BOM의 spring-boot3.x 다운그레이드 방지")
                }
                "org.springframework" -> {
                    // spring-batch 6.x 가 요구하는 Spring Framework 7.x 보장
                    if (requested.name.startsWith("spring-") &&
                        !requested.name.contains("security") &&
                        !requested.name.contains("data")
                    ) {
                        useVersion("7.0.6")
                        because("spring-boot4 모듈: root BOM의 spring-framework 6.x 다운그레이드 방지")
                    }
                }
                "org.springframework.batch" -> {
                    useVersion("6.0.3")
                    because("spring-boot4 모듈: root BOM의 spring-batch 5.x 다운그레이드 방지")
                }
            }
        }
    }
}

dependencies {
    // Spring Boot 4 BOM: platform()을 사용하면 compileClasspath/runtimeClasspath에만 적용되고
    // kotlinBuildToolsApiClasspath 같은 내부 Gradle 설정에는 영향을 주지 않음
    // (dependencyManagement 플러그인은 ALL configurations에 적용되어 kotlin-stdlib 버전 충돌 유발)
    implementation(platform(Libs.spring_boot4_dependencies))

    // Core
    api(Libs.kotlin_reflect)
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-virtualthread-api"))

    // Exposed
    api(Libs.exposed_spring7_transaction)
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)

    // Spring Batch (Spring Boot 4 BOM 버전 관리)
    api(Libs.springBoot4Starter("batch"))
    compileOnly(Libs.springBoot4("autoconfigure"))

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    // exposed-spring-boot-starter (spring-boot3 전이 의존성) 제외:
    // bluetape4k-exposed-jdbc-tests → exposed-spring-boot-starter → spring-boot:3.x BOM 이 spring-boot 4.x 를 다운그레이드 유발
    testImplementation(project(":bluetape4k-exposed-jdbc-tests")) {
        exclude(group = "org.jetbrains.exposed", module = "exposed-spring-boot-starter")
    }
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))
    testImplementation(Libs.springBoot4Starter("test"))
    testImplementation(Libs.springBoot4Starter("jdbc"))  // DataSource auto-configuration (Spring Boot 4 분리 모듈)
    testImplementation(Libs.spring_batch_test)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.mysql_connector_j)
}
