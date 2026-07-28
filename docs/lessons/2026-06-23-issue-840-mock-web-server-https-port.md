# 이슈 840 - mock-web-server HTTPS port

## 배경

`testing/mock-web-server` runtime configuration은 HTTPS port `8443`을 사용하고 Jib
container metadata도 port `80`과 `8443`을 노출한다. 하지만 양쪽 README locale은 여전히
HTTPS port를 `443`으로 문서화했다.

## 결정

architecture text, feature list, configuration table, Docker run command에서 영어와
한국어 README 모두 `8443`을 일관되게 문서화한다.

## 후속 가드

`ReadmeHttpsPortContractTest`는 README HTTPS documentation이 `application.yml` 및 Jib
port list와 일치하는지 확인하고, stale standalone `443` documentation pattern을 막는다.
