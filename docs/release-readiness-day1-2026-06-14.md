# 밥크로스 출시 준비 실행 기록 Day 1

작성일: 2026-06-14

## 목적

`docs/app-completeness-expert-review-v2-2026-06-14.md`의 7일 실행 계획 중 바로 실행 가능한 Day 1 항목을 실제 작업 기준으로 정리한다. 이 문서는 최종 릴리스 후보를 확정하는 문서가 아니라, 수정 범위와 동결 기준을 먼저 닫기 위한 기준선이다.

## 현재 기준선

- 로컬 Gradle 버전: `0.1.184 / versionCode 185`
- CHANGELOG 상단 버전: `0.1.184`
- Play 제출 문서 릴리스 후보: `0.1.167 / versionCode 168`
- Play 제출 문서의 현재 내부 테스트 최신 버전: `168 (0.1.167)`
- 판단: `0.1.184`는 임시 테스트 기준선이며, 회귀 테스트 중 수정이 발생하면 최종 후보 버전은 그 이후 버전으로 다시 올라간다.

## 출시 전 반드시 닫을 항목

### 1. 연결 UX 회귀

- 자동 연결 켜짐 상태에서 `밥신호 보내기`
- 자동 연결 꺼짐 상태에서 `밥신호 보내기`
- QR 참여가 자동 연결 꺼짐 상태에서도 명시적 참여 의도로 처리되는지 확인
- 연결 램프 초록/노랑/빨강/회색 상태별 팝업 확인
- 정상 연결/대기 상태에서 불필요한 액션 버튼이 없는지 확인

### 2. Nearby 실제 기기 회귀

- 2기기 코드 참여
- 2기기 QR 참여
- 수동 `메뉴 결정`
- 제한시간 종료
- 결과 카드 수신과 공유
- 3기기 이상에서 지각 참여자, 미응답자, 거절자, 모두 선택 완료 알림 확인

### 3. 권한/설정 실패 상태

- 위치 권한 거부
- Bluetooth off
- Wi-Fi off
- Nearby Wi-Fi 권한 거부
- 앱 설정 이동 후 복귀
- 권한 없이 체험 흐름 진입

### 4. 접근성/극단값

- 큰 글씨
- 굵은 글씨
- 화면 확대
- 긴 질문
- 긴 후보명
- 후보 10개 이상
- 주요 버튼과 연결 램프의 TalkBack 라벨

### 5. 릴리스 문서 동기화

최종 후보 버전이 정해진 뒤에만 아래 문서를 갱신한다.

- `docs/play-store-submission.md`
- `CHANGELOG.md` 상단 최종 버전 항목
- release AAB 해시와 산출물 기록
- 단말 설치 버전 확인 기록
- 내부 테스트 또는 폐쇄 테스트 업로드 결과

## 출시 후로 미룰 항목

- `MainActivity.kt` 대규모 구조 분리
- `NearbyMessageHandler` 분리
- `ConnectionReadinessPolicy` 분리
- `ResultCardTextPolicy` 분리
- `InviteCodePolicy` 분리
- 공유 루프 고도화 실험
- 카드형 보너스/밥카드 확장
- 원격 분석 SDK 도입 검토

## 버전 확정 원칙

1. Day 1에는 후보 버전을 확정하지 않는다.
2. Day 2-6 검증 중 코드나 리소스 수정이 발생하면 버전은 다시 올라간다.
3. 최종 후보는 회귀 테스트와 접근성/긴 텍스트 QA 이후 추가 수정이 없을 때만 확정한다.
4. 최종 후보 확정 후에만 Play 제출 문서, AAB 해시, 단말 설치 버전, Play Console 상태를 한 번에 맞춘다.
5. 내부 테스트에 이미 올라간 `0.1.167 / 168`과 로컬 임시 기준선 `0.1.184 / 185`를 혼동하지 않는다.

## Play 제출 문서 불일치 목록

현재 `docs/play-store-submission.md`에서 최종 후보 확정 후 갱신해야 할 항목:

- `최종 점검일`: `2026-06-13`
- `Release candidate`: `0.1.167`
- `Version code`: `168`
- 내부 테스트 체크리스트의 AAB 버전: `0.1.167 / 168`
- Play Console 배포 결과의 현재 최신 버전: `168 (0.1.167)`
- 단말 테스트 체크리스트의 설치 버전: `0.1.167 / 168`
- 보관 산출물의 release AAB SHA-256

## 릴리스 노트 후보

최종 후보 릴리스 노트에는 0.1.168 이후 변경을 전부 나열하지 않고 사용자에게 의미 있는 변경만 압축한다.

```text
- 연결 상태 표시를 숫자 중심 배지에서 상태 중심 연결 램프로 정리했습니다.
- 연결 램프 팝업은 자동 연결 꺼짐, 권한 필요, 기기 설정 필요처럼 실제 조치가 필요한 경우에만 버튼을 보여주도록 다듬었습니다.
- 자동 연결이 꺼진 상태에서도 QR 참여와 밥신호 보내기처럼 사용자가 명시적으로 연결하려는 행동은 흐름이 끊기지 않게 개선했습니다.
- 혼자 밥판을 열 때 불필요한 확인 팝업을 줄이고, 밥친구 연결 대기는 뒤에서 이어지도록 정리했습니다.
- 밥판 참여 현황, 모두 선택 완료 알림, Nearby 메시지 검증, 결과 공유 문구 계산을 테스트 가능한 로직으로 분리했습니다.
- 홈 화면의 밥닉/아바타 표시와 밥크로스 워드마크가 더 안정적으로 보이도록 다듬었습니다.
```

## Day 1 완료 판단

- 출시 전 필수 확인 항목과 출시 후 이관 항목을 분리했다.
- 후보 버전은 지금 확정하지 않는 원칙을 명시했다.
- Play 제출 문서의 버전 불일치 위치를 정리했다.
- 최종 후보 릴리스 노트 초안을 압축 형태로 준비했다.

## Day 2 기준선 검증 결과

실행일: 2026-06-14

