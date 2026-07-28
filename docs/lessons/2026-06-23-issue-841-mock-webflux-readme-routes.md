# 이슈 841 - mock-webflux README route

## 배경

`testing/mock-webflux-server` README file은 WebFlux controller가 구현하지 않은 여러
httpbin/admin endpoint를 나열했다. README를 integration-test contract로 사용하는
consumer는 해당 route를 호출하고 404 response를 받을 수 있었다.

## 결정

구현된 route surface는 변경하지 않고 README-only endpoint row를 양쪽 locale에서 제거한다.

제거된 row는 `/admin/info`, `/httpbin/stream-bytes/{n}`, `/httpbin/drip`,
`/httpbin/sse`, `/httpbin/brotli`, `/httpbin/html`, `/httpbin/xml`, `/httpbin/json`,
`/httpbin/robots.txt`, `/httpbin/deny`다.

## 후속 가드

matching WebFlux controller mapping이 추가되지 않는 한, `ReadmeRouteContractTest`는
stale route가 README endpoint table에 돌아오는 것을 막는다.
