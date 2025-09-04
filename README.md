# Kotlin Multiplatform 프로젝트

이것은 Android, Desktop (JVM)을 대상으로 하는 Kotlin Multiplatform 프로젝트입니다.

* [/composeApp](./composeApp/src)은 Compose Multiplatform 애플리케이션에서 공유될 코드를 위한 폴더입니다.
  여러 하위 폴더를 포함하고 있습니다:
- [commonMain](./composeApp/src/commonMain/kotlin)은 모든 타겟에서 공통으로 사용되는 코드를 위한 폴더입니다.
- 다른 폴더들은 폴더명에 표시된 플랫폼에서만 컴파일될 Kotlin 코드를 위한 폴더입니다.
  예를 들어, Kotlin 앱의 iOS 부분에서 Apple의 CoreCrypto를 사용하고 싶다면,
  [iosMain](./composeApp/src/iosMain/kotlin) 폴더가 그러한 호출을 위한 적절한 장소입니다.
  마찬가지로, Desktop (JVM) 특정 부분을 편집하고 싶다면, [jvmMain](./composeApp/src/jvmMain/kotlin)
  폴더가 적절한 위치입니다.

### Android 애플리케이션 빌드 및 실행

Android 앱의 개발 버전을 빌드하고 실행하려면, IDE 툴바의 실행 위젯에서 실행 구성을 사용하거나
터미널에서 직접 빌드하세요:
- macOS/Linux에서
```shell
./gradlew :composeApp:assembleDebug
```
- Windows에서
```shell
.\gradlew.bat :composeApp:assembleDebug
```

### Desktop (JVM) 애플리케이션 빌드 및 실행

데스크톱 앱의 개발 버전을 빌드하고 실행하려면, IDE 툴바의 실행 위젯에서 실행 구성을 사용하거나
터미널에서 직접 실행하세요:
- macOS/Linux에서
```shell
./gradlew :composeApp:run
```
- Windows에서
```shell
.\gradlew.bat :composeApp:run
```

---

[Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)에 대해 더 알아보세요…