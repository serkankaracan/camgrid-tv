# Fiziksel cihaz test planı

## Mevcut koşunun durumu

Bu plandaki gerçek C500, C510W ve Mi Stick senaryolarının tamamı 2026-09-03
koşusunda **BLOCKED** durumundadır. Fiziksel kameralara, Mi Stick'e, emülatöre
veya bağlı bir adb cihazına erişilemedi. Fake, JVM ya da derleme sonucu fiziksel
doğrulama yerine kullanılmaz.

Geliştirme ortamı Windows 11, Visual Studio Code, Codex ve PowerShell'dir. Proje
yerel Android komut satırı SDK'sını kullanır; Android Studio gerekli değildir ve
kurulu olduğu varsayılmaz.

Her senaryo için `docs/TEST_REPORT.md` içinde yalnızca `PASS`, `FAIL`, `BLOCKED`
veya `NOT RUN` ile kısa, redakte edilmiş kanıt kaydedin.

## Güvenli kanıt kuralları

- Kullanıcı adı, parola, tam RTSP URI, ev IP'si, MAC adresi, cihaz seri numarası
  ve ham adb/VLC logu commit'e, issue'ya, sohbete veya ekran görüntüsüne girmez.
- Kamera Hesabı, Tapo bulut hesabı veya Wi-Fi parolası değildir. Hesap her kamera
  için Tapo uygulamasında yerel yayın amacıyla oluşturulur ve yalnız uygulamanın
  Kamera Hesabı alanlarına girilir.
- Adres gerekiyorsa PowerShell'de `Read-Host` ile alın; gerçek değeri komut
  geçmişine veya rapora yazmayın.
- Kanıt örneği: `C500 stream2: PASS — 15 dakika kesintisiz, 0 crash`. Gerçek
  endpoint veya hesap bilgisi eklemeyin.

## Önkoşullar

1. C500, C510W, Mi Stick ve geliştirme bilgisayarı aynı güvenilir yerel ağdadır.
2. Her kamerada ONVIF/RTSP ve ayrı Kamera Hesabı etkinleştirilmiştir.
3. Mi Stick'te geliştirici seçenekleri ile desteklenen USB veya ağ hata ayıklama
   yöntemi açılmıştır.
4. `app\build\outputs\apk\debug\app-debug.apk` güncel kalite kapısından
   üretilmiştir.
5. Test sırasında internete kamera portu yönlendirilmez ve uygulamaya public IP
   veya internet host adı girilmez.

## PowerShell ile cihaz bağlantısı ve kurulum

VS Code içindeki PowerShell terminalinde depo kökünden çalıştırın:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
& $adb devices -l
```

Liste boşsa fiziksel testlere başlamayın; sonucu `BLOCKED` kaydedin. Mi Stick'in
desteklediği kablosuz eşleme yöntemi kullanılacaksa adresleri yalnız istemde
girin:

```powershell
$pairTarget = Read-Host 'Mi Stick eşleme adresi ve portu'
& $adb pair $pairTarget
$connectTarget = Read-Host 'Mi Stick bağlantı adresi ve portu'
& $adb connect $connectTarget
& $adb devices -l
```

Eşleme kodunu komut satırı argümanı olarak yazmayın; adb isteminde girin. Cihaz
`device` durumunda göründüğünde APK'yı kurun:

```powershell
& $adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

## Gerçek kabul senaryoları

### 1. C500 ve C510W otomatik keşfi

- Önkoşul: İki kamera açık, aynı LAN'da ve ONVIF etkin.
- Adım: Uygulamayı temiz başlangıçla açın, yerel ağ iznini verin ve taramayı
  tamamlayın.
- Beklenen: C500 ve C510W birer kez, gerçek IP gösterilmeden ayırt edilebilir
  adlarla görünür; yinelenen ProbeMatch satırları ikinci cihaz oluşturmaz.
- Kanıt: Model başına görünen kayıt sayısı ve tarama süresi; adresleri redakte
  edin.

### 2. C500 Kamera Hesabı ve `/stream2`

