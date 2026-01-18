package io.bluetape4k.utils

import io.bluetape4k.support.unsafeLazy
import java.util.*

/**
 * System Property 를 제공해주는 Object
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
    const val JAVA_CLASS_VERION = "java.class.version"
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

    /** Runtime package */
    val RuntimePackage: Package by unsafeLazy { Runtime::class.java.`package` }

    /** System Properties */
    val SystemProps: Properties by unsafeLazy { System.getProperties() }

    /** CPU Core count */
    val ProcessCount: Int by unsafeLazy { Runtime.getRuntime().availableProcessors() }

    /** JVM Compipler 정보 */
    val JavaCompiler: String? by unsafeLazy { System.getProperty("java.compiler") }

    /** JVM 버전 */
    val JavaVersion: String? by unsafeLazy { RuntimePackage.specificationVersion }

    /** JVM 구현 버전 */
    val JavaImplementationVersion: String? by unsafeLazy { RuntimePackage.implementationVersion }

    /** JVM 벤더 */
    val JavaVendor: String? by unsafeLazy { RuntimePackage.specificationVendor }

    /** JVM 벤더 URL */
    val JavaVendorUrl: String? by unsafeLazy { System.getProperty("java.vendor.url") }

    /** JVM 구현 벤더  */
    val JavaImplementationVendor: String? by unsafeLazy { RuntimePackage.implementationVendor }

    /** JVM 구현 벤더 URL */
    val JavaClassVersion: String? by unsafeLazy { System.getProperty(JAVA_CLASS_VERION) }

    /** JVM 라이브러리 경로 */
    val JavaLibraryPath: String? by unsafeLazy { System.getProperty("java.library.path") }

    /** JVM 런타임 명 */
    val JavaRuntimeName: String? by unsafeLazy { System.getProperty("java.runtime.name") }

    /** JVM 런타임 버전 */
    val JavaRuntimeVersion: String? by unsafeLazy { System.getProperty("java.runtime.version") }

    /** JVM 사양 이름 */
    val JavaSpecificationName: String? by unsafeLazy { System.getProperty("java.specification.name") }

    /** JVM 사양 벤더 명 */
    val JavaSpecificationVendor: String? by unsafeLazy { System.getProperty("java.specification.vendor") }

    /** Java 1.6 인가? */
    val IsJava6: Boolean by unsafeLazy { JavaVersion == "1.6" }

    /** Java 1.7 인가? */
    val IsJava7: Boolean by unsafeLazy { JavaVersion == "1.7" }

    /** Java 1.8 인가? */
    val IsJava8: Boolean by unsafeLazy { JavaVersion == "1.8" }

    /** Java 9 인가? */
    val IsJava9: Boolean by unsafeLazy { JavaVersion == "1.9" }

    /** Java 10 인가? */
    val IsJava10: Boolean by unsafeLazy { JavaVersion == "10" }

    /** Java 11 인가? */
    val IsJava11: Boolean by unsafeLazy { JavaVersion == "11" }

    /** Java 17 인가? */
    val IsJava17: Boolean by unsafeLazy { JavaVersion == "17" }

    /** Java 17 인가? */
    val IsJava19: Boolean by unsafeLazy { JavaVersion == "19" }

    /** Java 21 인가? */
    val IsJava21: Boolean by unsafeLazy { JavaVersion == "21" }

    /** Java 22 인가? */
    val IsJava22: Boolean by unsafeLazy { JavaVersion == "22" }

    /** Java 23 인가? */
    val IsJava23: Boolean by unsafeLazy { JavaVersion == "23" }

    /** JVM home directory */
    val JavaHome: String? by unsafeLazy { System.getProperty("java.home") }

    val LineSeparator: String by unsafeLazy { System.lineSeparator() }

    val FileSeparator: String by unsafeLazy { java.nio.file.FileSystems.getDefault().separator }

    val PathSeparator: String by unsafeLazy { java.io.File.pathSeparator }

    val FileEncoding: String by unsafeLazy {
        java.nio.charset.Charset.defaultCharset().displayName() ?: Charsets.UTF_8.name()
    }

    /** 사용자 정보 */
    val UserName: String? by unsafeLazy { System.getProperty(USER_NAME) }

    /** 사용자 홈 디렉토리 */
    val UserHome: String? by unsafeLazy { System.getProperty(USER_HOME) }

    /** 사용자 현재 디렉토리 */
    val UserDir: String? by unsafeLazy { System.getProperty(USER_DIR) }

    /** 사용자 언어 */
    val UserCountry: String? by unsafeLazy {
        System.getProperty("user.country") ?: System.getProperty("user.region")
    }

    /** 임시 디렉토리 */
    val TempDir: String? by unsafeLazy { System.getProperty(TEMP_DIR) }

    /** 임시 디렉토리 */
    val JavaIOTmpDir: String? by unsafeLazy { System.getProperty(TEMP_DIR) }

    /** OS 이름 */
    val OSName: String? by unsafeLazy { System.getProperty(OS_NAME) }

    /** OS 버전 */
    val OSVersion: String? by unsafeLazy { System.getProperty(OS_VERSION) }

    /** Windows 운영 체제인가? */
    val isWindows: Boolean by unsafeLazy { OSName?.contains("win") ?: false }

    /** MAC OSX 운영 체제인가? */
    val isMac: Boolean by unsafeLazy { OSName?.contains("mac") ?: false }

    /** Solaris 운영 체제인가? */
    val isSolaris: Boolean by unsafeLazy { OSName?.contains("sunos") ?: false }

    /** UNIX 운영 체제인가? */
    val isUnix: Boolean by unsafeLazy {
        OSName?.contains("nix") == true ||
                OSName?.contains("nux") == true ||
                OSName?.contains("aix") == true
    }


    /**
     * 지정한 키[name]에 해당하는 시스템 속성 정보를 가져옵니다. 없다면 null 을 반환합니다.
     *
     * @param name 시스템 속성 key
     * @return 시스템 속성 값
     */
    fun getProp(name: String): String? = System.getProperty(name)

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
