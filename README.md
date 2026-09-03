# KARACAM

KARACAM, aynı güvenilir yerel ağdaki ONVIF/RTSP kameraları otomatik bulan ve
seçilen canlı yayınları Android TV / Google TV üzerinde tek bir dinamik ızgarada
gösteren yerel-öncelikli bir uygulamadır. Backend, bulut hesabı, telemetri ve
uzaktan erişim kullanmaz.

> KARACAM bağımsız ve resmî olmayan bir açık kaynak projesidir. TP-Link veya
> Tapo tarafından geliştirilmez, desteklenmez ya da onaylanmaz. Üründe bu
> markalara ait logo veya görsel varlık kullanılmaz.

Hedef kabul modelleri Tapo C500 ve C510W'dir. Fiziksel doğrulama durumu
[test raporunda](docs/TEST_REPORT.md) açıkça belirtilir; model adı doğrulanmış test
iddiası anlamına gelmez.

## Nasıl çalışır?

```text
Android TV                  Güvenilir yerel ağ                 Kameralar
KARACAM     -- WS-Discovery 239.255.255.250:3702 -->  ONVIF endpoint'leri
            <-- ProbeMatch / UUID / XAddr ----------
            -- RTSP/TCP :554 /stream2 ------------->  Grid (düşük kalite)
            -- RTSP/TCP :554 /stream1 ------------->  Tam ekran (yüksek kalite)
            -- /stream1 oynatılamazsa /stream2 ---->  Tam ekran uyumluluk modu
```

Uygulama açılışta ONVIF WS-Discovery taraması yapar, tekrar gelen cevapları endpoint
UUID/XAddr/adres sırasıyla tekilleştirir ve aynı UUID yeni bir IP'de bulunursa
kaydı günceller. Bilinmeyen üreticinin uyumlu ONVIF cihazı reddedilmez. Kullanıcı
kameraları ve adlarını seçer, ayrı Kamera Hesabı bilgilerini girer ve bağlantıyı
sınar. Sonraki açılışta seçim korunur.

Koyu kontrol-odası arayüzü keşif, güvenli kurulum ve canlı izleme adımlarını
birbirinden ayırır. Kurulumun sağ panelindeki tek ana eylem önce **Bağlantıyı
doğrula** olarak görünür; hesap seçili hedeflere kaydedilip en az bir `/stream2`
yayını gerçekten **Canlı** olduğunda aynı eylem **N kamerayı izle**ye dönüşür.
Kamera keşfedilmiş olması tek başına izlemeyi açmaz: seçili her kameraya geçerli
bir hesap profili atanmış olmalıdır. Duvar tüm kameraları kaydırmasız ekrana
sığdırır; üst çubuk toplam canlı yayın sayısını gösterir. Canlı bir kutucukta
**Canlı** durumu yalnız başlıktaki rozette bir kez görünür; bağlantı ve hata
durumları video üzerindeki durum katmanında kalır. Kurulum önizlemesi ve duvar
kutucukları, video yüzeyinin Compose overlay'lerini veya odak çerçevesini
örtmemesi için `TextureView` kullanır. Odaktaki kutucuk kalın ve yüksek kontrastlı
bir çerçeveyle ayırt edilir. Çevrimdışı, yeniden bağlanan veya oynatılamayan yayın
olduğunda üst çubuktaki **Kameraları yeniden tara** eylemi keşfe dönüp yeni tarama
başlatır. D-pad odağı kutucuklar arasında taşır; OK odaktaki kamerayı tam ekran
açar, Back önceki ızgara odağını geri yükler.
Tam ekran önce `/stream1` yüksek kaliteli yayını dener. İlk oynatma denemesi
başarısız olursa (kimlik doğrulama veya yerel ağ yokluğu hariç) aynı oturumda
bir kez `/stream2` yayınına geçer. Sonraki tam ekran girişinde yüksek kalite
yeniden denenir. Tam ekran video için `SurfaceView` kullanılır. Yayın durumu ve
**Görüntü** kontrolü, tümünü çevreleyen bir bilgi paneli olmadan overscan-safe sağ
üstte; gölgeli kamera adı overscan-safe sağ alttadır. Ekranda ayrıca bir geri
dönüş talimatı gösterilmez, ancak Back aynı duvar odağına dönmeye devam eder.

Tam ekran ilk açılışta **Görüntü: Güvenli** modundadır: kaynak, genişliği ve
yüksekliği ekranın %90'ı olan ortalanmış alana sığdırılır. Kumandada Sağ veya OK
ile **Güvenli → Sığdır → Doldur → Güvenli**, Sol ile ters yönde geçilir. Güvenli
ve Sığdır kaynağın tamamını gösterip gerektiğinde siyah şerit bırakır. Doldur
oranı bozmadan ekranı kaplar ve yalnız kenarları kırpar; hiçbir mod görüntüyü
yatay ya da dikey esnetmez. Ses renderer'ı kapalıdır.

