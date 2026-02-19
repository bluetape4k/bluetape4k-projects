# Module bluetape4k-geocode

## 개요

위도/경도 좌표로부터 주소 정보를 조회하는 Reverse Geocoding 라이브러리입니다. Google Maps API와 Microsoft Bing Maps API를 지원합니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-geocode:${version}")
}
```

## 주요 기능

- **Reverse Geocoding**: 위도/경도 → 주소 변환
- **다중 제공자 지원**: Google Maps, Bing Maps
- **동기/비동기 지원**: 일반 함수 및 suspend 함수 모두 지원
- **다국어 지원**: 언어 코드로 결과 언어 지정

## 사용 예시

### Google Maps Geocoding

```kotlin
import io.bluetape4k.geocode.google.GoogleAddressFinder
import io.bluetape4k.geocode.Geocode

// Google Maps API Key로 생성
val addressFinder = GoogleAddressFinder("YOUR_GOOGLE_API_KEY")

// 위도/경도로 주소 조회
val geocode = Geocode(37.5665, 126.9780)  // 서울시청 좌표

// 한국어로 조회
val address = addressFinder.findAddress(geocode, "ko")

address?.let {
    println("국가: ${it.country}")               // "대한민국"
    println("도시: ${it.city}")                  // "서울특별시"
    println("상세주소: ${it.detailAddress}")     // "중구"
    println("우편번호: ${it.zipCode}")           // "04524"
    println("전체주소: ${it.formattedAddress}")  // "대한민국 서울특별시 중구..."
    println("Place ID: ${it.placeId}")           // Google Place ID
}
```

### Google Maps 비동기 조회

```kotlin
import io.bluetape4k.geocode.google.GoogleAddressFinder
import io.bluetape4k.geocode.Geocode
import kotlinx.coroutines.runBlocking

val addressFinder = GoogleAddressFinder("YOUR_GOOGLE_API_KEY")
val geocode = Geocode(37.5665, 126.9780)

// Coroutine 환경에서 비동기 조회
val address = runBlocking {
    addressFinder.suspendFindAddress(geocode, "ko")
}

address?.let {
    println("주소: ${it.formattedAddress}")
}
```

### Bing Maps Geocoding

```kotlin
import io.bluetape4k.geocode.bing.BingAddressFinder
import io.bluetape4k.geocode.Geocode

// Bing Maps API (키 없이도 제한적으로 사용 가능)
val addressFinder = BingAddressFinder()

val geocode = Geocode(37.5665, 126.9780)
val address = addressFinder.findAddress(geocode, "ko")

address?.let {
    println("이름: ${it.name}")
    println("국가: ${it.country}")
    println("도시: ${it.city}")
    println("상세주소: ${it.detailAddress}")
    println("우편번호: ${it.zipCode}")
    println("전체주소: ${it.formattedAddress}")
}
```

### Bing Maps 비동기 조회

```kotlin
import io.bluetape4k.geocode.bing.BingAddressFinder
import io.bluetape4k.geocode.Geocode
import kotlinx.coroutines.runBlocking

val addressFinder = BingAddressFinder()
val geocode = Geocode(40.7128, -74.0060)  // 뉴욕

val address = runBlocking {
    addressFinder.suspendFindAddress(geocode, "en")
}

address?.let {
    println("Address: ${it.formattedAddress}")
}
```

### Geocode 데이터 클래스

```kotlin
import io.bluetape4k.geocode.Geocode
import java.math.BigDecimal

// Double로 생성
val geocode1 = Geocode(37.5665, 126.9780)

// BigDecimal로 생성
val geocode2 = Geocode(
    latitude = BigDecimal("37.5665"),
    longitude = BigDecimal("126.9780")
)

// 문자열 파싱
val geocode3 = Geocode.parse("37.5665,126.9780")
val geocode4 = Geocode.parse("37.5665|126.9780", delimiter = "|")

// 반올림
val rounded = geocode1.round(scale = 2)  // 소수점 2자리

// 문자열 변환
val str = geocode1.toString()  // "37.5665,126.9780"
```

### Address 데이터 클래스

```kotlin
import io.bluetape4k.geocode.google.GoogleAddress

val address: GoogleAddress = // ...

// 공통 속성
    println("국가: ${address.country}")
println("도시: ${address.city}")

