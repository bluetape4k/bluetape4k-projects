configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    // javax.media:jai_core 는 Maven Central에 없으므로 모든 classpath에서 제외
    all {
        exclude(group = "javax.media", module = "jai_core")
    }
}

// JUnit 5 태그 필터링 — CI 기본은 slow-netcdf 제외, nightly 는 -PincludeTags 로 활성화
// include 가 명시되면 default exclude 적용 안 함 (충돌 방지 — Codex Plan v2.1 Critical#1)
tasks.test {
    useJUnitPlatform {
        val include = (project.findProperty("includeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val excludeProp = (project.findProperty("excludeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        val exclude = when {
            excludeProp != null -> excludeProp
            include.isNotEmpty() -> emptyList()
            else -> listOf("slow-netcdf")
        }
        include.forEach { includeTags(it) }
        exclude.forEach { excludeTags(it) }
    }
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-logging"))
    api(Libs.jts_core)

    // GIS / 좌표 변환 (LGPL — compileOnly로만 선언, JAI 제외)
    compileOnly(Libs.proj4j)
    compileOnly(Libs.proj4j_epsg)
    compileOnly(Libs.esri_geometry_api)

    // GeoTools (LGPL — compileOnly, javax.media:jai_core 제외 설정됨)
    compileOnly(Libs.geotools_shapefile)
    compileOnly(Libs.geotools_referencing)
    compileOnly(Libs.geotools_epsg_hsql)

    // NetCDF (UCAR netCDF-Java 5.9.1 — compileOnly, BSD-3-Clause)
    // 저장소: 루트 build.gradle.kts:71 에 Unidata Nexus 선언됨
    compileOnly(Libs.ucar_cdm_core)
    compileOnly(Libs.ucar_netcdf4)
    // cdm-core 가 ImmutableList<Variable> 등 Guava 컬렉션을 API 표면에 노출 → 컴파일 시 필요
    compileOnly(Libs.guava)

    // Micrometer — MeterRegistry 선택 주입 (compileOnly)
    compileOnly(Libs.micrometer_core)

    // Coroutines (compileOnly)
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    // Exposed / DB (선택적, compileOnly)
    compileOnly(project(":bluetape4k-exposed-jdbc"))
    compileOnly(project(":bluetape4k-exposed-postgresql"))
    compileOnly(project(":bluetape4k-exposed-jackson3"))
    compileOnly(Libs.postgis_jdbc)
    compileOnly(Libs.exposed_java_time)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(project(":bluetape4k-exposed-jdbc"))
    testImplementation(project(":bluetape4k-exposed-postgresql"))
    testImplementation(project(":bluetape4k-exposed-jackson3"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_postgresql)

    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.hikaricp)
}
