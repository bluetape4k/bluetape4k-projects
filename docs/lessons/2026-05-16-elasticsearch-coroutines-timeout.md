# Elasticsearch Coroutines Timeout

## 배경

Memgraph CI 수정에 대한 Nightly 전체 검증은 `graphdb-memgraph`를 통과했지만,
`Test / Infra (search-messaging)`에서 실패했다. 실패한 module은
`:bluetape4k-elasticsearch:test`였고, 구체적으로 `ElasticsearchCoroutinesTest`였다.

## 결정 또는 발견

실패한 테스트들은 `runTest(timeout = 30.seconds)`로 제한되어 있었다. GitHub Actions에서는
Elasticsearch container I/O와 index refresh 호출이 runner 부하 아래에서 이 한계를 넘을 수 있다.
인접한 Elasticsearch coroutine integration test들은 유사한 외부 I/O 경로에 이미 60초 timeout을
사용하고 있었다.

## 결과

`ElasticsearchCoroutinesTest`는 setup, teardown, CRUD/search integration test에
60초 `runTest` timeout을 사용한다. 테스트 경계는 유지하면서 현실적인 CI latency를 허용한다.

## 검증

- Local targeted class: `:bluetape4k-elasticsearch:test --tests io.bluetape4k.elasticsearch.coroutines.ElasticsearchCoroutinesTest`가 31초에 통과했다.
- GitHub Nightly full attempt 1과 failed-job rerun은 모두 이 변경 전
  `UncompletedCoroutinesError: After waiting for 30s`로 실패했다.

## 향후 가이드

실제 container나 remote-style client를 호출하는 coroutine test에서는 하위 operation이 결정적이고
순수 local인 경우가 아니라면 30초 `runTest` 제한을 피한다. CI runner latency를 이미 고려한
인접 integration test의 timeout 정책과 맞춘다.
