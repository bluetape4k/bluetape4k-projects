# Projects PR #1819 인라인 코드 검토

## 검토 범위

- 저장소: `bluetape4k/bluetape4k-projects`
- base: `develop` `72e869690dd8de6bf66246fd5d35fb7e4601f024`
- 코드 검토 head: `df46ceced17a4eb47cc97309178ac8e37106bb16`
- 검토 파일: central catalog 설정과 CI, BuildSrc POM 정규화·회귀 테스트, 관련 lesson 및 test log
- 검토 방식: `inline fallback review`; 독립 reviewer attestation이 아니다.

## 판정

수정 반영 뒤 판정은 PASS다. P0=0, P1=0이며, P2 한 건을 수정하고 회귀 테스트로 확인했다.

## 검토 결과

- **P2 — 서로 다른 POM 항목이 같은 fingerprint가 될 수 있음:** 기존 fingerprint는 노드 이름·속성·텍스트·자식 표현을 길이 경계 없이 연결했다. 구분자 역할을 하는 문자열을 가진 서로 다른 exclusion fixture에서 같은 관리 좌표가 같은 fingerprint로 판정되어, 정상화기가 한 항목을 제거했다. 회귀 테스트는 수정 전 해당 항목을 충돌로 거부하지 못해 실패했다.
- **수정:** 요소 종류, 속성 키·값, 텍스트와 자식 항목을 길이 접두 방식으로 인코딩하고 속성·자식 개수를 기록한다. 이제 내용이 다른 exclusion은 같은 관리 좌표의 충돌로 실패한다.
- **API·런타임:** 변경은 BuildSrc 게시 보조 코드와 테스트에 한정된다. Projects 공개 API·ABI 및 런타임 모듈은 변경하지 않는다.
- **Catalog pin:** 기본 catalog SHA와 CI의 catalog checkout은 모두 `198147fd6ca2992fae32e175bd5f3d396c497096`을 가리킨다.

## 검증 증거

- 수정 전 RED: 서로 다른 exclusion을 같은 fingerprint로 처리해 `GradleException`이 발생하지 않는 것을 확인했다.
- `:buildSrc:test --rerun-tasks --no-build-cache --no-configuration-cache --no-daemon --console=plain`: PASS — 7 suites, 35 tests, failures/errors/skipped 0.
- 중앙 catalog consumer 전체 POM 검증과 PR exact-head CI는 최신 원격 head 기준으로 다시 확인해야 한다.

## 제한과 DoD

- 이 결과는 인라인 검토이며 독립 code review 또는 architecture review가 아니다. 이전 독립 검토 시도는 사용량 제한으로 결과를 받지 못했다.
- BuildSrc에는 Detekt/ktlint task가 등록되어 있지 않다.
- PR exact-head CI, 최신 consumer POM/Maven model 증거, 독립 review gate, fresh merge approval 및 merge read-back은 아직 완료되지 않았다.

Final status: PENDING — 병합은 별도 승인 전까지 수행하지 않는다.