// Google 전용 속성
println("Place ID: ${address.placeId}")
println("상세주소: ${address.detailAddress}")
println("우편번호: ${address.zipCode}")
println("전체주소: ${address.formattedAddress}")
```

### 다국어 지원

```kotlin
import io.bluetape4k.geocode.google.GoogleAddressFinder
import io.bluetape4k.geocode.Geocode

val addressFinder = GoogleAddressFinder("YOUR_API_KEY")
val geocode = Geocode(35.6762, 139.6503)  // 도쿄

// 일본어
val jaAddress = addressFinder.findAddress(geocode, "ja")
println(jaAddress?.formattedAddress)  // "日本、東京都..."

// 영어
val enAddress = addressFinder.findAddress(geocode, "en")
println(enAddress?.formattedAddress)  // "Tokyo, Japan..."

// 한국어
val koAddress = addressFinder.findAddress(geocode, "ko")
println(koAddress?.formattedAddress)  // "일본 도쿄..."
```

## 제공자 비교

| 기능         | Google Maps    | Bing Maps |
|------------|----------------|-----------|
| 정확도        | 높음             | 보통        |
| API Key    | 필수             | 권장        |
| 가격         | 유료 (월 $200 무료) | 무료 제한 있음  |
| 다국어        | 지원             | 제한적       |
| 비동기        | 지원             | 지원        |
| Rate Limit | 50 QPS         | 기본 제한     |

## API Key 설정

### Google Maps API Key

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 또는 선택
3. Geocoding API 활성화
4. API Key 생성 및 제한 설정

```kotlin
import io.bluetape4k.geocode.google.GoogleAddressFinder

// API Key 직접 전달
val finder = GoogleAddressFinder("YOUR_GOOGLE_API_KEY")

// 기본 API Key 사용 (GoogleGeoService.apiKey 설정 필요)
val finder2 = GoogleAddressFinder()
```

### Bing Maps API Key

1. [Bing Maps Portal](https://www.bingmapsportal.com/) 접속
2. 계정 생성
3. Key 생성

```kotlin
import io.bluetape4k.geocode.bing.BingAddressFinder

// 키 없이도 제한적으로 사용 가능
val finder = BingAddressFinder()
```

## 에러 처리

```kotlin
import io.bluetape4k.geocode.google.GoogleAddressFinder
import io.bluetape4k.geocode.Geocode

val addressFinder = GoogleAddressFinder("YOUR_API_KEY")
val geocode = Geocode(0.0, 0.0)  // 유효하지 않은 좌표

// null 반환 (조회 실패)
val address = addressFinder.findAddress(geocode, "ko")
if (address == null) {
    println("주소를 찾을 수 없습니다.")
}

// 비동기에서 예외 처리
try {
    val address = addressFinder.suspendFindAddress(geocode, "ko")
} catch (e: IOException) {
    println("API 호출 실패: ${e.message}")
}
```

## 주요 기능 상세

| 파일                               | 설명                    |
|----------------------------------|-----------------------|
| `Geocode.kt`                     | 위도/경도 데이터 클래스         |
| `Address.kt`                     | 주소 정보 추상 클래스          |
| `GeocodeAddressFinder.kt`        | Geocoding 인터페이스       |
| `SuspendGeocodeAddressFinder.kt` | 비동기 Geocoding 인터페이스   |
| `google/GoogleAddressFinder.kt`  | Google Maps 구현        |
| `google/GoogleAddress.kt`        | Google 주소 모델          |
| `google/GoogleGeoService.kt`     | Google Maps 서비스       |
| `google/GeoApiContextSupport.kt` | Google API Context    |
| `bing/BingAddressFinder.kt`      | Bing Maps 구현          |
| `bing/BingAddress.kt`            | Bing 주소 모델            |
| `bing/BingMapService.kt`         | Bing Maps Feign 클라이언트 |
| `bing/BingMapModel.kt`           | Bing Maps 모델          |

## 참고 자료

- [Google Geocoding API](https://developers.google.com/maps/documentation/geocoding)
- [Google Maps Services Java](https://github.com/googlemaps/google-maps-services-java)
- [Bing Maps REST Services](https://learn.microsoft.com/en-us/bingmaps/rest-services/)
- [Bing Maps Portal](https://www.bingmapsportal.com/)
