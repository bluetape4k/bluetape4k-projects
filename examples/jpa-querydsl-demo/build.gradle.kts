plugins {
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

// JPA Entities 들을 Java와 같이 모두 override 가능하게 합니다 (Kotlin 은 기본이 final 입니다)
// 이렇게 해야 association의 proxy 가 만들어집니다.
// https://kotlinlang.org/docs/reference/compiler-plugins.html
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

kapt {
    correctErrorTypes = true
    showProcessorStats = true

    arguments {
        arg("querydsl.entityAccessors", "true")  // Association의 property는 getter/setter를 사용하도록 합니다.
        arg("querydsl.kotlinCodegen", "true") // QueryDSL Kotlin Codegen 활성화
    }
    javacOptions {
        option("--add-modules", "java.base")
    }
}

// NOTE: implementation 로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

// 전역 dependencyManagement가 spring_boot_dependencies BOM을 임포트하여 Spring Boot 4/SF7/H7 아티팩트를
// 구버전으로 다운그레이드한다. 테스트 설정에서 Spring Boot 4 호환 버전을 강제한다.
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group) {
            "org.springframework.boot" -> {
                useVersion("4.0.6")
                because("Spring Boot 4 테스트: global Spring Boot BOM 다운그레이드 방지")
            }
            "org.springframework" -> {
                useVersion("7.0.7")
                because("Spring Framework 7.0.7: Spring Boot 4 4.0.6 호환 버전 강제")
            }
            "org.hibernate.orm" -> {
                useVersion("7.2.7.Final")
                because("Hibernate 7.2.7.Final: Spring Boot 4 4.0.6 호환 버전 강제")
            }
            "jakarta.persistence" -> {
                useVersion("3.2.0")
                because("Jakarta Persistence 3.2: Hibernate 7 / Spring Boot 4 호환 버전 강제")
            }
        }
    }
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    implementation(project(":bluetape4k-hibernate"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.persistence.api)
    implementation(bt4k.hibernate.core)

    // QueryDsl
    implementation(bt4k.querydsl.jpa)
    kapt(variantOf(bt4k.querydsl.apt) { classifier("jakarta") })
    kaptTest(variantOf(bt4k.querydsl.apt) { classifier("jakarta") })
    kapt(libs.jakarta.persistence.api)

    // Vaidators
    implementation(libs.hibernate.validator)
    runtimeOnly(libs.jakarta.validation.api)

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    testImplementation(bt4k.hikaricp)
    testImplementation(libs.h2.v2)
    testImplementation(bt4k.mysql.connector.j)

    // TestContainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mysql)

    // Caching 테스트
    testImplementation(project(":bluetape4k-cache-core"))
    testImplementation(bt4k.hibernate.jcache)
    testImplementation(libs.caffeine.jcache)
}
