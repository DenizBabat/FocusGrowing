# Focus Growing — Mission · World · Intelligence

Android (Kotlin + Jetpack Compose + Material 3) odak / Pomodoro uygulaması.
`pomodro.md` spesifikasyonundaki V1 kapsamı ve paylaşılan ekran tasarımlarına göre yazıldı.

> ⚠️ Bu proje, Android SDK'ya erişimi olmayan bir ortamda yazıldı. **Domain katmanı (iş kuralları +
> use case'ler) Kotlin 2.1.21 ile derlendi ve 25 birim testi geçti**, ancak Android/Compose katmanı
> henüz derlenmedi. İlk açılışta Android Studio'da bir derleme hatası çıkarsa hata mesajını
> Claude'a yapıştırman yeterli.

---

## 1. Çalıştırma

1. **Android Studio** (Narwhal 2025.1 veya daha yeni) kur. JDK 17 gömülü gelir.
2. `File → Open` ile bu klasörü (`FocusGrowing`) aç, Gradle sync'in bitmesini bekle.
3. Bir emülatör ya da telefon seç, **Run ▶**.
4. Testler: `./gradlew test` (Windows: `gradlew.bat test`).

| Ayar | Değer |
|---|---|
| minSdk | 26 (Android 8.0) |
| target / compileSdk | 36 (Android 16 — Google Play'in Ağustos 2026 sonrası zorunlu hedefi) |
| Kotlin / AGP / Gradle | 2.1.21 / 8.11.1 / 8.14.3 |
| DI | Hilt · Veritabanı: Room · Ayarlar: DataStore · Görsel: Coil |

---

## 2. Tema sistemi (renk paletini değiştirmek)

Ekranlarda **hiçbir sabit renk yok**. Tüm renkler rol bazlı token'lardan gelir:

```
core/designsystem/theme/
├── FocusColors.kt   ← token tanımları (primary, xp, streak, progressTrack, illustration.*)
├── Palettes.kt      ← paletler (Mint Meadow, Ocean, Sunset, Lavender) — renkleri BURADA değiştir
├── Type.kt          ← yazı tipi ve metin stilleri (tek satırla font değişir)
├── Shape.kt         ← köşe yuvarlaklıkları, boşluklar (spacing), boyutlar
└── Theme.kt         ← FocusTheme(...) + FocusTheme.colors / .spacing / .typography erişimleri
```

**Hızlı renk değişimi:** `Palettes.kt` içindeki `MintSwatch` hex değerlerini değiştir → tüm uygulama
(butonlar, grafikler, dünya çizimleri dahil) yeni renklere geçer.

**Yeni palet eklemek:**
```kotlin
private val forestLight = mintLight.copy(primary = Color(0xFF2E7D5B), primaryGradientStart = ..., ...)
private val forestDark  = mintDark.copy(primary = Color(0xFF6FD3A6), ...)

object FocusPalettes {
    val Forest = FocusPalette("forest", "Forest", forestLight, forestDark, isPremium = false)
    val all = listOf(MintMeadow, Forest, Ocean, Sunset, Lavender)
}
```
Yeni palet otomatik olarak **Ayarlar → Görünüm → Color palette** listesinde çıkar.
Varsayılanı değiştirmek için `FocusPalettes.Default`'u güncelle.

**Font:** `res/font/` klasörüne `.ttf` dosyalarını koy, `Type.kt` içindeki `AppFontFamily`'yi değiştir.

**Kural:** Ekranlarda `Color(0xFF...)` yazılmaz; `FocusTheme.colors.xxx` kullanılır. Hazır
arka plan sahneleri (gün batımı vb.) içerik olduğu için kendi renklerini `Scenes.kt`'de tutar.

Kullanıcı tarafında: Açık/Koyu/Sistem tema, Android 12+ dinamik renk, yazı boyutu ve animasyon
azaltma seçenekleri Ayarlar'da.

---

## 3. Mimari

```
app/src/main/java/com/focusgrowing/app
├── domain        ← saf Kotlin: modeller, repository arayüzleri, use case'ler, kurallar
│   ├── logic     ← WorldProgression, StreakCalculator, TimerCalculator, StatisticsCalculator, InsightEngine
│   └── usecase   ← CompleteFocusSession, SaveMission, Observe* ...
├── data          ← Room (entity/dao), DataStore, repository implementasyonları
├── core
│   ├── designsystem  ← tema, ortak bileşenler, vektör çizimler (dünya adası, sahneler)
│   ├── timer         ← FocusTimerManager (timestamp tabanlı), alarm, receiver'lar
│   ├── notification  ← bildirim kanalları
│   ├── image         ← arka plan gösterimi (crop, aspect ratio, downsampling), URI izinleri
│   └── premium       ← PremiumManager / MockPurchaseManager
├── di            ← Hilt modülleri
└── presentation  ← ekranlar (Compose) + ViewModel'ler
```

Akış: `Composable → ViewModel → UseCase → Repository → Room/DataStore`. Composable'lar sadece
state çizer ve event gönderir.

**Timer:** Kalan süre her zaman `endAt - now` ile hesaplanır. Durum her değişimde DataStore'a
yazılır; ekran döndürme, arka plan ve process ölümü süreyi bozmaz. Seans bitişinde `AlarmManager`
uygulamayı uyandırır (foreground service gerekmez → Play politikası açısından sade). Uygulama
öne geldiğinde gecikmiş seanslar otomatik tamamlanır.

---

## 4. Ekranlar ↔ dosyalar

| Tasarım | Dosya |
|---|---|
| Splash + 3 onboarding + süre + ilk hedef | `presentation/onboarding/OnboardingScreen.kt` |
| Home / World | `presentation/home/HomeScreen.kt` |
| Missions, Mission Detail, yeni/düzenle | `presentation/mission/*` |
| Focus / Pomodoro | `presentation/focus/FocusScreen.kt` |
| Great Job, Mission Completed, Streak, Level Up, New Mission | `presentation/celebration/CelebrationScreen.kt` |
| Statistics / Intelligence | `presentation/statistics/*` |
| Profile, Settings | `presentation/profile/*`, `presentation/settings/*` |
| Background Gallery + konum/zoom ayarı | `presentation/background/*` |
| Premium | `presentation/premium/*` |
| Notifications (uygulama içi kutu) | `presentation/notifications/*` |
| Sistem bildirimi (kilit ekranı) | `core/notification/FocusNotifier.kt` |

Tasarımdan bilinçli farklar: V1'de hesap sistemi olmadığı için Profil'deki "Account" ve "Log Out"
yerine isim düzenleme ve Gizlilik Politikası var; reklam olmadığı için Premium'daki "Ad-free" satırı yok.
Tasarımdaki suluboya görseller yerine vektör çizimler kullanıldı (APK küçük kalır, her ekran
boyutunda keskin, tema renklerine uyar). Kendi görsellerini koymak istersen `res/drawable`'a ekleyip
ilgili illüstrasyon bileşenini `Image(painterResource(...))` ile değiştirebilirsin.

---

## 5. Free / Premium

Tüm kontroller `PremiumManager.hasAccess(PremiumFeature.X)` üzerinden yapılır.

- **Free:** timer, süre ayarı, görevler, dünya, 3 kişisel arka plan (galeri/kamera/dosya), 10 hazır sahne, günlük/haftalık istatistik, temel içgörüler.
- **Premium:** sınırsız arka plan, zoom/gelişmiş crop, bulanıklık, kendi motivasyon metnin, 30G/3A/1Y istatistikler, en iyi saatler, tahmin doğruluğu, kişisel içgörüler, ek paletler, ek dünya objeleri.

**Satın alma (Google Play Billing 9.1.0) kodu hazır:** aylık/yıllık abonelik, yerel fiyatlar, ücretsiz deneme,
imza doğrulama, acknowledge, geri yükleme ve abonelik yönetimi. Release derlemesi gerçek Play'i, debug derlemesi
simüle mağazayı kullanır. Play Console'da ürünleri oluşturma ve test adımları: **`docs/billing/BILLING_SETUP.md`**.

---

## 6. Google Play'e yayınlama kontrol listesi

1. **Paket adı:** `app/build.gradle.kts` → `applicationId` ve `namespace` değerini kendine ait yap
   (ör. `com.seninadin.focusgrowing`). Yayından sonra değiştirilemez.
2. **Gizlilik politikası:** `docs/privacy/index.html` hazır (TR + EN). GitHub Pages ile yayınlama adımları
   `docs/privacy/PUBLISHING.md` içinde; yayınlayınca `core/utility/AppConfig.kt` içindeki URL'yi güncelle.
3. **Upload key oluştur** (bir kere):
   ```bash
   keytool -genkeypair -v -keystore upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
   Proje köküne `keystore.properties` oluştur (git'e ekleme — `.gitignore`'da):
   ```
   storeFile=../upload-key.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. **Paket:** `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
   (Android Studio: *Build → Generate Signed App Bundle*.)
5. **Play Console:** Play App Signing'i aç, `.aab`'yi önce *Internal testing*'e yükle.
6. **Data safety formu:** AdMob, Firebase Analytics/Crashlytics eklendiğinde verilecek cevaplar
   `docs/privacy/PUBLISHING.md` içinde. Görevler, istatistikler ve fotoğraflar cihazdan çıkmaz.
7. **İzinler:** `POST_NOTIFICATIONS` (bildirim), `SCHEDULE_EXACT_ALARM` (seans tam zamanında bitsin —
   kullanıcı kapatabilir, Play beyanı gerektirmez), `RECEIVE_BOOT_COMPLETED`. Kamera için izin
   istenmez (sistem kamera uygulaması kullanılır). Foreground service yok.
8. **Mağaza materyali:** uygulama ikonu (512×512), feature graphic (1024×500), en az 2 ekran görüntüsü,
   kısa ve uzun açıklama. Uygulama ikonu şu an basit bir yaprak vektörü (`res/drawable/ic_launcher_foreground.xml`);
   Android Studio → *New → Image Asset* ile kendi ikonunu üretebilirsin.
9. **Sürüm:** her yüklemede `versionCode`'u artır.

---

## 7. Sonraki adımlar (V1 sonrası)

- Metinleri `strings.xml`'e taşıyıp Türkçe çeviri eklemek (arayüz şu an tasarımlar gibi İngilizce).
- Gerçek Google Play Billing entegrasyonu.
- İstatistik dışa aktarma, mevsimsel dünyalar, widget, Wear OS (mimari buna uygun).
- Room şeması `app/schemas/` altına otomatik yazılır; yayından sonra entity değişirse migration ekle.
