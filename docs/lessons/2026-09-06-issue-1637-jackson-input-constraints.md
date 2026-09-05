# Jackson 보안 패치의 실제 적용 버전과 입력 경계 검증

## 문제와 결정

[Issue #1637](https://github.com/bluetape4k/bluetape4k-projects/issues/1637)을 조사할 때,
중앙 catalog에 버전이 올라갔으므로 소비 모듈도 해당 버전을 사용할 것이라고 가정했다.
그러나 catalog ref `9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`에는
`jackson=2.22.1`, `jackson2=2.22.2`, `jackson3=3.2.2`가 공존했고,
Projects의 전역 dependency management와 Jackson2 BOM은 `jackson`을 참조했다.
실제 Jackson2 core와 CBOR도 `2.22.1`로 해석됐다.

전역 Jackson2 제약과 모듈 BOM을 중앙 `jackson2` 키로 정렬했다.
Jackson3와 공유 annotations의 별도 버전 키는 유지했다.
보안 입력 제한은 테스트별 factory에 설정해 검증한다.

## 회귀 테스트에서 확인한 것

- 최종 Jackson2 테스트 23개를 수정 전 selector로 실행하면 11개가 실패한다.
  async 문서 길이, Reader 조기 거부, CBOR/Smile 이름 제한, YAML merge 중첩,
  XML datatype 숫자 문자열 제한, Path scheme 거부가 해당한다.
- CBOR의 과도한 이름 길이는 실제 이름 바이트가 없어도 EOF보다 먼저 거부해야 한다.
- Reader 이름 제한은 예외 발생뿐 아니라 전체 이름을 읽기 전 거부하는지도 확인해야 한다.
- YAML merge 검증에는 `YAMLAnchorReplayingFactory`가 필요하다.
  일반 `YAMLFactory`를 사용한 첫 정상 대조군은 merge 결과를 만들지 못했다.
  정상 merge 결과와 초과 입력을 함께 검사해 테스트가 실제 수정 경로를 타는지 확인한다.
- `Path`를 입력 파일로 받는 API와 JSON 값을 `Path`로 역직렬화하는 API를 구분한다.
  후자는 명시적인 scheme 거부 예외와 메시지를 확인해야 provider 부재 오류를 성공으로 오인하지 않는다.
- Jackson3의 `asText()`는 deprecated API다. 새 테스트에는 `asString()`을 사용한다.
  독립 검토가 발견한 경고를 수정했으며, 컴파일 성공과 경고 없음은 별도로 확인한다.

## 재발 방지

1. 중앙 catalog 변경은 ref, 소비 코드의 버전 키, 실제 `dependencyInsight`를 함께 확인한다.
   BOM, core, databind, 모든 사용 dataformat을 확인하고 annotations의 독립 버전을 구분한다.
2. 제한 테스트에는 정상 경계값과 제한 초과를 짝지어 두고,
   예외 종류·거부 시점·콜백 미전달 등 실제 보호 동작을 검증한다.
3. `build -x test`는 모든 테스트를 제외하지 않는다.
   이번 검증에서도 `consumerRuntimeTest`, `coordinationLockPerformanceTest`, `k8sTest`가 실행됐다.
   컴파일 확인은 `compileKotlin compileTestKotlin`로 한정하고,
   컨테이너 테스트는 이름과 실행 순서를 명시한다. 중단한 build는 성공 증거로 사용하지 않는다.
4. workflow helper의 명령별 `--help`를 확인한다.
   owner 파일은 `.bluetape/handles/` 아래에 두고, lane 전이는 실제 agent ID와 관측 시각 및 evidence를 전달한다.
   초기 helper 인자 오류를 수정한 뒤 mutation-check를 통과했으며, 실패한 호출을 완료 증거로 세지 않는다.

## 검증과 범위

- Jackson2/Jackson3 제한 테스트 각각 23개 통과.
- 전체 모듈 테스트: Jackson2 499개, Jackson3 493개 통과.
- Jackson2 BOM/core/databind/dataformat `2.22.2`, Jackson3 `3.2.2`를 dependencyInsight로 확인.
- XML dataformat은 두 모듈에서 노출하지 않아 제외했다. JSON에서 XML datatype을 역직렬화하는 경로는 검증한다.
- 전역 컴파일, 최종 수정 후 재검증 및 검토 상태는 실행 계획의 DoD에서 관리한다.

## 공식 근거

- [YAML merge 중첩 제한](https://github.com/FasterXML/jackson-dataformats-text/issues/707)
- [Path scheme 제한](https://github.com/FasterXML/jackson-databind/pull/6129)
- [XML datatype 숫자 문자열 제한](https://github.com/FasterXML/jackson-databind/pull/6127)
