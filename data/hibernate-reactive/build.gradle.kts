plugins {
//    idea
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
    kotlin("kapt")
    alias(bt4k.plugins.kover)
}

// suspend inline fun + crossinline 람다는 Vert.x dispatcher 컨텍스트에서 실행되어
// Kover가 외부 래핑 람다 라인을 추적하지 못한다 (알려진 한계).
// SessionFactorySupport.kt(mutiny/stage) 두 파일을 커버리지 측정에서 제외한다.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "io.bluetape4k.hibernate.reactive.mutiny.SessionFactorySupportKt*",
                    "io.bluetape4k.hibernate.reactive.stage.SessionFactorySupportKt*",
                )
            }
        }
    }
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
}

// Hibernate ORM 7.x / Reactive 4.x requires Jakarta Persistence 3.2.0
// Spring Boot BOM manages older Netty lines but Vert.x 5 requires Netty 4.2.x.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "jakarta.persistence") {
            useVersion("3.2.0")
            because("Hibernate ORM 7.x requires Jakarta Persistence 3.2.0")
        }
        if (requested.group == "io.netty" && !requested.name.startsWith("netty-tcnative")) {
            useVersion("4.2.15.Final")
            because("Vert.x 5 requires Netty 4.2.x; netty-tcnative remains on its own 2.0.x line")
        }
    }
}

// NOTE: implementation 로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    kaptTest {
        exclude(group = "org.hibernate.orm", module = "hibernate-jpamodelgen")
        exclude(group = "org.hibernate.orm", module = "hibernate-processor")
    }
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    constraints {
        // netty-tcnative is published on the 2.0.x line; Spring Boot 4.1 currently constrains it to Netty core 4.2.x.
        implementation("io.netty:netty-tcnative-classes") {
            version {
                strictly(libs.versions.netty.tcnative.get())
            }
        }
    }

    api(project(":bluetape4k-hibernate"))
    api(project(":bluetape4k-mutiny"))
    api(project(":bluetape4k-vertx"))

    // NOTE: Java 9+ 환경에서 kapt가 제대로 동작하려면 javax.annotation-api 를 참조해야 합니다.
    kapt(libs.jakarta.annotation.api)

    api(libs.hibernate.reactive.core)

    // hibernate-reactive 는 querydsl 을 사용하지 못한다. 대신 jpamodelgen 을 사용합니다.
    kapt(libs.hibernate.jpamodelgen)

    api(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)
    testImplementation(libs.glassfish.expressly)  // Jakarta EL implementation for Hibernate Validator

    api(libs.mutiny.kotlin)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactive)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(project(":bluetape4k-junit5"))

    // bluetape4k-data-hibernate 의 테스트용 엔티티를 사용하기 위해 추가합니다
    testImplementation(project(path = ":bluetape4k-hibernate", configuration = "testJar"))

    // Converter 사용 시
    // compileOnly(project(":bluetape4k-crypto"))
    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-jackson3"))

    testImplementation(libs.kryo)
    testImplementation(bt4k.fory.kotlin)  // new Apache Fory

    testImplementation(libs.lz4.java)
    testImplementation(libs.snappy.java)
    testImplementation(bt4k.zstd.jni)

    testImplementation(project(":bluetape4k-idgenerators"))

    // Caching 테스트
    compileOnly(project(":bluetape4k-cache-core"))
    testImplementation("org.springframework.boot:spring-boot-starter-cache")
    testImplementation(libs.caffeine)
    testImplementation(libs.caffeine.jcache)

    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.vertx.mysql.client) // MySQL
    // Testcontainers MySQL 에서 검증을 위해 사용하기 위해 불가피하게 필요합니다
    // reactive 방식에서는 항상 verx-mysql-client 를 사용합니다
    testImplementation(bt4k.hikaricp)
    testImplementation(bt4k.mysql.connector.j)

    // bluetape4k-data-hibernate의 entity 들을 재사용하려고 testArchives 를 참조한다
    // persistence.xml 에서도 jar-file에 entity path를 추가해야 한다
    // see : https://github.com/hauner/gradle-plugins/tree/master/jartest
    // testImplementation(project(path = ":bluetape4k-data-hibernate", configuration = "testArchives"))

    // LifecycleEntity가 spring-data-jpa 의 @AuditingEntityListener를 사용해서 어쩔 수 없이 추가했다.
    // 실제로 사용 안한다
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