실행 명령:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --rerun-tasks :app:assembleDebug
```

결과:

- 첫 실행은 샌드박스가 `~/.gradle/wrapper/dists/.../gradle-9.0.0-bin.zip.lck` 접근을 막아 실패했다.
- 동일 명령을 권한 승인 후 재실행했고 `BUILD SUCCESSFUL`로 완료됐다.
- `:app:testDebugUnitTest` 통과
- `:app:compileDebugKotlin` 통과
- `:app:assembleDebug` 통과
- Debug APK 생성 확인: `app/build/outputs/apk/debug/BabCross.apk`

판단:

- 현재 로컬 기준선 `0.1.184 / versionCode 185`는 단위 테스트, Kotlin 컴파일, debug APK 조립 기준으로는 통과 상태다.
- 이 빌드는 최종 릴리스 후보가 아니라 Day 3-6 회귀 테스트에 들어가기 위한 기준선이다.

## Day 3 선행 준비 결과

실행일: 2026-06-14

연결 기기:

- Samsung Galaxy S21 (`SM_G996N`)
- Samsung Galaxy S24 Ultra (`SM_S928N`)

실행 내용:

- `app/build/outputs/apk/debug/BabCross.apk`를 두 기기에 설치했다.
- 두 기기 모두 설치 명령이 `Success`로 완료됐다.
- 두 기기 모두 `com.babcross.app` 설치 버전이 `versionName=0.1.184`, `versionCode=185`임을 확인했다.
- 두 기기 모두 `com.babcross.app/.MainActivity` 실행 intent를 보냈고, 앱이 이미 실행 중인 상태로 확인됐다.

남은 Day 3 실제 상호작용 검증:

- 코드 참여
- QR 참여
- 자동 연결 켜짐/꺼짐 상태별 밥신호 보내기
- 수동 종료
- 제한시간 종료
- 결과 카드 수신과 공유

판단:

- Day 3 회귀 테스트의 단말 설치와 앱 실행 준비는 완료됐다.
- 실제 Nearby 상호작용은 두 기기를 직접 조작하며 확인해야 하므로 아직 완료로 표시하지 않는다.

## Day 3 추가 기준선 캡처

실행일: 2026-06-14

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-g996n-current.png`
- `docs/device-captures-2026-06-14/sm-g996n-window.xml`
- `docs/device-captures-2026-06-14/sm-g996n-logcat.txt`
- `docs/device-captures-2026-06-14/sm-s928n-current.png`
- `docs/device-captures-2026-06-14/sm-s928n-window.xml`
- `docs/device-captures-2026-06-14/sm-s928n-logcat.txt`

기기 상태:

- `SM_G996N`: 기기 Bluetooth on, 기기 Wi-Fi on, 밥크로스 위치/Bluetooth/Nearby Wi-Fi 권한 allow 또는 foreground 상태
- `SM_S928N`: 기기 Bluetooth on, 기기 Wi-Fi on, 밥크로스 위치/Bluetooth/Nearby Wi-Fi 권한 allow 또는 foreground 상태
- 주의: 위 항목은 Android 기기 무선 스위치와 권한 상태를 뜻하며, 밥크로스 앱 내부 자동 연결이 켜져 있다는 뜻은 아니다.
- 앱 내부 자동 연결 저장값은 두 기기 모두 `auto_connect=false`로 확인했다.

현재 화면 기준:

- 두 기기 모두 홈 화면 기준선 캡처를 저장했다.
- 주요 화면 텍스트: `근처 밥친구와 메뉴를 정합니다`, `밥판 열기`, `처음이시네요`, `열린 밥판 없음`, `최근 결정`, `다시 열기`
- `SM_G996N` 밥닉: `가벼운연필`
- `SM_S928N` 밥닉: `가벼운풍선`
- 두 기기 모두 최근 결정에 `오늘도 아이스크림 괜찮을까요?`가 보인다.

로그 기준선:

- 최근 앱 프로세스 로그를 기기별로 저장했다.
- 현재 캡처 범위에서는 `FATAL EXCEPTION` 또는 `ANR`은 발견하지 못했다.
- 두 기기 모두 Nearby discovery 재시작 시점에 `STATUS_ALREADY_DISCOVERING` 경고가 보인다. 다만 앱 내부 자동 연결이 꺼져 있는 상태였으므로, Day 3 실제 연결 회귀에서는 먼저 자동 연결 꺼짐 상태의 램프/팝업이 의도대로 보이는지 확인한 뒤, `밥신호 보내기` 또는 QR 참여 같은 명시적 연결 행동에서 자동 연결이 켜지고 재탐색 로그가 정상화되는지 함께 관찰해야 한다.

판단:

- 실제 터치 회귀 전 기준선 증거는 확보했다.
- 다음 단계는 사람이 두 기기에서 코드 참여 또는 QR 참여를 실행하면서, 같은 캡처/로그 방식으로 성공 또는 실패 증거를 남기는 것이다.

## Day 4 선행 기준선

실행일: 2026-06-14

실제 기기 설정을 변경하지 않고 확인 가능한 권한/설정 기준선을 수집했다.

확인 완료:

- `SM_G996N`: 기기 Bluetooth on
- `SM_G996N`: 기기 Wi-Fi on
- `SM_G996N`: 밥크로스 `COARSE_LOCATION`, `FINE_LOCATION`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` appops가 allow 또는 foreground 상태
- `SM_S928N`: 기기 Bluetooth on
- `SM_S928N`: 기기 Wi-Fi on
- `SM_S928N`: 밥크로스 `COARSE_LOCATION`, `FINE_LOCATION`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` appops가 allow 또는 foreground 상태
- 두 기기 모두 앱 내부 자동 연결은 `auto_connect=false`

아직 직접 검증하지 않은 실패 상태:

- 위치 권한 거부 후 앱 안내
- Bluetooth off 후 앱 안내
- Wi-Fi off 후 앱 안내
- Nearby Wi-Fi 권한 거부 후 앱 안내
- 앱 설정 이동 후 복귀
- 권한 없이 체험 흐름 진입

판단:

- 현재 상태는 "기기 무선 스위치와 권한은 준비됐지만, 밥크로스 자동 연결은 꺼진 상태"다.
- Day 4 실패 상태 검증은 기기 설정을 실제로 바꾸는 작업이라, 다음 실행 때는 변경 전후 캡처와 원상복구까지 한 묶음으로 진행해야 한다.

## Day 3 연결 램프 실제 검증

실행일: 2026-06-14

자동 연결 꺼짐 팝업:

