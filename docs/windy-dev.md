# Windy.com 클론 개발 계획

> 작성일: 2026-04-01  
> 목표: Windy.com 수준의 기상/해양 데이터 시각화 웹 애플리케이션 (독립 Repository)

---

## 1. 개요

NetCDF/GRIB2 형식의 기상·해양 데이터를 읽어 지도 위에 시각화하는 Windy.com 유사 서비스.  
백엔드는 Kotlin/Spring Boot, 프론트엔드는 Leaflet.js + WebGL 기반.

---

## 2. 핵심 기술 원리

### 2.1 격자 데이터 렌더링 방식

NetCDF 데이터(예: 해수온)를 지도 위에 빠르게 표현하는 방법:

**LUT (Look-Up Table) + 벡터 연산**

```
float[] 온도 배열 → 정규화 (0.0~1.0) → LUT 인덱스 → RGBA 픽셀 배열 → 이미지
```

- 격자를 하나씩 처리하지 않고 배열 전체를 한 번에 변환
- Java에서 단순 int[] 루프도 JIT가 SIMD로 최적화
- `BufferedImage.raster.setDataElements()` 로 픽셀 배열을 이미지로 직변환

**GPU 셰이더 (WebGL)**

- 격자 데이터를 텍스처로 GPU 업로드
- Fragment Shader에서 픽셀마다 병렬 색상 계산
- 컬러맵도 1D 텍스처로 전달 → `texture2D()` 샘플링

### 2.2 타일 피라미드 (Tile Pyramid)

Google Maps / 해도와 동일한 방식:

- 전체 지도를 줌 레벨별 256×256 타일로 분할
- `/{z}/{x}/{y}.png` TMS 경로로 서빙
- 줌 레벨에 따라 적절한 해상도 타일 로드
- Redis 캐시로 재생성 비용 절감

### 2.3 재투영 (Reprojection)

- NetCDF 격자: **EPSG:4326** (위경도)
- 지도 타일: **EPSG:3857** (Web Mercator)
- 타일 픽셀 → 월드 좌표 → 위경도 → 격자 인덱스 → 이중선형 보간

### 2.4 바람 파티클 애니메이션

Windy의 핵심 기능은 오픈소스 기반:

```
Cameron Beccario (earth.nullschool.net) → Windy 상용화 → leaflet-velocity 오픈소스화
```

백엔드가 U/V 바람 벡터 JSON을 제공하면 `leaflet-velocity`가 파티클 시스템 처리.

---

## 3. 무료 공개 데이터 소스

| 데이터                 | 제공처              | 형식     | 해상도      | URL                                                          |
|---------------------|------------------|--------|----------|--------------------------------------------------------------|
| **OISST** (해수온)     | NOAA/NCEI        | NetCDF | 0.25°/일  | https://www.ncei.noaa.gov/products/optimum-interpolation-sst |
| **GFS** (기상 예보)     | NOAA/NCEP        | GRIB2  | 0.25°/시간 | https://nomads.ncep.noaa.gov/                                |
| **ERA5** (재분석)      | Copernicus/ECMWF | NetCDF | 0.25°/시간 | https://cds.climate.copernicus.eu/                           |
| **GHRSST** (위성 SST) | NASA Earthdata   | NetCDF | 0.01°    | https://podaac.jpl.nasa.gov/                                 |
| **GEBCO** (해저지형)    | GEBCO            | NetCDF | 15호초     | https://www.gebco.net/                                       |

> NOAA OISST: 가입 없이 HTTP 직접 다운로드 가능 — MVP 시작점으로 적합

---

## 4. 기술 스택

### 4.1 백엔드 (Kotlin/JVM)

| 역할        | Python (대체 대상) | Kotlin/JVM 대안                                |
|-----------|----------------|----------------------------------------------|
| NetCDF 읽기 | `netCDF4`      | **NetCDF-Java** (Unidata) — 공식 Java 구현       |
| 다차원 배열    | `numpy`        | **Multik** (JetBrains) — Kotlin 네이티브         |
| 행렬/선형대수   | `numpy`        | **ND4J** (DeepLearning4J) — BLAS/LAPACK 네이티브 |
| 컬러맵 렌더링   | `matplotlib`   | **커스텀 LUT** + `BufferedImage` — 더 빠름         |
| 좌표 변환     | `pyproj`       | **GeoTools** / **Proj4J**                    |
| 타일 생성     | `rasterio`     | **GeoTools** + `ImageIO`                     |
| 웹 프레임워크   | `FastAPI`      | **Spring Boot WebFlux** (suspend)            |
| 캐시        | `Redis`        | **Lettuce** (bluetape4k infra/lettuce)       |

