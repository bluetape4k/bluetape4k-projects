# 이슈 #1802 Java 직렬화 UID 호환성 교훈

## 맥락

공개된 `bluetape4k-geo:2.0.0`에서 `Geocode`, `Address`, `BingAddress`, `GoogleAddress`는 명시적 `serialVersionUID` 없이 Java 기본 computed UID를 사용했다. 후속 변경에서 네 클래스에 `1L`을 추가하면서, 좌표와 주소 데이터의 직렬화 필드가 유지됐어도 이전 stream과 클래스 descriptor가 달라졌다. 실제 `ObjectInputStream`은 `InvalidClassException`으로 기존 값을 거부했다.

## 결정

- 기존 UID는 현재 소스나 새 빌드에서 추정하지 않고 공개된 `2.0.0` 바이너리의 `ObjectStreamClass`에서 확인한다.
- 호환성이 필요한 클래스는 그 버전의 computed UID를 명시한다.
- 이전 artifact로 생성한 실제 `ObjectOutputStream` fixture를 저장하고, 현재 코드가 이를 읽은 뒤 모든 주요 필드 값을 복원하는지 검증한다. `Address`는 abstract이므로 Bing/Google 하위 클래스 fixture의 superclass descriptor와 UID assertion으로 확인한다. 두 fixture에는 `AbstractValueObject` descriptor도 포함되며 2.0.0 computed UID `-202736522154801100`을 함께 고정한다. 공통 base UID를 변경하거나 명시적으로 추가하면 상속하는 23개 타입의 직렬화 범위가 넓어지므로 별도 감사에서 다룬다.
- UID가 같다는 사실만으로 필드 schema 호환성을 가정하지 않는다. 필드가 추가·제거·변경됐다면 실제 legacy stream의 누락 필드와 기본값도 별도로 검증하고, 호환되지 않는 migration이면 그 계약을 명시한다.

## 확인된 UID와 fixture

기준 artifact는 `io.github.bluetape4k:bluetape4k-geo:2.0.0`이며 JAR SHA-256은 `1b5303ab04bf34197e1e9152c4849fd90d995b70d1361c6dfe867ca953c51328`이다.

| 클래스 | 2.0.0 computed UID |
|---|---:|
| `Geocode` | `-9090722762707661091` |
| `Address` | `7156298922689609881` |
| `BingAddress` | `-6688773129200423988` |
| `GoogleAddress` | `-8088611479544946254` |

테스트 fixture는 위 artifact의 클래스로 생성했다.

| Fixture | SHA-256 |
|---|---|
| `geocode-v2.0.0.ser` | `1770dd1fc3839966b073441fe6baef100f445a71882e5d6c1d161a50cb7a052a` |
| `bing-address-v2.0.0.ser` | `6e5504eb75f11cc779e0caee7f132d5574c35ad56f744b5c4472b5420f76a0bf` |
| `google-address-v2.0.0.ser` | `8a97a71796a18a22218ea3efa5bfb5139f73280d75a799247f9c4ca278a38825` |

테스트는 현재 구현의 UID와 각 fixture의 SHA-256을 함께 확인한다. fixture를 현재 코드로 다시 만들면 해시 assertion이 실패하므로, 회귀 테스트가 실제 2.0.0 stream을 계속 사용한다.

## 호환성 범위와 시험판 직렬화 자료 migration

이 변경은 stable `2.0.0`의 computed UID를 호환성 기준으로 선택한다. 2026-09-27 `Publish Snapshot` workflow run [#36325460027](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/36325460027)은 commit [`cf12244e041c0beb01696de4c715a429ef49254d`](https://github.com/bluetape4k/bluetape4k-projects/commit/cf12244e041c0beb01696de4c715a429ef49254d)을 Maven Central Snapshot에 게시했고, 그 artifact의 네 타입은 `serialVersionUID = 1L`을 사용했다. 하나의 Java serialization class descriptor에는 UID 하나만 적용되므로 이번 코드가 그 시험판 stream도 읽도록 하지는 않는다.

해당 `2.1.0-SNAPSHOT` 빌드에서 만든 직렬화 값이나 cache를 보존해야 하는 사용자는 이 버전으로 이동하기 전에 이를 삭제하고 다시 생성해야 한다. 두 계열의 stream을 함께 보존하려면 별도의 migration reader/tool과 fixture가 필요하며, 이번 변경에는 포함하지 않는다.

## 검증

- 수정 전 호환성 테스트 4개가 실패했다. 좌표 stream은 stream UID `-9090722762707661091`과 로컬 UID `1` 차이로 거부됐고, 주소 stream은 superclass `Address`의 `7156298922689609881`과 `1` 차이로 거부됐다.
- 수정 후 `JavaSerializationCompatibilityTest` 4개가 통과했다. 네 UID, `AbstractValueObject` 상위 descriptor UID, 좌표와 주소 필드, 세 fixture의 SHA-256을 확인한다.
- `:bluetape4k-geo:test --no-build-cache`: 241개 통과. Bing API key 갱신을 요구하는 `@Disabled` 테스트 클래스 2개에서 6개 invocation이 건너뛰었다.
- `:bluetape4k-geo:detekt --no-build-cache`: `BUILD SUCCESSFUL`. XML 보고서에는 15개 파일의 65개 진단이 있고 변경한 네 production 파일의 진단은 0개다.

## 다음 변경 시 지킬 점

이전에 Java serialization이 가능했던 public 타입에 처음 명시적 UID를 추가할 때 `1L`을 관례처럼 넣지 않는다. 직전 배포 artifact의 computed UID와 field descriptor를 확인하고, 직전 버전 stream을 읽는 회귀 fixture를 추가한다. API/ABI 점검과 별개로 `ObjectInputStream` 호환성은 실제 stream으로 검증한다.

실행 순서도 지킨다. 이 작업에서는 필수 workflow checklist를 첫 코드 변경 뒤에 만들었다. 뒤늦게 checklist를 작성해도 선행 절차가 소급해 통과한 것은 아니다. 다음 작업은 계획 승인 직후 checklist와 workflow receipt를 준비한 뒤 첫 파일 변경을 시작한다. 누락을 뒤늦게 발견하면 순서 위반을 기록하고 CL-06 복구를 수행한 다음, 영향을 받는 테스트와 최종 리뷰를 다시 실행한다.
