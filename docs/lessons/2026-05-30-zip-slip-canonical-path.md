# Zip Slip canonical path validation

## 배경

GitHub CodeQL이 file extraction sink의 `ZipFileSupport.unzip`에서 `java/zipslip`을
보고했다.

## 결정

모든 ZIP entry를 destination directory의 canonical file을 통해 resolve하고, output
stream을 열기 전에 canonical path가 해당 directory를 벗어나는 target을 거부한다.

## 결과

extraction path check는 이제 destination directory name과 textual prefix를 공유하는
sibling path까지 포함해, `FileOutputStream`에 전달되는 정확한 canonical file을 보호한다.

## 검증

- `./gradlew :bluetape4k-io:test --tests "io.bluetape4k.io.compressor.ZipFileSupportTest"`
- `git diff --check`

## 향후 가드

archive extraction code에서는 normalized string 또는 path comparison에만 의존하지 말고,
file sink와 같은 abstraction level에서 canonical target path를 검증한다.