### 4.2 프론트엔드

| 기능      | 라이브러리                | 비고                       |
|---------|----------------------|--------------------------|
| 지도 베이스  | **Leaflet.js**       | 무료, OSM 연동               |
| 바람 파티클  | **leaflet-velocity** | earth.nullschool 기반 오픈소스 |
| 고성능 렌더링 | **deck.gl**          | WebGL GPU 렌더링            |
| 3D 지구본  | **Cesium.js**        | Phase 3 옵션               |

### 4.3 인프라

```
NOAA FTP/HTTP → 스케줄러 (데이터 수집)
                    ↓
              PostgreSQL + PostGIS (메타데이터)
                    ↓
              Spring Boot WebFlux (타일 서버)
                    ↓
              Redis 타일 캐시 (z/x/y 키)
                    ↓
              Leaflet.js + leaflet-velocity (프론트)
```

---

## 5. 시스템 아키텍처

```
[데이터 수집 레이어]
  NoaaOisstFetcher     — OISST NetCDF HTTP 다운로드
  GfsForecastFetcher   — GFS GRIB2 실시간 수집
  DataScheduler        — 주기적 수집 (6시간 간격)

[데이터 처리 레이어]
  NetcdfParser         — NetCDF-Java 래퍼
  GribParser           — GRIB2 파싱 (NetCDF-Java GRIB 모듈)
  GridResampler        — 격자 해상도 조정
  ColormapRenderer     — LUT + BufferedImage 타일 생성
  WindVectorExporter   — U/V JSON 변환 (leaflet-velocity 포맷)

[API 레이어]
  TileController       GET /tiles/{layer}/{z}/{x}/{y}.png
  WindController       GET /api/wind/{model}/{datetime}
  DataController       GET /api/layers, GET /api/times
  PointController      GET /api/point?lat=&lon=&layer=

[프론트엔드]
  index.html           — Leaflet 지도 + 레이어 패널
  WindLayer.js         — leaflet-velocity 연동
  ColorbarWidget.js    — 컬러바 + 범례
  TimeSlider.js        — 예보 시간축 슬라이더
```

---

## 6. 핵심 구현 예시

### 6.1 컬러맵 LUT 타일 렌더링

```kotlin
fun buildJetColormap(size: Int = 256): IntArray {
    return IntArray(size) { i ->
        val t = i / (size - 1.0)
        val r = (255 * (1.5 - abs(4 * t - 3))).toInt().coerceIn(0, 255)
        val g = (255 * (1.5 - abs(4 * t - 2))).toInt().coerceIn(0, 255)
        val b = (255 * (1.5 - abs(4 * t - 1))).toInt().coerceIn(0, 255)
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}

fun renderTile(
    data: FloatArray, width: Int, height: Int,
    vmin: Float, vmax: Float, lut: IntArray
): BufferedImage {
    val pixels = IntArray(width * height)
    val range = vmax - vmin
    for (i in pixels.indices) {
        pixels[i] = if (data[i].isNaN()) 0
        else lut[((data[i] - vmin) / range * 255).toInt().coerceIn(0, 255)]
    }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        raster.setDataElements(0, 0, width, height, pixels)
    }
}
```

### 6.2 GeoTools 재투영 (4326 → 3857 타일)

```kotlin
val wgs84 = CRS.decode("EPSG:4326")
val webMercator = CRS.decode("EPSG:3857")
val toLatLon = CRS.findMathTransform(webMercator, wgs84)

fun renderTileWithReprojection(tileX: Int, tileY: Int, zoom: Int): BufferedImage {
    val pixels = IntArray(TILE_SIZE * TILE_SIZE)
    for (py in 0 until TILE_SIZE) {
        for (px in 0 until TILE_SIZE) {
            val mercatorXY = tilePixelToMercator(tileX, tileY, zoom, px, py)
            val latLon = toLatLon.transform(mercatorXY, null)
            val value = sstGrid.bilinearInterpolate(latLon.ordinate(0), latLon.ordinate(1))
            pixels[py * TILE_SIZE + px] = lut[normalize(value)]
        }
    }
    // 행별 병렬 처리로 최적화 가능
    return pixels.toBufferedImage(TILE_SIZE, TILE_SIZE)
}
```

