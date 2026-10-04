# 문자열 기반 지오코드 파싱의 정밀도 계약

## 맥락

이슈 [#1803](https://github.com/bluetape4k/bluetape4k-projects/issues/1803)은 `Geocode.parse(String)`가 입력 정밀도를 보존할지 `DefaultMathContext`를 적용할지 문서와 테스트로 확정한다. `Geocode(Double, Double)`는 12자리 `DefaultMathContext`를 적용하지만, 문자열 파싱은 입력한 십진수 값과 scale을 보존한다.

## 실패한 가정과 근거

커밋 [5b64fa2](https://github.com/bluetape4k/bluetape4k-projects/commit/5b64fa2ba5aa6298e531a704115ac816181dbfe3)는 스타일 정리 중 `parse`의 `toBigDecimal(DefaultMathContext)` 호출을 `toBigDecimal()`로 바꿨다. 두 호출은 표기만 다른 동등한 변환이 아니다. 전자는 유효숫자 12자리로 반올림하고, 후자는 문자열의 십진수 값과 scale을 유지한다. 이 차이를 고정한 테스트와 KDoc이 없어 변경 후 오랫동안 계약이 분명하지 않았다.

Jackson 직렬화에는 생성자 프로퍼티인 `latitude`, `longitude`뿐 아니라 계산 프로퍼티 `scale`도 포함된다. 처음 작성한 JSON 기대값에서 `scale`을 빠뜨렸고, 테스트가 실제 직렬화 결과 `"scale":15`를 보여 주어 기대값을 바로잡았다.

## 결정과 결과

`Geocode.parse(String)`는 입력 문자열의 정밀도와 scale을 보존하며 `DefaultMathContext`를 적용하지 않는다. `Geocode(Double, Double)`의 기존 12자리 변환 계약은 유지한다. 현재 문자열 파서는 이미 정확 변환을 사용하고 있었으므로 이번 수정은 런타임 결과를 바꾸지 않고 KDoc과 회귀 테스트로 공개 계약을 고정한다.

회귀 테스트는 12자리보다 유효숫자가 많은 좌표와 끝의 0을 포함해 BigDecimal 값과 각 좌표 scale, `Geocode.scale`, `toString()`, JSON 숫자 표현과 역직렬화를 확인한다. 별도 assertion은 `Geocode(Double, Double)`가 계속 `DefaultMathContext(12)`를 적용하는지도 고정해 문자열 파싱과 생성자 경로의 서로 다른 계약을 보호한다.

## 검증

- 문자열 파서에 `DefaultMathContext` 변환을 임시 적용했을 때 `37.123456789012345`가 `37.1234567890`으로 반올림되어 정밀도 보존 assertion이 실패했다. 반대로 `Double` 생성자에서 context를 제거하면 `37.123456789012344`가 기대값 `37.1234567890`과 달라 새 assertion이 실패한다. 원래의 두 계약을 복구한 뒤 `JsonSerializationTest` 17개가 통과했다.
- `:bluetape4k-geo:test --rerun-tasks --no-build-cache --no-configuration-cache --console=plain`: 성공. Gradle 콘솔 요약은 `SUCCESS: Executed 245 tests in 8s (2 skipped)`이고, 전체 실행 직후 생성된 `utils/geo/build/test-results/test/`의 JUnit XML은 `tests=249`, `skipped=6`, `failures=0`, `errors=0`을 기록했다. 건너뛰지 않은 243개 테스트 케이스는 모두 통과했다. 콘솔과 XML은 별도 보고 결과이므로 두 수치를 합산하거나 서로 대체하지 않는다. 당시 XML에서 건너뛴 6건은 `BingMapServiceTest`와 `BingAddressFinderTest`의 `@Disabled` 테스트이며, Google 키 환경변수가 설정되어 `GoogleAddressFinderTest`의 18개 테스트는 실행·통과했다.
- `:bluetape4k-geo:test --tests io.bluetape4k.geocode.JsonSerializationTest --rerun-tasks --no-build-cache --no-configuration-cache --console=plain`: 전체 모듈 실행 뒤 회귀 클래스만 다시 실행해 17개가 모두 통과했다. 이 필터 실행은 `utils/geo/build/test-results/test/` XML 산출물을 선택된 클래스 결과로 교체하므로 현재 XML에는 `JsonSerializationTest`의 17개 테스트만 있다. 앞선 전체 모듈 수치는 전체 실행 당시 기록한 값이며 현재 필터 XML과 합산하지 않는다.
- `./gradlew :bluetape4k-geo:detekt`: 성공. 기존 진단이 출력됐지만 새 회귀 테스트와 수정한 KDoc 줄에는 진단이 없었다.
- `ktlint`를 두 변경 Kotlin 파일에 실행했을 때 기존 스타일 진단 42건이 보고됐지만 `standard:kdoc` 진단은 없었고 새 테스트 줄에는 진단이 없었다. geo 모듈 Gradle task 목록에는 ktlint 검증 task가 없다.
- 저장소에는 `checkBinaryCompatibility`, `checkProductionAbi`, `apiCheck` Gradle task가 없다. `javap -p -s`에서 정적 facade와 `Geocode.Companion.parse`의 descriptor가 모두 `(Ljava/lang/String;Ljava/lang/String;)Lio/bluetape4k/geocode/Geocode;`임을 확인했다. 전체 공개 API baseline 검증은 아니다.

## 향후 지침

- import·스타일 정리에서 overload를 바꾸거나 인자를 생략할 때는 함수 이름뿐 아니라 해당 overload의 변환 규칙을 비교한다. `toBigDecimal()`와 `toBigDecimal(MathContext)`처럼 비슷한 호출도 정밀도와 scale 계약이 다를 수 있다.
- 숫자 문자열 파싱 계약을 바꿀 때는 12자리 초과 값, 끝의 0, 개별 scale, 문자열 결과, 직렬화 표현을 함께 고정한다.
- KDoc은 `@JvmStatic` 같은 annotation보다 앞에 두어 함수 선언에 연결되도록 한다. 문자열 파서와 `Double` 생성자처럼 overload 경로의 변환 계약이 다르면 양쪽을 별도 assertion으로 보호한다.
- Jackson 직렬화 테스트는 생성자 필드만 가정하지 말고 계산 getter가 JSON에 노출되는지도 확인한다.
- worktree에서 `bluetape-flow.py`를 실행하면 상태 루트는 해당 worktree의 `.bluetape`로 바뀐다. worktree 생성 전에 시작한 실행 영수증은 worktree 안의 mutation-check에서 보이지 않으므로 실행 루트를 분리하고, `--expected-head`에는 Git SHA 대신 앞선 helper 응답의 receipt checksum을 전달한다.