- 두 기기 모두 우하단 연결 램프의 `content-desc`가 `자동 연결 꺼짐`으로 잡혔다.
- 연결 램프 탭 후 두 기기 모두 `자동 연결 꺼짐`, `회색 · 자동 연결 꺼짐` 팝업이 표시됐다.
- 팝업에는 `자동 연결을 켜면 앱이 주변 밥친구를 다시 찾습니다.`, `연결된 밥친구 없음`, `자동 연결 켜기`, `닫기`가 표시됐다.
- 캡처 산출물:
  - `docs/device-captures-2026-06-14/sm-g996n-auto-off-popup.png`
  - `docs/device-captures-2026-06-14/sm-g996n-auto-off-popup.xml`
  - `docs/device-captures-2026-06-14/sm-s928n-auto-off-popup.png`
  - `docs/device-captures-2026-06-14/sm-s928n-auto-off-popup.xml`

자동 연결 켜기:

- 두 기기에서 팝업의 `자동 연결 켜기`를 눌렀다.
- 두 기기 모두 앱 내부 저장값이 `auto_connect=true`로 바뀌었다.
- 두 기기 모두 연결 램프 `content-desc`가 `밥친구 연결됨`으로 바뀌었다.
- 자동 연결 후 로그에서 Nearby keep-alive 송수신이 확인됐다.
- 현재 캡처 범위에서 `FATAL EXCEPTION` 또는 `ANR`은 발견하지 못했다.
- 캡처 산출물:
  - `docs/device-captures-2026-06-14/sm-g996n-auto-on-after.png`
  - `docs/device-captures-2026-06-14/sm-g996n-auto-on-after.xml`
  - `docs/device-captures-2026-06-14/sm-g996n-auto-on-logcat.txt`
  - `docs/device-captures-2026-06-14/sm-s928n-auto-on-after.png`
  - `docs/device-captures-2026-06-14/sm-s928n-auto-on-after.xml`
  - `docs/device-captures-2026-06-14/sm-s928n-auto-on-logcat.txt`

판단:

- 자동 연결 꺼짐 상태의 램프/팝업 UX는 의도대로 보인다.
- 팝업의 명시적 `자동 연결 켜기` 행동으로 자동 연결이 켜지고, 두 기기가 서로 연결된 상태까지 도달했다.
- Day 3의 `자동 연결 꺼짐/켜짐 연결 램프` 항목은 통과로 볼 수 있다.

## Day 3 실제 밥판 상호작용 검증

실행일: 2026-06-14

기기 역할:

- Host: Samsung Galaxy S24 Ultra (`SM_S928N`, 밥닉 `가벼운풍선`)
- Peer: Samsung Galaxy S21 (`SM_G996N`, 밥닉 `가벼운연필`)

실행 흐름:

1. Host에서 `밥판 열기`로 후식 밥판 작성 화면에 진입했다.
2. Host에서 `룰렛으로 후보 추가`를 실행했고 후보 `푸딩`, `와플`, `아이스크림`이 추가됐다.
3. 질문이 비어 있는 상태에서 `밥신호 보내기`를 눌렀을 때 밥판은 발신되지 않았다. Peer 화면도 초대 화면으로 바뀌지 않았다.
4. Host 질문을 `Dessert`로 입력한 뒤 `밥신호 보내기`를 다시 눌렀다.
5. Host는 `내가 연 밥판` 화면으로 이동했고, 참여 현황은 `밥판 참여 1명 · 선택 완료 0명`으로 표시됐다.
6. Peer는 `밥판 초대` 화면을 받았고 `새 밥신호 도착`, `Dessert`, `보낸 밥친구: 가벼운풍선` 문구가 표시됐다.
7. Peer에서 `밥판 참여`를 눌러 `메뉴 선택` 화면에 진입했다.
8. Host 참여 현황은 `밥판 참여 2명 · 선택 완료 0명 · 메뉴 대기 1명`으로 갱신됐다.
9. Peer에서 `푸딩`을 선택했고, Peer는 `선택 완료` 화면과 `영수증 수신 완료` 문구를 표시했다.
10. Host 참여 현황은 `밥판 참여 2명 · 선택 완료 1명`으로 갱신됐다.
11. Host에서 `푸딩`을 선택하자 `모두 골랐어요` 팝업이 표시됐고, `선택 완료 2/2명` 상태가 확인됐다.
12. Host에서 `지금 종료`를 눌러 결과를 확정했다.
13. Host와 Peer 모두 최종 결과 화면에서 `오늘의 후식은`, `푸딩`, `약 180 kcal`, `밥결정 완료. 이제 맛있게 즐기면 됩니다.`가 동일하게 표시됐다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-s928n-compose-entry.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-roulette-spin.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-send2-host.png`
- `docs/device-captures-2026-06-14/sm-g996n-after-send2-peer.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-join-host.png`
- `docs/device-captures-2026-06-14/sm-g996n-after-join-peer.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-peer-vote-host.png`
- `docs/device-captures-2026-06-14/sm-g996n-after-peer-vote.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-host-vote.png`
- `docs/device-captures-2026-06-14/sm-g996n-after-host-vote-peer.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-finalize-host.png`
- `docs/device-captures-2026-06-14/sm-g996n-after-finalize-peer.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-finalize-logcat.txt`
- `docs/device-captures-2026-06-14/sm-g996n-after-finalize-logcat.txt`

로그 판단:

- 최종 확인 범위에서 Host 로그에는 `FATAL EXCEPTION` 또는 `ANR`이 발견되지 않았다.
- 최종 확인 범위에서 Peer 로그에는 앱 크래시/ANR은 발견되지 않았고, 시스템 네트워크/렌더링 로그만 확인됐다.

판단:

- 2기기 Nearby 초대, 참여, 메뉴 선택, 모두 선택 완료 팝업, 수동 종료, 결과 카드 동기화는 실제 기기 기준으로 통과했다.
- 질문 빈 값 상태에서 발신이 진행되지 않는 동작은 확인했지만, 사용자에게 보이는 안내 문구는 별도 캡처에서 명확히 확인하지 못했다. 이후 접근성/극단값 QA 때 함께 재확인한다.
- Day 3의 `코드 참여`, `QR 참여`, `제한시간 종료`, `결과 공유`는 아래 후속 검증에서 이어서 상태를 갱신한다.

## Day 3 결과 공유와 비공개 코드 참여 검증

실행일: 2026-06-14

결과 공유:

- Host 결과 화면에서 `공유하기` 액션을 눌렀다.
- Android 공유 시트가 열렸고 제목은 `텍스트 공유`로 표시됐다.
- 공유 본문 미리보기에는 `밥크로스 오늘의 밥결정: 푸딩`, `후보: 푸딩 2표, 와플 0표, 아이스크림 0표`, `다음 밥판은 같이 고르기`가 표시됐다.
- 실제 외부 앱 전송은 실행하지 않았다.

비공개 코드 참여:

- Host 결과 화면의 `이 후보로 다시 열기`로 새 후식 밥판 작성 화면을 열었다.
- `규칙 변경`에서 `비공개로 보내기`를 켰고 규칙 요약이 `밥신호 · 5분 · 비공개 · 후보 추가 불가 · 밥친구별 선택 공개`로 바뀌었다.
- Host에서 비공개 밥신호를 발신했다.
- Host 화면에 `비공개 밥판 코드`와 코드 `8223`이 표시됐다.
- Host 화면에 `밥판 참여 QR`과 `QR은 연결 힌트로만 쓰고, 실제 밥판 내용은 가까운 기기 연결로 전달됩니다.` 안내가 표시됐다.
- Peer는 `밥판 초대` 화면에서 `보낸 밥친구: 가벼운풍선 · 밥판 코드를 입력하면 바로 참여합니다.`를 표시했다.
- Peer는 `밥판 코드 입력`, `코드가 맞으면 바로 메뉴 선택으로 이동합니다.`, `4자리 밥판 코드 입력`을 표시했다.
- Peer에 코드 `8223`을 입력하자 `메뉴 선택` 화면으로 이동했다.
- Peer 메뉴 선택 화면에는 `Dessert`, `보낸 밥친구: 가벼운풍선`, 후보 `푸딩`, `와플`, `아이스크림`이 표시됐다.
- Host 참여 현황은 `밥판 참여 2명 · 선택 완료 0명 · 메뉴 대기 1명`으로 갱신됐다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-s928n-result-actions-scrolled-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-share-sheet-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-after-reopen-tap.xml`
- `docs/device-captures-2026-06-14/sm-s928n-private-checked-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-private-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-private-host.xml`
- `docs/device-captures-2026-06-14/sm-g996n-private-peer-after-dismiss.png`
- `docs/device-captures-2026-06-14/sm-g996n-private-peer-after-dismiss.xml`
- `docs/device-captures-2026-06-14/sm-g996n-code-join-peer.png`
- `docs/device-captures-2026-06-14/sm-g996n-code-join-peer.xml`
- `docs/device-captures-2026-06-14/sm-s928n-code-join-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-code-join-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-code-join-logcat.txt`
- `docs/device-captures-2026-06-14/sm-g996n-code-join-logcat.txt`

