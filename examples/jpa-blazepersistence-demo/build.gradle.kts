plugins {
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group) {
            "org.springframework.boot" -> {
                useVersion("4.0.6")
                because("Spring Boot 4 example: prevent global Spring Boot BOM downgrade")
            }
            "org.springframework" -> {
                useVersion("7.0.7")
                because("Spring Framework 7.0.7: Spring Boot 4.0.6 compatible line")
            }
            "org.hibernate.orm"   -> {
                useVersion("7.0.3.Final")
                because("Blaze Persistence 1.6.16 hibernate-7.0 integration is compiled against Hibernate 7.0.3.Final")
            }
            "jakarta.persistence" -> {
                useVersion("3.2.0")
                because("Jakarta Persistence 3.2: Hibernate 7 compatible API")
            }
        }
    }
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    implementation(project(":bluetape4k-hibernate"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.persistence.api.v32)
    implementation(bt4k.hibernate.core)

    implementation(libs.blaze.persistence.core.api.jakarta)
    runtimeOnly(libs.blaze.persistence.core.impl.jakarta)
    implementation(libs.blaze.persistence.entity.view.api.jakarta)
    runtimeOnly(libs.blaze.persistence.entity.view.impl.jakarta)
    implementation(libs.blaze.persistence.jpa.criteria.api.jakarta)
    runtimeOnly(libs.blaze.persistence.jpa.criteria.impl.jakarta)
    runtimeOnly(libs.blaze.persistence.integration.hibernate7)

    implementation(libs.hibernate.validator)
    runtimeOnly(libs.jakarta.validation.api)

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
}