## Gizlilik ve güvenlik

- Kamera trafiği ve ayarlar cihazdan/LAN'dan çıkmaz; telemetry, analytics, reklam
  ve crash-reporting SDK'sı yoktur.
- Kamera seçimi ve secret olmayan metadata DataStore'dadır. Kullanıcı adı/parola,
  her yazımda yeni IV kullanan AES/GCM ile şifrelenir; anahtar Android Keystore'da
  dışa aktarılamaz. Keystore kaybında plaintext fallback yapılmaz.
- Android backup kapalıdır. Credential içeren URI'lerin normal metin gösterimi
  redacted'dır; ham URI yalnız Media3 sınırında açılır.
- RTSP şifrelenmemiş olabilir. Uygulamayı yalnız güvenilir, şifreli ev LAN/Wi-Fi
  ağında kullanın. 554 veya 2020 portlarını internete açmayın. KARACAM uzaktan
  erişim, port yönlendirme veya VPN kurulumu sağlamaz.
- Android 17/API 37+ üzerinde yerel ağ izni, herhangi bir discovery/RTSP soketi
  açılmadan istenir. Eski sürümlerde platformun uyumluluk davranışı kullanılır.

Ayrıntılar: [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md) ve
[mimari](docs/ARCHITECTURE.md).

## Kamera Hesabı oluşturma

Kamera Hesabı, Tapo bulut hesabı, Wi-Fi parolası ve modem parolasından farklıdır.
Her kameranın mobil uygulamasında:

1. Kameranın canlı görünümünü açın ve sağ üstten **Device Settings** ekranına gidin.
2. **Advanced Settings > Camera Account** yolunu açın.
3. Güçlü ve benzersiz bir kullanıcı adı/parola oluşturun.
4. Bilgileri yalnız KARACAM'ın kendi güvenli formuna girin; sohbete, terminale
   veya RTSP URL'sine yapıştırmayın.