로그 판단:

- Host 로그에는 Nearby payload transfer `SUCCESS, 100%`가 보였고, `FATAL EXCEPTION` 또는 `ANR`은 발견되지 않았다.
- Peer 로그에는 Nearby payload received/transfer `DETAIL_SUCCESS`, `SUCCESS, 100%`가 보였고, 앱 크래시/ANR은 발견되지 않았다.

판단:

- Day 3의 `결과 공유`는 공유 시트와 텍스트 미리보기 기준으로 통과했다.
- Day 3의 `코드 참여`는 비공개 밥판 코드 입력 후 메뉴 선택 진입 기준으로 통과했다.
- Day 3의 `QR 참여`는 Host의 QR 표시와 QR 설명 문구까지 확인했고, 실제 카메라 스캔과 QR URI 딥링크 진입은 아래 후속 검증에서 상태를 갱신한다.
- Day 3의 `제한시간 종료`는 아래 후속 검증에서 이어서 상태를 갱신한다.

## Day 3 제한시간 종료 검증

실행일: 2026-06-14

실행 흐름:

1. Host 홈 화면에서 새 밥판 작성을 시작했다.
2. 질문을 `Timeout`으로 입력했다.
3. 후식 룰렛으로 후보 `푸딩`, `와플`, `아이스크림`을 추가했다.
4. `규칙 변경`에서 제한시간을 `30초`로 바꿨고 규칙 요약이 `밥신호 · 30초 · 후보 추가 불가 · 밥친구별 선택 공개`로 표시됐다.
5. Host에서 밥신호를 발신했다.
6. 발신 직후 Host 화면에는 `밥신호 발신 중`, `Timeout`, `밥판 참여 QR`, `밥판 참여 1명 · 선택 완료 0명`이 표시됐다.
7. 35초 대기 후 Host 화면은 `홈 > 결과`의 `오늘의 밥결정` 화면으로 자동 이동했다.
8. 최종 결과 카드는 `결정 보류`, `Timeout`, `선택된 메뉴 없음`, `아직 아무도 고르지 않았어요`, `득표/참여 0/0`, `점유 0%`를 표시했다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-s928n-timeout-compose-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-roulette-popup.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-rules-open2.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-ready.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-published-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-published-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-expired-host.png`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-expired-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-timeout-expired-logcat.txt`

로그 판단:

- 제한시간 종료 후 Host 로그에는 `FATAL EXCEPTION` 또는 `ANR`이 발견되지 않았다.
- 확인 범위에는 삼성 GameManager, NullBinder, 렌더링 계층 캡처 같은 시스템 로그만 보였다.

판단:

- Day 3의 `제한시간 종료`는 30초 무응답 밥판이 자동으로 `결정 보류` 결과 카드로 이동하는 기준으로 통과했다.
- 무응답 만료 케이스의 사용자 문구는 이해 가능하지만, `득표/참여 0/0`은 밥판장도 참여자로 기대하는 사용자에게는 다소 차갑게 보일 수 있어 이후 UX 개선 후보로 남긴다.
- Day 3의 `QR 실제 스캔 또는 QR URI 딥링크 진입`은 아래 후속 검증에서 상태를 갱신한다.

## Day 3 QR 참여 검증

실행일: 2026-06-14

기기 역할:

- Host: Samsung Galaxy S24 Ultra (`SM_S928N`, 밥닉 `가벼운풍선`)
- Peer: Samsung Galaxy S21 (`SM_G996N`, 밥닉 `가벼운연필`)

실행 흐름:

