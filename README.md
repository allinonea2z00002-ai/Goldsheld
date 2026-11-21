# Goldsheld

UI-only Android project: the Premium Dark and Premium White look of the app, five tabs and the four
protection screens. **No real blocking logic is included** — power-menu block, app/feature block,
site/keyword block, location lock and the like are meant to be added separately. Buttons that would
change protection show a short "UI sample" message, and all numbers are sample data.

- Package / application id: `com.goldsheld.app` (debug builds add `.debug`)
- Kotlin, XML views, Material 3, `minSdk 26`, `compileSdk/targetSdk 36`
- Text is Bangla only (strings in `res/values/strings.xml`; sample data in the Kotlin files)

## Screens
| Tab | What it shows |
|---|---|
| হোম | Time ring, unlock conditions (ANY/ALL), warning banner, protection status list, emergency / urge buttons |
| সুরক্ষা | Sub tabs: লক সেটআপ · সুরক্ষা সুইচ · অ্যাপ ও ফিচার · সাইট ও কীওয়ার্ড |
| পরিসংখ্যান | Sample statistic cards and a 7-day bar chart |
| লোকেশন | Saved zones |
| সেটিংস | **Working** theme switch (System / White / Dark), placeholder rows |

## Theme
`res/values/colors.xml` = Premium White, `res/values-night/colors.xml` = Premium Dark; the theme
`Theme.Goldsheld` is a Material 3 DayNight theme using those names, so every screen follows. The choice
is stored in `ThemePrefs` (default: Dark) and applied in `GoldsheldApp`. To change a colour edit the
same name in both files.

## Build
```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
```
Copy `ci/build-apk.yml` to `.github/workflows/build-apk.yml` to build on GitHub Actions.

> **Status:** written without an Android SDK, so it has **not been compiled yet**. Expect to fix a few
> compile errors on the first build. The font is the system font (Hind Siliguri was used in the design
> mock-ups; add it under `res/font` if you want it).
