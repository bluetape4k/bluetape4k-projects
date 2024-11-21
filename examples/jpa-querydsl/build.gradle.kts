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
    // showProcessorStats = true
    // kapt 가 제대로 동작하지 않는 경우, 해당 클래스를 약간 수정해보세요. (Comments 추가 등으로)
    // correctErrorTypes = true
}

// NOTE: implementation 로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(project(":bluetape4k-hibernate"))
    testImplementation(project(":bluetape4k-junit5"))

    // NOTE: Java 9+ 환경에서 kapt가 제대로 동작하려면 javax.annotation-api 를 참조해야 합니다.
    // api(Libs.javax_annotation_api)
    api(Libs.jakarta_annotation_api)

    // api(Libs.javax_persistence_api)
    api(Libs.jakarta_persistence_api)
    api(Libs.hibernate_core)

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
