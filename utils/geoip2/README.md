# Module bluetape4k-geoip2

## 개요

[MaxMind GeoIP2](https://www.maxmind.com) 데이터베이스를 사용하여 IP 주소로부터 위치 정보를 조회하는 Kotlin 라이브러리입니다. 국가, 도시, 위도/경도, ISP 등의 정보를 빠르게 조회할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-geoip2:${version}")
}
```

## 주요 기능

- **IP 위치 조회**: IP 주소로부터 국가, 도시, 위도/경도 정보 조회
- **ASN 조회**: IP 주소의 ISP/ASN 정보 조회
- **Thread-Safe**: MaxMind DatabaseReader는 스레드 안전
- **Kotlin 확장 함수**: `tryCity()`, `tryCountry()` 등의 확장 함수 제공
- **IPv4/IPv6 지원**: 두 프로토콜 모두 지원

## 사전 요구사항

### GeoIP2 데이터베이스 파일

MaxMind GeoIP2 데이터베이스 파일(.mmdb)이 필요합니다:

- **GeoLite2-ASN.mmdb**: ISP/ASN 정보
- **GeoLite2-City.mmdb**: 도시 단위 위치 정보
- **GeoLite2-Country.mmdb**: 국가 단위 위치 정보

**다운로드 방법**:

1. [MaxMind 웹사이트](https://www.maxmind.com/en/geolite2/signup)에서 무료 계정 생성
2. GeoLite2 데이터베이스 다운로드
3. 프로젝트 리소스 디렉토리에 `.mmdb` 파일 배치

```bash
# git-lfs가 설치된 경우
git lfs install --system
```

## 사용 예시

### DatabaseReader 직접 사용

```kotlin
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.geoip2.tryCity
import io.bluetape4k.geoip2.tryCountry
import java.net.InetAddress

// City 데이터베이스로 조회
val ipAddress = InetAddress.getByName("8.8.8.8")
val cityResponse = Geoip.cityDatabase.tryCity(ipAddress).orElse(null)

println("Country: ${cityResponse?.country()?.name()}")           // "United States"
println("Country ISO: ${cityResponse?.country()?.isoCode}")       // "US"
println("City: ${cityResponse?.city()?.name()}")                  // null (Google DNS)
println("Region: ${cityResponse?.mostSpecificSubdivision()?.name()}") // "California"
println("Latitude: ${cityResponse?.location()?.latitude()}")      // 37.751
println("Longitude: ${cityResponse?.location()?.longitude()}")    // -97.822
println("Timezone: ${cityResponse?.location()?.timeZone()}")      // "America/Chicago"

// Country 데이터베이스로 조회
val countryResponse = Geoip.countryDatabase.tryCountry(ipAddress).orElse(null)
println("Country: ${countryResponse?.country()?.name()}")
println("Continent: ${countryResponse?.continent()?.name()}")
```

### Finder 사용 (권장)

```kotlin
import io.bluetape4k.geoip2.finder.GeoipCityFinder
import io.bluetape4k.geoip2.finder.GeoipCountryFinder
import java.net.InetAddress

// City Finder
val cityFinder = GeoipCityFinder()
val cityAddress = cityFinder.findAddress(InetAddress.getByName("8.8.8.8"))

cityAddress?.let {
    println("IP: ${it.ipAddress}")
    println("City: ${it.city}")
    println("Country: ${it.country}")              // "United States"
    println("Country ISO: ${it.countryIsoCode}")   // "US"
    println("Continent: ${it.continent}")
    println("Latitude: ${it.geoLocation?.latitude}")
    println("Longitude: ${it.geoLocation?.longitude}")
}

// Country Finder
val countryFinder = GeoipCountryFinder()
val countryAddress = countryFinder.findAddress(InetAddress.getByName("1.1.1.1"))

countryAddress?.let {
    println("Country: ${it.country}")
    println("Country ISO: ${it.countryIsoCode}")
    println("Continent: ${it.continent}")
}
```

### Address 데이터 클래스

```kotlin
import io.bluetape4k.geoip2.Address
import io.bluetape4k.geoip2.GeoLocation

// City Finder 결과
val address = cityFinder.findAddress(ipAddress)

address?.let {
    // 기본 정보
    println("IP: ${it.ipAddress}")
    println("Continent: ${it.continent}")          // "North America"
    println("Country: ${it.country}")              // "United States"
    println("Country ISO: ${it.countryIsoCode}")   // "US"
    println("Region: ${it.region}")                // "California"
    println("City: ${it.city}")                    // "Mountain View"
    println("Postal Code: ${it.postalCode}")       // "94035"
    
    // 위치 정보
    it.geoLocation?.let { loc ->
        println("Latitude: ${loc.latitude}")            // 37.386
        println("Longitude: ${loc.longitude}")          // -122.0838
        println("Timezone: ${loc.timeZone}")            // "America/Los_Angeles"
        println("Accuracy: ${loc.accuracyRadius} km")   // 1000
    }
}
```

### GeoLocation 상세 정보

```kotlin
import io.bluetape4k.geoip2.GeoLocation

