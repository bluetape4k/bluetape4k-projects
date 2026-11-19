# Issue #1860: Dependabot parser의 checkout과 cache 경계를 고정한다

## 맥락

Issue [#1860](https://github.com/bluetape4k/bluetape4k-projects/issues/1860)의
`catalog-governance` job은 중앙 catalog에서 `dependabot_core_commit`을 읽고
형식만 확인한 뒤 그 값을 `dependabot/dependabot-core`의 checkout `ref`로
사용했다. 같은 job에서 `gradle/actions/setup-gradle`는 PR이 아닐 때
쓰기 가능한 cache를 사용했고, `ruby/setup-ruby`는
`bundler-cache: true`로 공유 Bundler cache를 저장했다. 이 조합은 CodeQL
#56-#60의 `actions/cache-poisoning/poisonable-step` 경고가 지적한 신뢰 경계를
계약으로 고정하지 못했다.

## 결정

- workflow 환경에 승인된 Dependabot Core SHA
  `de71a59f11cc8373d39bbf5b7af5e8b73f45ca0a`를 고정한다.
- 중앙 mapping의 `dependabot_core_commit`이 승인 SHA와 다르면
  `Dependabot Core mapping drift`로 즉시 실패시킨다.
- 외부 checkout은 mapping step output이 아니라 고정된 workflow SHA를
  `ref`로 사용한다.
- `catalog-governance` job 권한을 `contents: read`로 제한한다.
- Gradle cache는 모든 trigger에서 `cache-read-only: true`로 사용한다.
- `ruby/setup-ruby`의 공유 cache를 끄고, Bundler 의존성은
  `${{ runner.temp }}/dependabot-bundle`에만 설치한다.
- workflow contract test는 승인 SHA, mapping drift guard, 동적 ref,
  Gradle cache 쓰기, Bundler 공유 cache 쓰기를 각각 검증한다.

GitHub Actions는 cache를 서명하거나 검증하지 않으며, low-trust trigger의
cache 쓰기를 제한하더라도 명시적인 쓰기 설정은 cache-poisoning 위험을 다시
열 수 있다. 따라서 이 job은 cache를 저장하지 않고 매번 격리 경로를
재생성한다. [GitHub dependency caching reference](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
및 [`ruby/setup-ruby`의 Bundler cache 동작](https://github.com/ruby/setup-ruby)을
기준으로 경계를 선택했다.

## 결과

mapping과 승인 SHA가 불일치하면 외부 코드 checkout 전에 job이 실패한다.
외부 Dependabot Core 코드는 더 이상 mapping output으로 임의 ref를 선택하지
않으며, 공유 Gradle/Bundler cache를 저장할 수 없다. 중앙 catalog tag의
object SHA와 peeled commit 검증은 기존대로 유지한다.

## 검증

- 수정 전 RED: 승인 SHA와 cache 경계 계약 테스트 2개가 실패했다.
- 수정 후 GREEN: `tests.test_catalog_workflows` 143개 테스트 통과.
- Issue #1860 전용 승인/동적 ref/cache 거부 테스트 4개 통과.
- `actionlint .github/workflows/ci.yml` 통과.
- `python3 scripts/verify-catalog-workflows.py`가
  `catalog workflow contract passed` 반환.
- `python3 -m py_compile tests/test_catalog_workflows.py scripts/verify-catalog-workflows.py`
  통과.
- `git diff --check` 통과.
- 독립 diff review: P0=0, P1=0, P2=0.
- CodeQL #56-#60의 동일 exact HEAD 재분석과 live disposition은 PR hosted CI
  이후 확인해야 하며, 로컬 검증으로 대체하지 않는다.

## 향후 지침

중앙 파일에서 읽은 commit을 형식만 검증해 외부 checkout ref로 전달하지
않는다. 외부 parser를 실행해야 할 때는 저장소 내 승인 SHA와 mapping을
비교하고, checkout ref는 승인 값으로 고정한다. cache가 필요한 의존성은
공유 Actions cache 대신 job-local 임시 경로를 사용하며, workflow contract
test에서 primary와 fallback 경로의 cache 쓰기 설정을 함께 거부한다.
