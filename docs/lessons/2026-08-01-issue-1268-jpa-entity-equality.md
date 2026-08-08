# #1268 Hibernate 엔티티 equality와 hash 안정성

## 배경

`AbstractJpaEntity`는 persisted entity를 identifier만으로 비교하고 transient
entity는 business signature로 비교했습니다. 이 경로는 서로 다른 entity type이
같은 identifier를 가질 때 동일하다고 판정했으며, identifier가 할당되면 hash code가
type hash에서 identifier hash로 바뀌어 `HashSet`과 `HashMap` 조회가 끊겼습니다.

## 결정 또는 발견

- equality는 `Hibernate.unproxy` 이후 effective entity type이 같은 경우에만
  persisted identifier 또는 transient business signature를 비교합니다.
- hash code는 persisted 여부와 무관하게 `Hibernate.getClass(this)`의 hash만
  사용합니다. 서로 다른 type의 hash 충돌은 허용되며 type equality가 이를
  구분합니다.
- proxy fixture는 테스트 필드로 재사용하고 `@BeforeEach`에서 `clearMocks`한 뒤
  각 테스트가 implementation만 설정하도록 했습니다.

## 결과

서로 다른 entity type과 proxy가 같은 identifier를 공유해도 equality가 false가
되며, 같은 type의 proxy는 실제 entity와 계속 equality를 유지합니다. transient
entity를 hash collection에 넣은 뒤 identifier를 할당해도 기존 key 위치와 조회가
유지됩니다.

## 검증

- RED: `AbstractJpaEntityUnitTest`에서 다른 type proxy equality와
  transient-to-persisted hash collection 조회의 2개 실패를 재현했습니다.
- GREEN: 대상 테스트 19개 통과, `BUILD SUCCESSFUL`.
- GREEN: `:bluetape4k-hibernate:test` 502개 통과, 13.6초, `BUILD SUCCESSFUL`.
- `git diff --check` 통과.
- `:bluetape4k-hibernate:detekt` task는 `BUILD SUCCESSFUL`이며 변경 파일 finding은
  없습니다. 기존 Hibernate 모듈의 generic exception, function-count,
  Serializable 경고는 남아 있습니다.
- `:bluetape4k-hibernate:dokkaGenerate`는 `BUILD SUCCESSFUL`입니다. 기존
  `README.ko.md` 링크 unresolved warning이 남아 있습니다.

## 향후 지침

JPA entity equality를 변경할 때는 identifier 비교만 확인하지 말고 Hibernate
proxy의 effective type, persisted 전환 전후 hash collection 동작, 서로 다른
subclass의 동일 identifier를 함께 검증합니다. Hash code에 mutable identifier를
포함하면 collection key가 소실될 수 있으므로 type 기반의 안정적인 hash 정책을
유지해야 합니다.
