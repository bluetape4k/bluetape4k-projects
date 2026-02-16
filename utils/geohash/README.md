# bluetape4k-geohash

GeoHash는 지구상의 위치를 문자열로 인코딩하는 공간 인덱싱 시스템입니다. 이 모듈은 WGS84 좌표계를 기반으로 한 GeoHash 구현을 제공합니다.

## 특징

- **정밀한 GeoHash 인코딩/디코딩**: Base32 및 Binary 문자열 지원
- **이웃 GeoHash 계산**: 8방위(북, 북동, 동, 남동, 남, 남서, 서, 북서) 이웃 계산
- **공간 쿼리**: 원형 검색, 경계 상자 검색
- **WGS84 지원**: Vincenty 지구측지법 알고리즘을 사용한 정확한 거리 계산
- **높은 정밀도**: 최대 64비트(12자 Base32)까지 지원

## 정밀도 표

| GeoHash 길이 | 위도 비트 | 경도 비트 | 위도 오차        | 경도 오차       | ~km 오차    |
|------------|-------|-------|--------------|-------------|-----------|
| 1          | 2     | 3     | ±23          | ±23         | ±2500     |
| 2          | 5     | 5     | ±2.8         | ±5.6        | ±630      |
| 3          | 7     | 8     | ±0.70        | ±0.70       | ±78       |
| 4          | 10    | 10    | ±0.087       | ±0.18       | ±20       |
| 5          | 12    | 13    | ±0.022       | ±0.022      | ±2.4      |
| 6          | 15    | 15    | ±0.0027      | ±0.0055     | ±0.61     |
| 7          | 17    | 18    | ±0.00068     | ±0.00068    | ±0.076    |
| 8          | 20    | 20    | ±0.000085    | ±0.00017    | ±0.019    |
| 9          | 22    | 23    | ±0.000021    | ±0.000021   | ±0.0024   |
| 10         | 25    | 25    | ±0.0000026   | ±0.0000055  | ±0.00061  |
| 11         | 27    | 28    | ±0.00000068  | ±0.00000068 | ±0.000076 |
| 12         | 30    | 30    | ±0.000000085 | ±0.00000017 | ±0.000019 |

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-geohash:${bluetape4kVersion}")
}
```

## 기본 사용법

### WGS84Point 생성

```kotlin
import io.bluetape4k.geohash.wgs84PointOf
import io.bluetape4k.geohash.moveInDirection

// 위도와 경도로 생성
val point = wgs84PointOf(37.5665, 126.9780)  // 서울

// 특정 방향으로 이동
val movedPoint = point.moveInDirection(bearingInDegrees = 90.0, distanceInMeters = 1000.0)
```

### GeoHash 생성

```kotlin
import io.bluetape4k.geohash.geoHashWithCharacters
import io.bluetape4k.geohash.geoHashWithBits
import io.bluetape4k.geohash.geoHashOfString

// 문자 정밀도로 생성 (5의 배수 비트)
val hash1 = geoHashWithCharacters(37.5665, 126.9780, 12)

// 비트 정밀도로 생성
val hash2 = geoHashWithBits(37.5665, 126.9780, 30)

// Base32 문자열로부터 생성
val hash3 = geoHashOfString("wydm9c8d2n8c")
```

### GeoHash 인코딩/디코딩

```kotlin
val hash = geoHashWithCharacters(37.5665, 126.9780, 12)

// Base32 문자열로 변환
val base32 = hash.toBase32()  // "wydm9c8d2n8c"

// Binary 문자열로 변환
val binary = hash.toBinaryString()

// long 값으로 변환
val longValue = hash.longValue

// 중심점 좌표
val center = hash.boundingBoxCenter

// Bounding Box
val bbox = hash.boundingBox
```

### 이웃 GeoHash 계산

```kotlin
val hash = geoHashOfString("9q8y")

// 8방위 이웃
val adjacent = hash.getAdjacent()
// [북, 북동, 동, 남동, 남, 남서, 서, 북서]

