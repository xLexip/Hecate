<div align="center">

## Adaptive Theme – Auto Dark Mode

[![Latest Version](https://img.shields.io/github/v/release/xLexip/Adaptive-Theme?style=flat&logo=github&label=Release)](https://github.com/xLexip/Adaptive-Theme/releases)
[![Stars](https://img.shields.io/github/stars/xLexip/Adaptive-Theme?style=flat&logo=github&label=Stars)]()
<a href="https://play.google.com/store/apps/details?id=dev.lexip.hecate">
<img src="https://img.shields.io/badge/Downloads-17,000+-brightgreen?logo=google-play&logoColor=white" alt="Play Store Download Count 17,000+">
</a>

Adaptive Theme uses your device’s **ambient light sensor** to intelligently switch between light and **dark theme**, optimizing readability and eye comfort for your current surroundings. 
It can also update your **wallpaper** to match the active theme.

<br>

<a href="https://play.google.com/store/apps/details?id=dev.lexip.hecate&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme_button">
    <img src=".github/resources/get-it-on-google-play.svg" alt="Get Adaptive Theme on Google Play" width="180"/>
</a>
&nbsp;&nbsp;
<a href="https://github.com/xLexip/Adaptive-Theme/releases">
    <img src=".github/resources/github-releases.png" alt="Get Adaptive Theme as .APK on GitHub Releases" width="180"/>
</a>
&nbsp;&nbsp;
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22dev.lexip.hecate%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FxLexip%2FAdaptive-Theme%22%2C%22author%22%3A%22xLexip%22%2C%22name%22%3A%22Adaptive%20Theme%22%7D">
    <img src="https://raw.githubusercontent.com/xLexip/Adaptive-Theme/develop/.github/resources/get-it-on-obtainium.svg" alt="Get Adaptive Theme on Obtainium" width="180">
</a>
&nbsp;&nbsp;
<a href="#">
    <img src=".github/resources/works-with-shizuku.png" alt="Works with Shizuku" width="180"/>
</a>
&nbsp;&nbsp;
</div>

---

### Featured On

> **[Android Authority](https://www.androidauthority.com/automatic-dark-mode-android-adaptive-theme-3650081/): "A brilliant app that once installed
makes you wonder how you lived without it. [...] This app gives Android the automatic dark mode feature it desperately needs."** – Andy Walker

> **[HowToMen (YouTube)](https://www.youtube.com/watch?v=iY3FBMTA15A&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&index=3&t=98s&ref=GitHub_xLexip): "No
tapping, no schedules needed. It just does it on its own. [...] And don't worry, it's not killing your battery either."** – Facundo Holzmeister

> **[Computerworld](https://www.computerworld.com/article/4154561/android-dark-mode-upgrade.html): "This is how Android's dark mode should have worked
since the start. Ready for an overdue injection of extra intelligence?"** – JR Raphael

> **[How-To Geek](https://www.howtogeek.com/i-ditched-sunrisesunset-dark-mode-for-this-android-app-it-uses-your-light-sensor/): "With a simple app,
you can make dark mode (and light mode) switch with the lighting around you."** – Joe Fedewa

---

### Setup

1. **Install** Adaptive Theme from Google Play or GitHub Releases.
2. **Grant the permission** using the [web setup tool](https://lexip.dev/setup), or [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api), or Root, or manual ADB.
3. **Set your lux threshold** or pick a preset, and you're done.

> [!TIP]
> If you don't use Shizuku, the [web setup tool](https://lexip.dev/setup) is the easiest option – no ADB, no Shizuku, just a browser. The app guides you through a step-by-step wizard.

> [!IMPORTANT]
> The required permission `WRITE_SECURE_SETTINGS` does **not** grant root access or read any user data. It only allows flipping system settings like the system dark mode and is fully reversible by uninstalling the app.

> [!NOTE]
> If you choose Shizuku: It is **only required once** to grant the permission during setup. The permission persists across reboots, so Shizuku does not need to remain running in the background for Adaptive Theme to function.

---

### Features

- **Sensor-driven switching**: Uses the physical ambient light sensor – not a clock or sunset schedule – to intelligently switch the system theme.
- **Custom lux threshold**: Dial in exactly when the theme flips, or choose a preset (e.g.indoor, outdoor, sunlight).
- **Night lock**: Optionally hold dark mode during a fixed time window, e.g. 9 PM – 6 AM.
- **Wallpaper Theme Sync**: Automatically swap your home and lock screen wallpaper when the system theme switches between light and dark mode.
- **Battery friendly**: The sensor is only checked once when you turn the screen on. Zero background drain.
- **50+ languages**: Fully localized for a global audience.
- **Shizuku support**: Includes native Shizuku integration as one of several setup options.
- **Material You design**: Dynamic UI that adapts to your system theme and colors.
- **Quick Settings Tile**: Toggle the service directly from your quick settings.
- **Free, open-source, no ads**

---

### How It Works

To avoid screen flicker and unnecessary background work, Adaptive Theme follows a strict, event-driven model:

- **Screen-on trigger** — The light sensor is only sampled right after the screen turns on, not continuously.
- **Obstruction check** — Validates that the sensor is not covered (e.g. by a hand or pocket).
- **Instant apply** — The theme is switched before you start interacting.

This design ensures mid-session theme changes never interrupt your use, since some apps handle live theme changes poorly.

---

### Support the Project

Adaptive Theme is completely free, ad-free, and developed in my spare time. If you find it useful, consider to:

- **Star this repository** at the top to help others find it.
- **Leave a rating on [**Google Play**](https://play.google.com/store/apps/details?id=dev.lexip.hecate)** — it makes a real difference in discoverability.
- **Share the app** with anyone who might benefit.
- **Follow me** [**@xLexip**](https://github.com/xLexip) if you like.
- **Buy me a coffee** if you're feeling [generous](https://buymeacoffee.com/lexip).

Issues, questions, and feature ideas are welcome — please [open an issue](https://github.com/xLexip/Adaptive-Theme/issues/new) or use the in-app feedback option.

---

### Architecture

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack-Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=Jetpack%20Compose&logoColor=white)](#)
[![Material-Design](https://img.shields.io/badge/material%20design-757575?style=for-the-badge&logo=material%20design&logoColor=white)](#)
[![](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)](https://lexip.dev/rr)
[![SonarQube](https://img.shields.io/badge/Sonarqube-5190cf?style=for-the-badge&logoColor=white&logo=sonarr)](#)

* **Modern UI:** Written in Kotlin using Jetpack Compose and Material 3 (Material You).
* **Architecture:** Follows the MVVM pattern with a Single-Activity architecture.
* **Reactive Data:** ViewModels expose data via Kotlin Flows and manage concurrency with Coroutines.
* **Persistence:** Type-safe settings storage utilizing Jetpack DataStore.
* **Background Work:** Sensor operations run event-driven – only upon screen-on
  broadcasts – ensuring zero unnecessary battery drain in the background.
* **WebADB Setup Website:** Simple browser-based setup tool for permission granting using WebADB ([source code](https://github.com/xLexip/Adaptive-Theme-Setup)).

---

### FAQ

**What is the minimum Android version?**
Adaptive Theme works on Android 14 and above.

**Why didn't the theme change right away?**
By design, the theme only switches immediately after the screen turns on. This prevents flicker, saves battery, and avoids interrupting an active app session.

**How does Wallpaper Theme Sync work?**
Choose a light and a dark wallpaper in the app. When Adaptive Theme changes the system theme, it replaces both the home screen and lock screen wallpapers with the matching image. The change can take a moment and may briefly affect performance, especially with large images.

**Does it work on tablets?**
No. Due to a technical limitation, Adaptive Theme is currently limited to smartphones.

**Does this require root?**
No. It works on stock devices. Root is supported as an optional setup method.

**How can I grant the required permission manually with ADB?**
Connect your device with USB debugging enabled, then run:

```shell
adb shell pm grant dev.lexip.hecate android.permission.WRITE_SECURE_SETTINGS
```

**Does it work with custom skins (MIUI, OneUI, etc.)?**
In most cases, yes. Any system that respects the native Android dark mode implementation is supported.

**Does it support Shizuku forks?**
Yes, forks like Nightzuku, Shizuku+, and Shevery are supported.

**Does Shizuku need to be running all the time or after a reboot?**
No. Shizuku is only required once to grant the permission. Once granted, the permission persists permanently across reboots, so Shizuku does not need to stay active for the app to function.

---

### References

**International Press**

- androidauthority.com — [**This app gives Android the automatic dark mode feature it desperately needs**](https://www.androidauthority.com/automatic-dark-mode-android-adaptive-theme-3650081/)
- computerworld.com — [**The Android dark mode upgrade you deserve**](https://www.computerworld.com/article/4154561/android-dark-mode-upgrade.html)
- howtogeek.com — [**I ditched sunrise/sunset dark mode for this Android app (it uses your light sensor)**](https://www.howtogeek.com/i-ditched-sunrisesunset-dark-mode-for-this-android-app-it-uses-your-light-sensor/)
- heise.de – [**Android-Dunkelmodus: Open-Source-App passt Systemdesign an Umgebungslicht an**](https://www.heise.de/news/Android-Dunkelmodus-Open-Source-App-passt-Systemdesign-an-Umgebungslicht-an-11282658.html)
- tchgdns.de — [**Adaptive Theme für Android: Dunkelmodus automatisch per Umgebungslicht aktivieren**](https://tchgdns.de/adaptive-theme-fuer-android-dunkelmodus-automatisch-per-umgebungslicht-aktivieren/)
- androidauthority.com — [**5 of the best new Android apps you need to try this April**](https://www.androidauthority.com/best-new-android-apps-games-april-2026-3653008/)
- androidauthority.com — [**10 awesome Shizuku apps I use to level up my Android experience**](https://www.androidauthority.com/best-shizuku-apps-android-3659353/)
- computerworld.com — [**The Android dark mode power-pack: 5 secrets for a smarter screen setup**](https://www.computerworld.com/article/4187935/android-dark-mode-power-pack.html)
- droidwin.com — [**Switch Dark and Light Mode Based on Surrounding Light Level**](https://droidwin.com/switch-dark-and-light-mode-based-on-surrounding-light-level/)
- androidinsider.ru — [**Автоматическая тёмная тема. Приложение, которого не хватало годами**](https://androidinsider.ru/obzory-prilozhenij/avtomaticheskaya-tyomnaya-tema-na-android-prilozhenie-kotorogo-ne-hvatalo-godami.html)
- pcguia.pt — [**App do Dia – Adaptive Theme: Modo Escuro**](https://www.pcguia.pt/2026/04/app-do-dia-adaptive-theme-modo-escuro/)
- android-zone.fr — [**Adaptive Theme Android: mode clair/sombre automatique**](https://www.android-zone.fr/adaptive-theme-android-mode-clair-sombre-automatique/)

**YouTube Videos**

- HowToMen — [**Top 15 Best Android Apps, February 2026**](https://www.youtube.com/watch?v=iY3FBMTA15A&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&index=3&t=98s)
- HowToMen — [**Top 15 Best Shizuku Apps to Use in 2026**](https://www.youtube.com/watch?v=mDQ8o4JlXjM&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&index=3&t=735s)
- Mr. Android FHD — [**8 INCREDIBLE Apps That Every Android User Needs in 2026**](https://www.youtube.com/watch?v=CH_4E1LzGcU&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&t=459s)
- TechTab — [**Top 10 Android Apps you need to try, March 2026**](https://www.youtube.com/watch?v=nSFYlenb_-U&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&t=298s)
- Gadget Geek — [**Top 10 Best Android Apps, March 2026**](https://www.youtube.com/watch?v=8zQmriP8wSg&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&t=306s)
- TechReviewBD — [**5 INSANE Android Apps That Will Change The Way You Use Your Phone**](https://youtu.be/9T895TReCcU?list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&si=HczGvueXQWgIU9aT&t=146)
- Tech Tricks — [**10 Best New Top Rated Android Apps**](https://www.youtube.com/watch?v=Ti4Pt6hNZzc&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&t=257s)
- El Androide Feliz (ES) — [**15 nuevas apps para Shizuku que son bestiales**](https://www.youtube.com/watch?v=eMznsQhldEw&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh&t=152s)
- Всё про Андроид (RU) — [**Светлая и тёмная тема по датчику освещённости**](https://www.youtube.com/watch?v=Oj-WHpc5vK8&list=PLMrRwQM3vue8Y3WFVgP5UkYPj_41ekXJh)

**Acknowledgements**

- [AlbertCaro](https://github.com/xLexip/Adaptive-Theme/pull/107) — Spanish translation strings
- [Nunito font](https://github.com/googlefonts/nunito) — SIL Open Font License 1.1, Copyright 2014 The Nunito Project Authors

---

<div align="center">
<b> Made with 🥨 in Germany </b>
</div>
