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
    implementation(platform(Libs.spring_boot3_dependencies))
    implementation(project(":bluetape4k-spring-boot3-hibernate-lettuce"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))
    implementation(Libs.micrometer_core)
    runtimeOnly(Libs.h2_v2)

    testImplementation(Libs.springBootStarter("test"))
    testImplementation(project(":bluetape4k-testcontainers"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
