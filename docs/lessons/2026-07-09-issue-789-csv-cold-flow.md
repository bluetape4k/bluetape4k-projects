# 이슈 789: CSV cold Flow file reader

## 배경

File 기반 suspending CSV/TSV reader overload는 `Flow`를 반환하기 전에
`FileInputStream`을 열었다. 이는 문서화된 cold-Flow contract를 위반했고 반환된 flow를
one-shot으로 만들었다.

## 결정

File opening을 flow builder 안으로 옮기고 collection 중 `use`로 stream을 닫는다.
InputStream overload는 caller가 해당 stream을 소유하므로 변경하지 않는다.

## 결과

Regression test는 file-backed suspending reader가 missing file failure를 collection까지
미루고, early termination 뒤 recollection에도 살아남으며, CSV/TSV와 transform overload
behavior를 다룬다는 점을 증명한다.

## 향후 지침

File-backed `Flow` API는 flow를 반환하기 전이 아니라 collection 안에서 resource를
열어야 한다. Flow가 blocking file I/O를 감싸면 upstream work는 `Dispatchers.IO`에서
실행하고 cold creation과 recollection을 모두 테스트한다.
