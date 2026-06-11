# 밥크로스 개선 사항 및 보안 취약점 검토

작성일: 2026-06-02

## 검토 범위

- Android 앱 패키지: `com.babcross.app`
- 기준 버전: `0.1.68 / versionCode 69`
- 주요 검토 파일:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/babcross/app/MainActivity.kt`
  - `app/src/main/java/com/babcross/app/nearby/NearbyVoteConnectionManager.kt`
  - `app/src/main/java/com/babcross/app/protocol/NearVoteMessage.kt`
  - `app/src/main/java/com/babcross/app/data/NearVoteModels.kt`
  - `app/src/main/java/com/babcross/app/data/NearVoteStore.kt`
  - `PRIVACY_POLICY.md`
  - `docs/play-store-submission.md`

## 결론 요약

밥크로스는 자체 서버, 광고 SDK, 분석 SDK 없이 Nearby Connections와 로컬 저장소 중심으로 동작한다. 현재 코드에는 백업 비활성화, cleartext 차단, FileProvider 비공개 설정, Nearby payload 크기 제한, JSON 파싱 방어, 발신자 ID 대조, 결과 해시 검증, 로컬 데이터 삭제 기능 등 기본 보안 장치가 이미 반영되어 있다.

다만 출시 전에는 Nearby 메시지의 재전송/신선도 검증, 권한 선언 정밀화, release 빌드 난독화/축소, 개인정보 문서 시행일 정렬, 악성 payload 단위 테스트 보강을 우선 처리하는 것이 좋다. 현재 발견된 항목은 원격 서버 침해형 취약점이라기보다, 근거리 참여자가 깨진 메시지나 조작된 메시지를 보냈을 때 앱 신뢰도와 데이터 일관성을 지키는 방어 과제에 가깝다.

## 2026-06-02 처리 현황

- 처리 완료: Nearby messageId 중복 재수신 방어
- 처리 완료: 오래되었거나 미래 시각인 Nearby 메시지 거부
- 처리 완료: release 빌드 R8 minify/resource shrink 명시
- 처리 완료: Android 13+ `NEARBY_WIFI_DEVICES` 권한에 `neverForLocation` 플래그 적용
- 처리 완료: 결과 해시 입력값 공백/유니코드 정규화
- 처리 완료: 손상된 SharedPreferences JSON 복구 처리
- 처리 완료: 공유 카드 cache 파일 보관 개수 제한
- 처리 완료: 개인정보처리방침 시행일 `2026-06-02` 정렬
- 남은 확인: minify 적용 release AAB의 실제 두 기기 Nearby 전체 플로우 검증

## 이미 반영된 보호 장치

### 앱/플랫폼 설정

- `android:allowBackup="false"`로 로컬 밥닉, 결과, 영수증, 템플릿이 Android Auto Backup으로 외부 복원되는 위험을 줄였다.
- `android:usesCleartextTraffic="false"`로 앱 단위 cleartext HTTP 사용을 막았다.
- `FileProvider`는 `exported=false`, `grantUriPermissions=true`이며 공유 대상도 `cache/shared_results/`로 제한되어 있다.
- 앱은 자체 서버 통신 구조가 없고, Google Nearby Connections 의존성 외 광고/분석 SDK가 없다.

### Nearby 메시지 방어

- Nearby raw payload는 64KB를 넘으면 무시한다.
- 메시지 envelope에는 `schemaVersion`, `messageId`, `createdAtMillis`가 들어간다.
- 수신 메시지는 `runCatching`으로 파싱 실패를 앱 크래시가 아닌 무시 처리로 돌린다.
- wrapper 레벨에서 schema version, messageId, senderId, payloadJson 길이, createdAtMillis를 검사한다.
- `PROFILE`, `POLL_RESPONSE`, `VOTE`는 payload 안의 사용자 ID가 wrapper의 `senderId`와 다르면 무시한다.
- 밥판/결과는 질문, 후보 수, 후보 길이, 참여자 수, 득표 수, 아바타 범위 등을 제한한다.
- `revealSelections=false` 결과에 참여자별 선택 상세가 포함되면 무효 처리한다.
- 결과 블록은 `SharedResult.computeHash` 기반 해시가 맞지 않으면 저장하지 않는다.

### 로컬 데이터 관리

- 저장 위치는 `SharedPreferences` 중심이며 앱 내부 `설정 > 내 로컬 데이터 관리`에서 지난 결정, 영수증/해시, 사용자 템플릿, 프로필/기본값, 전체 데이터를 나눠 삭제할 수 있다.
- 결과 기록은 최대 20개로 제한된다.
- 개인정보처리방침은 서버 미운영, 주변 기기 공유 범위, 로컬 삭제 한계를 설명한다.

## 우선순위 보안 개선안

### P0. Nearby 메시지 재전송 방어

현재 `messageId`와 `createdAtMillis`는 생성/전송되지만, 수신 측에서 최근 처리한 messageId를 저장해 중복 메시지를 차단하거나 오래된 메시지를 거르는 로직은 보이지 않는다. 근거리 악의 사용자가 예전 `VOTE` 또는 `RESULT_BLOCK`을 재전송하면 일부 흐름은 중복 선택/결과 pollId 검사로 막히지만, 메시지 envelope 차원의 재전송 방어는 아직 약하다.

권장 조치:

- 최근 messageId LRU 캐시를 두고 일정 시간 내 중복 messageId를 무시한다.
- `createdAtMillis`가 현재 시간 대비 너무 오래되었거나 비정상적으로 미래인 메시지를 무시한다.
- `PING`, `PROFILE` 같은 반복 메시지에는 더 짧은 TTL을 적용한다.
- 단위 테스트에 같은 messageId 2회 수신, 오래된 메시지, 미래 timestamp 케이스를 추가한다.

### P0. release 빌드 보안 설정 명시

현재 `release` buildType은 서명 설정만 조건부 적용하고 `isMinifyEnabled`, `isShrinkResources`, ProGuard/R8 규칙이 명시되어 있지 않다. 앱이 서버 비밀값을 포함하지 않는 구조라 긴급 위험은 낮지만, Play 배포 전에는 release 산출물의 크기와 정적 분석 노출을 줄이는 편이 좋다.

권장 조치:

- `release`에 `isMinifyEnabled = true`, `isShrinkResources = true`를 적용한다.
- Nearby/AndroidX 동작에 필요한 keep rule이 있는지 release 빌드와 두 기기 테스트로 확인한다.
- 내부 테스트 체크리스트에 `release debuggable=false`, `minify/shrink 적용`, `AAB 서명 확인`을 고정한다.

### P0. 권한 선언 정밀화

매니페스트에는 `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, Bluetooth, Wi-Fi, Nearby Wi-Fi 권한이 선언되어 있다. Nearby Connections 용도상 필요할 수 있으나 Android 버전별 필요 권한이 다르므로 Play 심사와 사용자 신뢰를 위해 더 정밀한 매트릭스가 필요하다.

