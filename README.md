# Public Stats

Bir Instagram kullanıcı adı girip, o hesabın **herkese açık (public)**
profil sayfasında zaten görünen bilgileri gösteren, premium abonelik ve
ödüllü reklam modeliyle gelen bir Android uygulaması.

## Bu uygulama ne YAPAR

- Herkese açık bir hesabın: profil fotoğrafı, bio, takipçi/takip/gönderi
  sayısı ve (mümkünse) son gönderi önizlemelerini gösterir.
- Günlük **5 ücretsiz sorgu** hakkı verir, gece yarısı sıfırlanır.
- Hak bitince: **ödüllü reklam** izleyerek +3 hak (günde en fazla 3 reklam,
  yani en fazla +9 bonus hak) **veya** Premium'a geçip sınırsız kullanım.
- Premium: sınırsız sorgu, reklamsız, arama geçmişi, iki profili yan yana
  karşılaştırma, profil kartında altın **Premium rozeti**, koyu tema.
- Google Play Billing ile gerçek, tek seferlik ("ömür boyu") satın alma.

## Bu uygulama ne YAPMAZ (ve neden)

- **"Profili kim gördü" bilgisini göstermez.** Bu veri Instagram'ın hiçbir
  resmi veya gayriresmi arayüzünde mevcut değil; hiçbir uygulama bunu
  gösteremez.
- **Gizli (private) hesapların içeriğini açmaz.** Hedef hesap seni
  onaylamadığı sürece bu teknik olarak imkânsızdır.
- **Otomatik beğeni, zamanlanmış/toplu takip-takipten çıkma, toplu gönderi
  silme gibi otomasyon özellikleri YOK.** Bunlar Instagram'ın kullanım
  şartlarını açıkça ihlal eder, hesap askıya alınmasına yol açar ve
  platformun bot tespit sistemini atlatmaya çalışmayı gerektirir — bu
  yüzden bilerek eklenmedi.
- **Sahte/uydurma veri üretmez.** Bir bilgi alınamazsa, placeholder içerik
  koymak yerine durumu açıkça söyler.
- **Instagram şifresi istemez, login yapmaz.**

## Yeni eklenen dosyalar (bu güncellemede)

| Dosya | Ne işe yarar |
|---|---|
| `PreferencesManager.kt` | Günlük ücretsiz hak sayacı, reklamdan kazanılan bonus hak, premium durumu, arama geçmişi (SharedPreferences, sunucu yok) |
| `BillingManager.kt` | Google Play Billing — tek seferlik "premium_lifetime" ürününü satın alma/geri yükleme |
| `RewardedAdManager.kt` | AdMob ödüllü reklam — kullanıcı reklamı tamamlarsa +3 hak |
| `PremiumActivity.kt` + `activity_premium.xml` | Premium özellik listesi, fiyat, satın al / geri yükle butonları |
| `HistoryActivity.kt` + `activity_history.xml` | Premium'a özel arama geçmişi ekranı (premium değilse kilitli ekran gösterir) |
| `CompareActivity.kt` + `activity_compare.xml` | Premium'a özel, iki profili yan yana karşılaştırma |
| `MainActivity.kt` (güncellendi) | Hak sayacı, "reklam izle" butonu, üst menü (Geçmiş/Karşılaştır/Premium), profil kartında Premium rozeti |

## Yayına çıkmadan önce DEĞİŞTİRMEN gerekenler

1. **AdMob ID'leri** — `AndroidManifest.xml`'deki `APPLICATION_ID` ve
   `RewardedAdManager.kt`'deki `TEST_REWARDED_AD_UNIT_ID`, şu an Google'ın
   **resmi test ID'leri**. Kendi AdMob hesabını açıp gerçek ID'lerle
   değiştirmelisin — aksi halde reklamlar hep test reklamı gösterir ve
   gerçek gelir oluşmaz.
2. **Play Billing ürünü** — Google Play Console'da bu `applicationId`
   altında `premium_lifetime` adında bir "yönetilen ürün" (managed
   in-app product) oluşturup fiyatını belirlemen gerekiyor. Uygulamayı
   kapalı test kanalına yükleyip bir test hesabıyla deneyebilirsin;
   debug derlemede gerçek satın alma ekranı çalışmaz.
3. **Satın alma doğrulaması** — bu basit sürümde satın alma sadece cihaz
   üstünde (client-side) kontrol ediliyor. Gerçek bir yayın için satın
   alma jetonunu kendi sunucunda Play Developer API ile doğrulaman
   önerilir (bu depoda o sunucu kısmı yok).
4. **Paket adı** — `applicationId` şu an `com.example.pubstats`, kendi
   adına göre değiştir.

## Derleme

1. [Android Studio](https://developer.android.com/studio) kur (ücretsiz).
2. Bu klasörü "Open an existing project" ile aç.
3. Gradle senkronizasyonunu bekle (internet bağlantısı gerekir).
4. Telefonunu bağla veya emülatör aç, ▶️ **Run**.

`gradlew` dosyası bu pakette yok; Android Studio projeyi ilk açtığında
senin için otomatik oluşturacaktır.

## GitHub Actions ile otomatik APK derleme

Bu depo `.github/workflows/apk-derle.yml` içeriyor. Repoyu GitHub'a
(gerçek dosya yapısıyla, zip olarak değil) yükleyip `main` dalına her
push yaptığında, GitHub'ın kendi sunucuları projeyi otomatik derler ve
sana kurulabilir bir `.apk` üretir — kendi bilgisayarında Android Studio
açmana gerek kalmaz.

**Sonucu indirmek için:** GitHub'da reponun **Actions** sekmesine git,
en son çalışan "APK derle" işine tıkla, en altta **Artifacts** kısmından
`PublicStats-APK` dosyasını indir (bir `.zip` olarak iner, içinden
`.apk`'yı çıkarıp telefona kurabilirsin — "Bilinmeyen kaynaklardan
yükleme" iznini açman gerekebilir).

Şu an derlenen **debug** sürümdür — test amaçlı otomatik bir anahtarla
imzalanır, doğrudan kurulabilir ama Google Play'e yüklenemez.

### İleri seviye: release imzalama (Play Store için)

Play Store'a yüklemek istersen gerçek bir "release" APK/AAB gerekir,
bunun için kendi imzalama anahtarını (keystore) oluşturup GitHub'ın
**Settings → Secrets and variables → Actions** kısmına şu sırları
eklemen gerekir: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. İstersen bu kısmı birlikte kurabiliriz.

## Ekleyebileceğin başka fikirler (istersen sonra ekleriz)

- Koyu tema anahtarı (layout'larda token'lar hazır, sadece bir `Settings`
  ekranı ve tema geçişi eklemek yeterli).
- Arama geçmişindeki aynı kullanıcı için takipçi sayısı değişimini basit
  bir çizgi grafikte gösterme (veri zaten `PreferencesManager` içinde
  zaman damgalı tutuluyor).
- Birden fazla premium planı (aylık/yıllık abonelik + ömür boyu) — şu an
  tek seferlik satın alma var, Play Billing aboneliği de destekler.
- "Günün önerisi" gibi ücretsiz, zararsız bir ekstra ekran (örn. ilham
  verici herkese açık hesap önerileri) — reklam gösterimini artırmak için.