1. Host에서 새 밥판 `QRTest`를 만들었다.
2. Host에서 후식 룰렛으로 후보 `마카롱`, `쿠키`, `타르트`를 추가했다.
3. Host에서 밥신호를 발신했고 `밥판 참여 QR` 화면이 표시됐다.
4. Host 내부 저장값에서 QR URI를 확인했다.
5. Peer에 `babcross://join?...` URI를 Android VIEW intent로 전달했다.
6. Peer에서 `홈 > 밥판 > QR 연결`, `QR 밥판 연결`, `밥판을 연결 중 입니다..` 화면이 열렸다.
7. Peer 화면은 `QR은 밥판을 찾기 위한 힌트로만 사용하고, 실제 메뉴와 참여 정보는 가까운 기기 연결로 받을게요.`, `남은 대기 시간 약 5분`, `다시 연결 시도`, `홈으로`를 표시했다.
8. Peer에서 `다시 연결 시도`를 눌렀지만 관찰 범위 안에서는 `메뉴 선택` 화면으로 자동 전환되지 않았다.
9. Peer 내부 저장값에는 대상 밥판이 `incomingPolls`로 수신된 상태가 확인됐다.
10. 사용자 직접 확인 결과, 실제 카메라 QR 스캔 경로에서는 QR로 밥판이 정상적으로 열렸다.

QR URI:

```text
babcross://join?v=1&poll=poll-7ab5ca9c-8e53-4438-998c-6a6b50a76d0f&code=&host=167eaffd13b208d7&exp=1781418258559&nonce=09d6390009
```

Peer 내부 상태:

- `incomingPolls[poll-7ab5ca9c-8e53-4438-998c-6a6b50a76d0f]`: 존재
- 수신 질문: `QRTest`
- 보낸 밥친구: `가벼운풍선`
- 수신 후보: `마카롱`, `쿠키`, `타르트`
- `activePolls[target]`: 없음
- `acceptedPollIds`에 대상 poll 없음
- `declinedPollIds`에 대상 poll 없음
- `pendingJoinHint`: 없음

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-s928n-qrtest-compose.xml`
- `docs/device-captures-2026-06-14/sm-s928n-qrtest-ready.xml`
- `docs/device-captures-2026-06-14/sm-s928n-qrtest-host.xml`
- `docs/device-captures-2026-06-14/sm-s928n-qrtest-host.png`
- `docs/device-captures-2026-06-14/sm-g996n-qrtest-deeplink-peer.png`
- `docs/device-captures-2026-06-14/sm-g996n-qrtest-after-retry-peer.png`
- `docs/device-captures-2026-06-14/sm-g996n-qrtest-after-second-retry-peer.png`
- `docs/device-captures-2026-06-14/sm-g996n-qrtest-state-summary.txt`

로그 판단:

- Peer 로그에서 `ClientProxy(com.babcross.app) reporting onPayloadReceived(...), with result: DETAIL_SUCCESS`가 확인됐다.
- Peer 로그에서 `onPayloadTransferUpdate(..., SUCCESS, 100%)`가 확인됐다.
- 확인 범위에서 `FATAL EXCEPTION`, `AndroidRuntime`, `ANR`은 발견되지 않았다.

판단:

- QR URI 딥링크가 앱의 QR 연결 화면을 여는 동작은 통과했다.
- QR 딥링크 이후 Nearby payload가 Peer까지 전달되고 내부 `incomingPolls`에 밥판이 저장되는 동작도 통과했다.
- 실제 카메라 QR 스캔 경로에서 밥판이 정상적으로 열렸으므로, Day 3의 `QR 참여`는 사용자 직접 검증 기준으로 통과로 판정한다.
- ADB VIEW intent로 URI를 직접 주입한 재현에서는 수신된 밥판이 QR 연결 대기 화면에서 `메뉴 선택` 화면으로 자동 승격되지 않았다. 이 케이스는 실제 QR 스캔 실패가 아니라 딥링크 재현 방식 또는 대기 화면 후처리의 별도 경계 케이스로 분리한다.
- 후속 개선 후보는 `QR 연결 대기 중 incomingPolls에 대상 poll이 도착하면 즉시 참여 수락 또는 메뉴 선택 화면으로 전환`하는 방어 로직이다.

## Day 3 현재 종합 판단

통과:

- 자동 연결 꺼짐/켜짐 연결 램프와 팝업
- 2기기 Nearby 초대와 참여
- 비공개 4자리 코드 참여
- 수동 `지금 종료`
- 모두 선택 완료 팝업
- 결과 카드 Host/Peer 동기화
- 결과 공유 시트와 공유 텍스트 미리보기
- 30초 제한시간 자동 종료
- 실제 카메라 QR 스캔으로 밥판 열기
- QR URI 앱 진입
- QR URI 이후 Nearby payload 수신과 내부 밥판 저장

참고 이슈:

- ADB VIEW intent로 QR URI를 직접 주입한 재현에서는 QR 연결 화면 진입과 데이터 수신은 됐지만, Peer UI가 메뉴 선택 화면으로 자동 전환되지 않았다.

남은 확인:

- 자동 연결 꺼짐 상태에서 실제 QR 참여가 같은 방식으로 통과하는지 재확인
- ADB URI 직접 주입 경계 케이스의 QR 대기 화면 후처리 개선 여부

## Day 4 권한/기기 설정 실패 상태 1차 검증

실행일: 2026-06-14

대상 기기:

- Samsung Galaxy S21 (`SM_G996N`, 밥닉 `가벼운연필`)

실행 전 상태:

- 위치 권한: `COARSE_LOCATION=foreground`, `FINE_LOCATION=foreground`
- Bluetooth: `enabled: true`, `state: ON`
- Wi-Fi off 검증은 현재 무선 ADB 연결이 끊길 수 있어 이번 차수에서는 실행하지 않았다.

위치 권한 거부:

1. `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` 권한을 임시로 revoke했다.
2. 앱을 재시작했다.
3. 앱은 `근처 밥친구를 찾기 전에` 권한 설명 화면을 표시했다.
4. 설명에는 `밥크로스는 서버 없이 가까운 기기끼리만 연결합니다.`, `수집하지 않아요`, `위치 기록`, `서버로 보내는 밥닉, 후보, 선택 결과`, `광고/분석 SDK 사용자 행동 전송`, `주변 밥판에 전달돼요`, `밥닉과 아바타`, `메뉴 후보`, `선택 결과 또는 득표 요약`이 표시됐다.
5. 하단 액션으로 `권한 허용하기`, `체험하기`가 표시됐다.
6. 확인 후 위치 권한을 다시 grant했고, appops가 `COARSE_LOCATION=foreground`, `FINE_LOCATION=foreground`로 복구된 것을 확인했다.

Bluetooth off:

1. `svc bluetooth disable`로 Bluetooth를 임시로 껐다.
2. 앱을 재시작했다.
3. 홈 화면의 준비 카드가 `근처 밥친구 연결 준비`를 표시했다.
4. 안내 문구는 `필요한 준비: Bluetooth 켜기`, `위치 권한은 위치 추적이 아니라 Nearby 연결을 위해 필요합니다.`로 표시됐다.
5. 액션 버튼은 `기기 설정 켜기`로 표시됐다.
6. 우하단 연결 램프의 `content-desc`는 `연결 준비 필요`로 확인됐다.
7. 확인 후 `svc bluetooth enable`로 Bluetooth를 다시 켰고, `enabled: true`, `state: ON`으로 복구된 것을 확인했다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-g996n-day4-before-settings.png`
- `docs/device-captures-2026-06-14/sm-g996n-location-denied-home.png`
- `docs/device-captures-2026-06-14/sm-g996n-location-denied-home.xml`
- `docs/device-captures-2026-06-14/sm-g996n-location-denied-lamp.png`
- `docs/device-captures-2026-06-14/sm-g996n-location-denied-lamp.xml`
- `docs/device-captures-2026-06-14/sm-g996n-bluetooth-off-home.png`
- `docs/device-captures-2026-06-14/sm-g996n-bluetooth-off-home.xml`
- `docs/device-captures-2026-06-14/sm-g996n-bluetooth-off-lamp2.png`
- `docs/device-captures-2026-06-14/sm-g996n-bluetooth-off-lamp2.xml`
- `docs/device-captures-2026-06-14/sm-g996n-day4-permission-settings-logcat.txt`

