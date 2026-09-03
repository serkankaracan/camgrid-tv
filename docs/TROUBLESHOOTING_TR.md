# Sorun giderme

## Kamera bulunamıyor

- TV ve kameranın aynı güvenilir ev LAN/Wi-Fi ağında olduğunu doğrulayın.
- Misafir Wi-Fi, istemci/AP isolation ve multicast filtrelemesini kapatın.
- Android 17 ve sonrasında CamGrid TV için Yerel Ağ iznini etkinleştirin.
- Kamera uygulamasında ONVIF/RTSP desteğinin ve ayrı Kamera Hesabı'nın açık
  olduğundan emin olun; bulut hesabı veya Wi-Fi parolası kullanmayın.
- WS-Discovery keşfi UDP multicast 3702 kullanır. Kameranın keşif sonucunda
  bildirdiği ONVIF endpoint'i genellikle TCP 2020, RTSP ise TCP 554 kullanır. Bu
  portları internete açmayın.

PowerShell ile yalnızca kullanıcıya ait yerel yapılandırmadaki adresleri sınamak
için (adresleri repoya veya terminal geçmişine kaydetmeden):

```powershell
$cameraHost = Read-Host 'Kameranın yalnız yerel test adresi'
Test-NetConnection -ComputerName $cameraHost -Port 2020
Test-NetConnection -ComputerName $cameraHost -Port 554
Remove-Variable -Name cameraHost
```

## Kimlik bilgileri reddediliyor

Kamera uygulamasında cihaz için oluşturduğunuz Kamera Hesabı kullanıcı adı ve
parolasını uygulamanın güvenli formuna yeniden girin. Parolayı RTSP URL'sine,
PowerShell komutuna, issue'ya veya loga yazmayın. Hesabı VLC'nin açtığı kimlik
doğrulama penceresinde sınarsanız CamGrid TV testinden önce yayını durdurup VLC'yi
tamamen kapatın. Bazı kameralar eşzamanlı RTSP oturumlarını sınırlar.

## Görüntü yok veya sık kopuyor

- Grid düşük kaliteli `/stream2`, tam ekran `/stream1` kullanır.
- Uygulama güvenilirlik için RTP-over-TCP kullanır; RTSP yine de şifrelenmemiş
  olabilir. Yalnız güvenilir ve şifreli ev ağı kullanın.
- Bir tile decoder hatası veriyorsa Android TV donanımının eşzamanlı H.264 decoder
  sınırına ulaşılmış olabilir. Bu sınır modele göre değişir.
- VLC, ffplay, NVR önizlemesi, tarayıcı ve üretici uygulamasındaki canlı görüntü
  dahil diğer RTSP istemcilerini kapatın. Açık kalan oturumlar kamera veya decoder
  sınırını tüketebilir.
- Kamera veya TV ağdan çıktıysa uygulama çevrimiçi olmasını bekler ve kontrollü
  biçimde yeniden bağlanır; kimlik doğrulama hatasında sonsuz deneme yapmaz.

## ADB ile debug APK yükleme

Android SDK platform-tools kurulu ve cihazın ağ/USB hata ayıklaması açıkken önce
durumu görüntüleyin:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
& $adb start-server
& $adb devices -l
```

API 33+ cihaz **Kablosuz hata ayıklama > Eşleme koduyla cihaz eşleştir** menüsünü
sunuyorsa gerçek değerleri yalnız PowerShell isteminde girin; eşleme kodunu komut
argümanına eklemeyin:

```powershell
$pairTarget = Read-Host 'Mi Stick eşleme IP:port değeri'
& $adb pair $pairTarget
$connectTarget = Read-Host 'Mi Stick kablosuz hata ayıklama IP:port değeri'
& $adb connect $connectTarget
& $adb devices -l
Remove-Variable -Name pairTarget, connectTarget
```

`adb tcpip 5555` cihazda bir ağ dinleyicisi açar; yarım kalan bir komut dizisi
portu açık bırakabilir. Kablosuz eşleme menüsü olmayan eski TV/Mi Stick için
tek satırlık komut kullanmayın. USB/OTG transport doğrulamasını, RFC1918 adres
kontrolünü, hata halinde otomatik kapatmayı ve test sonu port probunu içeren
[kanonik TCP 5555 akışını](MANUAL_TEST_PLAN_TR.md#eski-tvmi-stick-için-usbotgden-tcp-5555e-geçiş)
aynen izleyin.

- Liste boşsa fiziksel test `BLOCKED` olur.
- `unauthorized` için TV'deki RSA istemini kabul edin; istem yoksa hata ayıklama
  yetkilerini TV'den iptal edip bağlantıyı yeniden kurun.
- `offline` için kablosuz bağlantıyı ayırıp yeniden bağlayın; gerekirse adb
  sunucusunu ve cihazı yeniden başlatın.
- Birden çok cihaz varsa diğer emülatör/USB/ağ taşımalarını ayırın.
  `connectedDebugAndroidTest` yalnız hedef Mi Stick'e ait tek `device` satırı
  varken çalıştırılmalıdır.

Tek yetkili Mi Stick kaldığında APK'yı yükleyin ve herhangi bir Kamera Hesabı
girmeden önce instrumented testleri çalıştırın:

```powershell
$deviceSerial = Read-Host 'Tek yetkili Mi Stick adb seri/adres değeri'
& $adb -s $deviceSerial install -r '.\app\build\outputs\apk\debug\app-debug.apk'
.\gradlew.bat --no-daemon connectedDebugAndroidTest
```

`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, cihazdaki paketin farklı bir anahtarla
imzalandığını gösterir. Mümkünse aynı anahtarla derleyin. `adb uninstall` ve
`adb shell pm clear` seçimleri, ayarları ve Keystore ile korunan Kamera Hesabı
verilerini geri alınamaz biçimde siler; veri kaybını açıkça kabul etmeden
çalıştırmayın. USB/OTG → `tcpip 5555` bağlantısını test sonunda kapatma, ölçümlü
temiz başlangıç ve force-stop/yeniden açma komutları için
[manuel test planını](MANUAL_TEST_PLAN_TR.md) izleyin.

`adb logcat` çıktısını repoya eklemeyin. Paylaşım öncesinde kullanıcı adlarını,
parolaları, tam RTSP URI'lerini ve ağ/cihaz tanımlayıcılarını kaldırın.