권장 조치:

- Android 8-10, 11-12, 13, 14-15 기준으로 실제 요청 권한과 심사 설명을 표로 정리한다.
- `NEARBY_WIFI_DEVICES`에 `usesPermissionFlags="neverForLocation"` 적용 가능 여부를 검토한다.
- Android 12 이상에서 위치 권한이 여전히 필수인지 Nearby Connections 문서 기준으로 재확인한다.
- 앱 내부 권한 설명, Play Data safety, 개인정보처리방침의 표현을 같은 문장으로 맞춘다.

### P1. payload 모델 검증을 모델 계층으로 이동

현재 핵심 검증은 `MainActivity.kt`에 집중되어 있다. UI, Nearby 처리, 데이터 검증이 한 파일에 함께 있어 새 메시지 타입이나 필드가 추가될 때 검증 누락 위험이 생긴다.

권장 조치:

- `NearVoteMessageValidator` 또는 protocol/data 레벨 validator를 분리한다.
- `NearbyPoll`, `SharedResult`, `VoteReceipt`별 `validate()`를 두고 테스트에서 직접 검증한다.
- `MainActivity`는 검증 결과에 따른 화면/로그 처리만 담당하게 줄인다.

### P1. 결과 해시의 canonicalization 강화

결과 해시는 후보 순서, 득표, 참여자 ID, 선택 상세를 기반으로 계산된다. 현재 후보 문자열은 앱 내부 normalized option과 원본 option이 혼용될 수 있으므로, 공백/유니코드 정규화가 다른 기기에서 달라질 여지가 있다.

권장 조치:

- hash 입력 전에 option/question/userId를 동일한 normalization 규칙으로 정리한다.
- participant count와 revealSelections도 hash 입력에 포함할지 검토한다.
- hash mismatch 테스트에 공백 중복, 순서 변경, 선택 상세 제거, 득표 음수 케이스를 추가한다.

### P1. 초대/결과 수신 신뢰 모델 표시

밥크로스의 보안 모델은 “서버 검증”이 아니라 “가까운 참여자 기기 간 합의와 무결성 확인”에 가깝다. 사용자는 서버에 저장되지 않는다는 점과 동시에 주변 참여자에게 후보/선택/결과가 전달된다는 점을 함께 알아야 한다.

권장 조치:

- 초대 화면 규칙 카드에 `주변 기기에 전달됨`, `서버 저장 없음`, `상대 기기에 전달된 결과는 회수 불가`를 짧게 유지한다.
- 결과 공유 전 카드에 공개 범위와 `밥친구별 선택 공개/득표수만 공개` 상태를 다시 표시한다.
- Play 설명, 개인정보처리방침, 앱 내부 문구의 시행일과 표현을 맞춘다.

### P2. 로컬 저장소 손상/마이그레이션 방어

`NearVoteStore`는 저장된 JSON 파싱에 `runCatching`을 일부 적용하지만, receipt/result 일부 경로는 손상 JSON에서 예외가 날 수 있다. 일반 사용자가 만들 가능성은 낮지만 앱 업데이트, 데이터 복원, 수동 조작 시 문제가 될 수 있다.

