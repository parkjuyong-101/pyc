# 철근가공물량 산출 Android 출시 안내

## 1. 현재 준비 상태

- Android 프로젝트: `android-app/`
- 패키지명: `com.parkjuyong.rebarcalc`
- 앱 이름: `철근 물량 산출`
- 최소 지원: Android 8.0(API 26)
- 목표 API: Android 16(API 36)
- 앱 버전: `1.0.0` (`versionCode 1`)
- 네트워크 권한: 없음
- 사용자 계정/로그인: 없음
- 데이터 저장: 기기 내부 자동 저장 + 사용자가 선택한 `.rebar` 파일
- PC에서 만든 `.rebar`, `.rebar.json`, `.json` 프로젝트 가져오기 지원

2026년 8월 31일부터 Google Play 신규 앱과 업데이트는 Android 16(API 36) 이상을 대상으로 해야 한다. 이 프로젝트는 API 36으로 설정했다.

공식 기준: https://support.google.com/googleplay/android-developer/answer/11926878

## 2. 앱 빌드

### Android Studio에서 테스트 APK 만들기

1. Android Studio에서 `C:\Users\user\Desktop\pyc\android-app` 폴더를 연다.
2. SDK 설치 안내가 나오면 Android SDK Platform 36과 Build-Tools 36.x를 설치한다.
3. Gradle 동기화가 완료될 때까지 기다린다.
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)`를 누른다.
5. 결과 파일은 보통 `android-app/app/build/outputs/apk/debug/app-debug.apk`에 생성된다.

### Play Store용 AAB 만들기

1. `Build > Generate Signed Bundle / APK` 선택
2. `Android App Bundle` 선택
3. 처음이면 `Create new`로 업로드 키 생성
4. 키 파일과 비밀번호는 별도 USB/암호관리자에 백업
5. `release` 선택 후 생성
6. 결과 파일: `android-app/app/build/outputs/bundle/release/app-release.aab`

업로드 키를 잃으면 업데이트 출시가 복잡해지므로 GitHub에는 절대 올리지 않는다.

공식 빌드/서명 안내: https://developer.android.com/build/build-for-release

## 3. Google Play 개발자 계정

1. https://play.google.com/console 접속
2. 개인 또는 조직 계정 선택
3. 법적 이름, 주소, 이메일, 전화번호 및 결제 프로필 등록
4. 새 개인 계정은 Play Console 모바일 앱에서 실제 Android 기기 보유 인증 진행

조직 계정은 D-U-N-S 번호가 필요할 수 있다.

공식 계정 유형: https://support.google.com/googleplay/android-developer/answer/13634885

공식 기기 인증: https://support.google.com/googleplay/android-developer/answer/14316361

## 4. Play Console 앱 생성

1. `모든 앱 > 앱 만들기`
2. 앱 이름: `철근 물량 산출`
3. 기본 언어: 한국어
4. 앱 또는 게임: 앱
5. 무료 또는 유료: 무료 권장
6. 연락 이메일 입력
7. 선언 사항 동의 후 생성

Google Play는 신규 앱에 Android App Bundle(AAB)을 사용한다.

공식 안내: https://support.google.com/googleplay/android-developer/answer/9859152

## 5. 앱 콘텐츠 설정

- 개인정보처리방침 URL: `PRIVACY_POLICY.md` 내용을 공개 웹페이지로 게시한 주소
- 광고: 광고 없음
- 앱 액세스: 모든 기능에 로그인 불필요
- 타깃층: 성인 현장 실무자, 아동 대상 아님
- 뉴스 앱: 아님
- 건강 앱: 아님
- 금융 기능: 없음
- 정부 앱: 아님
- 데이터 보안: 개발자 서버로 수집하거나 공유하는 데이터 없음

개인정보처리방침은 PDF가 아닌 누구나 접근 가능한 공개 URL이어야 하며 앱 안에서도 확인할 수 있어야 한다.

공식 사용자 데이터 정책: https://support.google.com/googleplay/android-developer/answer/10144311

공식 데이터 보안 양식: https://support.google.com/googleplay/android-developer/answer/10787469

## 6. 스토어 등록정보

`android-app/store-assets/STORE_LISTING_KO.md`의 문구를 사용한다.

필수 준비물:

- 앱 아이콘 PNG 512×512
- 휴대전화 스크린샷 최소 2장 권장
- 그래픽 이미지 1024×500
- 개인정보처리방침 공개 URL
- 지원 이메일

스크린샷은 실제 Android 기기에서 다음 화면을 포함한다.

1. 프로젝트/전표 선택 화면
2. 철근 형상 선택 화면
3. 길이와 각도 입력 화면
4. 물량과 중량 합계 화면
5. 저장/불러오기 화면

## 7. 테스트 후 공개 출시

1. 내부 테스트에 AAB 업로드
2. 본인 휴대폰에서 설치 및 `MOBILE_TEST_CHECKLIST.md` 수행
3. 새 개인 개발자 계정이면 비공개 테스트 진행
4. 최소 12명의 테스터가 14일 연속 참여 상태를 유지
5. 테스트 결과와 수정 내용을 정리해 프로덕션 액세스 신청
6. 승인 후 프로덕션 트랙에 출시

공식 테스트 조건: https://support.google.com/googleplay/android-developer/answer/14151465

## 8. 업데이트 규칙

업데이트할 때마다 `android-app/app/build.gradle`에서 두 값을 올린다.

```gradle
versionCode 2
versionName '1.0.1'
```

- `versionCode`: Play에 올릴 때마다 반드시 증가
- `versionName`: 사용자에게 보이는 버전
- PC `index.html` 변경 후 Android 자산 파일도 동기화해야 함

```powershell
Copy-Item index.html android-app\app\src\main\assets\index.html -Force
```

## 9. 출시 전에 사용자가 직접 해야 하는 일

- Google Play 개발자 계정 생성 및 본인인증
- 공개 개인정보처리방침 URL 준비
- 업로드 키 생성 및 안전한 백업
- 실제 Android 휴대폰 테스트
- 새 개인 계정인 경우 12명/14일 비공개 테스트
- Play Console의 정책 질문에 사실대로 답변

