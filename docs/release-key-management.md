# 밥크로스 릴리스 키 관리

최종 정리일: 2026-06-13

## 목적

Google Play 업로드 키는 앱 업데이트에 필요한 중요 파일이므로 Git에 넣지 않고 별도 보관한다.

## 프로젝트 안에서 필요한 위치

- `keystore.properties`
- `signing/babcross-upload.jks`

두 파일은 `.gitignore`에 포함되어 있으며 Git 추적 대상이 아니어야 한다.

## 별도 보관 위치

로컬 별도 보관 위치:

```text
/Users/neo/.codex/secure-keys/babcross/
```

보관 파일:

- `/Users/neo/.codex/secure-keys/babcross/keystore.properties`
- `/Users/neo/.codex/secure-keys/babcross/signing/babcross-upload.jks`

## 복원 방법

프로젝트 루트에서 아래 명령으로 복원한다.

```bash
mkdir -p signing
cp /Users/neo/.codex/secure-keys/babcross/keystore.properties keystore.properties
cp /Users/neo/.codex/secure-keys/babcross/signing/babcross-upload.jks signing/babcross-upload.jks
chmod 600 keystore.properties signing/babcross-upload.jks
```

복원 후 확인:

```bash
git status --short
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

`keystore.properties`와 `.jks` 파일은 `git status --short`에 나타나지 않아야 한다.

2026-06-13 기준 확인:

- 릴리스 후보: `0.1.167`, `versionCode=168`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- release upload key SHA-1: `69:6F:8D:03:FC:B1:AE:D7:C0:55:C0:F2:D5:54:C5:F5:3E:8D:DD:F9`
- release upload key SHA-256: `97:96:DA:80:D7:E8:E2:B4:D3:9B:27:A7:BF:2F:BD:0F:4B:05:58:E1:15:DC:D1:BB:DE:DE:B2:36:82:06:78:F7`
- `:app:bundleRelease` 성공
- AAB SHA-256: `a10b0b14e87c498c379140b699b64aecacd4e1cfd7c6fe7c5ff3c550cb191275`
- release manifest 기준 `versionName=0.1.167`, `versionCode=168`, `android:debuggable` 속성 없음

## 주의

- `keystore.properties`에는 키 비밀번호가 들어 있으므로 채팅, 문서, GitHub, Notion에 붙여넣지 않는다.
- `babcross-upload.jks`를 잃어버리면 Google Play 앱 업데이트가 어려워질 수 있다.
- 별도 보관 위치 외에도 사용자가 관리하는 안전한 외부 저장소에 백업하는 것을 권장한다.
