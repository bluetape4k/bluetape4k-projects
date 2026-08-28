import org.gradle.api.tasks.testing.Test

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    testImplementation(project(":bluetape4k-junit5"))
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("tenant-retention-stress")
    }
}

tasks.register<Test>("tenantRetentionStress") {
    description = "Runs TenantContext platform/virtual-thread retention stress tests."
    group = "verification"
    maxHeapSize = "256m"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("tenant-retention-stress")
    }
    systemProperty(
        "tenant.retention.reportDir",
        layout.buildDirectory.dir("reports/tenant-retention").get().asFile.absolutePath,
    )
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    shouldRunAfter(tasks.test)
}