권장 조치:

- 모든 `SharedPreferences` JSON 읽기 경로를 `runCatching`으로 감싼다.
- 손상된 JSON은 해당 key 삭제 또는 빈 값으로 복구한다.
- store 단위 테스트에 손상 JSON, 빈 문자열, 너무 큰 배열을 추가한다.

### P2. 공유 카드 파일 수명 관리

공유 카드는 cacheDir의 `shared_results`에 저장되고 FileProvider로 공유된다. cache 영역이라 장기 보존 위험은 낮지만, 여러 번 공유하면 파일이 누적될 수 있다.

권장 조치:

- 공유 카드 생성 전 오래된 `bab-cross-*.png`를 정리한다.
- 파일명은 이미 pollId를 sanitize하므로 유지하되, 최대 보관 개수를 제한한다.
- 공유 실패 시 임시 파일 정리 여부를 확인한다.

## 제품 개선 우선순위

### P0. 첫 사용 신뢰 온보딩

첫 화면에서 “서버 없음”, “주변 기기끼리만 공유”, “위치 기록 저장 안 함”을 짧게 보여주면 권한 허용 장벽이 낮아진다. 지금 개인정보처리방침과 권한 카드에 좋은 문구가 있으므로, 첫 권한 요청 전 한 화면으로 압축한다.

### P0. 밥판 작성 흐름 추가 단축

최근 음식 종류로 바로 작성 화면에 진입하는 방향은 이미 문서화되어 있다. 반복 사용자는 점심 시간에 탭 수가 민감하므로, 최근 종류 저장과 `다른 종류로 시작` 보조 액션을 유지하는 것이 좋다.

### P1. 결과의 신뢰 점수/영수증 설명

영수증과 결과 해시가 존재하지만 일반 사용자는 의미를 알기 어렵다. 결과 화면에 `결과 확인됨`, `내 선택 영수증 저장됨` 같은 짧은 상태를 붙이면 보안 기능이 체감 가치가 된다.

### P1. 장애 상황 UX

Bluetooth 꺼짐, 위치 권한 거부, Nearby Wi-Fi 미허용, 연결 0명 상태에서 사용자가 무엇을 할 수 있는지 더 명확히 안내한다. 현재 `밥판 체험하기`가 있으므로 연결 실패 시 혼밥 테스트와 로컬 결과 카드 생성으로 자연스럽게 이어지게 한다.

### P2. 데이터 삭제 후 빈 상태 UX

로컬 데이터 삭제 기능이 들어가 있으므로 삭제 후 홈/결과/설정의 빈 상태가 어색하지 않아야 한다. “지난 결정 없음”, “저장된 템플릿 없음”, “프로필을 다시 만들었습니다” 같은 상태 문구를 점검한다.

## 출시 전 검증 체크리스트

- 64KB 초과 Nearby payload 무시
- malformed JSON 무시
- 알 수 없는 type 또는 schemaVersion 무시
- senderId와 payload 내부 userId 불일치 무시
- 같은 messageId 재수신 무시
- 오래된 createdAtMillis 무시
- 후보 20개 초과 무시
- 후보명 30자 초과 무시
- 질문 120자 초과 무시
- participantId 중복 결과 무시
- 음수 득표 또는 100표 초과 결과 무시
- hash 불일치 결과 저장 거부
- `revealSelections=false`인데 participantSelections 포함 시 거부
- Android 8, 11, 12, 13, 15 권한 요청 확인
- release AAB `debuggable=false` 확인
- release AAB minify/shrink 적용 후 두 기기 Nearby 전체 플로우 확인
- 개인정보처리방침 URL 공개 접근 확인
- 앱 내부 개인정보처리방침 시행일과 repo 문서 시행일 정렬

## 권장 1차 실행 순서

1. `NearVoteMessage` 중복 messageId/TTL 검증 추가
2. 악성 Nearby payload 단위 테스트 보강
3. release buildType minify/shrink 적용 및 AAB 재검증
4. 권한 매트릭스와 Play Console 설명 문구 확정
5. 개인정보처리방침 시행일과 앱 내부 문구 정렬
6. store 손상 JSON 복구 처리 추가
7. 공유 카드 cache 정리 로직 추가

## 참고 코드 위치

- 매니페스트 보안 설정: `app/src/main/AndroidManifest.xml`
- Nearby payload 크기 제한: `app/src/main/java/com/babcross/app/nearby/NearbyVoteConnectionManager.kt`
- 메시지 envelope: `app/src/main/java/com/babcross/app/protocol/NearVoteMessage.kt`
- 수신 메시지 검증/처리: `app/src/main/java/com/babcross/app/MainActivity.kt`
- 결과 모델과 해시: `app/src/main/java/com/babcross/app/data/NearVoteModels.kt`
- 로컬 저장/삭제: `app/src/main/java/com/babcross/app/data/NearVoteStore.kt`
- Play 등록 문구/권한 설명: `docs/play-store-submission.md`
