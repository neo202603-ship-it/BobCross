# Google Play 내부 테스트 등록 메모

작성일: 2026-05-31
최종 점검일: 2026-06-14

## 릴리스 후보

- Package name: `com.babcross.app`
- Local release candidate: `0.1.185`
- Local version code: `186`
- Play internal latest: `0.1.185` / version code `186` (2026-06-14 16:43 KST 내부 테스트 배포 완료)
- Min SDK: `26`
- Target SDK: `35`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Debug APK: `app/build/outputs/apk/debug/BabCross.apk`
- Local release AAB SHA-256: `323f2e4123d7ea5bbbe93747b26efe12f4a8c98844041fbc481cec7e576e8582`

## 업로드 키

로컬 업로드 키는 Git에 포함하지 않는다.

- Keystore: `signing/babcross-upload.jks`
- Properties: `keystore.properties`
- Alias: `babcross-upload`
- SHA-1: `69:6F:8D:03:FC:B1:AE:D7:C0:55:C0:F2:D5:54:C5:F5:3E:8D:DD:F9`
- SHA-256: `97:96:DA:80:D7:E8:E2:B4:D3:9B:27:A7:BF:2F:BD:0F:4B:05:58:E1:15:DC:D1:BB:DE:DE:B2:36:82:06:78:F7`

`signing/babcross-upload.jks`와 `keystore.properties`는 잃어버리면 앱 업데이트가 어려워질 수 있으니 안전한 곳에 백업한다.

## 개인정보처리방침

- 현재 URL: `https://neo202603-ship-it.github.io/BobCross/privacy-policy.html`
- 앱 내부 위치: `설정 > 개인정보처리방침`
- 앱 내부 데이터 삭제 위치: `설정 > 내 로컬 데이터 관리`

확인 완료:

- 개인정보처리방침 URL은 2026-06-12에 `HTTP 200` 공개 접근을 확인했다.
- 앱 내부 개인정보처리방침, Play Data safety 답변, 권한 설명은 모두 `개발자 서버 수집 없음`과 `사용자 주도 근거리 기기 간 전달`을 분리해 같은 표현으로 설명한다.
- 내 기기에서 삭제해도 이미 상대 기기에 전달된 밥판 데이터는 회수되지 않는다는 문구를 유지한다.

## Play 등록 문구 초안

짧은 설명:

```text
가까운 사람들과 메뉴 후보를 올리고 빠르게 고르는 근거리 밥판 앱
```

긴 설명:

```text
밥크로스는 점심, 회식, 음료수, 후식, 간식처럼 매번 고르기 어려운 선택을 가까운 사람들과 빠르게 정리하는 밥판 앱입니다.

종류를 고르면 그 종류에 맞는 템플릿과 룰렛 후보로 바로 밥판을 만들 수 있고, Nearby Connections로 주변 Android 기기에 초대를 보냅니다. 밥판장은 4자리 코드를 구두로 알려주거나 QR을 보여줄 수 있고, 참여자는 코드 입력 또는 QR 스캔으로 밥판 연결을 시작합니다. 결과는 득표 요약 또는 밥친구별 선택 공개 방식으로 확인할 수 있습니다.

밥크로스는 자체 서버, 광고 SDK, 분석 SDK를 사용하지 않습니다. 위치 기록을 저장하지 않고, 사용자가 밥판을 열거나 참여할 때 밥닉/아바타/후보/선택 결과는 밥판 기능을 위해 가까운 참여자 기기끼리만 전달됩니다. 설정에서 지난 결정, 영수증/해시, 사용자 템플릿, 프로필/기본값을 삭제할 수 있습니다.
```

## Data safety 권장 입력

밥크로스는 자체 서버, 광고 SDK, 분석 SDK를 사용하지 않는다. Play Console에는 `개발자 서버로 수집하는 사용자 데이터 없음`과 `앱 기능을 위한 사용자 주도 근거리 기기 간 전달`을 분리해 입력한다.

- Data collection: No user data collected
- Data sharing: No user data shared with third parties
- Data encrypted in transit: 해당 없음 또는 No server/developer transfer
- Users can request data deletion: 앱의 `설정 > 내 로컬 데이터 관리`, 앱 데이터 삭제, 앱 삭제로 로컬 데이터 삭제 가능