로그 판단:

- 확인 범위에서 `FATAL EXCEPTION`, `AndroidRuntime`, `ANR`은 발견되지 않았다.
- Bluetooth 복구 후 Nearby keep-alive 로그가 다시 확인됐다.

판단:

- 위치 권한 거부 상태의 사전 설명과 대체 진입(`체험하기`)은 통과로 볼 수 있다.
- Bluetooth off 상태의 홈 준비 카드와 설정 이동 액션은 통과로 볼 수 있다.
- Bluetooth off 상태의 연결 램프 팝업은 홈 카드보다 덜 명확하게 `밥친구 대기 중`으로 표시되는 캡처가 있어, 이후 램프 팝업과 홈 준비 카드의 상태 일관성을 재확인하는 것이 좋다.
- Wi-Fi off, Nearby Wi-Fi 권한 거부, 앱 설정 이동 후 복귀, 권한 없이 체험 흐름 진입은 다음 Day 4 차수로 남긴다.

## Day 4 권한/기기 설정 실패 상태 2차 검증

실행일: 2026-06-14

대상 기기:

- Samsung Galaxy S21 (`SM_G996N`, 밥닉 `가벼운연필`)

Nearby Wi-Fi 권한 거부:

1. `NEARBY_WIFI_DEVICES` 권한을 임시로 revoke했다.
2. 앱을 재시작했다.
3. 앱은 위치 권한 거부 때와 같은 `근처 밥친구를 찾기 전에` 사전 설명 화면을 표시했다.
4. 설명에는 서버 없이 가까운 기기끼리 연결한다는 안내, 수집하지 않는 항목, 주변 밥판에 전달되는 항목, `권한 허용하기`, `체험하기`가 표시됐다.
5. 확인 후 `NEARBY_WIFI_DEVICES` 권한을 다시 grant했고, appops에서 `NEARBY_WIFI_DEVICES=allow`로 복구된 것을 확인했다.

권한 없이 체험 흐름 진입:

1. `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` 권한을 임시로 revoke했다.
2. 권한 설명 화면에서 `체험하기` 버튼 bounds `[686,2165][978,2232]`를 확인하고 정확히 눌렀다.
3. 앱은 홈 화면으로 진입했고, `1. 밥판 열기 안내` 체험 코치마크를 표시했다.
4. 코치마크에는 `1/7`, `1. 밥판 열기`, `실제 홈 화면의 \`밥판 열기\`에서 시작합니다.`, `닫기`, `다음`이 표시됐다.
5. 권한이 없는 상태에서도 홈 화면의 `밥판 체험하기`, `처음이시네요`, `밥판 체험을 먼저 시작해볼까요?`가 유지됐다.
6. 동시에 홈 준비 카드에는 `근처 밥친구 연결 준비`, `필요한 준비: Nearby/위치 권한 허용`, `권한 허용`이 표시됐다.
7. 확인 후 위치 권한을 다시 grant했고, appops에서 `COARSE_LOCATION=foreground`, `FINE_LOCATION=foreground`로 복구된 것을 확인했다.

앱 설정 이동 후 복귀:

1. Bluetooth를 임시로 off 상태로 만들었다.
2. 앱 홈 준비 카드의 `기기 설정 켜기` 버튼 bounds `[89,1345][991,1471]`를 확인했다.
3. `기기 설정 켜기`를 누르자 Android `애플리케이션 정보` 화면으로 이동했다.
4. 설정 화면에는 `밥크로스(Bab-Cross)`, `설치됨`, `개인정보 보호`, `알림`, `권한`, `근처 기기 및 위치`, `사용 시간`, `삭제`, `강제 중지`가 표시됐다.
5. 뒤로 가기로 앱 화면에 복귀했다.
6. 확인 후 Bluetooth를 다시 켰고 `enabled: true`, `state: ON`으로 복구된 것을 확인했다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-g996n-nearby-wifi-denied-home.png`
- `docs/device-captures-2026-06-14/sm-g996n-nearby-wifi-denied-home.xml`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-before.png`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-before2.xml`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-after.png`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-after.xml`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-after2.png`
- `docs/device-captures-2026-06-14/sm-g996n-demo-no-permission-after2.xml`
- `docs/device-captures-2026-06-14/sm-g996n-settings-return-before.xml`
- `docs/device-captures-2026-06-14/sm-g996n-settings-return-system.png`
- `docs/device-captures-2026-06-14/sm-g996n-settings-return-system.xml`
- `docs/device-captures-2026-06-14/sm-g996n-settings-return-back.png`
- `docs/device-captures-2026-06-14/sm-g996n-day4-permission-settings-2-logcat.txt`
- `docs/device-captures-2026-06-14/sm-g996n-day4-permission-settings-3-logcat.txt`

로그 판단:

- 확인 범위에서 `FATAL EXCEPTION`, `AndroidRuntime`, `ANR`은 발견되지 않았다.

판단:

