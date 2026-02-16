# bluetape4k-geoip2

MaxMind GeoIP2 데이터베이스를 사용하여 IP 주소로부터 위치 정보를 조회하는 Kotlin 라이브러리입니다.

## 특징

- **간편한 IP 위치 조회**: IP 주소로부터 국가, 도시, 위도/경도 정보 조회
- **Thread-Safe**: MaxMind DatabaseReader는 thread-safe하여 동시성 환경에서 안전하게 사용 가능
- **Kotlin 확장 함수**: `tryCity()`, `tryCountry()` 등의 확장 함수 제공
- **Coroutines 지원**: 코루틴 환경에서도 사용 가능

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-geoip2:${bluetape4kVersion}")
}
```

## 사전 요구사항

### GeoIP2 데이터베이스 파일

MaxMind GeoIP2 데이터베이스 파일(.mmdb)이 필요합니다:

- GeoLite2-ASN.mmdb
- GeoLite2-City.mmdb
- GeoLite2-Country.mmdb

**묶여 있는 버전**을 사용하려면 git-lfs를 설치해야 합니다:

```bash
git lfs install --system
```

또는 [MaxMind 웹사이트](https://www.maxmind.com/en/accounts/379741/geoip/downloads)에서 직접 다운로드할 수 있습니다.

## 기본 사용법

### DatabaseReader 직접 사용

```kotlin
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.geoip2.tryCity
import io.bluetape4k.geoip2.tryCountry
import java.net.InetAddress

// IP 주소로 City 정보 조회
val ipAddress = InetAddress.getByName("8.8.8.8")
val cityResponse = Geoip.cityDatabase.tryCity(ipAddress).orElse(null)

println("Country: ${cityResponse?.country()?.name()}")
println("City: ${cityResponse?.city()?.name()}")
println("Latitude: ${cityResponse?.location()?.latitude()}")
println("Longitude: ${cityResponse?.location()?.longitude()}")

// IP 주소로 Country 정보 조회
val countryResponse = Geoip.countryDatabase.tryCountry(ipAddress).orElse(null)
println("Country: ${countryResponse?.country()?.name()}")
println("Continent: ${countryResponse?.continent()?.name()}")
```

### Finder 사용

```kotlin
import io.bluetape4k.geoip2.finder.GeoipCityFinder
import io.bluetape4k.geoip2.finder.GeoipCountryFinder

val cityFinder = GeoipCityFinder()
val countryFinder = GeoipCountryFinder()

val ipAddress = InetAddress.getByName("8.8.8.8")

// City 단위 정보 조회
val cityAddress = cityFinder.findAddress(ipAddress)
println("City: ${cityAddress?.city}")
println("Country: ${cityAddress?.country}")
println("Location: ${cityAddress?.geoLocation}")

// Country 단위 정보 조회
val countryAddress = countryFinder.findAddress(ipAddress)
println("Country: ${countryAddress?.country}")
println("ISO Code: ${countryAddress?.countryIsoCode}")
```

### Address 데이터 클래스

```kotlin
val address = cityFinder.findAddress(ipAddress)

address?.let {
    println("IP: ${it.ipAddress}")
    println("City: ${it.city}")
    println("Country: ${it.country}")
    println("Continent: ${it.continent}")
    println("ISO Code: ${it.countryIsoCode}")
    println("Location: ${it.geoLocation}")
}
```

### GeoLocation 정보

```kotlin
val geoLocation = address?.geoLocation

geoLocation?.let {
    println("Latitude: ${it.latitude}")
    println("Longitude: ${it.longitude}")
    println("TimeZone: ${it.timeZone}")
    println("Accuracy Radius: ${it.accuracyRadius} km")
}
```

## 고급 사용법

### Coroutines 환경에서 사용

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun findLocationAsync(ip: String): Address? {
    return withContext(Dispatchers.IO) {
        val ipAddress = InetAddress.getByName(ip)
        cityFinder.findAddress(ipAddress)
    }
}
```

### 멀티스레딩 환경에서 사용

```kotlin
import java.util.concurrent.ConcurrentHashMap

val resultMap = ConcurrentHashMap<String, Address>()
val ipAddresses = listOf("8.8.8.8", "1.1.1.1", "172.217.161.174")

ipAddresses.parallelStream().forEach { ip ->
    val address = cityFinder.findAddress(InetAddress.getByName(ip))
    address?.let { resultMap[ip] = it }
}
```

### Private IP 처리

Private IP 주소(예: 127.0.0.1, 10.x.x.x, 192.168.x.x)는 GeoIP 데이터베이스에 정보가 없으므로 `null`을 반환합니다:

```kotlin
val privateIp = InetAddress.getByName("127.0.0.1")
val address = cityFinder.findAddress(privateIp)  // null
```

### IPv6 지원

```kotlin
val ipv6Address = InetAddress.getByName("2001:4860:4860::8888")
val address = cityFinder.findAddress(ipv6Address)
```

## 참고 자료

- [GeoIP2 Java API](https://maxmind.github.io/GeoIP2-java/)
- [MaxMind GeoIP Downloads](https://www.maxmind.com/en/accounts/379741/geoip/downloads)
- [MaxMind GitHub](https://github.com/maxmind)

## 라이선스

Apache License 2.0