주의:

- 밥닉네임, 아바타, 메뉴 후보, 선택 내용, 결과는 사용자가 밥판을 열거나 참여할 때 Nearby Connections로 주변 참여 기기에 전달될 수 있다.
- 이는 앱 기능 수행을 위한 사용자 주도 근거리 기기 간 공유이며, 밥크로스 서버 수집이나 제3자 판매/광고 목적 공유는 아니다.
- 내 기기에서 삭제해도 이미 상대 기기에 전달된 밥판 데이터는 회수되지 않는다.

## 권한 설명

앱이 요청하는 권한은 주변 기기 검색과 연결을 위한 것이다.

- Bluetooth 계열 권한
- Wi-Fi / Nearby 기기 권한
- 위치 관련 권한

Play Console 심사 설명 초안:

```text
밥크로스는 서버 없이 가까운 Android 기기끼리 메뉴 후보와 선택 결과를 교환하는 앱입니다. Bluetooth, Wi-Fi, Nearby 기기, 위치 관련 권한은 Nearby Connections를 통해 주변 참여 기기를 찾고 연결하기 위해서만 사용합니다. 위치 관련 권한은 위치 추적용이 아니며, 위치 기록을 수집하거나 저장하지 않습니다. 광고/분석 SDK로 사용자 행동을 전송하지 않습니다.
```

## 릴리스 노트 초안

```text
- QR로 공개/비공개 밥판에 빠르게 참여하고, 연결 대기 화면에서 진행 상태를 확인할 수 있습니다.
- QR은 밥판 찾기 힌트만 담고 실제 후보와 참여 정보는 Nearby로 받은 뒤에만 열립니다.
- 우하단 연결 배지는 각 기기의 직접 연결 수로 정리하고, 밥판장 화면에는 밥판 참여 현황 카드를 별도로 추가했습니다.
- 결정 카드 복사/공유 이미지는 화면 카드와 같은 렌더링을 쓰도록 개선하고, 기기 큰 글씨/굵은 글씨 설정 영향을 줄였습니다.
- 코드 입력 시 타이핑하듯 짧은 진동 피드백을 제공합니다.
- 튜토리얼 설명풍선을 더 어둡고 읽기 쉽게 조정하고, 종료 말풍선이 기기 하단 버튼에 가려지지 않도록 보정했습니다.
- 결과 순위, 동률 추천, 무득표 상태, 공유 문구 계산을 단위 테스트 가능한 순수 로직으로 분리했습니다.
```

## 스크린샷 시나리오

1. 홈: 메뉴 못 정할 때 밥판 열기
2. 밥판 열기: 종류별 템플릿과 룰렛으로 후보 만들기
3. 초대/참여: 4자리 밥판 코드 또는 QR로 참여 후 바로 고르기
4. 결과 카드: 오늘의 식사/요리/음료수 결정
5. 신뢰 안내: 서버 저장 없음, 주변 기기 공유 범위, 로컬 데이터 관리

생성된 스크린샷:

- `store-assets/google-play/screenshots/phone-sm-s928n-01-home.png`
- `store-assets/google-play/screenshots/phone-sm-s928n-02-open-board.png`
- `store-assets/google-play/screenshots/phone-sm-s928n-03-compose-board.png`
- `store-assets/google-play/screenshots/phone-sm-s928n-04-settings.png`
- `store-assets/google-play/screenshots/fold-sm-f966n-01-home.png`

생성된 태블릿 업로드용 스크린샷:

- `store-assets/google-play/tablet-7-inch/tablet-7-01-home.png`
- `store-assets/google-play/tablet-7-inch/tablet-7-02-open-board.png`
- `store-assets/google-play/tablet-7-inch/tablet-7-03-compose-board.png`
- `store-assets/google-play/tablet-7-inch/tablet-7-04-settings.png`
- `store-assets/google-play/tablet-10-inch/tablet-10-01-home.png`
- `store-assets/google-play/tablet-10-inch/tablet-10-02-open-board.png`
- `store-assets/google-play/tablet-10-inch/tablet-10-03-compose-board.png`
- `store-assets/google-play/tablet-10-inch/tablet-10-04-settings.png`

