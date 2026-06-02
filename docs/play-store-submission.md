# Google Play 내부 테스트 등록 메모

작성일: 2026-05-31

## 릴리스 후보

- Package name: `com.babcross.app`
- Release candidate: `0.1.75`
- Version code: `76`
- Min SDK: `26`
- Target SDK: `35`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Debug APK: `app/build/outputs/apk/debug/BabCross.apk`

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

검토 필요:

- GitHub Pages 설정에서 Source를 `Deploy from a branch`, Branch를 `main`, Folder를 `/docs`로 지정한 뒤 위 URL이 로그인 없이 열리는지 확인한다.
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

종류를 고르면 그 종류에 맞는 템플릿과 룰렛 후보로 바로 밥판을 만들 수 있고, Nearby Connections로 주변 Android 기기에 초대를 보냅니다. 참여자는 규칙을 확인하고 한 번 선택하며, 결과는 득표 요약 또는 밥친구별 선택 공개 방식으로 확인할 수 있습니다.

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
- 반복 사용 시 최근 밥판 종류로 더 빠르게 작성 화면에 진입합니다.
- 작성 화면을 기본 입력과 밥판 규칙으로 나눠 첫 사용 부담을 줄였습니다.
- 음식 데이터, 룰렛 후보, 칼로리 힌트를 같은 카탈로그로 정리했습니다.
- 음료수/후식/간식 템플릿과 룰렛 후보를 보강했습니다.
- Nearby 메시지 검증과 로컬 데이터 관리 기능을 강화했습니다.
- 개인정보처리방침과 Play 제출 문구에서 개발자 서버 수집 없음, 로컬 저장, 사용자 주도 근거리 기기 간 전달 범위를 더 명확히 설명했습니다.
- Nearby 메시지 재전송/오래된 메시지 방어와 release 빌드 보안 설정을 보강했습니다.
- 밥판 규칙 변경은 링크로 가볍게 열고, 템플릿 저장은 별도 액션으로 분리했습니다.
- 혼자 먼저 정리할 때는 상대 프리뷰 대신 결과 카드 미리보기를 보여줍니다.
- 작성 화면 상단의 종류 라벨과 배지 간격을 줄이고 선택 종류 배지를 키웠습니다.
- 실패한 상황별 이미지 적용을 되돌리고 기존 밥크로스 원본 이미지 기준으로 복원했습니다.
- 혼자 먼저 정리할 때는 메뉴를 먼저 고른 뒤에만 결과 카드를 만들 수 있게 했습니다.
```

## 스크린샷 시나리오

1. 홈: 메뉴 못 정할 때 밥판 열기
2. 밥판 열기: 종류별 템플릿과 룰렛으로 후보 만들기
3. 초대/참여: 이 밥판 규칙 보고 바로 고르기
4. 결과 카드: 오늘의 식사/요리/음료수 결정
5. 신뢰 안내: 서버 저장 없음, 주변 기기 공유 범위, 로컬 데이터 관리

## 내부 테스트 체크리스트

- `:app:testDebugUnitTest` 통과
- `:app:compileDebugKotlin` 통과
- `:app:assembleDebug` 통과
- `:app:bundleRelease` 통과
- AAB `versionName=0.1.75`, `versionCode=76` 확인
- AAB 서명 확인
- release 빌드 `debuggable=false` 확인
- release 빌드 R8 minify/resource shrink 적용 확인
- 개인정보처리방침 URL 공개 접근 확인
- Play Console 앱 콘텐츠, 콘텐츠 등급, 타겟 연령층, Data safety 입력

## 단말 테스트 체크리스트

- Samsung Galaxy Z Fold7 (`SM_F966N`) 설치 및 기본 실행
- 두 기기 이상에서 밥판 생성, 초대 수락, 투표, 조기 종료, 시간 만료, 결과 공유 확인
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
- Debug APK: `app/build/outputs/apk/debug/BabCross.apk`
- 변경 이력: `CHANGELOG.md`
- 6차 개선안: `docs/app-sixth-improvements-2026-05-31.md`
