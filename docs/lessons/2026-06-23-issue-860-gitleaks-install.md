# 이슈 860 - deterministic gitleaks install

## 배경

`CI`의 `Secret Scan (gitleaks)` job은 scan 전에 실패했다. installer가 authentication 없이
GitHub releases API에서 latest gitleaks tag를 조회했기 때문이다. GitHub가 HTTP 403을
반환했고, 그래서 `Run gitleaks` step은 실행되지 않았다.

같은 latest-release lookup은 weekly `Security` workflow에도 있었다. 이전 fix는 이미
hand-built latest URL에서 release metadata로 옮겼지만, CI path는 여전히 rate-limited API
call에 의존했다.

## 결정

`GITLEAKS_VERSION`을 `v8.30.1`로 고정하고 tag URL에서 정확한 Linux x64 release asset을
download한다. 이렇게 installer에서 unauthenticated releases API lookup을 제거하면서 기존
`gitleaks detect` command semantic은 보존한다.

## 후속 가드

`CI`와 `Security` gitleaks installer를 맞춰 유지한다. pinned scanner version이 바뀌면 양쪽
workflow env block을 업데이트하고, commit 전에 asset URL을 검증한다.