## 내부 테스트 체크리스트

- `:app:testDebugUnitTest` 통과
- `:app:compileDebugKotlin` 통과
- `:app:assembleDebug` 통과
- `:app:bundleRelease` 통과
- AAB `versionName=0.1.185`, `versionCode=186` 확인
- AAB 서명 확인: release upload key SHA-1 `69:6F:8D:03:FC:B1:AE:D7:C0:55:C0:F2:D5:54:C5:F5:3E:8D:DD:F9`
- release 빌드 `debuggable=false` 확인: release manifest에 `android:debuggable` 속성 없음
- release 빌드 R8 minify/resource shrink 적용 확인: `app/build/outputs/mapping/release/` 및 `usage.txt`, `resources.txt` 생성
- 개인정보처리방침 URL 공개 접근 확인
- Play Console 앱 콘텐츠, 콘텐츠 등급, 타겟 연령층, Data safety 입력

## Play Console 배포 결과

- 2026-06-13 20:14 KST에 내부 테스트 트랙으로 `0.1.156` 배포 완료
- Play Console 상태: `내부 테스터에게 제공됨`
- 업로드된 App Bundle: `157 (0.1.156)`
- 당시 내부 테스트 최신 버전: `157 (0.1.156)`
- 2026-06-13 21:27 KST에 내부 테스트 트랙으로 `0.1.167` 배포 완료
- Play Console 상태: `내부 테스터에게 제공됨`
- 업로드된 App Bundle: `168 (0.1.167)`
- 당시 내부 테스트 최신 버전: `168 (0.1.167)`
- 2026-06-14 16:00 KST에 내부 테스트 트랙으로 `0.1.184` 배포 완료
- Play Console 상태: `내부 테스터에게 제공됨`
- 업로드된 App Bundle: `185 (0.1.184)`
- 당시 내부 테스트 최신 버전: `185 (0.1.184)`
- 2026-06-14 16:43 KST에 내부 테스트 트랙으로 `0.1.185` 배포 완료
- Play Console 상태: `내부 테스터에게 제공됨`
- 업로드된 App Bundle: `186 (0.1.185)`
- 현재 내부 테스트 최신 버전: `186 (0.1.185)`
- Play Console 검토 화면 기준 지원 기기 감소: 전화 `0`, 태블릿 `0`, TV `0`, 차량 `0`, Chromebook `0`, Android XR `0`

## 단말 테스트 체크리스트

- Samsung Galaxy S24 Ultra (`SM_S928N`) 설치 버전 확인: `versionName=0.1.185`, `versionCode=186`
- Samsung Galaxy S21 (`SM_G996N`) 설치 버전 확인: `versionName=0.1.185`, `versionCode=186`
- Samsung Galaxy Z Fold7 (`SM_F966N`) 설치 버전 확인: `versionName=0.1.185`, `versionCode=186`
- 두 기기 이상에서 밥판 생성, 참여, 투표, 결과 확인 플로우 이상 없음 확인
- 권한 전체 허용
- 위치 권한 거부
- Bluetooth 꺼짐
- Wi-Fi 꺼짐
- 앱 설치 직후 첫 실행
- 앱 데이터 삭제 후 재실행
- 긴 질문, 긴 메뉴명, 후보 10개 이상
- 동률 결과, 무득표 결과, 비공개 선택 결과
- `설정 > 내 로컬 데이터 관리`의 항목별 삭제 확인

## 보관 산출물

- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Release AAB SHA-256: `323f2e4123d7ea5bbbe93747b26efe12f4a8c98844041fbc481cec7e576e8582`
- Debug APK: `app/build/outputs/apk/debug/BabCross.apk`
- Google Play 앱 아이콘: `store-assets/google-play/icon-512.png`
- Google Play 그래픽 이미지: `store-assets/google-play/feature-graphic-1024x500.png`
- Google Play 스크린샷: `store-assets/google-play/screenshots/`
- Google Play 태블릿 스크린샷: `store-assets/google-play/tablet-7-inch/`, `store-assets/google-play/tablet-10-inch/`
- 변경 이력: `CHANGELOG.md`
- 6차 개선안: `docs/app-sixth-improvements-2026-05-31.md`
