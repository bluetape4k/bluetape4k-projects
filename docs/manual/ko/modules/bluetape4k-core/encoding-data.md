---
title: Encoding과 데이터 경계
description: Byte, String, Base64, hex 변환에서 format과 failure를 명시합니다.
manualId: bluetape4k-core
chapterId: encoding-data
---

# Encoding과 데이터 경계

## 해결할 문제

Binary 데이터를 text로 옮길 때 charset과 wire format을 암묵적으로 두지 않아야 합니다.

## Mental model

Encoding은 암호화가 아닙니다. Base64는 binary transport, hex는 사람이 확인할 짧은 identifier와 dump에 적합합니다.

## 최소 API surface

ByteArray/String helper 중 format을 드러내는 함수를 선택하고 text 변환에는 charset을 명시합니다.

## 완전한 예제

UTF-8 text를 bytes로 바꾸고 Base64로 전송한 뒤 decode와 UTF-8 복원을 같은 예제에서 검증합니다.

## 선택 기준

Protocol 요구, 크기 overhead, 사람이 읽을 필요를 기준으로 Base64와 hex를 선택합니다.

## 실패·취소·수명주기 계약

Malformed input을 빈 값으로 바꾸지 않습니다. Secret이나 token을 encoded form이라는 이유로 log에 남기지 않습니다.

## 운영과 문제 진단

대용량 변환의 allocation과 payload growth를 관찰하고 streaming이 필요한 크기인지 확인합니다.

## Source와 representative test

근거는 [`encoding`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec) 관련 source와 representative test입니다.

## 이어 읽기와 runnable workshop

검증 실패 정책은 [검증과 불변식](./validation.md), 조립 예제는 [Recipes](./recipes.md)에서 이어집니다.
