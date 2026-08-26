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

    arguments {
        arg("querydsl.entityAccessors", "true")  // Association의 property는 getter/setter를 사용하도록 합니다.
        arg("querydsl.kotlinCodegen", "true") // QueryDSL Kotlin Codegen 활성화
    }
    javacOptions {
        option("--add-modules", "java.base")
    }
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
            "org.hibernate.orm"   -> {
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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    create("testJar")
}

val consumerRuntimeTest = sourceSets.create("consumerRuntimeTest") {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

val consumerRuntimeTestImplementation = configurations.getByName("consumerRuntimeTestImplementation")

tasks.register<Test>("consumerRuntimeTest") {
    description = "Runs Hibernate converter smoke tests with the published runtime classpath."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    testClassesDirs = consumerRuntimeTest.output.classesDirs
    classpath = consumerRuntimeTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("consumerRuntimeTest")
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
    implementation(platform(bt4k.spring.boot4.dependencies))
    testImplementation(platform(bt4k.junit.bom))

    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(bt4k.jakarta.persistence.v32)
    kapt(bt4k.jakarta.persistence.v32)
    api(bt4k.jakarta.transaction.api)

    api(bt4k.hibernate.core)
    api(libs.hibernate.micrometer)

    // NOTE: Kotlin 2.1.0 에서 QueryDSL 5.1.0 과 같이 사용하는 경우 예에가 발생한다. (QueryDSL만 사용하는 것을 추천합니다)
    // kapt(libs.hibernate.jpamodelgen)
    // kaptTest(libs.hibernate.jpamodelgen)

    // Querydsl
    // QueryDSL 7.x publishes Jakarta JPA support in the main querydsl-jpa artifact.
    api(bt4k.querydsl.jpa)
    kapt(variantOf(bt4k.querydsl.apt) { classifier("jakarta") })
    kaptTest(variantOf(bt4k.querydsl.apt) { classifier("jakarta") })

    // Validator
    api(bt4k.jakarta.el.api)
    api(bt4k.jakarta.validation.api)
    api(bt4k.hibernate.validator)
    testImplementation(bt4k.glassfish.expressly)  // Jakarta EL 6.0 implementation for Hibernate Validator 9.x

    // Converter
    // compileOnly(project(":bluetape4k-crypto"))
    api(project(":bluetape4k-tink"))
    api(project(":bluetape4k-jackson3"))

    runtimeOnly(bt4k.kryo)
    runtimeOnly(bt4k.fory.kotlin)  // new Apache Fory

    runtimeOnly(bt4k.commons.compress)
    runtimeOnly(bt4k.snappy.java)
    runtimeOnly(bt4k.at.yawk.lz4.java)
    runtimeOnly(bt4k.zstd.jni)

    api(project(":bluetape4k-idgenerators"))
    api(bt4k.java.uuid.generator)

    // querydsl-kotlin-codegen 후보는 clean :bluetape4k-hibernate:compileKotlin에서
    // AnnotationProcessingError caused by NPE
    // (ExtensionsKt.asTypeName -> KotlinEntitySerializer.introClassHeader)로 실패한다.
    // 따라서 Java APT 경로만 유지하고, 후보 설정은 재현 가능한 대체 경로로 남긴다.
    // https://github.com/querydsl/querydsl/issues/3454
    // kapt(libs.querydsl.kotlin.codegen)
    // kaptTest(libs.querydsl.kotlin.codegen)

    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-hibernate")  // Spring Boot 4: HibernatePropertiesCustomizer 이동된 모듈
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testImplementation(bt4k.hikaricp)
    testRuntimeOnly(bt4k.h2.v2)
    testRuntimeOnly(bt4k.mysql.connector.j)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mysql)

    // Caching 테스트
    testImplementation(project(":bluetape4k-cache-core"))
    testImplementation(bt4k.hibernate.jcache)
    testImplementation(bt4k.caffeine.jcache)

    // JDBC 와 같이 사용
    testImplementation(project(":bluetape4k-jdbc"))

    consumerRuntimeTestImplementation(project(":bluetape4k-junit5"))
}
