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

USB zorunlu değildir: Mi Stick standart kablosuz eşleme menüsünü sunuyorsa aynı
güvenilir Wi-Fi üzerinden ADB kullanılabilir. Doğrudan USB için veri taşıyan
kablo/OTG gerekir; yalnız APK'yı USB belleğe kopyalamak ADB bağlantısı sağlamaz
ve sadece kimlik bilgisiz kurulum/açılış smoke testine izin verir. Bu yolla Kamera
Hesabı veya yayın kabulüne geçmeyin.

Bağlantı yöntemini seçmek ve doğru debug APK'yı doğrulayıp kurmak için
[fiziksel cihaz test planındaki hızlı başlangıcı](MANUAL_TEST_PLAN_TR.md)
izleyin. Aşağıdaki komutları depo kökünde çalıştırın; buradaki durumlar hata
ayıklama içindir:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
& $adb start-server
& $adb devices -l
```

- Liste boşsa fiziksel test `BLOCKED` olur.
- Doğrudan USB/OTG bağlı olduğu hâlde liste boşsa veri kablosunu ve güç geçişli
  OTG düzenini kontrol edin; Windows Aygıt Yöneticisi cihazı tanımıyorsa modelle
  uyumlu sürücü için [Android'ın resmî OEM USB sürücüsü
  rehberini](https://developer.android.com/studio/run/oem-usb) kullanın. Rastgele
  sürücü paketi kurmayın.
- `unauthorized` için TV'deki RSA istemini kabul edin; istem yoksa hata ayıklama
  yetkilerini TV'den iptal edip bağlantıyı yeniden kurun.
- `offline` için kablosuz bağlantıyı ayırıp yeniden bağlayın; gerekirse adb
  sunucusunu ve cihazı yeniden başlatın.
- Birden çok cihaz varsa diğer emülatör/USB/ağ taşımalarını ayırın.
  `connectedDebugAndroidTest` yalnız hedef Mi Stick'e ait tek `device` satırı
  varken çalıştırılmalıdır.
- `adb pair` başarılı fakat cihaz görünmüyorsa eşleme portu yerine ana Kablosuz
  hata ayıklama ekranındaki bağlantı `IP:port` değerini `adb connect` için
  kullanın. İki port aynı olmak zorunda değildir.
- `adb tcpip 5555` yarıda kalırsa ağ dinleyicisi açık kalabilir. Eski cihazda
  yalnız [korumalı TCP 5555 akışını](MANUAL_TEST_PLAN_TR.md#legacytcp5555-eski-tvmi-stickte-usbotgden-tcp-5555e-geçiş)
  kullanın ve test sonu port probunu atlamayın.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, aynı debug paketinin farklı imzayla
  kurulu olduğunu gösterir. Önce aynı debug anahtarını kullanmayı deneyin.
- `INSTALL_FAILED_NO_MATCHING_ABIS` veya parse hatası alırsanız dosyanın gerçekten
  `app-debug.apk` olduğunu ve hash/aktarımı bozulmadığını doğrulayın; test APK'sını
  veya imzasız release APK'sını uygulama olarak kurmayın.

`adb uninstall` ve `adb shell pm clear` seçimleri, ayarları ve Android Keystore
ile korunan Kamera Hesabı verilerini geri alınamaz biçimde siler; manuel plandaki
açık onay adımı olmadan çalıştırmayın.

`adb logcat` çıktısını repoya eklemeyin. Paylaşım öncesinde kullanıcı adlarını,
parolaları, tam RTSP URI'lerini ve ağ/cihaz tanımlayıcılarını kaldırın.