- Önkoşul: C500 için doğrulanmış yerel Kamera Hesabı mevcut.
- Adım: Yalnız uygulama içinde hesabı girin ve C500 bağlantısını test edin.
- Beklenen: Düşük kaliteli H.264 `/stream2` görüntüsü açılır; hesap bilgisi UI
  veya logda düz metin görünmez.
- Kanıt: `C500 stream2: PASS/FAIL` ve kısa süre/güvenli hata özeti.

### 3. C510W Kamera Hesabı ve `/stream2`

- Önkoşul: C510W için doğrulanmış yerel Kamera Hesabı mevcut.
- Adım: Yalnız uygulama içinde hesabı girin ve C510W bağlantısını test edin.
- Beklenen: Düşük kaliteli H.264 `/stream2` görüntüsü açılır; hesap bilgisi UI
  veya logda düz metin görünmez.
- Kanıt: `C510W stream2: PASS/FAIL` ve kısa süre/güvenli hata özeti.

### 4. İki yayınlı duvar dayanıklılığı

- Önkoşul: Senaryo 2 ve 3 ayrı ayrı PASS.
- Adım: İki kamerayı seçin ve iki `/stream2` tile'ı en az 15 dakika açık tutun.
- Beklenen: Crash, sürekli yeniden başlatılan player veya diğer tile'ı kapatan
  tekil hata oluşmaz.
- Kanıt: Başlangıç/bitiş süresi, crash sayısı ve her tile'ın son durumu.

### 5. Fiziksel D-pad odağı

- Önkoşul: İki tile duvarda görünür ve Mi Stick kumandası bağlı.
- Adım: Yön tuşlarıyla iki tile ve duvar eylemleri arasında dolaşın.
- Beklenen: Odak çerçevesi her zaman görünür, öngörülebilir ve dokunma gerektirmez.
- Kanıt: İzlenen odak sırası; seri numarası içermeyen fotoğraf isteğe bağlıdır.

### 6. C500 `/stream1` tam ekran

- Önkoşul: C500 tile'ı canlı ve odakta.
- Adım: OK ile tam ekranı açın, görüntüyü gözlemleyin ve Back ile dönün.
- Beklenen: C500 yüksek kaliteli `/stream1` ile tek player olarak açılır; Back
  aynı C500 tile odağını duvarda geri yükler.
- Kanıt: `C500 fullscreen: PASS/FAIL`, açılış süresi ve geri dönen odak adı.

### 7. C510W `/stream1` tam ekran

- Önkoşul: C510W tile'ı canlı ve odakta.
- Adım: OK ile tam ekranı açın, görüntüyü gözlemleyin ve Back ile dönün.
- Beklenen: C510W yüksek kaliteli `/stream1` ile tek player olarak açılır; Back
  aynı C510W tile odağını duvarda geri yükler.
- Kanıt: `C510W fullscreen: PASS/FAIL`, açılış süresi ve geri dönen odak adı.

### 8. Arka plan ve ön plan toparlanması

- Önkoşul: İki yayınlı duvar canlı.
- Adım: Home ile uygulamayı arka plana alın, kısa süre sonra yeniden açın.
- Beklenen: Arka planda player'lar serbest bırakılır; dönüşte yalnız istenen iki
  player yeniden kurulur, kopya ses/video veya crash oluşmaz.
- Kanıt: Önce/sonra tile durumları ve gözlenen player sayısı.

### 9. Yerel ağ kaybı ve geri dönüş

- Önkoşul: İki yayın canlı; router ve kameralar güvenli biçimde erişilebilir.
- Adım: Mi Stick Wi-Fi bağlantısını kısa süre kesin ve yeniden bağlayın.
- Beklenen: Tile'lar offline olur, sıkı retry döngüsü oluşmaz ve ağ dönünce
  kontrollü biçimde yeniden bağlanır.
- Kanıt: Kesinti süresi, toparlanma süresi ve crash/retry özeti.

### 10. Yanlış parola davranışı