### 6.3 바람 벡터 JSON (leaflet-velocity 포맷)

```kotlin
data class WindGridHeader(
    val nx: Int, val ny: Int,
    val lo1: Double, val la1: Double,
    val lo2: Double, val la2: Double,
    val dx: Double, val dy: Double,
    val refTime: String
)

data class WindGrid(
    val header: WindGridHeader,
    val data: FloatArray
)

// GET /api/wind/gfs/2024040100
fun getWindData(model: String, datetime: String): List<WindGrid> {
    val (uData, vData) = gfsReader.readWindComponents(model, datetime)
    return listOf(
        WindGrid(header.copy(parameterUnit = "m.s-1", parameterNumber = 2), uData),
        WindGrid(header.copy(parameterUnit = "m.s-1", parameterNumber = 3), vData)
    )
}
```

### 6.4 Spring Boot 타일 엔드포인트

```kotlin
@RestController
@RequestMapping("/tiles")
class TileController(
    private val sstRenderer: SstTileRenderer,
    private val tileCache: LettuceNearCache<ByteArray>
) {
    @GetMapping("/sst/{z}/{x}/{y}.png")
    suspend fun sstTile(
        @PathVariable z: Int, @PathVariable x: Int, @PathVariable y: Int,
        @RequestParam(defaultValue = "latest") time: String
    ): ResponseEntity<ByteArray> {
        val cacheKey = "sst:$time:$z:$x:$y"
        val tile = tileCache.getOrPut(cacheKey) {
            sstRenderer.render(z, x, y, time)
        }
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
            .body(tile)
    }
}
```

### 6.5 Leaflet 프론트엔드

```html
<!-- index.html 핵심 부분 -->
<script>
    const map = L.map('map').setView([36, 128], 5);

    // 베이스맵
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap'
    }).addTo(map);

    // SST 오버레이
    const sstLayer = L.tileLayer('/tiles/sst/{z}/{x}/{y}.png?time=' + currentTime, {
        opacity: 0.7,
        attribution: 'NOAA OISST'
    }).addTo(map);

    // 바람 파티클 (leaflet-velocity)
    fetch(`/api/wind/gfs/${currentTime}`)
            .then(r => r.json())
            .then(data => {
                L.velocityLayer({
                    displayValues: true,
                    data: data,
                    maxVelocity: 30,
                    velocityScale: 0.005,
                    colorScale: ['#3288bd', '#99d594', '#e6f598', '#fee08b', '#d53e4f']
                }).addTo(map);
            });
</script>
```

---

## 7. 개발 로드맵

### Phase 1 — MVP (3~4주)

- [ ] NOAA OISST NetCDF 다운로드 + 파싱
- [ ] Jet/Viridis/Plasma 컬러맵 LUT 구현
- [ ] TMS 타일 서버 (`/tiles/sst/{z}/{x}/{y}.png`)
- [ ] Leaflet + OSM 베이스맵
- [ ] SST 오버레이 + 컬러바 범례
- [ ] 날짜 선택기

### Phase 2 — Windy 핵심 기능 (4~6주)

- [ ] GFS GRIB2 파싱 (NetCDF-Java GRIB 모듈)
- [ ] 바람 U/V 벡터 JSON API
- [ ] `leaflet-velocity` 바람 파티클 애니메이션
- [ ] 기온 / 기압 / 강수량 레이어
- [ ] 예보 시간축 슬라이더 (+6h, +12h, ... +120h)
- [ ] Redis 타일 캐시 연동
- [ ] 클릭-포인트 상세 데이터 팝업

### Phase 3 — 고품질 (2~3개월)