Üreticinin güncel adımları:
[Tapo RTSP/ONVIF ve Camera Account](https://www.tp-link.com/us/support/faq/2680/).

## Windows 11'de Android Studio olmadan derleme

Gerekenler:

- Git
- JDK 17 (örneğin Temurin 17)
- Google'ın [resmî Android SDK Command-line Tools
  paketi](https://developer.android.com/studio#command-tools)
- PowerShell 5.1 veya PowerShell 7

Aşağıdaki komutların tamamını VS Code içindeki PowerShell terminalinde çalıştırın.
Temiz bir terminalde önce JDK 17 klasörünü tanımlayıp sürümü doğrulayın:

```powershell
git clone https://github.com/serkankaracan/camgrid-tv.git
Set-Location .\camgrid-tv
$javaHomeInput = Read-Host 'JDK 17 klasörü'
$env:JAVA_HOME = (Resolve-Path -LiteralPath $javaHomeInput).Path
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -version
```

Resmî command-line tools ZIP'ini çıkarıp `sdkmanager.bat` yolunu belirledikten
sonra API 37 SDK'sını proje-yerel, Git tarafından yok sayılan alana kurun:

```powershell
.\scripts\setup-android-sdk.ps1 `
    -SdkManagerPath "C:\Android\cmdline-tools\latest\bin\sdkmanager.bat" `
    -JavaHomePath $env:JAVA_HOME `
    -AcceptLicenses
```

Kalite kapısı; debug/release lint'i, JVM testlerini, debug APK'yı, debug
instrumented-test APK'sını ve küçültülmüş release APK'yı derler:

```powershell
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath "$PWD\.android-sdk"
```

Doğrudan eşdeğer Gradle komutu:

```powershell
.\gradlew.bat --no-daemon `
    spotlessCheck `
    lintDebug `
    lintRelease `
    testDebugUnitTest `
    assembleDebug `
    assembleDebugAndroidTest `
    assembleRelease
```

APK konumu:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Android TV / Mi Stick kurulumu

**USB kablosu her zaman gerekli değildir.** Mi Stick'te **Kablosuz hata
ayıklama > Eşleme koduyla cihaz eşleştir** menüsü varsa bilgisayar ve Mi Stick
aynı güvenilir Wi-Fi ağındayken kablosuz ADB kullanın. Bu menü yoksa veri
taşıyabilen USB/OTG bağlantısını kullanın; eski cihazlarda USB üzerinden yetki
verdikten sonra kontrollü TCP 5555 geçişi gerekebilir. APK'yı USB belleğe kopyalayıp
dosya yöneticisiyle kurmak yalnız kimlik bilgisiz kurulum/açılış smoke testi
sağlar; Kamera Hesabı veya yayın kabulüne geçilmez. Instrumented test ve logcat
için ADB gerekir.

| Dosya | Kullanım |
| --- | --- |
| `app\build\outputs\apk\debug\app-debug.apk` | Mi Stick'e kurulacak imzalı test uygulaması |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | Gradle'ın yönettiği test paketi; elle açılmaz |
| `app\build\outputs\apk\release\app-release-unsigned.apk` | İmzasız doğrulama çıktısı; cihaza kurulmaz |

Geliştirici seçeneklerini açma, kablosuz eşleme, doğrudan USB, kontrollü eski
cihaz TCP 5555 akışı, USB bellekle kurulum, uygulamayı başlatma ve ekran ekran
C500/C510W kontrolü için
[fiziksel cihaz test planını](docs/MANUAL_TEST_PLAN_TR.md) baştan sona izleyin.
Testi başka bir Windows 11 bilgisayarda yapacaksanız aynı planın
[başka bilgisayar hazırlığı](docs/MANUAL_TEST_PLAN_TR.md#başka-bir-windows-11-bilgisayarda-test)
bölümünde tam test ve yalnız APK kurulumu yolları ayrı ayrı anlatılır.
ADB bağlantısı `device` durumuna geldikten sonra temel kurulum şöyledir:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
$apk = (Resolve-Path -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk').Path
$appId = 'io.github.serkankaracan.camgridtv.debug'
$deviceSerial = Read-Host 'Tek yetkili Mi Stick adb seri/adres değeri'
$deviceState = (& $adb -s $deviceSerial get-state 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $deviceState -cne 'device') {
    throw 'Mi Stick yetkili device durumunda değil.'
}
$installOutput = (& $adb -s $deviceSerial install -r $apk 2>&1 | Out-String).Trim()
$installExitCode = $LASTEXITCODE
$installOutput
if ($installExitCode -ne 0 -or $installOutput -notmatch '(?m)^Success$') {
    throw 'Debug APK kurulamadı; veri silmeden önce manuel plandaki hata bölümünü okuyun.'
}
$packagePath = (& $adb -s $deviceSerial shell pm path $appId 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $packagePath -notmatch '^package:') {
    throw 'Kurulan debug paketi doğrulanamadı.'
}
Remove-Variable -Name deviceState, installOutput, installExitCode, packagePath
```

**Herhangi bir Kamera Hesabı girmeden önce**, adb listesinde yalnız bir yetkili
Mi Stick varken cihaz testlerini çalıştırın:

```powershell
.\gradlew.bat --no-daemon connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { throw 'Mi Stick instrumented testleri başarısız oldu.' }
```

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` imza uyuşmazlığıdır. Aynı imza anahtarını
kullanmak tercih edilir; uygulamayı kaldırmak veya `pm clear` çalıştırmak kamera
seçimlerini, ayarları ve Keystore ile korunan Kamera Hesabı verilerini geri
alınamaz biçimde siler. Veri kaybını açıkça kabul etmeden bu komutları
çalıştırmayın. `unauthorized`, `offline`, çoklu cihaz, imza uyuşmazlığı, ölçümlü
temiz başlangıç ve test sonu bağlantı kapatma adımları manuel test planında
bulunur.

Uygulama yalnız `LEANBACK_LAUNCHER` içerir, landscape'dir ve dokunmatik ekran
gerektirmez. Android Studio kurulumu zorunlu değildir. Kamera bağlantı testinden
önce VLC, ffplay, NVR önizlemesi ve diğer RTSP istemcilerini kapatın; kameranın
eşzamanlı oturum sınırı aksi halde yanıltıcı bağlantı/decoder hatası doğurabilir.

## Kumanda kullanımı

- D-pad: listede, butonlarda ve kamera tile'larında gezinme
- Metin alanında D-pad odağı: gezinme modunda alanı seçer; ekran klavyesini açmaz
  ve yön tuşları imleç yerine komşu kontrollere geçer
- Metin alanında OK/Enter: düzenleme modunu ve ekran klavyesini açar; parola
  ekranda maskeli kalır
- Metin alanında Back veya klavyedeki Bitti: düzenlemeyi kapatıp gezinme moduna
  döner; kurulum ekranından çıkmaz
- OK/Enter: diğer kontrollerde seçim veya odaktaki kamerayı tam ekran açma
- Tam ekrandaki **Görüntü** kontrolünde Sağ veya OK: Güvenli → Sığdır → Doldur;
  Sol: aynı döngüde geri gitme
- Back: tam ekrandan aynı grid/odağa, gridden kamera kurulumuna, kurulumdan standart
  Android TV çıkış davranışına dönme; ekran klavyesi açıksa önce klavyeyi kapatma

## Portlar ve yayınlar

| Amaç | Varsayılan |
| --- | --- |
| ONVIF servis portu | TCP 2020 |
| RTSP servis portu | TCP 554 |
| Grid | `/stream2` |
| Tam ekran | Önce `/stream1`; oynatma başarısızlığında otomatik `/stream2` |
| RTP taşıması | RTSP üzerinden TCP'ye zorlanır |
| Ses | Devre dışı |

Kod model, IP veya kamera sayısını sabitlemez. 9'dan fazla cihaz için de genel grid
hesabı yapılır.

## Sorun giderme ve bilinen sınırlar

- Multicast keşfi; router multicast ayarı, misafir ağ ve AP isolation nedeniyle
  engellenebilir.
- Aynı anda oynatılabilen yayın sayısı TV donanımının H.264 decoder ve bellek
  kapasitesine bağlıdır. Uygulama keyfî bir sayı sınırı koymaz; başarısız tile diğer
  yayınları kapatmaz.
- Pil/solar ile çalışan bazı kamera modelleri sürekli RTSP/ONVIF desteklemeyebilir.
- Emülatörde multicast ve donanım decoder davranışı fiziksel TV'yi temsil etmez.
- Bu alpha kayıt, PTZ, ses, bildirim, mobil UI ve uzaktan izleme içermez.

Türkçe ayrıntılı yardım için [sorun giderme](docs/TROUBLESHOOTING_TR.md), gerçek
cihaz adımları için [manuel test planı](docs/MANUAL_TEST_PLAN_TR.md) dosyasına bakın.

## Test durumu

Güncel kalite kapısı debug/release lint'i, JVM testlerini, debug ve küçültülmüş
release APK'larını, ayrıca debug instrumented-test APK'sını kapsar. Son entegre
koşunun sayıları, APK özeti ve CI sonucu ancak gerçekten çalıştırıldıktan sonra
[docs/TEST_REPORT.md](docs/TEST_REPORT.md) içine yazılır; eski değerler yalnız
tarihsel kanıt olarak etiketlenir. İlk fiziksel koşu iki eşzamanlı duvar yayınını
doğruladı; C510W tam ekran ve görüntü oranı sorunlarını da ortaya çıkardı. Bu ilk
koşunun fotoğrafları düzeltme öncesi kanıttır. Daha sonra `e720069` çalışma ağacı
(`3a65112` uygulama kodu) için sağlanan `IMG_9754` fotoğrafı aynı karede
iki canlı duvar akışını ve görünür odak çerçevesini; `IMG_9755` ise C500 tam ekran
**Güvenli** görünümünü, kameranın zaman damgasını ve o sürümdeki sağ bilgi panelini
gösterdi. Bunlar tek karelik, sürüme bağlı gözlemlerdir; 15 dakikalık kararlılığı,
D-pad odak hareketini, üç modun döngüsünü veya C510W fallback'ini kanıtlamaz.
Fotoğraflardan sonra yapılan kamera adını sağ alta taşıma, çevreleyen paneli
kaldırma ve yinelenen **Canlı** durumunu tekilleştirme değişiklikleri için fiziksel
regresyon ile ölçümlü adb kabul senaryoları henüz tamamlanmadı. Fake/emülatör
testleri fiziksel kabul testi sayılmaz; ham kamera fotoğrafları özel ortam
ayrıntıları nedeniyle depoya eklenmez.

## Katkı ve lisans

Katkı kuralları [CONTRIBUTING.md](CONTRIBUTING.md), bağımlılık lisansları
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) içindedir. Proje Apache License
2.0 ile lisanslanır.

## English summary

KARACAM is a local-only Android TV viewer that discovers ONVIF cameras and
shows selected RTSP streams in a remote-friendly dynamic grid. It stores camera
credentials encrypted with an Android Keystore-backed AES/GCM key and contains no
backend or telemetry. Grid streams use `/stream2`; fullscreen first tries
`/stream1` and falls back to `/stream2` when the first playback attempt fails,
except for authentication or missing-local-network failures.
Embedded setup and wall feeds use `TextureView` so focus and status overlays stay
visible, while fullscreen uses `SurfaceView`. A live wall tile has one header
badge; non-live states remain over the video and the screen-level count is
aggregate. Fullscreen status and view-mode controls stay at the safe upper right
without an enclosing information panel, while the camera name is at the safe
bottom right. Its Safe 90% → Fit → Fill control always preserves source aspect
ratio; only Fill crops edges. Audio is disabled. Remote-first fields stay in
D-pad browse mode until OK opens editing, and setup uses one adaptive Verify →
Watch action with a reachable wall rescan when feeds fail. It is unofficial and
is not affiliated with or endorsed by TP-Link/Tapo.