- Önkoşul: Doğru hesabın çalıştığı daha önce kanıtlanmış.
- Adım: Geçici olarak yanlış parola girip yalnız bir kamerayı test edin; ardından
  doğru bilgiyi uygulama içinde yeniden girin.
- Beklenen: Güvenli kimlik doğrulama hatası gösterilir, sonsuz otomatik retry
  yapılmaz ve diğer tile etkilenmez.
- Kanıt: Hata sınıfı ve retry'nin durduğu; yanlış veya doğru parolayı yazmayın.

### 11. Yeniden başlatma ve seçim kalıcılığı

- Önkoşul: İki kamera seçilmiş, güvenli profiller kaydedilmiş.
- Adım: Uygulamayı tamamen kapatıp yeniden açın.
- Beklenen: Seçimler, özel kamera adları ve sıralama korunur; kaydedilmiş duvar
  izin hazır olduğunda güvenli biçimde açılır.
- Kanıt: Yeniden başlatma öncesi/sonrası seçili model adları ve sıra.

### 12. Endpoint UUID ile adres değişimi

- Önkoşul: Ağ yöneticisi kameranın DHCP adresini güvenli biçimde değiştirebilir.
- Adım: Aynı kamerayı yeni adreste yeniden keşfedin.
- Beklenen: Aynı endpoint UUID yeni kamera oluşturmaz; kayıt yeni yerel adrese
  taşınır ve diğer farklı UUID'li cihazla birleşmez.
- Kanıt: Kamera sayısı ile redakte edilmiş `eski adres -> yeni adres` ifadesi.

### 13. Logcat gizlilik denetimi

- Önkoşul: adb cihazı bağlı ve gerçek yayın senaryoları çalıştırılabilir.
- Adım: Ham logu kaydetmeden yerel terminalde canlı inceleyin:

  ```powershell
  & $adb logcat -c
  & $adb logcat
  ```

  İnceleme bitince `Ctrl+C` kullanın.
- Beklenen: Kullanıcı adı, parola, credential taşıyan tam RTSP URI, ev IP'si veya
  hassas anahtar malzemesi görünmez.
- Kanıt: Yalnız `sensitive matches: 0` veya redakte edilmiş hata sınıfı; ham logu
  commit etmeyin.

### 14. Bellek, decoder ve tekil hata izolasyonu

- Önkoşul: İki yayınlı duvar en az 15 dakika çalışmış.
- Adım: Belleği yerel olarak gözlemleyin; mümkünse bir kamerayı kısa süre kapatın:

  ```powershell
  & $adb shell dumpsys meminfo io.github.serkankaracan.camgridtv
  ```

- Beklenen: Bellek kontrolsüz büyümez; bir tile hatası diğer canlı tile'ı
  kapatmaz; decoder yetersizliği güvenli hata olarak görünür.
- Kanıt: Redakte edilmiş toplam bellek özeti, tile durumları ve crash sayısı.

### 15. Üç yayınlı grid (isteğe bağlı)

- Önkoşul: Üçüncü güvenli gerçek veya yerel fake RTSP yayını fiziksel cihazdan
  erişilebilir; üretim arayüzüne manuel IP aracı eklenmez.
- Adım: Üç tile'ı açıp D-pad ile tümüne gidin.
- Beklenen: 2×2 yerleşimde son satır ortalanır ve odak yolu kararlı kalır.
- Kanıt: Tile sayısı, odak sırası ve `PASS/FAIL`; önkoşul yoksa `BLOCKED`.

## Sonuç kaydı

Özellikle şu sonuçları ayrı satırlarda tutun: C500 keşif, C510W keşif, C500
`/stream2`, C510W `/stream2`, iki yayın/15 dakika, C500 `/stream1` tam ekran,
C510W `/stream1` tam ekran, Mi Stick D-pad, lifecycle, ağ geri dönüşü, yanlış
parola, kalıcılık, log gizliliği ve decoder/bellek. Her fiziksel cihaz sonucu
kanıt yürütülene kadar `BLOCKED` kalır.
