# 이슈 832 - findBean failure boundary

## 배경

Spring helper의 `findBean` 계열 API는 bean lookup failure를 nullable result처럼 보이게
만들 위험이 있었다. optional lookup과 required lookup의 failure boundary가 문서와 test에서
명확해야 했다.

## 결정

optional helper는 bean이 없을 때 `null`을 반환하고, required helper는 Spring container의
lookup failure를 그대로 노출하도록 boundary를 분리한다. README/KDoc/example은 nullable
lookup과 required lookup을 같은 contract처럼 설명하지 않게 맞춘다.

## 후속 가드

Spring bean lookup helper를 바꿀 때는 optional/required path를 모두 test하고, exception을
삼켜 nullable contract로 바꾸는 regression을 막는다.
