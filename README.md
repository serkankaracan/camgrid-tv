# CamGrid TV

CamGrid TV, aynı güvenilir yerel ağdaki ONVIF/RTSP kameraları otomatik bulan ve
seçilen canlı yayınları Android TV / Google TV üzerinde tek bir dinamik ızgarada
gösteren yerel-öncelikli bir uygulamadır. Backend, bulut hesabı, telemetri ve
uzaktan erişim kullanmaz.

> CamGrid TV bağımsız ve resmî olmayan bir açık kaynak projesidir. TP-Link veya
> Tapo tarafından geliştirilmez, desteklenmez ya da onaylanmaz. Üründe bu
> markalara ait logo veya görsel varlık kullanılmaz.

Hedef kabul modelleri Tapo C500 ve C510W'dir. Fiziksel doğrulama durumu
[test raporunda](docs/TEST_REPORT.md) açıkça belirtilir; model adı doğrulanmış test
iddiası anlamına gelmez.

## Nasıl çalışır?

```text
Android TV                  Güvenilir yerel ağ                 Kameralar
CamGrid TV  -- WS-Discovery 239.255.255.250:3702 -->  ONVIF endpoint'leri
            <-- ProbeMatch / UUID / XAddr ----------
            -- RTSP/TCP :554 /stream2 ------------->  Grid (düşük kalite)
            -- RTSP/TCP :554 /stream1 ------------->  Tam ekran (yüksek kalite)
```

Uygulama açılışta ONVIF WS-Discovery taraması yapar, tekrar gelen cevapları endpoint
UUID/XAddr/adres sırasıyla tekilleştirir ve aynı UUID yeni bir IP'de bulunursa
kaydı günceller. Bilinmeyen üreticinin uyumlu ONVIF cihazı reddedilmez. Kullanıcı
kameraları ve adlarını seçer, ayrı Kamera Hesabı bilgilerini girer ve bağlantıyı
sınar. Sonraki açılışta seçim korunur.

Wall tüm kameraları kaydırmasız ekrana sığdırır. D-pad odağı tile'lar arasında
taşır; OK odaktaki kamerayı tam ekran açar, Back önceki grid odağını geri yükler.
Görüntüler kırpılmaz (`FIT`/letterbox). Ses renderer'ı kapalıdır.

## Gizlilik ve güvenlik

- Kamera trafiği ve ayarlar cihazdan/LAN'dan çıkmaz; telemetry, analytics, reklam
  ve crash-reporting SDK'sı yoktur.
- Kamera seçimi ve secret olmayan metadata DataStore'dadır. Kullanıcı adı/parola,
  her yazımda yeni IV kullanan AES/GCM ile şifrelenir; anahtar Android Keystore'da
  dışa aktarılamaz. Keystore kaybında plaintext fallback yapılmaz.
- Android backup kapalıdır. Credential içeren URI'lerin normal metin gösterimi
  redacted'dır; ham URI yalnız Media3 sınırında açılır.
- RTSP şifrelenmemiş olabilir. Uygulamayı yalnız güvenilir, şifreli ev LAN/Wi-Fi
  ağında kullanın. 554 veya 2020 portlarını internete açmayın. CamGrid TV uzaktan
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
4. Bilgileri yalnız CamGrid TV'nin kendi güvenli formuna girin; sohbete, terminale
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

