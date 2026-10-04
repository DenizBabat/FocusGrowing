# Gizlilik politikasını yayınlama ve Play Console

Bu klasörde:
- `index.html`: yayınlanacak sayfa (İngilizce + Türkçe, dil düğmeli; Türkçe tarayıcıda otomatik Türkçe açılır, `#tr` / `#en` ile link verilebilir)
- `privacy-policy.en.md`, `privacy-policy.tr.md`: metnin kaynağı. Değişiklik yaptıktan sonra sayfayı yeniden üretmek için Claude'a "politikayı güncelle" demen yeterli.

## 1. GitHub Pages ile yayınlama (ücretsiz, ~5 dakika)

1. github.com'da hesabın yoksa oluştur.
2. **New repository** → ad: `focusgrowing-privacy` → **Public** → *Create repository*.
3. *Add file → Upload files* → bu klasördeki **`index.html`** dosyasını yükle → *Commit changes*.
4. Repo'da **Settings → Pages** → *Source: Deploy from a branch* → Branch: `main`, klasör: `/ (root)` → *Save*.
5. 1–2 dakika sonra sayfa şu adreste açılır: `https://KULLANICI_ADIN.github.io/focusgrowing-privacy/`
6. Adresi tarayıcıda (gizli sekmede) açıp kontrol et.
7. `app/src/main/java/com/focusgrowing/app/core/utility/AppConfig.kt` içindeki `GITHUB_USERNAME` kısmını kendi kullanıcı adınla değiştir.
8. Play Console → *Policy and programs → App content → Privacy policy* alanına aynı adresi yapıştır.

## 2. Play Console "Data safety" formu (politikayla uyumlu cevaplar)

> AdMob ve Firebase eklendikten sonra geçerlidir. Google'ın güncel tablolarıyla son bir kez karşılaştır:
> [AdMob veri açıklaması](https://developers.google.com/admob/android/privacy/play-data-disclosure) ·
> [Firebase veri açıklaması](https://firebase.google.com/docs/android/play-data-disclosure)

- **Uygulama kullanıcı verisi topluyor veya paylaşıyor mu?** Evet
- **Tüm veriler aktarımda şifreleniyor mu?** Evet
- **Kullanıcılar verilerinin silinmesini isteyebilir mi?** Evet (e-posta ile; hesap sistemi olmadığı için "hesap silme URL'si" gerekmez)

| Veri türü | Toplanan | Paylaşılan | Amaç | Kaynak |
|---|---|---|---|---|
| Konum → Yaklaşık konum (IP'den) | ✔ | ✔ | Reklam, Analitik, Dolandırıcılığın önlenmesi | AdMob, Analytics |
| Uygulama etkinliği → Uygulama etkileşimleri | ✔ | ✔ | Reklam, Analitik | AdMob, Analytics |
| Uygulama bilgisi ve performansı → Kilitlenme günlükleri | ✔ | | Analitik (uygulama işlevselliği) | Crashlytics |
| Uygulama bilgisi ve performansı → Teşhis | ✔ | ✔ | Analitik, Reklam | AdMob, Crashlytics |
| Cihaz veya diğer kimlikler | ✔ | ✔ | Reklam, Analitik, Dolandırıcılığın önlenmesi | AdMob, Analytics, Crashlytics |

İşaretleme: bu verilerin hepsi **otomatik toplanır, isteğe bağlı değildir**. İstisna: kişiselleştirilmiş reklam, onaya bağlıdır.
**Toplanmayan veriler:** ad, e-posta, fotoğraflar, dosyalar, mesajlar, kişiler, hassas konum. Görevler ve fotoğraflar cihazdan çıkmaz. Google'ın tanımına göre cihazdan çıkmayan veriler "toplanan" sayılmaz.

Diğer App content cevapları:
- **Ads:** Uygulama reklam içeriyor → Evet
- **Advertising ID:** Evet → amaçlar: Reklam veya pazarlama, Analitik
- **Target audience:** 13+ (politikada 13 yaş altına yönelik olmadığı yazıyor). 18+ seçersen Aileler politikası ve çocuk reklam kuralları devreye girmez.
- **Financial features / Health / News:** Hayır

## 3. Politikanın verdiği sözler: yayından önce kodda olmalı

Politika bunları vaat ediyor. SDK'ları eklerken bunları da eklemeliyiz (istersen hepsini ben yazarım):

1. **Ayarlar → Gizlilik** bölümü:
   - "Reklam gizlilik seçenekleri" düğmesi (Google UMP *privacy options form*; AB/AEA için zorunlu),
   - "Kullanım ve çökme verilerini paylaş" anahtarı (`setAnalyticsCollectionEnabled` + `setCrashlyticsCollectionEnabled`),
   - uygulama örneği kimliğinin (`FirebaseAnalytics.getAppInstanceId`) gösterilmesi ve kopyalanabilmesi.
2. Uygulama açılışında reklam yüklemeden önce **UMP onay formu** ve Analytics için **Consent Mode v2**.
3. Analytics olaylarına ve Crashlytics özel anahtarlarına **görev başlığı, not veya isim yazılmayacak** (yalnızca süre, sayı, ekran adı gibi teknik veriler).
4. Firebase / Google Analytics konsolunda **veri saklama süresi ≤ 14 ay**.
5. AdMob'da reklam içerik derecesi (ör. en fazla PG), uygulama "çocuklara yönelik değil" olarak işaretlenecek.
6. Derlemeden sonra birleştirilmiş manifestte `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID` izinlerinin olduğunu kontrol et. Politikadaki izin tablosu bunlarla uyumlu.

İlk sürümde bu hizmetlerden birini **eklemeyeceksen**, politikadan ilgili bölümü çıkarmak gerekir. Politika, uygulamanın gerçekte yaptığıyla aynı olmalı.

## 4. Değişiklik yaparken

- Metinleri (`.md`) güncelle, sayfayı yeniden üret, `index.html`'i GitHub'a tekrar yükle.
- Metnin başındaki yürürlük tarihini değiştir. Önemli değişiklikleri uygulama içinde de duyur.

> Not: Bu metin genel bir şablon değil, uygulamanın koduna göre hazırlandı. Yine de hukuki danışmanlık yerine geçmez. Özellikle KVKK yurt dışı aktarım yükümlülükleri (standart sözleşme bildirimi vb.) için bir avukata danışman önerilir.
