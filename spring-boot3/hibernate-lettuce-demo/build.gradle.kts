plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

// Hibernate ORM 7.x requires Jakarta Persistence 3.2.0.
// Spring Boot 3.x BOM constrains jakarta.persistence to 3.1.0; override it here.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "jakarta.persistence") {
            useVersion("3.2.0")
            because("Hibernate ORM 7.x requires Jakarta Persistence 3.2.0")
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    implementation(project(":bluetape4k-spring-boot3-hibernate-lettuce"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.micrometer.core)
    runtimeOnly(libs.h2.v2)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":bluetape4k-testcontainers"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
