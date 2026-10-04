# Premium satın alma: Google Play kurulumu

Kod hazır (Google Play Billing Library 9.1.0). Satın almanın gerçekten çalışması için Play Console'da aşağıdaki
ürünleri **birebir aynı kimliklerle** oluşturman gerekiyor. Kimlikler `core/billing/BillingConfig.kt` içinde.

| Play Console | Değer |
|---|---|
| Abonelik (Subscription) ürün kimliği | `premium` |
| Temel plan (base plan) 1 | `monthly`: otomatik yenilenen, 1 ay |
| Temel plan 2 | `yearly`: otomatik yenilenen, 1 yıl |
| (İsteğe bağlı) yıllık plana teklif | ör. `trial`: 7 gün ücretsiz deneme, yalnızca yeni aboneler |

## 1. Hazırlık (bir kere)

1. **Ödeme profili:** Play Console → *Setup → Payments profile*. Banka hesabını ve vergi bilgilerini gir. Bu olmadan ücretli ürün oluşturulamaz.
2. **İlk yükleme:** Play, ürün oluşturmadan önce faturalandırma kütüphanesi içeren bir sürüm görmek ister.
   `./gradlew bundleRelease` ile `.aab` üret ve **Internal testing** kanalına yükle (yayınlaman gerekmez, yüklemen yeter).

## 2. Aboneliği oluştur

1. *Monetize with Play → Products → Subscriptions → Create subscription*
   - Product ID: `premium` (sonradan değiştirilemez)
   - Name: `Focus Growing Premium`
2. **Add base plan** → Base plan ID: `monthly` → *Auto-renewing*, Billing period: **1 month** → fiyatı gir (ör. ₺49,99; *Set prices* ile diğer ülkelere otomatik dağıtılır) → **Activate**.
3. **Add base plan** → `yearly` → *Auto-renewing*, **1 year** → fiyat (ör. ₺399,99) → **Activate**.
4. (İsteğe bağlı) `yearly` planında **Add offer** → Offer ID `trial` → Eligibility: *New customer acquisition* → Phase: *Free trial, 7 days* → **Activate**.
   Kod, kullanıcı uygunsa deneme teklifini otomatik gösterir ("Start 7-day free trial").
5. Grace period (ödeme sorununda ek süre) varsayılan olarak açık kalsın. Kullanıcı bu sürede Premium'u kullanmaya devam eder.

Fiyat örnekleri yalnızca örnek; istediğin fiyatı koy. Ekrandaki "Save %…" rozeti aylık ve yıllık fiyattan otomatik hesaplanır.

## 3. Lisans anahtarını ekle (önerilir)

*Monetize with Play → Monetization setup → Licensing* → **Base64-encoded RSA public key** değerini kopyala ve
`core/billing/BillingConfig.kt` → `PLAY_LICENSE_KEY` içine yapıştır. Böylece her satın alımın imzası cihazda
doğrulanır ve sahte satın alma verileri reddedilir. Bu anahtar gizli değildir; koda koymak güvenlidir.

## 4. Test et

**Debug derlemesi (Android Studio'dan Run):** Gerçek Play yerine **simüle mağaza** kullanır. Ekranda "Test mode"
etiketi görünür, fiyatlar örnektir, para çekilmez. Premium ekranını ve kilitli özellikleri test etmek için bunu kullan.
"Reset test purchase" ile ücretsiz sürüme dönebilirsin.

**Gerçek satın alma testi (ücretsiz, test kartıyla):**
1. Play Console → *Setup → License testing* → kendi Gmail adresini ekle → *License response: RESPOND_NORMALLY*.
2. *Testing → Internal testing → Testers* bölümüne aynı adresi ekle, **opt-in linkini** telefonda aç ve uygulamayı Play Store'dan yükle.
   (Android Studio'dan yüklenen debug sürüm gerçek Play faturalandırmasıyla çalışmaz, çünkü paket adı `.debug` ile bitiyor.)
3. Premium → plan seç → Play sayfasında **"Test card, always approves"** kartını seç.
4. Test aboneliklerinde süreler kısalır: aylık plan **5 dakikada**, yıllık plan **30 dakikada** yenilenir ve birkaç yenilemeden sonra otomatik biter.
   Bittiğinde uygulamayı tekrar açınca Premium kendiliğinden kapanır. Bu da süresi dolan aboneliklerin doğru işlendiğini gösterir.
5. Senaryolar: iptal (Play Store → Ödemeler ve abonelikler), "Restore purchase" (uygulamayı silip yükle), bekleyen ödeme ("Slow test card, approves after a few minutes").

## 5. Kod nasıl çalışıyor (kısaca)

- `core/billing/PlayBillingManager.kt`: Play'e bağlanır, planları ve yerel fiyatları çeker, satın alma sayfasını açar,
  satın alımı doğrular ve **acknowledge** eder (edilmezse Play 3 gün sonra otomatik iade eder).
- Uygulama her açıldığında veya öne geldiğinde Play'den aktif abonelikler yeniden okunur. Yenileme, iptal, iade ve süre dolması böylece
  otomatik yansır. İnternet yoksa son bilinen durum korunur.
- Premium durumu `SubscriptionRepository` içinde saklanır; tüm ekranlar `PremiumManager.hasAccess(...)` ile kontrol eder.
- Release derlemesi her zaman gerçek Play'i, debug derlemesi simülasyonu kullanır (`BuildConfig.FAKE_BILLING`).

## 6. Bilinen sınırlar / sonraki adımlar

- **Sunucu yok:** Doğrulama cihazda imza kontrolüyle yapılıyor. Bu, sunucusuz uygulamalar için makul bir seviye. Daha güçlü koruma için
  ileride Google Play Developer API ile sunucu tarafı doğrulama eklenebilir.
- **Plan değiştirme:** Aylıktan yıllığa geçiş için uygulama içi akış yok. Kullanıcı "Manage subscription" ile Play'de iptal edip
  yıllığı alabilir. İstersen uygulama içi yükseltme (replacement) akışını ekleyebilirim.
- **Ömür boyu (tek seferlik) satın alma** yok. İstersen ekleyebilirim.
- Gizlilik politikası Google Play Faturalandırma'yı zaten kapsıyor; Data safety formunda ek bir şey gerekmez.