- [ ] deck.gl WebGL 렌더링으로 교체 (대용량 성능)
- [ ] ECMWF 모델 추가 (ERA5 재분석)
- [ ] 해류 벡터 레이어 (OSCAR/HYCOM)
- [ ] 파고/주기 레이어 (WAVEWATCH III)
- [ ] 모바일 반응형 UI
- [ ] 알림/즐겨찾기 기능

---

## 8. 독립 Repository 구성안

```
windy-clone/
├── backend/                    # Kotlin/Spring Boot
│   ├── data-collector/         # 스케줄러, NOAA/GFS 수집
│   ├── tile-server/            # 타일 렌더링 서버
│   ├── science-core/           # NetCDF/GRIB 파싱, 컬러맵, 재투영
│   └── api-server/             # REST API
│
├── frontend/                   # Vanilla JS or React
│   ├── src/
│   │   ├── map/                # Leaflet 지도 레이어
│   │   ├── layers/             # SST, 바람, 기온 레이어
│   │   └── ui/                 # 컨트롤 패널, 타임슬라이더
│   └── public/
│
├── infra/                      # Docker Compose, K8s
│   ├── docker-compose.yml
│   └── redis/
│
└── docs/
    ├── architecture.md
    └── data-sources.md
```

---

## 9. 참고 오픈소스

| 프로젝트                 | URL                                        | 참고 포인트          |
|----------------------|--------------------------------------------|-----------------|
| earth.nullschool.net | https://github.com/cambecc/earth           | 바람 파티클 원본 구현    |
| leaflet-velocity     | https://github.com/onaci/leaflet-velocity  | 바람 Leaflet 플러그인 |
| deck.gl              | https://deck.gl                            | WebGL 대용량 렌더링   |
| GeoServer            | https://geoserver.org                      | WMS/WFS 서버 참고   |
| THREDDS              | https://www.unidata.ucar.edu/software/tds/ | NetCDF 타일 서버 참고 |

---

## 10. 선박 운행 현황 레이어 (AIS)

### 10.1 AIS란?

AIS(Automatic Identification System) — 전세계 선박이 의무 송출하는 위치/속도/방향 신호.  
이를 수집·집계하면 Windy처럼 지도 위에 실시간 선박 현황을 표시할 수 있다.

### 10.2 무료/저비용 데이터 소스

| 소스                | 무료 여부          | 범위      | 특징                                  |
|-------------------|----------------|---------|-------------------------------------|
| **AISHub**        | **무료** (기여 조건) | 전세계     | 수신기 1대 기증 시 전체 데이터 접근, WebSocket 제공 |
| **NOAA AIS**      | **완전 무료**      | 미국 연안   | 역사 데이터 CSV/NetCDF, 분석용              |
| **Kystverket**    | **완전 무료**      | 노르웨이 해역 | REST API 공개                         |
| **MarineTraffic** | 제한적 무료         | 전세계     | API 유료, 웹 시각화 무료                    |
| **VesselFinder**  | 제한적 무료         | 전세계     | 실시간 웹 무료                            |

> **현실적인 전략**: AISHub 가입 + 저가 AIS 수신기($20~$30) 기증 → 전세계 실시간 데이터 접근  
> 수신기 없이 시작: NOAA 역사 데이터로 시각화 먼저 완성

### 10.3 AIS 메시지 구조

```
MMSI      — 선박 고유 식별자 (9자리)
위도/경도  — 실시간 위치
SOG       — 대지속력 (Speed Over Ground, knots)
COG       — 대지침로 (Course Over Ground, degrees)
선수방위  — Heading (degrees)
선박명    — 선박 이름
선종      — 화물선/유조선/여객선/어선 등
```

### 10.4 시스템 아키텍처

```
[AISHub WebSocket / NOAA HTTP]
        ↓
AisCollector (Kotlin Coroutines Flow)
        ↓
AisMessageParser (NMEA 0183 디코딩)
        ↓
Redis Pub/Sub (실시간 브로드캐스트)
  +  PostgreSQL+PostGIS (위치 이력 저장)
        ↓
Spring Boot WebSocket (SSE 또는 WS)
        ↓
Leaflet.js MarkerCluster 레이어
```

### 10.5 백엔드 구현 예시