- Nearby Wi-Fi 권한 거부 상태의 사전 설명은 통과로 볼 수 있다.
- 권한 없이 `체험하기`로 홈과 7단계 체험 코치마크에 진입하는 흐름은 통과로 볼 수 있다.
- `기기 설정 켜기`는 실제 Android 앱 정보 화면으로 이동하고 뒤로 복귀가 가능했다.
- 버튼명은 `기기 설정 켜기`지만 실제 이동지는 Bluetooth 설정 패널이 아니라 앱 정보 화면이었다. 권한 관리에는 유효하지만, Bluetooth off 사용자가 기대하는 즉시 Bluetooth 켜기 동선으로는 다소 간접적이다.
- Wi-Fi off 직접 검증은 무선 ADB 연결 유지를 위해 아직 실행하지 않았다.

## Day 5 접근성/극단값 1차 검증

실행일: 2026-06-14

대상 기기:

- Samsung Galaxy S21 (`SM_G996N`, 밥닉 `가벼운연필`, font scale `2.0`)
- Samsung Galaxy S24 Ultra (`SM_S928N`, 밥닉 `가벼운풍선`, font scale `0.9` -> 임시 `1.3` -> `0.9` 복구)
- Samsung Galaxy Z Fold7 (`SM_F966N`, 밥닉 `든든한나침반`, font scale `1.0`)

설치 버전:

- 세 기기 모두 `versionName=0.1.184`, `versionCode=185` 확인.
- `SM_F966N`은 검증 전 `0.1.176/177`이었고, `app/build/outputs/apk/debug/BabCross.apk` 설치 후 `0.1.184/185`로 맞췄다.

큰 글씨/폴더블 홈 화면:

1. `SM_G996N`은 이미 시스템 글자 크기 `2.0` 상태였고, 홈 화면을 그대로 캡처했다.
2. `SM_S928N`은 시스템 글자 크기를 임시로 `1.3`으로 변경한 뒤 앱을 재시작해 홈 화면을 캡처했다.
3. `SM_S928N` 검증 후 시스템 글자 크기를 원래 `0.9`로 복구했고, `settings get system font_scale`에서 `0.9`를 확인했다.
4. `SM_F966N`은 폴더블 기본 글자 크기 `1.0` 상태로 홈 화면을 캡처했다.

접근성 라벨/텍스트 확인:

- 홈 헤더 마스코트는 `밥크로스 마스코트`로 노출됐다.
- 앱 로고는 `밥크로스`, 보조 문구는 `근처 밥친구와 메뉴를 정합니다`로 노출됐다.
- 닉네임 영역은 `내 밥닉네임 관리`로 노출됐다.
- 주요 CTA `밥판 열기`, 체험 카드 `밥판 체험하기`, `시작`, 하단 탭 `홈`, `밥판`, `결과`, `설정`이 XML에 노출됐다.
- 우하단 연결 램프는 세 기기 모두 `밥친구 연결됨`으로 노출됐다.

화면 판단:

- `SM_F966N` 기본 글씨 폴더블 화면은 홈 주요 카드, 열린 밥판, 최근 결정, 하단 탭, 연결 램프가 안정적으로 표시됐다.
- `SM_S928N` font scale `1.3`에서는 홈 기능은 모두 보이고 조작 가능했다. 다만 최근 결정 카드 하단 텍스트 일부가 화면 하단에 걸쳐 잘리는 상태가 보여 여유 공간 개선이 필요하다.
- `SM_G996N` font scale `2.0`에서는 홈 진입과 하단 탭/연결 램프는 가능하지만 상단 헤더의 닉네임이 말줄임 처리되고, `근처 밥친구 연결 준비` 카드가 크게 확장되어 한 화면 정보 밀도가 떨어진다. 극단 큰 글씨 사용자를 위해 헤더/상태 카드 레이아웃을 추가 개선하는 것이 좋다.

작성 화면 긴 입력:

1. `SM_F966N`에서 `밥판 열기`를 눌러 작성 화면에 진입했다.
2. 작성 화면에는 `홈 > 밥판 > 밥판 열기`, `밥판 열기`, `종류`, `요리`, `요리 템플릿 선택`, `오늘의 질문`, `메뉴 후보`, `룰렛으로 후보 추가`, `밥판 규칙`, `규칙 변경`, `밥신호 보내기`가 표시됐다.
3. 질문에 `Accessibility test long question for BabCross menu decision with many friends`를 입력했고, 같은 질문이 `상대에게 이렇게 보여요` 미리보기 영역에도 반영됐다.
4. 후보에는 `Candidate01ExtraLongMenuName`이 칩으로 추가됐다.
5. 후보 10개 이상 자동 입력 시도 중 키보드 표시로 입력 좌표가 이동해 나머지 후보가 한 입력 필드에 이어 붙었다. 이어 붙은 긴 후보를 추가하려 하자 앱은 `메뉴 후보는 30자 이하로 입력해 주세요.` 토스트를 표시했다.
6. 따라서 이번 차수에서는 긴 후보명 방어 로직은 확인했지만, 후보 10개 이상 완료 상태는 확인하지 못했다. 후보 10개 이상은 30자 이하 짧은 후보명으로 별도 재검증한다.

후보 10개 이상 재시도:

1. `SM_F966N`에서 앱을 재시작하고 새 작성 화면으로 다시 진입했다.
2. 질문에 `Day5Ten`을 입력하고 후보 `M01`부터 `M10`까지 짧은 후보명 자동 입력을 시도했다.
3. 첫 후보 `M01`은 칩으로 정상 추가됐다.
4. 후보 칩이 생긴 뒤 입력칸이 아래로 이동하면서 좌표 기반 자동 입력이 다시 흔들렸고, `M04M05M06M07M08M09M10`이 한 입력 필드에 이어 붙은 상태로 남았다.
5. 이번 기기 자동화만으로는 후보 10개 완료 화면을 재현하지 못했다. 다만 코드 기준으로 `MAX_POLL_OPTION_COUNT = 20`, `MAX_OPTION_LENGTH = 30`이고, `OptionTagEditor`의 `addOption`/`submitOption`은 20개까지 후보 추가를 허용하며 중복과 30자 초과를 막는다.
6. 수동 입력 또는 Espresso/UIAutomator의 텍스트 필드 식별 기반 자동화로 10개 이상 완료 화면을 재검증하는 것이 필요하다.

굵은 글씨/화면 확대:

