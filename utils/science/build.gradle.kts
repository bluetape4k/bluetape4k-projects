configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    // javax.media:jai_core 는 Maven Central에 없으므로 모든 classpath에서 제외
    all {
        exclude(group = "javax.media", module = "jai_core")
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

    // NetCDF (UCAR — compileOnly)
    // TODO: edu.ucar:netcdfAll 아티팩트 좌표 확인 필요 (Unidata Maven 저장소 재구성됨)
    // Phase 4 (NetCDF 구현) 시작 전에 정확한 버전/저장소 확인
    // compileOnly(Libs.ucar_netcdf)

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
