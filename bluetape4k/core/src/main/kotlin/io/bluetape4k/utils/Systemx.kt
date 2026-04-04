package io.bluetape4k.utils

import java.util.*

/**
 * JVM 시스템 속성(System Property)과 환경 변수(Environment Variable)를 편리하게 조회하는 유틸리티 객체입니다.
 *
 * Java 버전, OS 정보, 사용자 정보, 파일 경로 등의 시스템 정보를 lazy 프로퍼티로 제공하며,
 * 시스템 속성 키를 문자열 상수로 정의하여 오타 없이 사용할 수 있습니다.
 *
 * ```kotlin
 * // OS 및 Java 버전 확인
 * println(Systemx.osName)           // 예: "Mac OS X"
 * println(Systemx.javaVersion)      // 예: "21"
 * println(Systemx.availableProcessors) // 예: 8
 *
 * // 시스템 속성 조회
 * val userDir = Systemx.getProp(Systemx.USER_DIR)  // 현재 디렉토리
 *
 * // 환경 변수 조회
 * val home = Systemx.getenv("HOME")  // /Users/username
 *
 * // OS 타입 분기
 * if (Systemx.isMac) {
 *     println("macOS에서 실행 중")
 * } else if (Systemx.isUnix) {
 *     println("Unix/Linux에서 실행 중")
 * }
 * ```
 */
object Systemx {

    const val USER_DIR = "user.dir"
    const val USER_NAME = "user.name"
    const val USER_HOME = "user.home"
    const val JAVA_HOME = "java.home"
    const val TEMP_DIR = "java.io.tmpdir"
    const val OS_NAME = "os.name"
    const val OS_VERSION = "os.version"
    const val JAVA_VERSION = "java.version"
    @Deprecated(
        message = "Use JAVA_CLASS_VERSION",
        replaceWith = ReplaceWith("JAVA_CLASS_VERSION"),
    )
    const val JAVA_CLASS_VERION = "java.class.version"

    const val JAVA_CLASS_VERSION = "java.class.version"
    const val JAVA_SPECIFICATION_VERSION = "java.specification.version"
    const val JAVA_VENDOR = "java.vendor"
    const val JAVA_CLASSPATH = "java.class.path"
    const val PATH_SEPARATOR = "path.separator"
    const val HTTP_PROXY_HOST = "http.proxyHost"
    const val HTTP_PROXY_PORT = "http.proxyPort"
    const val HTTP_PROXY_USER = "http.proxyUser"
    const val HTTP_PROXY_PASSWORD = "http.proxyPassword"
    const val FILE_ENCODING = "file.encoding"
    const val SUN_BOOT_CLASS_PATH = "sun.boot.class.path"

    private fun systemProperty(name: String): String? = System.getProperty(name)

    /** Runtime package */
    val runtimePackage: Package by lazy { Runtime::class.java.`package` }

    /** System Properties */
    val systemProps: Properties by lazy { System.getProperties() }

    /** CPU Core count */
    @Deprecated(
        message = "Use availableProcessors",
        replaceWith = ReplaceWith("availableProcessors"),
    )
    val processCount: Int by lazy { Runtime.getRuntime().availableProcessors() }

    /** CPU Core count */
    val availableProcessors: Int by lazy { Runtime.getRuntime().availableProcessors() }

    /** JVM Compipler 정보 */
    val javaCompiler: String? by lazy { systemProperty("java.compiler") }

    /** JVM 버전 */
    val javaVersion: String? by lazy {
        systemProperty(JAVA_SPECIFICATION_VERSION) ?: runtimePackage.specificationVersion
    }

    /** JVM 구현 버전 */
    val javaImplementationVersion: String? by lazy { runtimePackage.implementationVersion }

    /** JVM 벤더 */
    val javaVendor: String? by lazy { runtimePackage.specificationVendor }

    /** JVM 벤더 URL */
    val javaVendorUrl: String? by lazy { systemProperty("java.vendor.url") }

    /** JVM 구현 벤더  */
    val javaImplementationVendor: String? by lazy { runtimePackage.implementationVendor }

    /** JVM 구현 벤더 URL */
    val javaClassVersion: String? by lazy { systemProperty(JAVA_CLASS_VERION) }

    /** JVM 라이브러리 경로 */
    val javaLibraryPath: String? by lazy { systemProperty("java.library.path") }

    /** JVM 런타임 명 */
    val javaRuntimeName: String? by lazy { systemProperty("java.runtime.name") }

    /** JVM 런타임 버전 */
    val javaRuntimeVersion: String? by lazy { systemProperty("java.runtime.version") }

    /** JVM 사양 이름 */
    val javaSpecificationName: String? by lazy { systemProperty("java.specification.name") }

    /** JVM 사양 벤더 명 */
    val javaSpecificationVendor: String? by lazy { systemProperty("java.specification.vendor") }

    private val JavaFeatureVersion: Int? by lazy {
        val raw = javaVersion ?: return@lazy null
        if (raw.startsWith("1.")) raw.removePrefix("1.").substringBefore('.').toIntOrNull()
        else raw.substringBefore('.').toIntOrNull()
    }