val geoLocation = address?.geoLocation

geoLocation?.let {
    println("위도: ${it.latitude}")
    println("경도: ${it.longitude}")
    println("시간대: ${it.timeZone}")
    println("정확도 반경: ${it.accuracyRadius} km")
    println("평균 소득: ${it.averageIncome}")
    println("인구 밀도: ${it.populationDensity}")
}
```

### IPv6 지원

```kotlin
import java.net.InetAddress

// IPv6 주소 조회
val ipv6Address = InetAddress.getByName("2001:4860:4860::8888")  // Google DNS IPv6
val address = cityFinder.findAddress(ipv6Address)

println("Country: ${address?.country}")
println("Location: ${address?.geoLocation}")
```

### Coroutines 환경

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

suspend fun findLocationAsync(ip: String): Address? {
    return withContext(Dispatchers.IO) {
        val ipAddress = InetAddress.getByName(ip)
        cityFinder.findAddress(ipAddress)
    }
}

// 사용
val address = findLocationAsync("8.8.8.8")
```

### 멀티스레딩 환경

```kotlin
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

val cityFinder = GeoipCityFinder()
val resultMap = ConcurrentHashMap<String, Address>()

val ipAddresses = listOf("8.8.8.8", "1.1.1.1", "172.217.161.174", "208.67.222.222")

ipAddresses.parallelStream().forEach { ip ->
    val address = cityFinder.findAddress(InetAddress.getByName(ip))
    address?.let { resultMap[ip] = it }
}

resultMap.forEach { (ip, addr) ->
    println("$ip -> ${addr.country}, ${addr.city}")
}
```

### Private IP 처리

```kotlin
import java.net.InetAddress

// Private IP는 데이터베이스에 정보가 없어 null 반환
val privateIp = InetAddress.getByName("127.0.0.1")
val address = cityFinder.findAddress(privateIp)  // null

val localIp = InetAddress.getByName("192.168.1.1")
val address2 = cityFinder.findAddress(localIp)   // null

val dockerIp = InetAddress.getByName("10.0.0.1")
val address3 = cityFinder.findAddress(dockerIp)  // null
```

## DatabaseReader 확장 함수

```kotlin
import io.bluetape4k.geoip2.tryCity
import io.bluetape4k.geoip2.tryCountry
import io.bluetape4k.geoip2.tryAsn

// Optional 반환 (null 안전)
val cityResponse = Geoip.cityDatabase.tryCity(ipAddress).orElse(null)
val countryResponse = Geoip.countryDatabase.tryCountry(ipAddress).orElse(null)
val asnResponse = Geoip.asnDatabase.tryAsn(ipAddress).orElse(null)

// ASN 정보
asnResponse?.let {
    println("ASN: ${it.autonomousSystemNumber}")
    println("ISP: ${it.autonomousSystemOrganization}")
}
```

## 주요 기능 상세

| 파일                             | 설명                       |
|--------------------------------|--------------------------|
| `Geoip.kt`                     | GeoIP2 DatabaseReader 제공 |
| `GeoLocation.kt`               | 위도/경도 데이터 클래스            |
| `Address.kt`                   | 주소 정보 데이터 클래스            |
| `DatabaseReaderExtensions.kt`  | DatabaseReader 확장 함수     |
| `finder/GeoipFinder.kt`        | IP 위치 조회 인터페이스           |
| `finder/GeoipCityFinder.kt`    | City 단위 위치 조회            |
| `finder/GeoipCountryFinder.kt` | Country 단위 위치 조회         |

## 데이터베이스 종류

| 데이터베이스           | 제공 정보              |
|------------------|--------------------|
| GeoLite2-City    | 국가, 도시, 위도/경도, 시간대 |
| GeoLite2-Country | 국가, 대륙 정보          |
| GeoLite2-ASN     | ISP, ASN 번호        |

## 성능 최적화

- **캐싱**: 내부적으로 CHMCache 사용하여 자주 조회되는 IP 캐싱
- **메모리 매핑**: 파일 기반 조회로 메모리 효율적
- **Thread-Safe**: 동시성 환경에서 안전하게 사용 가능

## 참고 자료

- [GeoIP2 Java API](https://maxmind.github.io/GeoIP2-java/)
- [MaxMind GeoLite2 다운로드](https://www.maxmind.com/en/geolite2/signup)
- [MaxMind GitHub](https://github.com/maxmind)