TV'de geliştirici seçeneklerini ve desteklenen USB/ağ hata ayıklama yöntemini
etkinleştirin. Proje-yerel platform-tools yolunu kullanın:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
& $adb start-server
& $adb devices -l
```

API 33+ Mi Stick **Kablosuz hata ayıklama > Eşleme koduyla cihaz eşleştir**
menüsünü sunuyorsa eşleme ve bağlantı adreslerini ayrı ayrı, yalnız istemde
girin:

```powershell
$pairTarget = Read-Host 'Mi Stick eşleme IP:port değeri'
& $adb pair $pairTarget
$connectTarget = Read-Host 'Mi Stick kablosuz hata ayıklama IP:port değeri'
& $adb connect $connectTarget
& $adb devices -l
Remove-Variable -Name pairTarget, connectTarget
```

Eşleme kodunu komut argümanına yazmayın; adb istediğinde girin. `adb tcpip 5555`
cihazda bir ağ dinleyicisi açar ve yarım kalan bir komut dizisi bu portu açık
bırakabilir. Kablosuz eşleme menüsü olmayan eski TV/Mi Stick için tek satırlık
komut kullanmayın; USB/OTG doğrulamasını, RFC1918 adres kontrolünü ve hata halinde
otomatik kapatmayı içeren
[kanonik TCP 5555 akışını](docs/MANUAL_TEST_PLAN_TR.md#eski-tvmi-stick-için-usbotgden-tcp-5555e-geçiş)
aynen izleyin.

Boş liste, `unauthorized` veya `offline` durumunda ilerlemeyin. Birden çok cihaz
varsa diğerlerini ayırın; `connectedDebugAndroidTest` hedefleyebileceği tüm bağlı
cihazlarda çalışır. Yalnız Mi Stick'e ait tek bir `device` satırı kaldığında
güncel APK'yı kurup, **Kamera Hesabı girmeden önce** cihaz testlerini çalıştırın:

```powershell
$deviceSerial = Read-Host 'Tek yetkili Mi Stick adb seri/adres değeri'
& $adb -s $deviceSerial install -r '.\app\build\outputs\apk\debug\app-debug.apk'
.\gradlew.bat --no-daemon connectedDebugAndroidTest
```

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` imza uyuşmazlığıdır. Aynı imza anahtarını
kullanmak tercih edilir; uygulamayı kaldırmak veya `pm clear` çalıştırmak kamera
seçimlerini, ayarları ve Keystore ile korunan Kamera Hesabı verilerini geri
alınamaz biçimde siler. Veri kaybını açıkça kabul etmeden bu komutları
çalıştırmayın. `unauthorized`, `offline`, çoklu cihaz, imza uyuşmazlığı, ölçümlü
temiz başlangıç ve test sonu TCP 5555 kapatma adımları
[manuel test planında](docs/MANUAL_TEST_PLAN_TR.md) bulunur.

Uygulama yalnız `LEANBACK_LAUNCHER` içerir, landscape'dir ve dokunmatik ekran
gerektirmez. Android Studio kurulumu zorunlu değildir. Kamera bağlantı testinden
önce VLC, ffplay, NVR önizlemesi ve diğer RTSP istemcilerini kapatın; kameranın
eşzamanlı oturum sınırı aksi halde yanıltıcı bağlantı/decoder hatası doğurabilir.

## Kumanda kullanımı

- D-pad: listede, butonlarda ve kamera tile'larında gezinme
- OK/Enter: seçim veya odaktaki kamerayı tam ekran açma
- Back: tam ekrandan aynı grid/odağa, gridden kamera kurulumuna, kurulumdan standart
  Android TV çıkış davranışına dönme

## Portlar ve yayınlar

| Amaç | Varsayılan |
| --- | --- |
| ONVIF servis portu | TCP 2020 |
| RTSP servis portu | TCP 554 |
| Grid | `/stream2` |
| Tam ekran | `/stream1` |
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
tarihsel kanıt olarak etiketlenir. Fiziksel C500/C510W yayınları, Mi Stick
instrumented testleri ve kabul senaryoları donanım erişimi olmadığı için
**BLOCKED** durumundadır. Fake/emülatör testleri fiziksel kabul testi sayılmaz.

## Katkı ve lisans

Katkı kuralları [CONTRIBUTING.md](CONTRIBUTING.md), bağımlılık lisansları
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) içindedir. Proje Apache License
2.0 ile lisanslanır.

## English summary

CamGrid TV is a local-only Android TV viewer that discovers ONVIF cameras and
shows selected RTSP streams in a remote-friendly dynamic grid. It stores camera
credentials encrypted with an Android Keystore-backed AES/GCM key and contains no
backend or telemetry. Grid streams use `/stream2`, fullscreen uses `/stream1`, and
audio is disabled. It is unofficial and is not affiliated with or endorsed by
TP-Link/Tapo.
