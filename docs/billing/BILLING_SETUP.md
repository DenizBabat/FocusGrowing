# Premium: Google Play kurulumu ve test

Kod hazır (Google Play Billing Library 9.1.0). Çalışması için Play Console'da **üç ürünü**
aşağıdaki kimliklerle **birebir aynı** oluşturman gerekiyor. Kimlikler `core/billing/BillingProducts.kt` içinde.

| Ürün (Product ID) | Tür | Ayrıntı |
|---|---|---|
| `premium_monthly` | Abonelik | Temel plan `monthly`: otomatik yenilenen, 1 ay |
| `premium_yearly` | Abonelik | Temel plan `yearly`: otomatik yenilenen, 1 yıl |
| `premium_lifetime` | Tek seferlik ürün (one-time product) | Bir kez ödenir, Premium kalıcı olur |

Üçünü de oluşturmak zorunda değilsin: uygulama yalnızca Play'de **aktif** olan ürünleri gösterir.
Örneğin sadece `premium_lifetime` oluşturursan ekranda yalnızca ömür boyu seçeneği çıkar.

> Neden iki ayrı ürün? Google Play uygulamaya bir satın alımın **ürün kimliğini** söyler ama temel planını söylemez.
> İki ürün olunca uygulama, yeni telefonda veya yeniden kurulumda bile kullanıcının aylık mı yıllık mı olduğunu bilir.
> (Önceki rehberdeki tek ürünlü `premium` yapısı artık kullanılmıyor.)

## 1. Hazırlık (bir kere)

1. **Ödeme profili:** Play Console → *Setup → Payments profile*. (Ödeme yöntemini ekledin; banka hesabı ve vergi bilgisi de tamam olmalı.)
2. **İlk yükleme:** Play, ürün oluşturmadan önce faturalandırma kütüphanesi içeren bir sürüm görmek ister.
   `gradlew.bat bundleRelease` ile `.aab` üret ve **Testing → Internal testing** kanalına yükle (yayınlaman gerekmez).
   *Monetize with Play → Products → Subscriptions* sayfası hâlâ kilitliyse yüklemeden sonra birkaç dakika bekle.

## 2. Ürünleri oluştur

*Monetize with Play → Products → Subscriptions → Create subscription*

**Aylık**
1. Product ID: `premium_monthly` · Name: `Focus Growing Premium (Monthly)` → Create
2. **Add base plan** → Base plan ID: `monthly` → Type: *Auto-renewing* → Billing period: **Monthly**
3. Grace period: 7 gün (öneri) · Resubscribe: açık
4. **Set prices** → fiyatı gir (ör. ₺49,99) → *Update prices* ile tüm ülkelere uygula → **Save** → **Activate**

**Yıllık**
1. Product ID: `premium_yearly` · Name: `Focus Growing Premium (Yearly)` → Create
2. **Add base plan** → Base plan ID: `yearly` → *Auto-renewing* → Billing period: **Yearly**
3. Fiyat (ör. ₺399,99) → **Save** → **Activate**
4. (İsteğe bağlı) **Add offer** → Offer ID: `trial` → Eligibility: *New customer acquisition → Never had this subscription*
   → Phase: *Free trial*, 7 days → **Activate**. Uygulama, kullanıcı uygunsa "Start 7-day free trial" gösterir.

**Ömür boyu (tek seferlik)**

*Monetize with Play → Products → One-time products → Create one-time product*
1. Product ID: `premium_lifetime` · Name: `Focus Growing Premium (Lifetime)`
2. Satın alma seçeneği (purchase option): **Buy** (kiralama değil) → fiyatı gir (ör. ₺999,99) → tüm ülkelere uygula
3. **Save** → **Activate**

Bu ürün "tüketilmeyen" (non-consumable) bir üründür: uygulama onu asla tüketmez, yalnızca onaylar (acknowledge).
Satın alım kullanıcının Google hesabına kalıcı bağlanır ve yeni telefonda **Restore purchase** ile geri gelir.
Fiyat için yaygın yaklaşım yıllık fiyatın 2–3 katıdır; karar senin.

Ürün kimlikleri sonradan değiştirilemez. Fiyatları istediğin zaman değiştirebilirsin; ekrandaki "Save %…" rozeti
aylık ve yıllık fiyattan otomatik hesaplanır.

## 3. Lisans anahtarı

*Monetize with Play → Monetization setup → Licensing* → **Base64-encoded RSA public key** değerini
`core/billing/BillingConfig.kt` → `PLAY_LICENSE_KEY` içine yapıştır (yapıştırdıysan tekrar gerekmez).
Böylece her satın alımın imzası cihazda doğrulanır. Bu anahtar gizli değildir.

## 4. Test kullanıcısı

Play Console ana sayfa (uygulama seçmeden) → *Setup → License testing* → telefonda kullandığın Gmail adresini ekle
→ License response: **RESPOND_NORMALLY** → Save. Test kullanıcıları gerçek para ödemez; satın alma sayfasında
"Test card, always approves" gibi test kartları çıkar.

## 5. Uygulamada test et

Android Studio → sol altta **Build Variants** → `app` için varyant seç:

| Varyant | Mağaza | Ne için |
|---|---|---|
| `debug` | Simüle (ekranda "Test mode") | Ekranları ve kilitli özellikleri hızlıca denemek. Para yok, Play gerekmez. |
| **`playTest`** | **Gerçek Google Play** | Gerçek satın alma akışını Android Studio'dan çalıştırıp denemek. |
| `release` | Gerçek Google Play | Play'e yüklenecek sürüm. |

