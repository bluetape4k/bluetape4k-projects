plugins {
//    idea
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
        arg("querydsl.entityAccessors", "true")
        arg("querydsl.kotlinCodegen", "true") // QueryDSL Kotlin Codegen 활성화
    }
}

//idea {
//    module {
//        val kaptMain = file("build/generated/source/kapt/main")
//        sourceDirs.plus(kaptMain)
//        generatedSourceDirs.plus(kaptMain)
//
//        val kaptTest = file("build/generated/source/kapt/test")
//        testSources.plus(kaptTest)
//    }
//}

// NOTE: implementation 로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-hibernate"))
    api(project(":bluetape4k-mutiny"))
    api(project(":bluetape4k-vertx-core"))

    // NOTE: Java 9+ 환경에서 kapt가 제대로 동작하려면 javax.annotation-api 를 참조해야 합니다.
    api(Libs.jakarta_annotation_api)

    api(Libs.jakarta_persistence_api)
    api(Libs.hibernate_reactive_core)

    // hibernate-reactive 는 querydsl 을 사용하지 못한다. 대신 jpamodelgen 을 사용합니다.
    // hibernate 6.3.0+ 는 hibernate-jpamodelgen 예서 예외가 발생합니다. (6.2.x 를 사용하세요)
    kapt(Libs.hibernate_jpamodelgen)
    kaptTest(Libs.hibernate_jpamodelgen)

    api(Libs.jakarta_validation_api)
    implementation(Libs.hibernate_validator)

    api(Libs.mutiny_kotlin)
    api(Libs.kotlinx_coroutines_jdk8)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(project(":bluetape4k-junit5"))

    // bluetape4k-data-hibernate 의 테스트용 엔티티를 사용하기 위해 추가합니다
    testImplementation(project(path = ":bluetape4k-hibernate", configuration = "testJar"))

    // Converter 때문에
    compileOnly(project(":bluetape4k-crypto"))
    testImplementation(project(":bluetape4k-jackson"))

    testImplementation(Libs.kryo)
    testImplementation(Libs.fury)

    testImplementation(Libs.lz4_java)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.zstd_jni)

    testImplementation(project(":bluetape4k-idgenerators"))

    // Caching 테스트
    compileOnly(project(":bluetape4k-cache"))
    testImplementation(Libs.springBootStarter("cache"))
    testImplementation(Libs.caffeine)
    testImplementation(Libs.caffeine_jcache)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.vertx_mysql_client) // MySQL
    // Testcontainers MySQL 에서 검증을 위해 사용하기 위해 불가피하게 필요합니다
    // reactive 방식에서는 항상 verx-mysql-client 를 사용합니다
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.mysql_connector_j)

    // bluetape4k-data-hibernate의 entity 들을 재사용하려고 testArchives 를 참조한다
    // persistence.xml 에서도 jar-file에 entity path를 추가해야 한다
    // see : https://github.com/hauner/gradle-plugins/tree/master/jartest
    // testImplementation(project(path = ":bluetape4k-data-hibernate", configuration = "testArchives"))

    // LifecycleEntity가 spring-data-jpa 의 @AuditingEntityListener를 사용해서 어쩔 수 없이 추가했다.
    // 실제로 사용 안한다
    testImplementation(Libs.springBootStarter("data-jpa"))
}
