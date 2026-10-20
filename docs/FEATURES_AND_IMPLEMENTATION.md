# PublicStatsApp — Güncelleme Paketi ve Özellik Planı

## Mevcut projede bulunanlar
- Android/Kotlin uygulama kaynakları
- Herkese açık profil istatistiklerini görüntüleme altyapısı
- Geçmiş ve profil karşılaştırma ekranları
- Google Play Billing entegrasyonunun temel sınıfları
- Ödüllü reklam yöneticisinin temel sınıfı
- Premium ekranı ve animasyonlu Premium rozeti
- GitHub Actions ile APK derleme iş akışı

## Premium ürün deneyimi
Premium ekranında gösterilecek avantajlar:
- Günlük sorgu limitinin kaldırılması (uygulamanın veri kaynağı/servis limitlerine tabi)
- Reklamsız kullanım
- Arama geçmişi ve kayıtlı sonuçlar
- İki herkese açık profilin karşılaştırılması
- Profil alanında Premium rozeti
- Koyu tema

Satın alma fiyatını sabit metin olarak yazmayın; Google Play Billing'den ürün fiyatı ve para birimiyle alın. Gerçek ürün kimliği Play Console'da tanımlanmalı. Satın alma sonucu güvenilir şekilde doğrulanmadan Premium açılmamalı. Satın alımı geri yükleme akışı test edilmeli.

## Ödüllü reklam / ek hak önerisi
- Reklam tamamlanınca ödül ver; reklam yüklenmediyse veya yarıda kapandıysa hak verme.
- Günlük reklam sayısı ve ödül miktarını yapılandırılabilir tut.
- Hakları yalnızca istemci tarafında artırmak kötüye kullanıma açık olabilir; mümkünse sunucu doğrulaması veya Play/AdMob ödül doğrulaması kullan.
- Örnek ürün tasarımı: bir ödüllü reklam karşılığında +1 ek profil analizi; günde en fazla 3 reklam. Değerler deney ve gelir testine göre değiştirilebilir.

## Eklenmesi planlanan, bu pakette henüz tamamlanmış kod olmayan modüller
- Günlük hedefler ve görev hatırlatıcıları
- Gelişmiş istatistik grafikleri
- Kayıtlı profil listeleri ve karşılaştırma geçmişi
- Premium avantajlarını açıklayan karşılaştırma tablosu
- Reklamla ek hak kazanma ekranının tamamlanması

## Platform ve güvenlik sınırları
Bu uygulama, Instagram hesabına giriş bilgisi toplamamalı veya saklamamalı. Spam limitlerini aşmak amacıyla otomatik beğeni/takipten çıkma ya da toplu gönderi silme botu eklenmemelidir. Instagram'ın resmî API'lerinin izin verdiği kapsam dışındaki hesap işlemleri garanti edilemez. Uygulama, herkese açık verilerin analizi ve kullanıcı tarafından başlatılan işlemlerle sınırlı tutulmalıdır.

## APK derleme
GitHub deposuna yükledikten sonra Actions sekmesinden `APK derle` iş akışını çalıştırın. Başarılı iş akışının Artifact bölümünden APK çıktısını indirin. Bu, kaynak kodu derler; dokümanda önerilen tüm gelecek modülleri otomatik olarak oluşturmaz.

## Ön izleme
`PREVIEW_CONCEPT.png` yalnızca görsel konsepttir; ekran görüntüsündeki her kontrol çalışan uygulama özelliği değildir.