```kotlin
// AIS 메시지 수신 → 파싱 → Redis 저장
@Component
class AisCollector(
    private val redisClient: RedisClient,
    private val vesselRepository: VesselRepository
) {
    fun startCollection(): Flow<VesselPosition> = flow {
        // AISHub WebSocket 연결
        val session = connectAisHub()
        session.incoming.consumeAsFlow()
            .filter { it is Frame.Text }
            .map { parseNmea((it as Frame.Text).readText()) }
            .filterNotNull()
            .collect { pos ->
                emit(pos)
                // Redis에 최신 위치 저장 (TTL 30분)
                redisClient.setex("vessel:${pos.mmsi}", 1800, pos.toJson())
                // PostGIS에 이력 저장
                vesselRepository.savePosition(pos)
            }
    }
}

data class VesselPosition(
    val mmsi: String,
    val lat: Double,
    val lon: Double,
    val sog: Float,      // 속력 (knots)
    val cog: Float,      // 침로 (degrees)
    val heading: Int,    // 선수방위
    val shipName: String?,
    val shipType: ShipType,
    val timestamp: Instant
)
```

### 10.6 프론트엔드 선박 레이어

```javascript
// Leaflet.markercluster + 선박 아이콘 회전
const vesselLayer = L.markerClusterGroup({maxClusterRadius: 40});

// WebSocket으로 실시간 업데이트
const ws = new WebSocket('/ws/vessels');
ws.onmessage = (event) => {
    const vessel = JSON.parse(event.data);
    updateVesselMarker(vessel);  // 위치 + 방향 갱신
};

// 선박 아이콘 (선수방위에 따라 회전)
function createVesselIcon(vessel) {
    return L.divIcon({
        html: `<div class="vessel-icon ship-${vessel.type}"
                    style="transform: rotate(${vessel.heading}deg)">▲</div>`,
        className: ''
    });
}

// 클릭 시 상세 정보 (선박명, 속력, 목적지)
marker.bindPopup(`
    <b>${vessel.shipName}</b><br>
    속력: ${vessel.sog} kts | 침로: ${vessel.cog}°<br>
    선종: ${vessel.shipType}
`);
```

### 10.7 Windy + AIS 통합 시너지

```
기상/해양 레이어 (Windy 기능)
  + 선박 위치 레이어 (AIS)
  = 항해 안전 대시보드
```

- 태풍 경로 + 선박 위치 → 위험 해역 선박 식별
- 해수온 + 어선 분포 → 어장 분석
- 파고 레이어 + 항로 → 항해 최적화 시각화
- 해류 레이어 + 선박 속력 → 연료 효율 분석

### 10.8 Phase 2에 추가할 기능

- [ ] AISHub WebSocket 연결 + NMEA 파싱
- [ ] 선박 위치 Redis 캐시 + PostGIS 이력
- [ ] Leaflet MarkerCluster 선박 레이어
- [ ] 선박 상세 팝업 (이름, 속력, 선종, 목적지)
- [ ] 선박 항적 표시 (최근 24시간 경로)
- [ ] 선종별 필터 (화물선/유조선/여객선/어선)

---

## 11. 한국 시장 특화 — 항행·항만 교통 대시보드

### 11.1 지도 API

네이버/카카오 맵 모두 **외부 TMS 타일 레이어 오버레이** 공식 지원.  
백엔드 타일 서버는 완전히 동일 — 프론트엔드 지도 SDK만 교체.

```javascript
// 네이버 맵 위에 SST/AIS 오버레이
const sstLayer = new naver.maps.TileLayer({
    tileSize: new naver.maps.Size(256, 256),
    getTileUrl: (x, y, z) => `/tiles/sst/${z}/${x}/${y}.png`
});
sstLayer.setMap(map);

// 카카오 맵도 동일 패턴
kakao.maps.addCustomOverlay(['/tiles/sst/{z}/{x}/{y}.png']);
```

> **네이버 맵 추천**: 국내 항만/연안 도로 정확도가 Google Maps보다 우수

### 11.2 한국 특화 무료 데이터 소스

모두 **data.go.kr** 에서 API 키 발급 — 완전 무료

