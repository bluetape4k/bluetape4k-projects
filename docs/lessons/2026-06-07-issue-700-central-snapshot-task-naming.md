# 이슈 #700: Central snapshot task naming audit

## 배경

root README file과 snapshot workflow는 여전히 legacy Central publish task name을
안내했지만, repo-local guidance는 이미 NMCP task name을 사용하고 있었다.

## 결정

public guidance와 GitHub Actions에서 SNAPSHOT publishing에는
`nmcpPublishAggregationToCentralPortalSnapshots`를, release publishing에는
`nmcpPublishAggregationToCentralPortal`을 사용한다.

## 검증

- `./gradlew tasks --all | rg "publishAggregation|nmcpPublishAggregation|CentralPortal|CentralSnapshots"`
- `rg "publishAggregationToCentralSnapshots|publishAggregationToCentralPortal|publishAggregationToCentralPortalSnapshots|publishAllPublicationsToCentralPortalSnapshots|publishAllPublicationsToCentralSnapshots" README.md README.ko.md .github/workflows build.gradle.kts`

## 향후 가드

release 또는 snapshot publish task가 바뀌면 README.md, README.ko.md, workflow command,
repo-local contributor guidance를 함께 업데이트한다.
