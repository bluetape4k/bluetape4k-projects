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

dependencies {
    implementation(project(":bluetape4k-hibernate"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(Libs.jakarta_annotation_api)
    implementation(Libs.jakarta_persistence_api)
    implementation(Libs.hibernate_core)

    // QueryDsl
    implementation(Libs.querydsl_jpa + ":jakarta")
    kapt(Libs.querydsl_apt + ":jakarta")
    kaptTest(Libs.querydsl_apt + ":jakarta")

    // Vaidators
    implementation(Libs.hibernate_validator)
    runtimeOnly(Libs.jakarta_validation_api)

    // Spring Boot
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("validation"))
    testImplementation(Libs.springBoot("autoconfigure"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }

    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mysql_connector_j)

    // TestContainers
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mysql)

    // Caching 테스트
    testImplementation(project(":bluetape4k-cache"))
    testImplementation(Libs.hibernate_jcache)
    testImplementation(Libs.caffeine_jcache)
}
