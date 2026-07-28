# Bluetape Annotations

## 배경

Repository 전반의 annotation 사용은 module과 API boundary마다 목적이 달랐다. 문서화 없이 annotation을
추가하면 binary/source compatibility, opt-in level, internal API 노출 범위가 흐려진다.

## 결정

Annotation은 실제 contract를 고정할 때만 추가한다. Runtime marker, experimental API, internal helper,
test-only utility를 구분하고, KDoc에는 annotation이 강제하는 사용 조건과 migration expectation을 적는다.

## 결과

Annotation 도입과 정리는 해당 module의 API surface를 기준으로 판단한다. 단순 설명용 marker를 늘리지 않고,
compiler나 tooling이 실제로 enforcement하는 annotation을 우선한다.

## 검증

- Public API와 internal API boundary 확인.
- Annotation target과 retention 확인.
- `git diff --check`.

## 향후 가이드

Annotation을 추가할 때는 "누가 무엇을 잘못 쓰지 못하게 하는가"를 먼저 적는다. 설명만 필요한 경우에는
annotation보다 KDoc이나 guide 문서가 더 적합하다.
