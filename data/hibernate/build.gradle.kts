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

idea {
    module {
        val kaptMain = file("build/generated/source/kapt/main")
        sourceDirs.plus(kaptMain)
        generatedSourceDirs.plus(kaptMain)

        val kaptTest = file("build/generated/source/kapt/test")
        testSources.plus(kaptTest)
    }
}

kapt {
    correctErrorTypes = true
    showProcessorStats = true

//    arguments {
//        arg("querydsl.entityAccessors", "true")  // Association의 property는 getter/setter를 사용하도록 합니다.
//        arg("querydsl.kotlinCodegen", "true") // QueryDSL Kotlin Codegen 활성화
//    }
    javacOptions {
        option("--add-modules", "java.base")
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    create("testJar")
}


// 테스트 코드를 Jar로 만들어서 다른 프로젝트에서 참조할 수 있도록 합니다.
tasks.register<Jar>("testJar") {
    dependsOn(tasks.testClasses)
    archiveClassifier.set("test")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testJar", tasks["testJar"])
}

dependencies {
    testImplementation(platform(Libs.junit_bom))
    
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.hibernate_core)
    api(Libs.hibernate_micrometer)

    // NOTE: Kotlin 2.1.0 에서 QueryDSL 5.1.0 과 같이 사용하는 경우 예에가 발생한다. (QueryDSL만 사용하는 것을 추천합니다)
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
    api(Libs.jakarta_validation_api)
    api(Libs.hibernate_validator)

    // Converter
    compileOnly(project(":bluetape4k-crypto"))
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    testImplementation(Libs.kryo)
    testImplementation(Libs.fury_kotlin)

    testImplementation(Libs.commons_compress)
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.zstd_jni)

    compileOnly(project(":bluetape4k-idgenerators"))

    // TODO: querydsl-kotlin-codegen 은 tree entity 도 못 만들고, spring-data-jpa 의 repository에서 문제가 생긴다.
    // https://github.com/querydsl/querydsl/issues/3454
    // kapt(Libs.querydsl_kotlin_codegen)
    // kaptTest(Libs.querydsl_kotlin_codegen)

    compileOnly(Libs.springBootStarter("data-jpa"))
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
    testImplementation(Libs.hibernate_jcache)
    testImplementation(Libs.caffeine_jcache)

    // JDBC 와 같이 사용
    testImplementation(project(":bluetape4k-jdbc"))
}
