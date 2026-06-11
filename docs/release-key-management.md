# 밥크로스 릴리스 키 관리

최종 정리일: 2026-06-06

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

## 주의

- `keystore.properties`에는 키 비밀번호가 들어 있으므로 채팅, 문서, GitHub, Notion에 붙여넣지 않는다.
- `babcross-upload.jks`를 잃어버리면 Google Play 앱 업데이트가 어려워질 수 있다.
- 별도 보관 위치 외에도 사용자가 관리하는 안전한 외부 저장소에 백업하는 것을 권장한다.
