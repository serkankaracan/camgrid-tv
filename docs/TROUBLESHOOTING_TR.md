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
PowerShell komutuna, issue'ya veya loga yazmayın. Önce aynı hesabı VLC'nin açtığı
kimlik doğrulama penceresinde sınayabilirsiniz.

## Görüntü yok veya sık kopuyor

- Grid düşük kaliteli `/stream2`, tam ekran `/stream1` kullanır.
- Uygulama güvenilirlik için RTP-over-TCP kullanır; RTSP yine de şifrelenmemiş
  olabilir. Yalnız güvenilir ve şifreli ev ağı kullanın.
- Bir tile decoder hatası veriyorsa Android TV donanımının eşzamanlı H.264 decoder
  sınırına ulaşılmış olabilir. Bu sınır modele göre değişir.
- Kamera veya TV ağdan çıktıysa uygulama çevrimiçi olmasını bekler ve kontrollü
  biçimde yeniden bağlanır; kimlik doğrulama hatasında sonsuz deneme yapmaz.

## ADB ile debug APK yükleme

Android SDK platform-tools kurulu ve cihazın ağ/USB hata ayıklaması açıkken:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
& $adb devices -l
& $adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

`adb logcat` çıktısını repoya eklemeyin. Paylaşım öncesinde kullanıcı adlarını,
parolaları, tam RTSP URI'lerini ve ağ/cihaz tanımlayıcılarını kaldırın.