1. `SM_S928N`은 재확인 시점에 ADB 연결 목록에서 빠져 있어 추가 설정 변경을 진행하지 않았다.
2. `SM_G996N`에서 현재 화면 밀도와 글자 크기를 확인했다: `Physical density: 450`, `Override density: 420`, `font_scale=2.0`.
3. 화면 확대 시뮬레이션을 위해 `wm density 520`을 임시 적용하고 앱 재시작/캡처를 시도했다.
4. 캡처 후 즉시 `wm density 420`으로 복구했고, `wm density`에서 `Override density: 420`을 확인했다.
5. 다만 확대 상태 캡처는 앱 홈이 아니라 잠금/검은 화면 상태로 저장되어 앱 UI 검증 증거로 쓰기 어렵다.
6. 굵은 글씨는 Samsung 기기에서 ADB로 안전하게 토글할 명확한 설정 키가 확인되지 않아 임의 변경하지 않았다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0.png`
- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0.xml`
- `docs/device-captures-2026-06-14/sm-s928n-day5-home-font1.3.png`
- `docs/device-captures-2026-06-14/sm-s928n-day5-home-font1.3.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-font1.0.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-font1.0.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-long-10.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-long-10.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-long-final.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-long-final.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-short-10.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-short-10.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-short-10-retry.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-short-10-retry.xml`
- `docs/device-captures-2026-06-14/sm-g996n-day5-density520-font2.0-home.png`
- `docs/device-captures-2026-06-14/sm-g996n-day5-density520-font2.0-home.xml`
- `docs/device-captures-2026-06-14/sm-g996n-day5-logcat.txt`
- `docs/device-captures-2026-06-14/sm-s928n-day5-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-compose-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-short-10-retry-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-after-retry-logcat.txt`

로그 판단:

- 확인 범위에서 밥크로스 `FATAL EXCEPTION`, `AndroidRuntime`, `ANR`은 발견되지 않았다.
- `SM_G996N`에서는 Nearby payload transfer `SUCCESS` 로그가 다수 확인되어 세 기기 연결 상태가 유지되는 정황이 있었다.
- `SM_F966N`에서는 시스템 Wi-Fi scan cache 메시지와 Samsung GameManager의 일반 패키지 식별 로그, 키보드 insets 로그만 확인됐다.
- 후보 10개 재시도 후 추가 로그에서도 밥크로스 크래시/ANR은 발견되지 않았다.

남은 Day 5 항목:

- 굵은 글씨 시스템 옵션.
- 화면 확대/디스플레이 줌 앱 화면 재검증. 이번 차수에서는 밀도 복구까지 확인했지만 앱 UI 캡처는 잠금/검은 화면으로 남았다.
- 후보 10개 이상 작성 화면과 결과 화면. 코드상 후보 20개까지 허용되는 것은 확인했지만, 이번 좌표 기반 기기 자동화에서는 10개 완료 화면을 재현하지 못했다.
- TalkBack 실제 음성 순서 확인.

## Day 5 접근성 개선 반영 및 재검증

실행일: 2026-06-14

수정 버전:

- `versionName=0.1.185`, `versionCode=186`

수정 내용:

- 홈 상단 브랜드/프로필/`밥판 열기` 영역이 큰 글씨에서 고정 높이에 눌리지 않도록 `WRAP_CONTENT` 기반으로 조정했다.
- 홈 `밥판 열기` 버튼 텍스트에 내부 여백과 2줄 허용을 추가해 큰 글씨에서 잘림 가능성을 줄였다.
- 최근 결정 카드는 기존 88dp보다 여유 있는 112dp 고정 높이로 조정하고, 제목 2줄과 버튼 2줄을 허용했다.
- 첫 수정에서 최근 결정 카드가 정상 글씨 폴더블에서 과도하게 커지는 문제가 보여, 최종 수정에서는 카드 높이를 112dp로 고정해 재보정했다.

검증:

1. `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --rerun-tasks :app:assembleDebug`를 실행했고 성공했다.
2. `SM_G996N`, `SM_F966N`에 `app/build/outputs/apk/debug/BabCross.apk`를 설치했다.
3. 두 기기 모두 `versionName=0.1.185`, `versionCode=186`으로 확인했다.
4. `SM_G996N` font scale `2.0`에서 홈 화면을 재캡처했다.
5. `SM_F966N` font scale `1.0`에서 홈 화면을 재캡처했다.

판단:

- `SM_F966N` 기본 글씨에서는 최근 결정 카드 과확장 문제가 사라졌고, 홈 상단/체험 카드/열린 밥판/최근 결정/하단 탭이 안정적으로 표시됐다.
- `SM_G996N` font scale `2.0`에서는 홈 진입과 주요 버튼 조작은 가능하지만, 상단 브랜드 보조 문구와 최근 결정 카드 하단 일부는 여전히 화면 여유가 부족하다. 이번 수정은 부분 개선으로 보고, 극단 큰 글씨에서는 홈 카드의 정보량을 더 줄이거나 최근 결정 영역을 접이식/더보기 구조로 바꾸는 후속 개선이 필요하다.
- 최종 로그에서 밥크로스 `FATAL EXCEPTION`, `AndroidRuntime`, `ANR`은 발견되지 않았다.

캡처 산출물:

- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0-v0.1.185.png`
- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0-v0.1.185.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-v0.1.185.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-v0.1.185.xml`
- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0-v0.1.185-final.png`
- `docs/device-captures-2026-06-14/sm-g996n-day5-home-font2.0-v0.1.185-final.xml`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-v0.1.185-final.png`
- `docs/device-captures-2026-06-14/sm-f966n-day5-home-v0.1.185-final.xml`
- `docs/device-captures-2026-06-14/sm-g996n-day5-v0.1.185-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-v0.1.185-logcat.txt`
- `docs/device-captures-2026-06-14/sm-g996n-day5-v0.1.185-final-logcat.txt`
- `docs/device-captures-2026-06-14/sm-f966n-day5-v0.1.185-final-logcat.txt`

Play Console 반영:

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease`로 release AAB를 재생성했다.
- Release AAB SHA-256: `323f2e4123d7ea5bbbe93747b26efe12f4a8c98844041fbc481cec7e576e8582`
- 2026-06-14 16:43 KST에 Play Console 내부 테스트 트랙으로 `186 (0.1.185)`를 저장 및 출시했다.
- Console 확인 상태: `최신 출시 버전: 186 (0.1.185)`, `내부 테스터에게 제공됨`, `검토되지 않음`
- 지원 기기 감소: 전화 `0`, 태블릿 `0`, TV `0`, 차량 `0`, Chromebook `0`, Android XR `0`
