# NetCDF `CoordinateAxis2D`·CF auxiliary coordinate 설계

- **일자**: 2026-08-27
- **이슈**: [#1352](https://github.com/bluetape4k/bluetape4k-projects/issues/1352)
- **Epic**: [#1421](https://github.com/bluetape4k/bluetape4k-projects/issues/1421)
- **선행 train 단계**: [PR #1512](https://github.com/bluetape4k/bluetape4k-projects/pull/1512) — #1343 문서 계약, `develop` 반영 완료
- **구현 branch/worktree**: `feat/1352-coordinate-axis2d-cf-grid` / `.worktrees/feat/1352-coordinate-axis2d-cf-grid`
- **기준**: `origin/develop@45260871f58433a78f2d633c235010f661d22c6e`
- **대상 모듈**: `bluetape4k-science` (`utils/science`)
- **상태**: Step 2-R 통합 PASS (2026-08-27 사용자 명세 재승인 완료; Step 3-R 계획 검토 진행)

## 1. 문제와 목표

현재 `NetCdfCatalogService`는 데이터 변수의 dimension 이름으로 `CoordinateAxis1D`만 찾습니다. 따라서 curvilinear grid의 `CoordinateAxis2D` lat/lon과 데이터 변수의 CF `coordinates` 속성에 열거된 auxiliary coordinate를 `MissingCoordinate`로 거부합니다. 데이터 값과 셀 좌표를 함께 보존해야 하는 #1352의 수용 기준을 만족하려면 축 탐지, 셀 인덱싱, CRS 변환, 저장 경로를 하나의 내부 계약으로 확장해야 합니다.

이번 변경의 목표는 다음과 같습니다.

1. `CoordinateAxis1D`와 `CoordinateAxis2D`를 같은 내부 좌표 sampler 계약으로 읽는다.
2. CF `coordinates` 속성으로 선언된 lat/lon 및 추가 numeric auxiliary coordinate를 해석한다.
3. 변수 dimension 순서와 좌표 dimension 순서를 별도로 보존해 `[time, x, y]` 같은 데이터도 정확히 매핑한다.
4. canonical `(lon, lat)`은 기존 PostGIS `location`에 저장하고, lat/lon 이외의 auxiliary 값은 기존 JSONB `attrs`에 저장한다.
5. 기존 rank 1–4, 1D 축, CRS whitelist, NaN/`_FillValue`, resume/lease, metrics 계약을 회귀 없이 유지한다.

## 2. 현재 근거와 제약

### 2.1 저장소 근거

- `NetCdfCatalogService.kt`의 `importSlice2D`는 `UcarArray.indexIterator`와 `(i, j)`를 직접 결합한다. 데이터 dimension 순서가 바뀌면 좌표와 값의 대응을 보장할 수 없다.
- `VariableAxisMap.kt`는 `dataset.findCoordinateAxis(dimensionName)` 결과가 `CoordinateAxis1D`가 아니면 `MissingCoordinate`를 발생시킨다.
- `CoordinateReprojector.kt`는 현재 `CoordinateAxis1D`만 캐시하고, EPSG:4326/4269/3857/32601–32660/32701–32760/3413/3031을 지원한다.
- `NetCdfGridValueTable.location`은 nullable PostGIS POINT(EPSG:4326)이고 `attrs`는 nullable JSONB `Map<String, Any?>`다. 기존 partial unique index는 `location`과 `(fileId, variableName, timeIdx, levelIdx)`에만 의존하므로 auxiliary 값을 `attrs`에 넣어도 중복 계약은 변하지 않는다.
- `NetcdfDatasets.openDataset(String)`은 NetCDF-Java 5.9.1의 default enhance mode로 dataset을 열며, CF convention이 `coordinates`에 열거된 변수를 coordinate axis로 만든다. `CoordinateAxis2D.getCoordValue(int, int)`로 2D 값을 셀 단위로 읽을 수 있다.
- `utils/science` compile baseline은 `./gradlew :bluetape4k-science:compileKotlin --no-configuration-cache --console=plain`으로 성공했다.

### 2.2 범위 제약

- 공개 서비스 메서드 시그니처와 blocking 호출 모델은 유지한다.
- 새 dependency, 새 테이블, schema migration, workflow 변경은 포함하지 않는다.
- `rotated_latitude_longitude`·tripolar처럼 EPSG whitelist로 해석할 수 없는 grid mapping은 이번 변경에서도 지원하지 않고 typed exception으로 거부한다.
- 이 서비스의 파일 open/import API는 신뢰된 운영 경계에서만 호출하는 trusted-admin API다. HTTP 요청 등 외부 입력을 그대로 `filePath`로 전달하지 않으며, 구현은 NUL/control 문자·URI scheme·regular file이 아닌 경로·경로 구성요소의 심볼릭 링크를 `FileOpen`으로 거부한다. 원격 URL은 지원하지 않는다. canonical path를 구성요소별 `NOFOLLOW_LINKS`로 확인하고, open 직전·직후 `fileKey/size/lastModifiedTime` identity를 재검증해 TOCTOU를 차단한다.
- 파일 크기는 open 전과 resume 시 `MAX_FILE_BYTES = 64 GiB` 이하인지 확인한다. NetCDF-Java의 blocking open 자체에는 이 모듈이 별도 timeout을 강제하지 않으므로 외부 호출자는 전체 deadline을 소유한다.
- 대용량 파일의 전체 데이터 배열을 한 번에 읽지 않는다. 기존 논리적 slice와 lease 갱신 경계를 유지하되, 각 slice는 bounded tile read로 나눈다.
- 한 import는 하나의 blocking caller/DB connection만 사용하고 tile을 병렬화하지 않는다. 내부 무한 재시도·자동 backoff는 없으며, caller의 deadline/interrupt가 다음 tile 경계에서 관찰된다. 전체 실행 시간·connection 점유 상한은 호출자가 제공하는 deadline으로 제한한다.

### 2.3 자원 예산과 중복 좌표 정책

악의적이거나 손상된 metadata가 CPU·heap·DB 연결을 고갈시키지 않도록 다음 hard limit을 import 전에 검사한다. 모든 dimension product와 byte 계산은 `Long` checked arithmetic로 수행한 뒤 `Int` API에 내린다.

| 예산 | 기본 상한 | 초과 시 동작 |
|---|---:|---|
| `coordinates` attribute token 수 | 32 | `ResourceLimitExceeded` + `netcdf.import.rejected{reason=resource}` |
| auxiliary coordinate 변수 수 | 16 | 동일 |
| coordinate variable 이름 UTF-8 길이 | 128 bytes | 동일 |
| NetCDF variable 수 | 1,024 | 동일 |
| dataset 전체 group dimension 수 | 256 | 동일 |
| 등록 metadata UTF-8 payload | 1 MiB | 동일 |
| 한 import logical cell 수 | 100,000,000 | 동일 |
| 한 import logical slice 수 | 1,000,000 | 동일 |
| 한 spatial tile read 셀 수 | 65,536 | tile shape을 더 작게 분할 |
| 한 JDBC `executeBatch` 행 수 | 1,000 | 같은 tile 안에서 추가 flush |
| 한 셀 auxiliary JSONB payload | 8,192 bytes | `ResourceLimitExceeded` |
| import 중 coordinate cache | 64 MiB | 전체-grid cache를 만들지 않고 tile/lazy 경로로 전환; 그래도 초과하면 실패 |
| duplicate 검사용 primitive key set | 32 MiB (64 MiB 총 예산에 포함) | uniqueness를 증명할 수 없으므로 `ResourceLimitExceeded` |
| 한 import의 owned working-set (tile/data/serializer/cache/set) | 128 MiB | tile을 줄여도 초과하면 실패 |

2D coordinate는 import 수명 동안 전체 grid를 캐시하지 않는다. geographic 2D와 auxiliary 값은 현재 tile에서만 읽고 폐기하며, projected 변환 pair cache도 위 64 MiB 예산 안에서만 유지한다. cache와 duplicate 검사용 primitive key set의 크기는 실제 셀 수가 아닌 `Long` 바이트 산식으로 계측한다. tile data buffer·serializer scratch·cache·duplicate set을 합친 모듈 소유 working-set은 128 MiB를 넘지 않는다. JVM/NetCDF-Java 내부 peak heap은 이 예산과 분리한 관찰값으로 보고한다. 빈 auxiliary map은 JSONB를 생성하지 않고 `attrs=null`로 저장한다.

내부 `MemoryBudget`는 다음 checked `Long` 산식으로 tile 전에 상한을 판정한다: `tileBufferBytes = tileCells × 8`, `coordinateBytes = tileCells × (spatialAxisCount + auxiliaryCount) × 8`, `serializerScratchBytes = MAX_AUXILIARY_JSONB_BYTES`, `duplicateSetBytes = sliceCells × 32` (open-addressed primitive pair key의 보수적 entry 비용), `ownedWorkingSet = 합계`. `coordinateBytes + duplicateSetBytes`는 64 MiB, 전체 `ownedWorkingSet`은 128 MiB를 넘을 수 없다. 이 계산은 JVM peak heap 추정치가 아니라 서비스가 소유한 배열·key·serializer buffer에만 적용한다.

현재 schema의 `(file_id, variable_name, time_idx, level_idx, location-hash)` unique index는 같은 canonical 위치를 가진 서로 다른 셀을 식별하지 못한다. 이번 child에서 schema migration을 추가하지 않으므로, 각 `(timeIdx, levelIdx)` spatial slice는 DB insert 전에 canonical `(lon,lat)`의 exact bit-pair 중복을 bounded primitive set으로 검사한다. key 생성 시 `-0.0`과 `+0.0`은 PostGIS의 동일 좌표 의미에 맞춰 `+0.0`으로 canonicalize하고, NaN/무한대는 앞선 spatial validation에서 거부한다. 중복이 발견되면 `DuplicateCoordinate`로 해당 slice 전체를 rollback하며, 조용한 `ON CONFLICT DO NOTHING` 유실은 허용하지 않는다. duplicate 검사 예산을 초과해 uniqueness를 증명할 수 없는 slice도 `ResourceLimitExceeded`로 거부한다. 기존 재시도는 이미 성공한 slice의 동일 row를 `ON CONFLICT DO NOTHING`으로 멱등 처리한다.

## 3. 설계 대안과 결정

### 대안 A — 내부 sampler와 기존 `location`/`attrs` 재사용 (채택)

축의 차원 수와 방향을 숨기는 내부 `CoordinateSampler`를 추가하고, 서비스는 sampler에서 셀 좌표와 auxiliary 값을 받는다. 저장 schema는 바꾸지 않고 `location`에 canonical 공간 좌표를, `attrs`에 추가 auxiliary 값을 저장한다.

- 장점: 기존 API·DDL·partial unique index·조회 모델을 유지한다. 1D와 2D를 같은 import loop에서 검증할 수 있다.
- 비용: JSONB를 셀마다 직렬화하며 auxiliary 값의 ad-hoc 조회는 전용 테이블보다 불편하다. 따라서 지원 auxiliary는 numeric rank 1/2와 grid dimension에 정렬되는 값으로 제한하고, 셀 payload와 allocation 예산을 문서화한다.

### 대안 B — 셀 좌표 전용 테이블 추가

`netcdf_grid_value_coordinates`를 추가해 coordinate variable 이름, 값, dimension 위치를 정규화한다.

- 장점: 개별 auxiliary 값의 검색과 인덱싱이 명확하다.
- 거부 이유: migration·foreign key·upsert·cleanup·repository API가 함께 바뀌고, 이번 child가 요구하는 기존 import 계약보다 범위가 커진다.

### 대안 C — 2D 축을 1D로 평탄화

행/열별 대표 좌표로 축을 줄여 기존 `CoordinateAxis1D` 경로를 재사용한다.

- 거부 이유: curvilinear grid의 셀별 좌표를 잃어 수용 기준을 위반한다.

## 4. 내부 계약

### 4.1 `VariableAxisMap`

기존 `latDim`/`lonDim`만으로는 auxiliary 2D 축의 dimension 순서를 표현할 수 없으므로 내부 map을 다음 정보로 확장한다.

- `timeDim`, `levelDim`: 데이터 변수 rank에서의 시간·레벨 위치. 해당 축이
  없는 rank에서는 `null`이며, full-rank index에 조건부로만 반영한다.
- `gridRowDim`, `gridColumnDim`: 공간 격자의 행·열 dimension 위치
- `latBinding`, `lonBinding`: coordinate variable 이름, `CoordinateAxis1D` 또는 `CoordinateAxis2D`, 축 dimension 위치와 shape
- `auxiliaryBindings`: lat/lon이 아닌 CF coordinate variable의 이름, numeric rank, dimension 매핑

축 후보는 다음 순서로 수집한다.

1. 데이터 변수 dimension에 대응하는 `dataset.findCoordinateAxis(dimensionName)` 결과
2. 데이터 변수의 `coordinates` attribute에 나열된 이름
3. `AxisType`, `standard_name`, 단위, 기존 이름 fallback(`lat`, `latitude`, `y`, `lon`, `longitude`, `x` 등)

후보 이름은 full name과 short name을 모두 비교하되, 같은 역할에 둘 이상의 서로 다른 축이 남으면 추측하지 않고 `UnsupportedCoordinateAxis`를 발생시킨다. 2D lat/lon binding은 두 축이 공유하는 두 grid dimension을 기록하며, 축 내부의 행/열 순서는 coordinate axis의 dimension 순서를 따른다.

지원하는 auxiliary coordinate는 다음과 같다.

- numeric rank 1: 하나의 grid dimension에 정렬된 값
- numeric rank 2: 두 grid dimension에 정렬된 값
- grid dimension의 부분집합이 아닌 rank 또는 문자열/object 값: `UnsupportedCoordinateAxis`

### 4.2 `CoordinateSampler`와 `CoordinateReprojector`

내부 sampler의 논리적 결과는 다음과 같다.

```text
CoordinateSample(
    longitude: Double,
    latitude: Double,
    auxiliary: Map<String, Double>
)
```

`CoordinateSample`의 `auxiliary`는 타일의 다음 셀을 샘플링하기 전까지 유효한
읽기 전용 복사본이다. sampler는 다음 호출에서 내부 map을 재사용할 수 있고,
`TileBatchWriter`는 행을 보유하거나 비동기로 전달하지 않고 같은 호출 안에서
JSONB를 즉시 직렬화한 뒤에만 다음 셀을 요청한다. 연속 셀의 값이 섞이지 않는
fixture를 고정해 이 lifetime 계약을 검증한다.

- 1D axis는 `CoordinateReader.read1D(axisName, origin, length)`가 반환한
  bounded window에서 row/column 값을 조회한다.
- 2D axis는 `CoordinateReader.read2D(axisName, rowOrigin, columnOrigin,
  rowCount, columnCount)`가 반환한 bounded window에서 binding이 기록한 axis
  dimension 순서로 local 값을 조회한다. sampler가 `getCoordValue`를 직접
  호출하거나 reader seam을 우회하는 경로는 허용하지 않는다.
- auxiliary 값은 같은 row/column 인덱스로 샘플링하고 lat/lon 이름은 중복 저장하지 않는다.
- CF `coordinates` 목록의 `AxisType.Time` 및 level/vertical 축은 기존 `timeDim`/`levelDim` binding으로 소비하며 `auxiliaryBindings`에 다시 넣지 않는다. 같은 token이 spatial 역할과 time/level 역할에 동시에 매핑되거나 역할이 충돌하면 `UnsupportedCoordinateAxis`다.
- spatial 좌표는 유한한 값이어야 한다. lat/lon이 NaN 또는 무한대이면 잘못된 POINT를 쓰지 않고 `UnsupportedCoordinateAxis`로 import을 실패시킨다. 비공간 auxiliary의 비유한 값은 해당 key를 생략한다.

CRS 정책은 기존 whitelist를 유지한다.

- EPSG:4326/4269: 1D 또는 2D geographic 좌표를 그대로 `(lon, lat)`으로 사용
- EPSG:3857, UTM 32601–32660/32701–32760, EPSG:3413/3031: source x/y를 셀 좌표로 읽어 EPSG:4326으로 변환
- `grid_mapping_name=latitude_longitude`는 EPSG:4326으로 해석
- 그 외 mapping name, EPSG, proj4j 변환 실패는 `UnsupportedProjection`

Projected 좌표는 tile 범위에서만 source pair를 읽고 필요할 때 bounded pair cache에 넣는다. 2D source axis와 auxiliary coordinate는 axis dimension binding으로 `(row, column)`을 계산한다. 전체 grid를 `grid cell 수 × 2 doubles`로 복제하지 않으며, pair/auxiliary cache·duplicate key set이 64 MiB 예산을 넘으면 즉시 실패한다. 반복되는 time/level slice는 tile을 다시 읽되 coordinate 전체를 장기간 보유하지 않는다. 1D `coordValues`도 전체 축 배열을 materialize하지 않고 필요한 tile 구간만 bounded read/cache한다.

source 좌표와 재투영 후 좌표 모두 유한해야 한다. 최종 longitude는 `[-180, 180]`, latitude는 `[-90, 90]` 범위여야 하며 범위를 벗어나면 `UnsupportedProjection`으로 거부한다. `grid_mapping` attribute가 존재하면 mapping variable을 반드시 해석한다. `latitude_longitude` 또는 ASCII 정규식 `[0-9]+`에 맞는 exact integer EPSG 문자열만 허용하고, 공백·부호·소수형(`4326.0`)·중복/충돌 attribute·malformed/missing/overflow 값은 WGS84로 fallback하지 않고 `UnsupportedProjection`으로 거부한다.

EPSG attribute는 `String`의 ASCII digits 또는 NetCDF integral numeric type만 허용하며, integral numeric은 소수부가 0인지와 `Int` 범위 내인지 먼저 확인한 뒤 문자열로 변환한다. `epsg_code`, `spatial_ref`, `grid_mapping_name`이 함께 있으면 동일한 CRS를 가리킬 때만 허용하고, 하나라도 충돌하거나 `latitude_longitude`와 다른 EPSG가 공존하면 거부한다.

### 4.3 dimension 순서를 보존하는 데이터 read

각 rank별 논리적 slice는 기존 origin/shape와 lease 경계를 유지하되, 셀 값을 `indexIterator` 순서에 의존하지 않는다. spatial dimension이 0이거나 dimension product가 음수/overflow이면 빈 import으로 진행하지 않고 `UnsupportedCoordinateAxis`로 거부한다. spatial dimension은 결정적인 row-major tile planner로 나눈다. `tileCols = min(lonN, 65,536)`, `tileRows = min(latN, max(1, 65,536 / tileCols))`로 시작하고, `tileRows * tileCols`를 checked `Long`으로 계산해 65,536을 넘으면 더 작은 shape으로 내린다. rank 2–4의 `variable.read(origin, shape)`에는 time/level dimension을 1로, tile row/column dimension을 planner shape으로 넣으며, 그 외 dimension은 1로 둔다. tile마다 read된 `UcarArray`와 coordinate/serializer scratch는 transaction 종료 후 즉시 참조를 버린다.

```text
timeDim?.let { indices[it] = 0 }    // currentTime은 tileOrigin[it]에 있음
levelDim?.let { indices[it] = 0 }   // currentLevel은 tileOrigin[it]에 있음
indices[gridRowDim] = row - tileRowOrigin
indices[gridColumnDim] = column - tileColumnOrigin
raw = tileData.getDouble(tileIndex.set(indices))
sample = sampler.sample(tileRowOrigin + localRow, tileColumnOrigin + localColumn)
```

따라서 `[time, y, x]`, `[time, x, y]`, `[y, x, time]` 모두 같은 셀 좌표와 값으로 저장된다. `timeIdx`와 `levelIdx`는 map의 위치를 사용하고, `lastSliceIdx` 선형화와 heartbeat lease는 변경하지 않는다.

`tileIndex`와 `indices`는 `tileShape` rank로 한 번 생성해 tile 안에서 재사용한다. `variable.read(tileOrigin, tileShape)` 결과의 local time/level은 항상 0이고 global currentTime/currentLevel은 `tileOrigin`에만 둔다. `timeDim`/`levelDim`이 `null`이면 해당 assignment를 생략하고, 존재할 때만 local index 0을 쓴다. local row/column offset만 `tileIndex`에 넣고, 2D axis는 `tileRowOrigin + localRow`/`tileColumnOrigin + localColumn`, 1D axis는 해당 global offset으로 읽는다. tile의 `UcarArray`, axis window, auxiliary window, serializer scratch는 `finally`에서 참조를 해제하며 다음 tile과 공유하지 않는다.

`Index`와 `IntArray`는 tile 안에서 재사용하고, 가능한 경우 precomputed stride를 사용한다. 별도 column을 추가하지 않고 `lease_expires_at`의 DB timestamp를 opaque lease token으로 사용한다. 기존 public `acquireLease(fileId, variableName, leaseTtl: Duration)` 시그니처와 custom TTL을 유지하며, 기본 TTL만 5분이다. `leaseTtl`은 양의 whole-second로 검증하고 SQL에는 초 단위 parameter를 `CURRENT_TIMESTAMP + (? * INTERVAL '1 second')`로 바인딩해 애플리케이션 clock을 사용하지 않는다. acquire마다 DB UTC 기준의 새 expiry를 발급하며 token 재사용을 허용하지 않는다. 각 tile transaction은 progress row lock이 tile commit까지 유지되는 다음 순서를 지킨다.

1. `assertLeaseOwner`/`touchLease`가 `WHERE id=? AND status='IN_PROGRESS' AND lease_expires_at=? AND lease_expires_at > CURRENT_TIMESTAMP` 조건으로 현재 expected lease token을 원자적으로 검증·연장한다. affected row가 1이 아니면 즉시 `ImportLeaseLost`를 발생시킨다. 이 UPDATE/`SELECT ... FOR UPDATE`가 보유한 progress row lock은 transaction commit까지 유지되어 takeover가 대기하도록 한다.
2. tile을 최대 1,000행씩 JDBC batch flush한다.
3. commit 직전에 같은 token/status/`lease_expires_at > CURRENT_TIMESTAMP` 조건으로 `assertLeaseOwner`를 다시 수행한다. 마지막 tile에서만 `renewLease(lastSliceIdx=sliceIdx)`를 같은 transaction에 포함하고, 중간 tile은 checkpoint를 전진시키지 않고 `touchLease`만 수행한다. 이 fence가 통과하지 않으면 insert·checkpoint·lease 변경 모두 rollback한다.

각 spatial slice는 **두 pass**로 처리한다. 첫 pass는 tile read만 수행해 slice 전체 canonical `(lon,lat)` exact key를 duplicate set에 넣고 uniqueness를 증명한다. preflight tile을 읽은 뒤 매 경계에서 checkpoint를 전진시키지 않는 `touchLease`/expiry fence와 cancellation/deadline 검사를 수행한다. 이 pass가 성공하기 전에는 어떤 DB insert도 시작하지 않는다. 두 번째 pass에서만 tile transaction을 실행한다. 따라서 tile 경계의 duplicate도 놓치지 않으며, preflight 실패는 이전 tile commit이 없는 상태에서 slice 전체를 거부한다.

tile 중간 장애는 논리적 slice를 미완료로 남기고, 재시도 시 `ON CONFLICT DO NOTHING`으로 이미 기록된 동일 row를 건너뛴다. lease token 검증이 시작·commit 직전 어느 쪽에서든 실패하면 insert와 progress 변경이 같은 transaction에서 rollback되어 stale importer가 새 owner의 progress를 덮어쓸 수 없다. `markFailed`도 동일한 token/status/`lease_expires_at > CURRENT_TIMESTAMP` 조건과 affected-row 검사를 사용하며, 실패 시 원래 예외를 가리지 않고 suppressed로 보존한다.

내부 seam의 소유권과 트랜잭션 경계는 다음 signature 수준으로 고정한다.

- `VariableReader.read(origin: IntArray, shape: IntArray): UcarArray`는 data
  variable만 감싸며, 구현은 각 호출의 rank·origin·shape·cell count를 기록할 수
  있어야 한다. production은 full-array read를 만들지 않고 tile만 반환하며,
  반환 배열의 소유자는 호출자이고 tile `finally`에서 참조를 폐기한다.
- `CoordinateReader.read1D(axisName: String, origin: Int, length: Int): DoubleArray`와
  `read2D(axisName: String, rowOrigin: Int, columnOrigin: Int, rowCount: Int,
  columnCount: Int): DoubleArray`는 모든 `CoordinateAxis1D`/`CoordinateAxis2D`와
  auxiliary 접근을 감싼다. 내부에서 NetCDF `getCoordValue`를 사용하더라도
  bounded window 안에서만 호출하며, recording seam은 두 API의 origin/shape와
  cell count를 기록한다. `read2D` 결과는 row-major flat 배열이며
  `index = localRow * columnCount + localColumn`으로 해석한다. 반환 배열은 tile
  `finally`에서 폐기하고 full-grid materialization은 금지한다.
- `CoordinateSampler.sample(globalRow: Int, globalColumn: Int,
  target: MutableCoordinateSample): Unit`은 `VariableReader`/`CoordinateReader`가
  제공한 읽기 window와 axis dimension 순서를 사용한다. `target`은 호출자가
  소유하고 다음 셀 전에 clear하며, sampler는 `CoordinateSample` 또는 auxiliary
  map을 보유하지 않는다.
- `TileBatchWriter.write(connection: java.sql.Connection,
  rows: List<TileRow>): BatchWriteResult`는 **현재 Exposed transaction이 보유한
  동일 `Connection`만** 받는다. production writer는 `DataSource.getConnection`,
  새 JDBC/Exposed transaction, connection close를 수행하지 않으며
  `PreparedStatement`만 닫는다. 따라서 lease fence, insert, checkpoint, commit과
  rollback이 하나의 DB transaction/row lock 안에 있다. 테스트 seam은
  `connection === TransactionManager.current().connection.connection`,
  executeBatch 행 수, commit 순서와 강제 rollback 뒤의 rows-written를 기록해
  이 원자성을 검증한다.

운영 JDBC writer는 위 `TileBatchWriter` seam을 통해
`PreparedStatement.addBatch/executeBatch`를 호출한다. 테스트 구현은 각
`executeBatch` 호출의 pending 행 수 목록(모든 원소가 `<=1,000`인지 assertion),
flush 횟수·commit 순서·rows-written를 기록한다. `VariableReader` recording seam은
모든 data `read(origin, shape)`와 `CoordinateReader`의 1D/2D window 및 cell
count를 기록해 non-contiguous dimension, coordinate window 상한, full-array read를
검증한다.
PostgreSQL batch 결과는 `1`(insert) 또는 `0`(멱등 conflict)만 정상으로 취급하며,
`SUCCESS_NO_INFO`/기타 반환값은 동일 canonical key의 별도 bounded 조회로
검증한 뒤 설명되지 않으면 transaction을 rollback한다. 0행 conflict도 preflight와
기존 checkpoint가 가리키는 동일 row인지 확인하지 못하면 rollback한다.

### 4.4 저장 계약

`NetCdfGridValueTable`의 기존 컬럼을 그대로 사용한다.

| 값 | 저장 위치 | 계약 |
|---|---|---|
| spatial longitude/latitude | `location` | `ST_SetSRID(ST_MakePoint(lon, lat), 4326)`; longitude first |
| 추가 CF auxiliary 값 | `attrs` | `{ "coordinateVariableName": numericValue }`; lat/lon key는 제외 |
| rank 1 time-only 값 | `location=null`, `attrs=null` | 기존 계약 유지 |

`attrs`는 셀별 map을 재사용하는 serializer로 numeric 값만 직렬화하고, 빈 map은 `NULL`로 둔다. 결과 문자열은 escaped UTF-8 byte length를 계산한 뒤 8,192 bytes 이하일 때만 `PreparedStatement`의 typed `jsonb`/`PGobject` placeholder로 바인딩하며 SQL interpolation은 하지 않는다. key는 원래 full coordinate variable name을 Unicode NFC로 정규화한 strict UTF-8 형태로 사용하고, invalid surrogate·control 문자·길이 초과·canonicalization 후 충돌을 거부한다. 예약된 `__bluetape4k_` 접두사는 coordinate variable name으로 사용할 수 없다. JSONB 직렬화 또는 batch binding 실패는 해당 tile transaction을 실패시키고 원래 예외를 보존한 채 best-effort `FAILED` 기록을 남긴다. 기존 partial unique index와 `ON CONFLICT DO NOTHING`은 재시도 멱등성을 위해 유지한다.

내부 `TileRow`의 `longitude`/`latitude`는 `Double?`이다. rank 1 time-only 행은 두 값이
모두 `null`이어야 하며, rank 2–4 spatial 행은 둘 다 non-null이어야 한다. 한 쪽만
`null`인 mixed 상태는 SQL에 바인딩하기 전에 내부 invariant 위반으로 거부한다. writer는
두 값이 모두 `null`이면 기존 `location` column에 typed SQL `NULL`을 바인딩하고, 두 값이
모두 non-null이면 `(lon, lat)` 순서로 PostGIS point를 만든다. 이 invariant를 rank별
테스트와 recording writer에서 고정해 nullable API와 저장 계약이 어긋나지 않게 한다.

## 5. 오류와 상태 전이

- 필수 spatial coordinate가 없으면 `MissingCoordinate`.
- coordinate axis가 ambiguous하거나 rank/shape/dimension이 지원 계약과 맞지 않으면 새 `NetCdfException.UnsupportedCoordinateAxis`.
- 같은 spatial slice에서 canonical 위치가 중복되면 `NetCdfException.DuplicateCoordinate`.
- dimension product, token/count, cache, batch, JSONB 또는 파일 크기 예산을 넘으면 `NetCdfException.ResourceLimitExceeded`.
- whitelist 밖 CRS 또는 proj4j 변환 실패, malformed/missing `grid_mapping`(projected axis), 정확히 해석할 수 없는 EPSG 값은 `UnsupportedProjection`이다. `grid_mapping`이 존재할 때 WGS84로 묵시적 fallback하지 않는다. EPSG는 정수 문자열을 exact parse하며 numeric narrowing/overflow를 허용하지 않는다.
- NUL/control 문자, URI scheme, 원격 URL, 심볼릭 링크 경로, regular file이 아닌
  경로 또는 실제 open 실패는 `NetCdfException.FileOpen`으로 감싼다. 호출자는
  허용된 regular file 경로로 수정한 뒤 `registerFile`을 새로 호출하고 새
  `fileId`로 import해야 하며, 잘못된 경로의 progress를 resume하지 않는다.
- 데이터 값 NaN/`_FillValue`는 기존처럼 행을 skip하고 `netcdf.import.nan.skipped`를 증가시킨다.
- import 시작 시 persisted file identity `(fileKey, size, lastModifiedTime)`와 현재 regular-file identity를 비교한다. 이 identity는 기존 file record의 JSONB metadata에 예약된 `__bluetape4k_source_fingerprint`로 보존하며, fingerprint는 `fileKey|size|lastModifiedTime.epochNanos`의 strict ASCII 표현으로 기록한다. `fileKey`가 없는 파일과 예약 key를 이미 가진 input metadata는 등록을 거부한다. 누락되거나 불일치하는 fingerprint는 항상 `FileChanged`로 중단하고, 수정된 파일은 반드시 새 등록·새 `fileId`를 만든다. 기존 `fileId`의 progress 또는 partial rows를 조용히 재사용하지 않는다. 누락된 구형 record도 새 import만 허용하고 resume은 `FileChanged`로 중단한다. 파일 내용은 progress가 존재하는 동안 immutable하다는 운영 전제를 함께 문서화하고, validation 직후 다시 stat하여 TOCTOU 교체도 감지한다.
- progress invariant는 `PENDING/FAILED => lease=null, completedAt=null`, `IN_PROGRESS => lease!=null, completedAt=null`, `COMPLETED => lease=null, completedAt!=null`이며, checkpoint는 dataset의 실제 `totalSlices`에 대해 `-1..totalSlices-1` 범위여야 한다. `IN_PROGRESS`인데 `lease_expires_at IS NULL`인 malformed progress는 acquire 단계에서 row lock 아래 원자적으로 `FAILED` repair 후 새 lease를 부여한다. 상태/owner/token invariant가 서로 어긋나거나 `COMPLETED` checkpoint가 마지막 slice와 다르면 `CorruptProgress`로 중단한다. 이때 `COMPLETED`로 추측 전환하지 않는다.
- 모든 tile transaction은 lease fence를 수행한다. `CancellationException`/`InterruptedException`은 원래 예외와 interrupt 상태를 보존하고 `FAILED`로 바꾸지 않으며 lease expiry에 맡긴다. 그 밖의 실패는 best-effort `markFailed`를 시도하되 mark 자체의 오류를 원래 예외에 suppressed로 추가한다. slice timer는 성공·실패 모두 `finally`에서 종료한다.
- 기존 `ImportAlreadyRunning`, `ImportLeaseLost`, `FAILED`/`COMPLETED` 및 재호출 no-op 계약은 변경하지 않는다.

lease 만료 비교·연장은 DB의 UTC `CURRENT_TIMESTAMP`를 단일 기준으로 사용하고, 애플리케이션 clock을 SQL 비교값으로 사용하지 않는다. 테스트에서는 repository에 주입한 deterministic clock 또는 DB clock fixture로 만료 직전/동시 takeover 경계를 고정한다. 운영 문서에는 DB와 importer 호스트의 UTC 동기화 요구를 남긴다.

`UnsupportedCoordinateAxis`, `DuplicateCoordinate`, `ResourceLimitExceeded`,
`FileChanged`, `CorruptProgress`는 `2.0.0` major release에서 sealed base에 추가하는
다섯 public subtype이다. 따라서 `1.x` 소비자가 구체 subtype을 exhaustive `when`으로
분기했다면 재컴파일 때 다섯 branch를 추가해야 하며, 새 API를 받는 코드는 구체 subtype
대신 `NetCdfException`을 catch하고 `when`에 `else`/기본 오류 경로를 둔다. 공개 API
호환성 테스트는 다섯 subtype branch와 base-type `else` fixture를 모두 컴파일하고,
각 구조화 필드와 stable reason이 보존되는지 검증한다. 오류 메시지에는 변수·축·원인,
fileId/progressId 또는 resource limit 등 subtype별 진단 필드를 포함한다.

## 6. 테스트 설계

`NetCdfSampleWriter`에 다음 fixture를 추가한다.

1. 2D geographic `lat(y,x)`, `lon(y,x)`, `temperature(time,y,x)` fixture
2. CF `coordinates="lat lon altitude"`와 `altitude(y,x)` auxiliary fixture
3. 데이터 dimension 순서를 `[time,x,y]`로 바꾼 fixture
4. 지원 projected CRS fixture와 unsupported `grid_mapping_name` fixture
5. duplicate canonical point, malformed/overflow EPSG, invalid range, remote/symlink/NUL path fixture
6. `coordinates="time lat lon altitude"` fixture에서 time/level rank-1 축을 auxiliary로 오인하지 않는 경로

`NetCdfCatalogServiceTest` 및 필요한 internal unit test에서 다음을 고정한다.

- 각 셀의 `(lon, lat, value)` read-back과 JSONB auxiliary 값
- 2D 축의 행/열 orientation과 비표준 변수 dimension 순서
- `CoordinateReader.read2D` row-major index 공식과 row/column이 뒤집힌 축 fixture
- 지원 CRS reprojection 결과와 `UnsupportedProjection`
- 누락·ambiguous·shape 불일치 축의 `UnsupportedCoordinateAxis`
- 기존 1D 및 rank 1–4, NaN/`_FillValue`, upsert 회귀
- 2D fixture의 resume, stale lease, 동시 호출, progress 상태, Micrometer counter/timer
- 강제 lease takeover 중 stale importer rollback, null lease repair, out-of-range checkpoint, same-size 파일 교체와 validation/open 사이 TOCTOU 후 resume 거부
- 서로 다른 두 tile 경계(row/column offset이 다른 위치)에 동일 canonical point를 배치한 fixture에서 두 번째 pass 시작 전 `rows-written=0`과 `DuplicateCoordinate`를 확인한다.
- progress 상태 invariant, owner/token 불일치, `COMPLETED` checkpoint 불일치, DB UTC clock 만료 경계와 injected clock 테스트
- `NetCdfException`을 base 타입으로 catch하고 `else` 경로를 둔 대표 `2.0.0` 소비자
  compile fixture, 새 `UnsupportedCoordinateAxis`의 변수·축·원인 필드 보존
- 대용량 contract fixture는 고정 `1024×1024` spatial cells, deterministic values, one auxiliary, `slow-netcdf` tag로 생성한다. 1D regression baseline은 동일한 cell 수·값 생성기를 사용하는 기존 1D path로 별도 고정하고, 2D+auxiliary `cells/sec`는 workload가 다르므로 절대 측정값으로만 보고한다. 각 baseline/feature run은 새 unique fileId와 clean DB(테이블 truncate 또는 새 Testcontainers instance)로 시작해 `ON CONFLICT`/기존 progress가 측정치를 왜곡하지 않게 한다. baseline과 feature 모두 동일 JVM/Gradle/PostGIS image, 1 warm-up + 3 sequential measured runs, median을 사용한다. `cells/sec` 범위는 fixture read 시작부터 마지막 tile commit까지(생성·register 제외)다. 결과 artifact에 JVM `-Xmx`, Docker image, Gradle command, run durations/median, peak heap(관찰값), owned working-set counters를 남긴다. tile read 셀 수 `<=65,536`, JDBC batch 행 수 `<=1,000`, owned working-set `<=128 MiB`, 1D path 처리량은 origin baseline median 대비 20% 이상 하락하지 않아야 한다.
- JSONB key control/quote/injection-like name, invalid surrogate, empty attrs, escaped UTF-8 payload의 8,192/8,193-byte 경계, typed placeholder binding을 검증한다.

실제 PostGIS/Testcontainers 검증은 모듈 간 동시 실행을 피하고 한 Gradle invocation에서 순차적으로 수행한다.

## 7. 문서와 호환성

- `NetCdfCatalogService`와 internal class KDoc에서 `CoordinateAxis2D`가 지원 범위임을 설명하고, unsupported axis/CRS 예외를 갱신한다.
- `utils/science/README.md`와 `README.ko.md`의 기능표·예제·제약을 동일하게 갱신한다.
- `docs/followup-issues/utils-science-readme-rewrite.md`의 #1352 후속 상태를 구현 결과와 일치시킨다.
  - public schema migration은 없다. 기존 `attrs`를 읽는 소비자는 영향이 없고, 새 auxiliary key를 해석하지 않아도 `location` 기반 기존 조회는 계속 동작한다.
- README와 KDoc에는 다음 blocking 호출·복구 예시를 포함한다.

  ```kotlin
  val executor = Executors.newVirtualThreadPerTaskExecutor()
  val task: Future<Long> = executor.submit {
      val fileId = service.registerFile("/srv/netcdf/grid.nc") // trusted-admin 경계
      service.importGridValues(fileId, "temperature")
      fileId
  }
  try {
      val fileId = task.get(30, TimeUnit.MINUTES)
      // DB read-back: location=(lon, lat), attrs["altitude"]=numeric value
      fileId
  } catch (timeout: TimeoutException) {
      task.cancel(true) // cooperative interrupt; completion is not implied
      throw timeout
  } finally {
      executor.shutdownNow()
      check(executor.awaitTermination(30, TimeUnit.SECONDS)) {
          "import worker did not terminate"
      }
  }
  ```

  `cancel(true)`는 협력적 취소일 뿐 worker 종료나 side effect 부재를 보장하지 않는다.
  timeout/interrupt 뒤에는 bounded `awaitTermination`을 기다리고, 등록된 `fileId`가
  있으면 DB에서 progress·lease·partial rows를 재조회해 active connection=0, 추가
  renew 없음, 현재 tile rollback을 확인한 뒤에만 재시도한다. worker가 30초 안에 종료되지
  않거나 상태를 증명할 수 없으면 재시도하지 않고 운영자에게 escalation한다.

  `UnsupportedCoordinateAxis`/`UnsupportedProjection`/`ResourceLimitExceeded`는
  입력·metadata를 고친 뒤 항상 새 등록·새 `fileId`로 호출한다. `FileOpen`/`FileChanged`/
  `MissingCoordinate`/`VariableNotFound`/`UnsupportedVariable`/`DuplicateCoordinate`도
  경로·파일·축·변수를 수정한 뒤 새 `fileId`를 만들며, 변경된 fingerprint의 기존
  progress/partial rows를 재사용하지 않는다. `CorruptProgress`는 자동 재시도를 중지하고,
  application-owned admin 절차에서 progress row를 `SELECT ... FOR UPDATE`로 잠근 뒤
  상태·checkpoint·lease의 기준 상태 기록과 `progressId`를 audit/quarantine에 기록한다.
  기존 column만 사용하는 경우 기준 상태 기록 후 `PENDING/IN_PROGRESS/FAILED` row는
  stable `CORRUPT_PROGRESS:<progressId>` 오류와 함께 `FAILED`, `lease=null`로 격리하고,
  `COMPLETED` row는 변경하지 않은 채 old `fileId`를 차단한다. partial rows는 old
  `fileId`에 묶어 보존하거나 tenant retention 정책에 따라 삭제한 뒤, 수정·검증한 파일을
  반드시 새 등록·새 `fileId`로 import한다. library 내부에는 progress in-place reset API가
  없으며, 명시적 admin repair 없이 reset하지 않는다.

  `ImportAlreadyRunning`/`ImportLeaseLost`는 각각 최대 3회만 재시도하고 backoff는
  1초·2초·4초로 제한한다. 세 번 소진하면 원래 typed exception을 표면화하고 alert를
  발생시키며 무한 loop를 만들지 않는다. `CancellationException`/interrupt는 원래
  예외와 interrupt 상태를 보존한 채 호출자가 취소 결과를 처리한다.
- `CHANGELOG.md`와 release note는 이번 요청에 포함하지 않으며, release workflow에서 별도 작성한다.

## 8. 실패 모드와 완화

| 실패 모드 | 조기 신호 | 완화 |
|---|---|---|
| 2D axis 행/열 방향을 잘못 해석 | fixture의 한 셀 위치가 기대값과 불일치 | axis dimension 순서와 데이터 dimension 위치를 별도 binding하고 read-back 테스트 고정 |
| auxiliary 값과 데이터 셀 shape 불일치 | map 생성 시 rank/shape mismatch | import 전에 shape 검증 후 `UnsupportedCoordinateAxis`로 조기 거부 |
| dimension 순서가 바뀐 변수의 값/좌표 엇갈림 | `[time,x,y]` fixture에서 값이 전치됨 | `Index.set(IntArray)` 기반 full-rank lookup 사용, iterator 사용 금지 |
| JSONB 직렬화 또는 batch binding 실패 | tile transaction 예외, progress `FAILED` | numeric map·payload cap·typed placeholder만 허용하고 원래 예외를 보존한 best-effort failure 기록 |
| projected 2D cache의 메모리 증가 | 대용량 fixture heap pressure | bounded tile/lazy cache, 64 MiB budget, checked `Long` 산식, peak heap/cells-sec gate |
| unsupported grid mapping을 WGS84로 오인 | rotated/tripolar fixture가 성공함 | EPSG/grid_mapping whitelist를 먼저 검사하고 typed exception 테스트 고정 |
| lease 만료 중 stale tile이 새 owner를 덮어씀 | takeover race에서 progress/row 혼합 | 모든 tile transaction에 lease fence, 중간 tile checkpoint 금지, 실패 시 전체 transaction rollback |
| 동일 위치의 서로 다른 셀이 unique index에서 사라짐 | duplicate point fixture의 row 수 부족 | schema migration 없이 duplicate preflight 후 `DuplicateCoordinate` rollback; 조용한 conflict 유실 금지 |
| malformed progress가 영구 잠금 또는 조기 완료를 만듦 | null lease/범위 밖 checkpoint | acquire repair와 범위 검증, `CorruptProgress`/metric, COMPLETED 추측 금지 |
| 파일 교체 후 resume으로 데이터 혼합 | size mismatch 또는 fixture fingerprint 차이 | trusted immutable-file 전제와 size check; 불일치 시 `FileChanged`로 재등록 요구 |
| lease token/상태 invariant가 깨진 progress | null owner, stale token, 조기 COMPLETED | row lock 아래 원자 repair/검증, DB UTC clock과 affected-row fence, `CorruptProgress`로 중단 |
| metadata 총량 또는 압축 고팽창으로 자원 고갈 | variable/dimension/metadata/cell/slice 예산 초과 | 등록·import 전 hard limit, checked `Long`, rejection metric, caller deadline |

## 9. 수용 기준과 DoD

| #1352 기준 | 검증 방법 | 완료 조건 |
|---|---|---|
| 2D lat/lon 및 CF auxiliary fixture import | `NetCdfCatalogServiceTest` | fixture 생성·import·row read-back PASS |
| 셀 좌표와 값 매핑 보존 | 2D/비표준 dimension order 테스트 | 모든 셀의 좌표·값이 기대 인덱스와 일치 |
| 지원 CRS reprojection 및 unsupported CRS error | EPSG fixture와 `assertFailsWith` | 변환 결과 PASS, `UnsupportedProjection` PASS |
| 기존 1D/rank 1–4 및 resume/lease 회귀 | 기존 전체 service test + 2D resume/concurrency | 회귀 0건, progress 상태와 lease 경계 PASS |
| 대용량 memory/performance 및 schema 영향 문서화 | KDoc/README/spec, 고정 bounded fixture fresh run | 전체 배열 read 없음, tile/batch/cache/owned working-set/throughput 예산 PASS, JVM peak은 별도 관찰값, migration 없음과 duplicate 거부 정책 명시 |

최종 구현 DoD는 다음 상태에서만 `PASS`다.

- 승인된 명세·계획과 branch diff가 일치한다.
- targeted Testcontainers 테스트, compile, detekt/static check, `git diff --check`가 fresh run에서 성공한다.
- Type A 독립 review 6개 관점과 통합 review에 P0/P1이 없다.
- PR body가 한국어이고 마지막 섹션이 정확히 `## DoD Status`다.
- exact-head CI/review와 merge-ready report를 확인한 뒤, 사용자의 fresh approval 후에만 rebase merge한다.

## 10. Writer gate

| 항목 | 결과 | 근거 |
|---|---|---|
| SPW-01 audience/purpose/evidence | PASS | #1352/#1421, current source anchors, NetCDF-Java 5.9.1 API, baseline compile을 명시했다. |
| SPW-02 artifact contract | PASS | 문제, 제약, 대안, 내부 계약, 자원·보안 경계, 오류, 테스트, 호환성, 실패 모드, 수용 기준을 포함했다. |
| SPW-03 Korean technical register | PASS | 한국어 기술 문체를 사용하고 API·명령·식별자·URL·숫자는 보존했다. |
| SPW-04 technical traceability | PASS | 기존 `VariableAxisMap`, `CoordinateReprojector`, service read path, schema unique index/lease, issue acceptance를 각 결정에 연결했다. |
| SPW-05 read-back | PASS | Markdown heading/table/code fence와 결정·제약·unchecked 범위를 재독했다. |