| 데이터           | 제공처            | 특징                      |
|---------------|----------------|-------------------------|
| **AIS 선박 위치** | 해양경찰청 / 해수부    | 국내 연안 실시간 AIS, REST API |
| **항만 입출항 정보** | 해양수산부          | 부산/인천/광양 등 주요 항만 실시간    |
| **조위/조류**     | KHOA (국립해양조사원) | 전국 조위 관측소 실시간           |
| **해수온/염분**    | KHOA 국가해양관측망   | 부이 관측 데이터               |
| **해양기상**      | 기상청            | 파고/파주기/풍향풍속 격자 데이터      |
| **선박 입출항**    | 관세청 Unipass    | 세관 신고 기반 입출항 이력         |
| **전자해도 타일**   | KHOA           | S-57/S-63 포맷 해도         |

### 11.3 시각화 아이디어

```
부산항 입항 선박 밀도 히트맵
    → 시간대별 항만 교통 혼잡도 예측 / 선석 배정 최적화

남해안 조류 벡터 + 어선 AIS
    → 어장 분석 / 조업 패턴 / 금어구역 위반 감지

기상청 파고 레이어 + 연안 선박 위치
    → 악천후 위험 해역 선박 실시간 식별 / 해경 대응

한국 EEZ 경계 + 외국 선박 AIS
    → 해양 영토 모니터링

조류 + 해수온 + 어선 집중도
    → 어장 예측 모델 (수산업 특화)
```

### 11.4 아키텍처 (한국 특화)

```
[data.go.kr API]          [KHOA 관측망]      [기상청 격자 API]
  AIS / 항만 입출항          조위 / 해수온         파고 / 기상
        ↓                       ↓                    ↓
              Spring Boot 데이터 수집 스케줄러
                          ↓
              PostgreSQL + PostGIS (공간 쿼리)
              Redis (실시간 위치 캐시)
                          ↓
              타일 서버 / WebSocket API
                          ↓
              네이버 맵 or 카카오 맵
              + 커스텀 TMS 오버레이
              + Leaflet-velocity (해류/바람)
              + MarkerCluster (선박)
```

### 11.5 독립 Repository 구성안

```
korean-maritime-dashboard/
├── backend/
│   ├── data-collector/
│   │   ├── AisKoreaCollector.kt       # data.go.kr AIS API
│   │   ├── PortTrafficCollector.kt    # 항만 입출항
│   │   ├── KhoaOceanCollector.kt      # KHOA 조위/해수온
│   │   └── KmaWeatherCollector.kt     # 기상청 파고/기상
│   ├── tile-server/                   # SST/파고 타일 렌더링
│   └── api-server/                    # REST + WebSocket
│
├── frontend/
│   ├── naver/                         # 네이버 맵 버전
│   └── kakao/                         # 카카오 맵 버전
│
└── docs/
```

### 11.6 개발 로드맵

**Phase 1 — 한국 연안 MVP**

- [ ] data.go.kr API 키 발급 + AIS 데이터 수집
- [ ] 네이버 맵 기반 선박 위치 마커 표시
- [ ] 선박 클릭 → 상세 정보 (선명, 속력, 침로)
- [ ] 항만 입출항 현황 사이드 패널

**Phase 2 — 기상/해양 통합**

- [ ] KHOA 해수온 타일 오버레이
- [ ] 기상청 파고 격자 → Leaflet-velocity 해류/파랑 레이어
- [ ] 조위 관측소 실시간 마커
- [ ] 선박 항적 (24시간 경로)

**Phase 3 — 분석 대시보드**

- [ ] 항만별 입출항 밀도 히트맵 + 시간대별 통계
- [ ] 악천후 위험 선박 자동 알림
- [ ] 어선 조업 패턴 분석
- [ ] EEZ 경계 레이어 + 외국 선박 식별

---

## 12. 성능 고려사항

- **타일 캐시**: Redis에 `sst:{date}:{z}:{x}:{y}` 키로 PNG 바이트 저장 (TTL 1시간)
- **재투영 병렬화**: 타일 행 단위 `Dispatchers.Default` 병렬 처리
- **GRIB2 인덱스**: 전체 파일 파싱 대신 GRIB 인덱스 파일(`.idx`) 활용
- **점진적 로드**: 저해상도 타일 먼저 표시 → 고해상도로 교체
- **데이터 압축**: NetCDF 원본은 LZ4 압축 보관, 타일은 PNG (손실 없음)