    /** Java 1.6 인가? */
    @Deprecated(message = "Java 21+ 환경에서는 항상 false입니다.")
    val isJava6: Boolean by lazy { JavaFeatureVersion == 6 }

    /** Java 1.7 인가? */
    @Deprecated(message = "Java 21+ 환경에서는 항상 false입니다.")
    val isJava7: Boolean by lazy { JavaFeatureVersion == 7 }

    /** Java 1.8 인가? */
    @Deprecated(message = "Java 21+ 환경에서는 항상 false입니다.")
    val isJava8: Boolean by lazy { JavaFeatureVersion == 8 }

    /** Java 9 인가? */
    @Deprecated(message = "Java 21+ 환경에서는 항상 false입니다.")
    val isJava9: Boolean by lazy { JavaFeatureVersion == 9 }

    /** Java 10 인가? */
    @Deprecated(message = "Java 21+ 환경에서는 항상 false입니다.")
    val isJava10: Boolean by lazy { JavaFeatureVersion == 10 }

    /** Java 11 인가? */
    val isJava11: Boolean by lazy { JavaFeatureVersion == 11 }

    /** Java 17 인가? */
    val isJava17: Boolean by lazy { JavaFeatureVersion == 17 }

    /** Java 19 인가? */
    val isJava19: Boolean by lazy { JavaFeatureVersion == 19 }

    /** Java 21 인가? */
    val isJava21: Boolean by lazy { JavaFeatureVersion == 21 }

    /** Java 22 인가? */
    val isJava22: Boolean by lazy { JavaFeatureVersion == 22 }

    /** Java 23 인가? */
    val isJava23: Boolean by lazy { JavaFeatureVersion == 23 }

    /** Java 24 인가? */
    val isJava24: Boolean by lazy { JavaFeatureVersion == 24 }

    /** Java 25 인가? */
    val isJava25: Boolean by lazy { JavaFeatureVersion == 25 }

    /** JVM home directory */
    val javaHome: String? by lazy { systemProperty(JAVA_HOME) }

    val lineSeparator: String by lazy { System.lineSeparator() }

    val fileSeparator: String by lazy { java.nio.file.FileSystems.getDefault().separator }

    val pathSeparator: String by lazy { java.io.File.pathSeparator }

    val fileEncoding: String by lazy {
        java.nio.charset.Charset.defaultCharset().displayName() ?: Charsets.UTF_8.name()
    }

    /** 사용자 정보 */
    val userName: String? by lazy { systemProperty(USER_NAME) }

    /** 사용자 홈 디렉토리 */
    val userHome: String? by lazy { systemProperty(USER_HOME) }

    /** 사용자 현재 디렉토리 */
    val userDir: String? by lazy { systemProperty(USER_DIR) }

    /** 사용자 언어 */
    val userCountry: String? by lazy {
        systemProperty("user.country") ?: systemProperty("user.region")
    }

    /** 임시 디렉토리 */
    val tempDir: String? by lazy { systemProperty(TEMP_DIR) }

    /** 임시 디렉토리 */
    @Deprecated(
        message = "Use tempDir",
        replaceWith = ReplaceWith("tempDir"),
    )
    val javaIoTmpDir: String? by lazy { systemProperty(TEMP_DIR) }

    /** OS 이름 */
    val osName: String? by lazy { systemProperty(OS_NAME) }

    /** OS 버전 */
    val osVersion: String? by lazy { systemProperty(OS_VERSION) }

    private val normalizedOsName: String by lazy { osName?.lowercase(Locale.ROOT).orEmpty() }

    /** Windows 운영 체제인가? */
    val isWindows: Boolean by lazy { normalizedOsName.contains("win") }

    /** MAC OSX 운영 체제인가? */
    val isMac: Boolean by lazy { normalizedOsName.contains("mac") }

    /** Solaris 운영 체제인가? */
    val isSolaris: Boolean by lazy { normalizedOsName.contains("sunos") }

    /** UNIX 운영 체제인가? */
    val isUnix: Boolean by lazy {
        normalizedOsName.contains("nix") ||
                normalizedOsName.contains("nux") ||
                normalizedOsName.contains("aix")
    }


    /**
     * 지정한 키[name]에 해당하는 시스템 속성 정보를 가져옵니다. 없다면 null 을 반환합니다.
     *
     * @param name 시스템 속성 key
     * @return 시스템 속성 값
     */
    fun getProp(name: String): String? = systemProperty(name)

    /**
     * 환경설정 키[name]에 해당하는 값을 가져옵니다. 없다면 null 을 반환합니다.
     *
     * @param name 환경설정 Key
     * @return 환경설정 값
     */
    fun getenv(name: String): String? = System.getenv(name)

    /**
     * 모든 환경설정 정보를 가져옵니다.
     *
     * @return 환경설정 정보
     */
    fun getenv(): Map<String, String> = System.getenv()
}