// 개별 방향
val north = hash.getNorthernNeighbor()
val south = hash.getSouthernNeighbor()
val east = hash.getEasternNeighbor()
val west = hash.getWesternNeighbor()
```

### BoundingBox

```kotlin
import io.bluetape4k.geohash.boundingBoxOf
import io.bluetape4k.geohash.wgs84PointOf

// 두 좌표로 BoundingBox 생성
val bbox = boundingBoxOf(
    southWestCorner = wgs84PointOf(37.4, 126.8),
    northEastCorner = wgs84PointOf(37.7, 127.1)
)

// 개별 값으로 생성
val bbox2 = boundingBoxOf(37.4, 37.7, 126.8, 127.1)

// 중심점
val center = bbox.getCenter()

// 포함 여부 검사
bbox.contains(wgs84PointOf(37.5, 126.9))  // true

// 다른 BoundingBox와의 교차 검사
val bbox3 = boundingBoxOf(37.5, 37.8, 126.9, 127.2)
bbox.intersects(bbox3)  // true
```

## 공간 쿼리

### 원형 검색

```kotlin
import io.bluetape4k.geohash.queries.geoHashCircleQueryOf

// 중심점과 반경으로 원형 검색
val center = wgs84PointOf(37.5665, 126.9780)
val query = geoHashCircleQueryOf(center, radius = 1000.0)  // 1km 반경

// GeoHash가 쿼리 영역 내에 있는지 확인
val hash = geoHashWithCharacters(37.5665, 126.9780, 12)
query.contains(hash)  // true/false

// WGS84Point가 쿼리 영역 내에 있는지 확인
query.contains(wgs84PointOf(37.5666, 126.9781))  // true/false

// 검색에 필요한 GeoHash 목록
val searchHashes = query.getSearchHashes()
```

### 경계 상자 검색

```kotlin
import io.bluetape4k.geohash.queries.geoHashBoundingBoxQueryOf

// BoundingBox로 검색
val bbox = boundingBoxOf(37.4, 37.7, 126.8, 127.1)
val query = geoHashBoundingBoxQueryOf(bbox)

// 또는 직접 좌표 지정
val query2 = geoHashBoundingBoxQueryOf(37.4, 37.7, 126.8, 127.1)

// 검색에 필요한 GeoHash 목록
val searchHashes = query.getSearchHashes()
```

## 거리 계산

### Vincenty 지구측지법

```kotlin
import io.bluetape4k.geohash.wgs84PointOf
import io.bluetape4k.geohash.distanceInMeters

val seoul = wgs84PointOf(37.5665, 126.9780)
val busan = wgs84PointOf(35.1796, 129.0756)

// 두 지점 간 거리 (미터)
val distance = seoul.distanceInMeters(busan)

// 특정 방향으로 이동
val moved = seoul.moveInDirection(bearingInDegrees = 45.0, distanceInMeters = 10000.0)
```

## 활용 예시

### 위치 기반 서비스

```kotlin
// 사용자 주변의 가게 검색
fun findNearbyStores(userLocation: WGS84Point, radiusInMeters: Double): List<Store> {
    val query = geoHashCircleQueryOf(userLocation, radiusInMeters)

    // 데이터베이스에서 GeoHash로 필터링
    val candidateHashes = query.getSearchHashes()
    val stores = storeRepository.findByGeoHashIn(candidateHashes.map { it.toBase32() })

    // 정확한 거리로 필터링
    return stores.filter { store ->
        userLocation.distanceInMeters(store.location) <= radiusInMeters
    }
}
```

### 지리적 클러스터링

```kotlin
// GeoHash를 사용한 간단한 클러스터링
fun clusterLocations(locations: List<WGS84Point>, precision: Int): Map<String, List<WGS84Point>> {
    return locations.groupBy { location ->
        geoHashWithCharacters(location.latitude, location.longitude, precision).toBase32()
    }
}
```

## 참고 자료

- [GeoHash (Wikipedia)](https://en.wikipedia.org/wiki/Geohash)
- [geohash.org](http://geohash.org)
- [Vincenty's Formulae (Wikipedia)](https://en.wikipedia.org/wiki/Vincenty%27s_formulae)
