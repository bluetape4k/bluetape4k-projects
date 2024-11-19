plugins {
    idea
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
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
    showProcessorStats = true
    // kapt 가 제대로 동작하지 않는 경우, 아래 옵션을 제거해보세요.
    correctErrorTypes = true
}

idea {
    module {
        val kaptMain = file("build/generated/source/kapt/main")
        sourceDirs.plus(kaptMain)
        generatedSourceDirs.plus(kaptMain)

        val kaptTest = file("build/generated/source/kapt/test")
        testSources.plus(kaptTest)
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-hibernate"))
    testImplementation(project(":bluetape4k-junit5"))

    // NOTE: Java 9+ 환경에서 kapt가 제대로 동작하려면 javax.annotation-api 를 참조해야 합니다.
    // api(Libs.javax_annotation_api)
    api(Libs.jakarta_annotation_api)

    // api(Libs.javax_persistence_api)
    api(Libs.jakarta_persistence_api)
    api(Libs.hibernate_core)
    testImplementation(Libs.hibernate_testing)

    // NOTE: Querydsl 과 같이 사용할 시에 가끔 예외를 일으킨다. 
    // kapt(Libs.hibernate_jpamodelgen)
    // kaptTest(Libs.hibernate_jpamodelgen)

    // Querydsl
    // Hibernate 6+ jakarta 용은 claasifier로 ":jpa" 대신 ":jakarta" 를 사용해야 합니다.
    // https://github.com/querydsl/querydsl/issues/3493
    api(Libs.querydsl_jpa + ":jakarta")
    kapt(Libs.querydsl_apt + ":jakarta")
    kaptTest(Libs.querydsl_apt + ":jakarta")

    api(Libs.jakarta_el_api)

    // Validator
    // api(Libs.javax_validation_api)
    api(Libs.jakarta_validation_api)
    api(Libs.hibernate_validator)

    // Converter
    compileOnly(project(":bluetape4k-crypto"))
    compileOnly(project(":bluetape4k-jackson"))

    testImplementation(Libs.kryo)
    testImplementation(Libs.fury)

    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    // TODO: querydsl-kotlin-codegen 은 tree entity 도 못 만들고, spring-data-jpa 의 repository에서 문제가 생긴다.
    // https://github.com/querydsl/querydsl/issues/3454
    // kapt(Libs.querydsl_kotlin_codegen)
    // kaptTest(Libs.querydsl_kotlin_codegen)

    api(Libs.springBootStarter("data-jpa"))
    testImplementation(Libs.springBoot("autoconfigure"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testImplementation(Libs.hikaricp)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.mysql_connector_j)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mysql)

    // Caching 테스트
    testImplementation(project(":bluetape4k-cache"))
    testImplementation(Libs.springBootStarter("cache"))
    testImplementation(Libs.caffeine)
    testImplementation(Libs.caffeine_jcache)

    // JDBC 와 같이 사용
    testImplementation(project(":bluetape4k-jdbc"))
}
