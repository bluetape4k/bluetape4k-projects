# README Diagram Layout Fixes

## 배경

Generated README diagram 중 일부는 label overlap, clipped content, overly fixed height 문제를 보였다.

## 결정

Fixed canvas assumption을 줄이고 content-driven dimension, wrapping, grouped placement를 적용한다.
Wide class hierarchy와 sequence diagram은 known-risk case로 별도 inspect한다.

## 검증

- Rendered image count와 missing asset count 확인.
- README image link check.
- Shape sanity check.
- `git diff --check`.

## 향후 가이드

Layout fix는 link validation만으로 끝내지 않는다. 사람이 읽을 수 있는지, label이 잘리지 않는지,
section 간 overlap이 없는지 visual QA를 포함한다.
