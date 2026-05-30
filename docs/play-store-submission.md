# Google Play 등록 메모

## 업로드 파일

- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Package name: `com.babcross.app`
- Version: `0.1.0`
- Version code: `1`

## 개인정보처리방침

- URL: `https://github.com/neo202603-ship-it/BobCross/blob/main/PRIVACY_POLICY.md`
- 앱 내부 위치: `설정 > 개인정보처리방침`

## 업로드 키

로컬 업로드 키는 Git에 포함하지 않는다.

- Keystore: `signing/babcross-upload.jks`
- Properties: `keystore.properties`
- Alias: `babcross-upload`
- SHA-1: `69:6F:8D:03:FC:B1:AE:D7:C0:55:C0:F2:D5:54:C5:F5:3E:8D:DD:F9`
- SHA-256: `97:96:DA:80:D7:E8:E2:B4:D3:9B:27:A7:BF:2F:BD:0F:4B:05:58:E1:15:DC:D1:BB:DE:DE:B2:36:82:06:78:F7`

`signing/babcross-upload.jks`와 `keystore.properties`는 잃어버리면 앱 업데이트가 어려워질 수 있으니 안전한 곳에 백업한다.

## Data safety 권장 입력

밥크로스는 자체 서버, 광고 SDK, 분석 SDK를 사용하지 않는다.

- Data collection: No user data collected
- Data sharing: No user data shared with third parties
- Data encrypted in transit: 해당 없음 또는 No server transfer
- Users can request data deletion: 앱 삭제 또는 앱 데이터 삭제로 로컬 데이터 삭제 가능

주의: 밥닉네임, 아바타, 메뉴 후보, 선택 내용, 결과는 Nearby Connections로 주변 참여 기기에 전달될 수 있다. 이는 앱 기능 수행을 위한 근거리 기기 간 공유이며, 밥크로스 서버 수집은 아니다. 개인정보처리방침에도 같은 취지로 고지되어 있다.

## 권한 설명

앱이 요청하는 권한은 주변 기기 검색과 연결을 위한 것이다.

- Bluetooth 계열 권한
- Wi-Fi / Nearby 기기 권한
- 위치 관련 권한

Play Console 심사 설명에는 다음 취지로 적는다.

> 밥크로스는 서버 없이 가까운 Android 기기끼리 메뉴 후보와 선택 결과를 교환하는 앱입니다. Bluetooth, Wi-Fi, Nearby 기기, 위치 관련 권한은 Nearby Connections를 통해 주변 참여 기기를 찾고 연결하기 위해서만 사용하며, 위치 기록을 수집하거나 저장하지 않습니다.

## 출시 전 체크

- 두 기기 실사용 테스트에서 참여 가능 인원 카운트가 양쪽 모두 정상인지 확인
- 밥판 생성, 초대 수락, 양쪽 메뉴 선택, 조기 종료, 결과 공유 확인
- 개인정보처리방침 URL이 로그인 없이 공개 접근 가능한지 확인
- Play Console 앱 콘텐츠 항목 완료
- 콘텐츠 등급 설문 완료
- 타겟 연령층 및 데이터 보안 섹션 완료
