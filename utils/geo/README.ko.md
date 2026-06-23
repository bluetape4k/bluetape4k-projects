# Module bluetape4k-geo

[English](./README.md) | 한국어

지리 정보 처리를 위한 단일 통합 모듈입니다. Geocode, GeoHash, GeoIP2 기능을 제공합니다.

> 구 `utils/geocode`, `utils/geohash`, `utils/geoip2` 모듈이 이 모듈로 통합되었습니다.

## 아키텍처

### 모듈 구성

![geo Architecture diagram](../../docs/images/readme-diagrams/utils-geo-diagram-01.png)

### 클래스 다이어그램

![Geo Class Structure diagram](../../docs/images/readme-diagrams/utils-geo-diagram-02.png)

### GeoHash 인코딩/디코딩 흐름

![GeoHash / diagram](../../docs/images/readme-diagrams/utils-geo-sequence-01.png)

## 제공 기능

### Geocode (구 `utils/geocode`)

- Google Maps Services 기반 주소 ↔ 좌표 변환
- Bing Maps API 연동 지원
- Feign HTTP 클라이언트 기반 비동기 요청
- Coroutines 확장 (선택적)

### GeoHash

- 위도/경도 좌표를 Base32 문자열로 인코딩
- GeoHash 디코딩 및 이웃 셀 계산
- 반경 내 GeoHash 목록 생성
- 정밀도 제어 (1~12자리)

### GeoIP2 (구 `utils/geoip2`)

- MaxMind GeoIP2 데이터베이스 기반 IP → 지리 정보 변환
- City, Country, ASN 조회 지원
- Coroutines 확장 (선택적)

## 사용 예시

### GeoHash 인코딩/디코딩

```kotlin
import io.bluetape4k.geohash.geoHashOfString
import io.bluetape4k.geohash.geoHashWithCharacters
import io.bluetape4k.geohash.getAdjacent

// 좌표 → GeoHash (9자리 정밀도)
val hash = geoHashWithCharacters(latitude = 37.5665, longitude = 126.9780, numberOfChars = 9)
val base32 = hash.toBase32()
// 예: "wydm9mufd"

// GeoHash → 좌표
val point = geoHashOfString(base32).originatingPoint
println("lat=${point.latitude}, lon=${point.longitude}")

// 이웃 GeoHash
val neighbors = hash.getAdjacent().map { it.toBase32() }
```

`GeoHashCircleQuery` 제약:

- 반경(`radius`)은 meter 단위이며 0 이상이어야 합니다.
- 음수 반경은 `IllegalArgumentException`으로 즉시 거부됩니다.

### Geocode (Google Maps)

Google/Bing Geocode API 키는 리소스 파일에 두지 않고 환경변수로 제공합니다.

```bash
export GOOGLE_GEOCODE_API_KEY="YOUR_GOOGLE_API_KEY"
export BING_GEOCODE_API_KEY="YOUR_BING_API_KEY"
```

```kotlin
import io.bluetape4k.geocode.Geocode
import io.bluetape4k.geocode.google.GoogleAddressFinder

val finder = GoogleAddressFinder(apiKey = System.getenv("GOOGLE_GEOCODE_API_KEY"))

// 좌표 → 주소 (역지오코딩)
val address = finder.findAddress(Geocode(37.5665, 126.9780), language = "ko")
println("국가=${address?.country}, 도시=${address?.city}")
```

### GeoIP2

```kotlin
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.geoip2.tryFindCity
import java.net.InetAddress

// GeoLite2-City.mmdb 파일을 애플리케이션 classpath에 둡니다.
val ipAddress = InetAddress.getByName("8.8.8.8")
val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress).getOrNull()

println("국가: ${cityResponse?.country()?.name()}")
println("도시: ${cityResponse?.city()?.name()}")
println("위도: ${cityResponse?.location()?.latitude()}")
println("경도: ${cityResponse?.location()?.longitude()}")
```

## 설치

각 기능은 `compileOnly`로 선언되어 있으므로, 사용할 라이브러리를 런타임 의존성으로 추가해야 합니다.

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-geo:${bluetape4kVersion}")

    // Geocode (Google Maps) 사용 시
    implementation("io.github.bluetape4k:bluetape4k-feign:${bluetape4kVersion}")
    implementation("io.github.bluetape4k:bluetape4k-jackson3:${bluetape4kVersion}")
    implementation("io.github.bluetape4k:bluetape4k-resilience4j:${bluetape4kVersion}")
    implementation("com.google.maps:google-maps-services:2.2.0")
    implementation("io.github.openfeign:feign-core:13.12")
    implementation("io.github.openfeign:feign-kotlin:13.12")
    implementation("io.github.openfeign:feign-slf4j:13.12")
    implementation("io.github.openfeign:feign-jackson:13.12")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6.1")
    implementation("org.apache.httpcomponents.client5:httpclient5-cache:5.6.1")

    // GeoIP2 사용 시
    implementation("com.maxmind.geoip2:geoip2:5.0.2")
}
```