**`playTest` ile gerçek akış:**
1. Telefonda Play Store'da, License testing'e eklediğin hesapla oturum açık olsun (emülatörse "Google Play" imajlı olmalı).
2. Varyantı `playTest` yap → **Run**. (Paket adı Play'deki ile aynıdır; cihazda Play'den yüklü sürüm varsa önce kaldır.)
3. Profile → Premium: fiyatlar Play'den gelir. Plan seç → Play sayfasında **Test card, always approves** → satın al.

**Deneyebileceğin senaryolar**

| Senaryo | Nasıl | Beklenen |
|---|---|---|
| Yeni abonelik | Aylık veya yıllık satın al | "Welcome to Premium!", kilitler açılır |
| Aylık → yıllık | Premium ekranında **Switch to yearly** | Hemen geçer; yıllık ücret alınır, aylığın kullanılmayan kısmı gün olarak eklenir |
| Yıllık → aylık | **Switch to monthly** | "Switches to the monthly plan when the current period ends" yazar; dönem sonunda geçer |
| Ömür boyu (yeni kullanıcı) | "Lifetime" seç → **Buy lifetime Premium** | "Premium is yours forever", ekran "Lifetime Premium" olur |
| Ömür boyu (abone iken) | Premium ekranında **Go lifetime** | Premium kalıcı olur; kırmızı kartta "aboneliğin hâlâ aktif, Play'de iptal et" uyarısı çıkar |
| İptal | **Manage in Google Play** → Cancel | Uygulamaya dönünce "Cancelled. Premium stays active until…" |
| Yeniden abone ol | **Resubscribe in Google Play** | "Renews automatically" |
| Süre dolması | İptalden sonra bekle | Dönem bitince Premium kendiliğinden kapanır |
| Geri yükleme | Uygulamayı sil, tekrar kur → **Restore purchase** | Premium ve plan (veya ömür boyu) geri gelir |
| Ömür boyu iadesi | Play Console → Order management → Refund (ve "revoke") | Uygulama tekrar açılınca Premium kapanır |
| Bekleyen ödeme | "Slow test card, approves after a few minutes" | "Your payment is being processed…", onaylanınca açılır |
| Reddedilen ödeme | "Test card, always declines" | Hata mesajı, Premium açılmaz |

Test aboneliklerinde süreler kısalır: **aylık 5 dakikada, yıllık 30 dakikada** yenilenir ve 6 yenilemeden sonra kendiliğinden biter.

**Sorun giderme**
- *"Premium isn't available in your country or on this Google Play account yet"*: ürünler bulunamadı. Kimlikler birebir aynı mı,
  temel planlar **Active** mi, `.aab` bir test kanalına yüklendi mi, paket adı (`applicationId`) Play'deki ile aynı mı?
  Yeni oluşturulan ürünlerin görünmesi birkaç saat sürebilir.
- *"Google Play purchases aren't available on this device"*: Play Store yok veya oturum açılmamış.
- *"The purchase couldn't be verified"*: `PLAY_LICENSE_KEY` yanlış uygulamanın anahtarı olabilir.

## 6. Kod nasıl çalışıyor

- `core/billing/PlayBillingManager.kt`: Play'e bağlanır, iki ürünü ve yerel fiyatları çeker, satın alma sayfasını açar,
  satın alımı imzayla doğrular ve **acknowledge** eder (edilmezse Play 3 gün sonra iade eder).
- **Plan değiştirme:** Kullanıcının mevcut aboneliği yenisiyle değiştirilir (replacement).
  Aylık → yıllık `CHARGE_FULL_PRICE` (hemen), yıllık → aylık `DEFERRED` (dönem sonunda).
  Google'ın "farkı öde" modu yalnızca birim fiyatı artan geçişlerde çalıştığı için aylık → yıllıkta kullanılamıyor.
- **Ömür boyu:** Aboneliklerden bağımsız, tek seferlik bir satın almadır. Google Play bir aboneliği otomatik iptal etmez;
  bu yüzden abone iken ömür boyu alan kullanıcıya uygulama, aboneliğini Play'de iptal etmesini hatırlatır.
  Yeni kullanıcılar için hangi planın önceden seçili geleceği `PremiumUiState.DEFAULT_PLAN` ile ayarlanır (şu an ömür boyu).
- Uygulama her açıldığında, öne geldiğinde ve Premium ekranına dönüldüğünde abonelik Play'den yeniden okunur:
  yenileme, iptal, iade, plan değişimi ve süre dolması otomatik yansır. İnternet yoksa son bilinen durum korunur.
- Premium durumu `SubscriptionRepository` içinde saklanır; tüm ekranlar `PremiumManager.hasAccess(...)` ile kontrol eder.

## 7. Bilinen sınırlar

- **Sunucu yok:** Doğrulama cihazda imza kontrolüyle yapılır. Bitiş/yenileme **tarihi** Play tarafından uygulamaya verilmediği için
  ekranda tarih gösterilmez (bunun için Google Play Developer API ve bir sunucu gerekir).
- Yıllık → aylık geçiş planlandıktan sonra uygulama silinip yeniden kurulursa "planlandı" notu görünmez; geçiş yine de Play'de gerçekleşir.
- Test hesabıyla alınan tek seferlik ürün kalıcıdır. Tekrar denemek için Play Console → Order management'tan iade et
  ya da License testing'deki başka bir hesabı kullan.
