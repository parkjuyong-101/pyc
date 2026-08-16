# 안드로이드 배포 파일

- `철근가공물량-1.0.0-debug.apk`: 안드로이드 휴대폰에 직접 설치하여 기능을 시험하는 파일입니다.
- `철근가공물량-1.0.0-unsigned.aab`: Play Console 업로드 전 서명이 필요한 릴리스 번들입니다.

APK를 휴대폰으로 옮겨 설치하면 현재 버전을 시험할 수 있습니다. Play 스토어 등록용 AAB는 `ANDROID_RELEASE.md`의 **업로드 키와 서명** 절차를 완료한 뒤 새로 생성해야 합니다. 업로드 키 비밀번호는 저장소나 메신저에 올리지 마세요.

빌드 검증 환경: Android API 36, JDK 17, Android Gradle Plugin 9.3.0, Gradle 9.5.1.
