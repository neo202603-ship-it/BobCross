# 밥크로스(Bab-Cross)

전설의 화가 이름을 살짝 비튼, 근거리 점심 메뉴 결정 Android 앱입니다.

`Bab` 신호가 블루투스로 교차(`Cross`)하면 주변 밥친구들이 같은 밥판에서 메뉴 후보를 고르고, 결정 순간에는 밥공기 마스코트가 말합니다.

```text
어때요, 메뉴 고르기 참 쉽죠?
```

## 현재 범위

- Nearby Connections 기반 주변 기기 자동 연결
- 밥판 열기, 밥판 초대 수락/거절, 메뉴 후보 선택
- 제한시간 종료 또는 수동 `메뉴 결정` 시 결과 공유
- 결정 완료 팝업과 밥크로스 마스코트 런처 아이콘
- 밥닉네임, 아바타, 자동 연결 설정
- 기본 밥판 템플릿과 사용자 템플릿 저장/삭제
- 결과 기록, 영수증, 결과 해시 검증
- 한 기기용 `혼밥 테스트`와 개발자 진단 화면

## 열기

Android Studio에서 이 폴더를 열고 Gradle sync를 실행합니다.

```text
/Users/neo/Documents/밥크로스(Bab-Cross)
```

터미널 빌드:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

생성 APK:

```text
app/build/outputs/apk/debug/BabCross.apk
```

## 두 기기 테스트

1. 두 Android 기기에 같은 APK를 설치합니다.
2. 두 기기 모두 Bluetooth, Wi-Fi, 위치를 켭니다.
3. 앱 권한 요청을 모두 허용합니다.
4. 하단 접속 배지에 본인을 포함한 참여 가능 인원 `2`가 표시되는지 확인합니다.
5. 한 기기에서 `밥판` 탭의 `밥판 열기`로 메뉴 후보를 보내고, 다른 기기에서 초대를 수락합니다.
6. 메뉴를 고른 뒤 게시자 기기에서 `메뉴 결정`을 누르거나 제한시간 종료를 기다립니다.
7. 양쪽 기기에서 결정 결과와 완료 팝업이 표시되는지 확인합니다.

## 코드 구조

- `MainActivity.kt`: 밥크로스 화면 구성과 사용자 흐름
- `data/NearVoteModels.kt`: 밥판 선택/영수증/결과 모델과 JSON 변환
- `data/NearVoteStore.kt`: 밥닉네임, 영수증, 지난 결과, 템플릿 저장
- `nearby/NearbyVoteConnectionManager.kt`: Nearby Connections 광고, 탐색, 메시지 송수신
- `protocol/NearVoteMessage.kt`: 네트워크 메시지 envelope
