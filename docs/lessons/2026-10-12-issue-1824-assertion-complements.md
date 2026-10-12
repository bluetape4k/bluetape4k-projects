# Issue #1824: 검증 API의 대칭쌍은 overload와 경계까지 확인한다

## 맥락

[Issue #1824](https://github.com/bluetape4k/bluetape4k-projects/issues/1824)는 Assertions의 모든 공개 조건 API에 receiver와 인자 overload별 긍정·부정 쌍을 요구한다. Core는 자주 쓰는 공통 `requireXxxx`와 `checkXxxx`만 선택하되, 선택한 각 계열은 양쪽 표현을 모두 제공한다. 사용자는 이 구분을 재차 명확히 했다. Assertions 전체를 Core에 1:1 복제하지 않더라도 긍정·부정 대칭은 유지해야 한다.

기준 `RequireSupport.kt`의 named `requireEndsWith` overload는 `requireNotNull(parameterName)`을 호출해 수신 문자열이 아닌 매개변수 이름을 검사했다. `parameterName`은 non-null `String`이므로 null receiver도 이후 predicate까지 도달할 수 있었다. 같은 검토에서 near tolerance, Map null 반환형, Duration README 행의 불일치도 확인했다.

## 결정

- 표면 API를 먼저 overload 단위로 조사하고, 기존 이름·receiver nullability·반환형·parameter name·lazy callback shape를 compile fixture에서 고정한다.
- `requireEndsWith`는 receiver를 명시해 `this.requireNotNull(parameterName)`으로 검증한다. named와 lazy overload 모두 null receiver regression을 둔다.
- Float/Double 부정 비교는 positive 조건을 수치 비교 연산자로 다시 쓰지 말고 검증된 positive predicate의 논리 부정을 구현한다. NaN, infinities, signed zero, tolerance를 별도로 테스트한다. 특히 양의 무한 tolerance와 같은 infinity의 기존 판정을 바꾸지 않는다.
- Map의 부정 함수는 null receiver에서 positive 함수의 실패를 통과로 반전하고, 반환형도 `Map<K,V>?`로 보존한다.
- Duration 부호 검증은 `java.time.Duration`과 `kotlin.time.Duration`을 각각 제공한다. 유한수 조건과 일반 부호 조건은 이름과 동작에서 분리한다.
- README에는 실제 제공하는 API만 기록한다. Core는 선택된 공통 family만 담되 각 선택 family의 `require`와 `check` 양쪽에서 positive/negative를 제공한다.

## 결과

Assertions의 누락된 부정 API와 Duration·유한수 함수, Core의 선택된 helper 쌍을 추가했다. `requireEndsWith`의 receiver 버그를 수정했고, near 비교 tolerance 설명과 Map nullable 반환형을 고정했다. 두 README locale에서 잘못된 Duration 항목을 제거했다.

전체 Assertions/Core JVM descriptor 비교에서 기존 owner의 descriptor 제거는 없었다. Assertions 12개 기존 owner에 120개, Core 6개 기존 owner에 256개 descriptor가 추가됐다. Assertions Detekt 진단은 기준 36건에서 35건으로 줄었다. `Maps.kt`의 함수 수가 10개에서 12개로 늘어 `TooManyFunctions` 한 건이 새로 생겼으며, 진단 회피를 위해 API owner를 바꾸거나 suppression을 추가하지 않았다. Core는 기준·최종 모두 269건이다.

독립 최종 코드 리뷰 lane은 제한된 probe/interrupt 이후 만료되어 workflow에서 BLOCKED로 기록됐다. 주 세션은 이미 알려진 finding을 다시 대조했지만, 이것은 독립 리뷰 PASS가 아니다. PR과 merge는 이슈 범위에 없으며 진행하지 않았다.

## 검증

- Assertions: 707 tests, failures/errors/skips 0. API compile fixture 포함.
- Core: 1,717 tests, failures/errors/skips 0. `require`/`check` API fixture 포함.
- JVM signatures: Assertions 12/12 기존 owner, Core 6/6 기존 owner에서 제거 descriptor 0개; 추가 descriptor만 확인.
- Detekt: Assertions 36→35, Core 269→269. Assertions의 새 `TooManyFunctions` 진단 한 건을 위와 같이 기록했다.
- workflow runner unittest 15건 및 Python `py_compile` 통과.

## 놓치기 쉬운 점

- `requireNotNull(parameterName)`은 이름 문자열만 검증한다. 함수 이름이 `requireEndsWith`여도 receiver 검증 여부는 구현 본문과 null receiver test로 확인해야 한다.
- `!(abs(a - b) > tolerance)`는 NaN에서 `abs(a - b) <= tolerance`와 다른 결과를 낸다. 부정 API를 만드는 과정에서 관계 연산자를 기계적으로 바꾸면 두 API가 논리적 부정이 아니게 될 수 있다.
- nullable Map의 negative check는 성공할 수 있으며 null을 반환한다. non-null 반환형으로 바꾸면 실제 receiver를 체이닝하는 호출부를 깨뜨린다.
- 검증 이름이 README에 있다고 해서 실제 overload가 있다는 뜻은 아니다. 소스·compile fixture·문서를 서로 대조한다.

## 향후 guard

- 대칭 API를 추가할 때 먼저 overload inventory를 작성하고, 각 행에 positive/negative 선언, receiver nullability, 반환형, callback 형태, runtime boundary test를 연결한다.
- 숫자 비교는 NaN과 ±Infinity, signed zero, 허용 오차 경계를 각 자료형에서 확인한다. 유한수 조건을 일반 부호 조건에 섞지 않는다.
- nullable receiver가 가능한 assertion은 positive와 negative 각각에서 null 결과와 성공/실패를 compile/runtime 양쪽에서 고정한다.
- Core로 공개할 helper를 선택할 때는 사용 빈도를 기준으로 범위를 좁혀도 되지만, 선택한 각 계열의 `require`와 `check`는 긍정·부정을 쌍으로 완성한다.
