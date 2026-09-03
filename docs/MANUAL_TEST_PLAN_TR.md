# Fiziksel cihaz test planı

## Mevcut koşunun durumu

Bu plandaki gerçek C500, C510W ve Mi Stick senaryolarının tamamı **BLOCKED**
durumundadır. Fiziksel kameralara, Mi Stick'e, emülatöre veya bağlı bir adb
cihazına erişilemedi. Fake, JVM, derleme veya port erişimi sonucu fiziksel
doğrulama yerine kullanılmaz.

Akış Windows 11 ve PowerShell içindir. Proje Android SDK komut satırı araçlarını
kullanır; Android Studio gerekli değildir ve kurulu olduğu varsayılmaz.

## Zorunlu yürütme sırası

1. Kalite kapısını çalıştırın ve debug, debug Android-test ve release APK
   derlemelerinin tamamlandığını doğrulayın.
2. Yalnız hedef Mi Stick'i adb'de yetkili `device` durumuna getirin.
3. Güncel debug APK'yı yükleyin. İmza uyuşmazlığında veri kaybı uyarısını okuyup
   açıkça onaylamadan uygulamayı kaldırmayın.
4. **Herhangi bir Kamera Hesabı bilgisi girmeden önce**, bağlı fiziksel Mi Stick
   üzerinde `connectedDebugAndroidTest` çalıştırın. Bu adım PASS değilse kimlik
   bilgili senaryolara geçmeyin.
5. Yalnız veri kaybını açıkça kabul ettiyseniz uygulama verisini temizleyip
   ölçümlü temiz başlangıç senaryosunu çalıştırın.
6. VLC, ffplay, NVR önizlemesi, tarayıcı veya üretici uygulamasındaki canlı
   görüntü gibi diğer RTSP istemcilerini kapatın.
7. Aşağıdaki fiziksel kamera senaryolarını sırayla yürütün.

Her senaryo için `docs/TEST_REPORT.md` içinde yalnız `PASS`, `FAIL`, `BLOCKED`
veya `NOT RUN` ile kısa ve redakte edilmiş kanıt kaydedin.

## Güvenli kanıt kuralları

- Kullanıcı adı, parola, tam RTSP URI, ev IP'si, MAC adresi, cihaz seri numarası,
  eşleme kodu ve ham adb/VLC logu commit'e, issue'ya, sohbete veya ekran
  görüntüsüne girmez.
- Kamera Hesabı, Tapo bulut hesabı veya Wi-Fi parolası değildir. Hesap her kamera
  için Tapo uygulamasında yerel yayın amacıyla oluşturulur ve yalnız CamGrid
  TV'nin Kamera Hesabı alanlarına girilir.
- Adres ve adb seri değeri gerekiyorsa PowerShell'de `Read-Host` ile alın; gerçek
  değeri komut geçmişine veya rapora yazmayın.
- Yalnız biçim örneği (mevcut sonuç değildir):
  `C500 stream2: PASS — 15 dakika kesintisiz, 0 crash`. Gerçek endpoint veya
  hesap bilgisi eklemeyin.

## Önkoşullar

1. C500, C510W, Mi Stick ve geliştirme bilgisayarı aynı güvenilir yerel ağdadır.
2. Her kamerada ONVIF/RTSP ve ayrı Kamera Hesabı etkinleştirilmiştir.
3. Mi Stick'te geliştirici seçenekleri ile desteklenen USB veya ağ hata ayıklama
   yöntemi açılmıştır.
4. Kalite kapısı güncel çalışma ağacı için tamamlanmıştır.
5. Test sırasında kamera portları internete yönlendirilmez; uygulamaya public IP
   veya internet host adı girilmez.
6. Başka bir RTSP istemcisi aynı yayını kullanmıyordur. Bazı kameralar eşzamanlı
   oturum sayısını sınırlar; açık kalan başka bir istemci yanlış bağlantı veya
   decoder hatası üretebilir.

## PowerShell ile adb hazırlığı

Depo kökünde proje-yerel platform-tools yolunu ve debug paketini tanımlayın:

```powershell
$adb = (Resolve-Path -LiteralPath '.\.android-sdk\platform-tools\adb.exe').Path
$apk = (Resolve-Path -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk').Path
$appId = 'io.github.serkankaracan.camgridtv.debug'
$legacyTcpMode = $false
& $adb start-server
& $adb devices -l
```

Liste boşsa fiziksel testlere başlamayın ve sonucu `BLOCKED` kaydedin. Bir cihaz
satırı tam olarak `device` durumuna gelmeden yükleme veya test çalıştırmayın.

### API 33+ kablosuz eşleme

Mi Stick API 33+ üzerinde **Kablosuz hata ayıklama > Eşleme koduyla cihaz
eşleştir** menüsünü sunuyorsa, ekrandaki eşleme adresi/portu ile bağlantı
adresi/portunun farklı olabileceğini dikkate alın. Değerleri yalnız istemde
girin:

```powershell
$pairTarget = Read-Host 'Mi Stick eşleme IP:port değeri'
& $adb pair $pairTarget
$connectTarget = Read-Host 'Mi Stick kablosuz hata ayıklama IP:port değeri'
& $adb connect $connectTarget
& $adb devices -l
Remove-Variable -Name pairTarget, connectTarget
```

Eşleme kodunu `adb pair` komutuna argüman olarak yazmayın; adb istediğinde
etkileşimli olarak girin. TV'de gösterilen RSA/eşleme isteğini yalnız test
bilgisayarının parmak izi doğruysa kabul edin.

### Eski TV/Mi Stick için USB/OTG'den TCP 5555'e geçiş

Kablosuz eşleme menüsü olmayan cihazı önce veri taşıyabilen USB/OTG bağlantısıyla
bağlayın, TV'deki RSA yetki istemini kabul edin ve USB satırının `device`
olduğunu doğrulayın. Sonra güvenilir LAN içinde şu akışı kullanın:

```powershell
& $adb devices -l
$usbSerial = Read-Host 'Yetkili USB cihazının adb seri değeri'
$tvHost = Read-Host 'Mi Stick yerel IP adresi'
$parsedTvHost = $null
$hostIsValid = [System.Net.IPAddress]::TryParse($tvHost, [ref]$parsedTvHost) -and
    $parsedTvHost.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork -and
    $parsedTvHost.ToString() -ceq $tvHost
if (-not $hostIsValid) { throw 'Mi Stick adresi kanonik bir IPv4 değeri olmalıdır.' }
$hostOctets = $parsedTvHost.GetAddressBytes()
$hostIsPrivate = $hostOctets[0] -eq 10 -or
    ($hostOctets[0] -eq 172 -and $hostOctets[1] -ge 16 -and $hostOctets[1] -le 31) -or
    ($hostOctets[0] -eq 192 -and $hostOctets[1] -eq 168)
if (-not $hostIsPrivate) { throw 'Mi Stick adresi RFC1918 yerel ağda olmalıdır.' }
$tcpTarget = '{0}:5555' -f $tvHost
$usbState = (& $adb -s $usbSerial get-state 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $usbState -cne 'device') {
    throw 'Seçilen USB transportu yetkili device durumunda değil.'
}

try {
    # tcpip komutundan önce temizleme yükümlülüğünü kaydet.
    $legacyTcpMode = $true
    $tcpipOutput = & $adb -s $usbSerial tcpip 5555
    $tcpipExitCode = $LASTEXITCODE
    $tcpipOutput
    if ($tcpipExitCode -ne 0) { throw 'adbd TCP 5555 moduna alınamadı.' }

    Start-Sleep -Seconds 2
    $connectOutput = & $adb connect $tcpTarget
    $connectExitCode = $LASTEXITCODE
    $connectOutput
    if ($connectExitCode -ne 0) { throw 'TCP 5555 transportuna bağlanılamadı.' }

    $tcpState = (& $adb -s $tcpTarget get-state 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $tcpState -cne 'device') {
        throw 'TCP 5555 transportu yetkili device durumuna gelmedi.'
    }
} catch {
    $usbModeRestored = $false
    foreach ($cleanupTarget in @($tcpTarget, $usbSerial) | Select-Object -Unique) {
        $restoreOutput = & $adb -s $cleanupTarget usb 2>&1
        if ($LASTEXITCODE -eq 0) {
            $restoreOutput
            $usbModeRestored = $true
            break
        }
    }
    & $adb disconnect $tcpTarget | Out-Null
    if ($usbModeRestored) {
        $legacyTcpMode = $false
    } else {
        Write-Warning 'TCP 5555 otomatik kapatılamadı; cihazı yeniden başlatın veya ağ hata ayıklamasını kapatın.'
    }
    throw 'TCP 5555 hazırlığı güvenli biçimde tamamlanamadı.'
}
Remove-Variable -Name parsedTvHost, hostIsValid, hostOctets, hostIsPrivate, usbState,
    tcpipOutput, tcpipExitCode, connectOutput, connectExitCode, tcpState
```

`get-state` çıktısı `device` olduktan sonra USB/OTG bağlantısını çıkarın ve
`& $adb devices -l` ile yalnız ağ taşımasının kaldığını doğrulayın. TCP 5555
yalnız güvenilir yerel ağda kullanılmalıdır. `$usbSerial`, `$tcpTarget` ve
`$legacyTcpMode` değişkenlerini test sonuna kadar koruyun; aşağıdaki test sonu
adımı adbd'yi yeniden USB moduna alıp TCP 5555 dinleyicisini kapatır.

### `unauthorized`, `offline` ve birden çok cihaz

- `unauthorized`: TV ekranındaki RSA istemini kontrol edin. İstem yoksa TV'de USB
  hata ayıklama yetkilerini iptal edip hata ayıklamayı kapatıp açın, kabloyu veya
  ağ bağlantısını yeniden kurun. Durum `device` olmadan devam etmeyin.
- `offline`: Kablosuz taşıma için `adb disconnect` uygulayıp yeniden bağlanın;
  gerekirse `adb kill-server`, `adb start-server` ve cihaz yeniden başlatmasını
  deneyin. USB için veri kablosunu/OTG adaptörünü kontrol edin. Düzelmezse ilgili
  kontrolleri `BLOCKED` kaydedin.
- Birden çok cihaz: `connectedDebugAndroidTest` bağlı uygun cihazların tamamını
  hedefleyebilir. Emülatörleri durdurun, diğer USB cihazlarını çıkarın ve diğer
  ağ taşımalarını `adb disconnect` ile ayırın. Komutu ancak listede hedef Mi
  Stick'e ait tek bir `device` satırı kaldığında çalıştırın.

Tek cihaz kaldıktan sonra seri/adres değerini istemde alın ve tüm manuel adb
komutlarında `-s` kullanın:

```powershell
$deviceSerial = Read-Host 'Tek yetkili Mi Stick adb seri/adres değeri'
& $adb -s $deviceSerial get-state
```

Çıktı tam olarak `device` değilse devam etmeyin. Seri/adres değerini test
raporuna kopyalamayın.

## Derleme, kurulum ve cihaz testleri

Önce güncel kalite kapısını çalıştırın:

```powershell
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath "$PWD\.android-sdk"
```

Güncel debug APK'yı kurun:

```powershell
& $adb -s $deviceSerial install -r $apk
```

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` veya başka bir imza uyuşmazlığı, cihazdaki
aynı paket kimliğinin farklı bir anahtarla imzalandığını gösterir. **Körlemesine
`uninstall` çalıştırmayın.** Uygulamayı kaldırmak; seçilen kameraları, özel adları,
ayarları ve Android Keystore ile korunan Kamera Hesabı verilerini geri alınamaz
biçimde siler. Mümkünse aynı imza anahtarıyla yeniden derleyin. Yalnız bu veri
kaybını açıkça kabul ediyorsanız ve gizli olmayan kurulumu yeniden yapabilecek
durumdaysanız şu komutları kullanın:

```powershell
$eraseApproval = Read-Host 'Tüm debug uygulama verisini silmek için SIL yazın'
if ($eraseApproval -cne 'SIL') { throw 'Kaldırma kullanıcı tarafından iptal edildi.' }
& $adb -s $deviceSerial uninstall $appId
if ($LASTEXITCODE -ne 0) { throw 'Uygulama kaldırılamadı.' }
& $adb -s $deviceSerial install $apk
if ($LASTEXITCODE -ne 0) { throw 'APK kurulamadı.' }
Remove-Variable -Name eraseApproval
```

### Kimlik bilgilerinden önce zorunlu instrumented test

Mi Stick açık ve kilitsizken, listede yalnız bu cihazın `device` durumunda
olduğunu bir kez daha doğrulayın. Henüz hiçbir Kamera Hesabı bilgisi girmeden:

```powershell
& $adb devices -l
.\gradlew.bat --no-daemon connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { throw 'Mi Stick instrumented testleri başarısız oldu.' }
```

Komut, bağlı fiziksel Mi Stick üzerinde tamamlanıp exit code 0 vermelidir. Test
sayısını ve sonuç dosyasını güncel koşudan kaydedin; bir derleme başarısını cihaz
testi yerine yazmayın. Altyapı/bağlantı sorunu varsa `BLOCKED`, test assertion'ı
başarısızsa `FAIL` kaydedin ve kimlik bilgili senaryolara geçmeyin.

### Ölçümlü temiz başlangıç

`pm clear` da kaldırma gibi **tüm uygulama verilerini ve Kamera Hesabı
şifrelerini geri alınamaz biçimde siler**. Bu adımı instrumented testten sonra ve
ilk Kamera Hesabı girişinden önce uygulayın. Veri kaybını açıkça onaylayın:

```powershell
$clearApproval = Read-Host 'Tüm debug uygulama verisini silmek için TEMIZLE yazın'
if ($clearApproval -cne 'TEMIZLE') { throw 'Temiz başlangıç kullanıcı tarafından iptal edildi.' }
$clearResult = & $adb -s $deviceSerial shell pm clear $appId
if ($LASTEXITCODE -ne 0 -or $clearResult -notmatch 'Success') {
    throw 'Uygulama verisi temizlenemedi.'
}
& $adb -s $deviceSerial shell am force-stop $appId
$stoppedPid = (& $adb -s $deviceSerial shell pidof $appId | Out-String).Trim()
if ($stoppedPid) { throw 'Uygulama force-stop sonrasında çalışmaya devam ediyor.' }
$cleanStartTimer = [System.Diagnostics.Stopwatch]::StartNew()
$launchOutput = & $adb -s $deviceSerial shell am start -W `
    -a android.intent.action.MAIN `
    -c android.intent.category.LEANBACK_LAUNCHER `
    -p $appId
$launchExitCode = $LASTEXITCODE
$launchOutput
if ($launchExitCode -ne 0) { throw 'Uygulama başlatılamadı.' }
Read-Host 'İlk kurulum/keşif ekranı kararlı olduğunda Enter'
$cleanStartTimer.Stop()
'Temiz başlangıç gözlem süresi: {0:N1} saniye' -f $cleanStartTimer.Elapsed.TotalSeconds
Remove-Variable -Name clearApproval, clearResult, stoppedPid, cleanStartTimer, launchOutput, launchExitCode
```

PASS için `pm clear` çıktısı `Success`, force-stop sonrası PID boş ve launch exit
code 0 olmalıdır. Rapora `am start -W` içindeki `TotalTime`, ekrana kadar ölçülen
süre, ilk ekranın adı ve taramanın sonuç/boş/hata durumuna ulaşma süresini yazın;
seri, IP veya credential yazmayın.

## Gerçek kabul senaryoları

### 1. C500 ve C510W otomatik keşfi

- Önkoşul: İki kamera açık, aynı LAN'da ve ONVIF etkin; temiz başlangıç PASS.
- Adım: Yerel ağ iznini verin, PowerShell kronometresiyle taramayı başlatın ve
  terminal duruma ulaştığında süreyi durdurun.
- Beklenen: C500 ve C510W birer kez, gerçek IP rapora yazılmadan ayırt edilebilir
  adlarla görünür; yinelenen ProbeMatch satırları ikinci cihaz oluşturmaz.
- Kanıt: Model başına görünen kayıt sayısı, izin ekranına kadar geçen süre ve
  taramanın tamamlanma süresi; adresleri redakte edin.

### 2. C500 Kamera Hesabı ve `/stream2`

- Önkoşul: C500 için doğrulanmış yerel Kamera Hesabı mevcut;
  `connectedDebugAndroidTest` PASS; diğer RTSP istemcileri kapalı.
- Adım: Yalnız uygulama içinde hesabı girin ve C500 bağlantısını test edin.
- Beklenen: Düşük kaliteli H.264 `/stream2` görüntüsü açılır; hesap bilgisi UI
  veya logda düz metin görünmez.
- Kanıt: `C500 stream2: PASS/FAIL`, ilk kareye kadar süre ve güvenli hata özeti.

### 3. C510W Kamera Hesabı ve `/stream2`

- Önkoşul: C510W için doğrulanmış yerel Kamera Hesabı mevcut;
  `connectedDebugAndroidTest` PASS; diğer RTSP istemcileri kapalı.
- Adım: Yalnız uygulama içinde hesabı girin ve C510W bağlantısını test edin.
- Beklenen: Düşük kaliteli H.264 `/stream2` görüntüsü açılır; hesap bilgisi UI
  veya logda düz metin görünmez.
- Kanıt: `C510W stream2: PASS/FAIL`, ilk kareye kadar süre ve güvenli hata özeti.

### 4. İki yayınlı duvar dayanıklılığı

- Önkoşul: Senaryo 2 ve 3 ayrı ayrı PASS; diğer RTSP istemcileri kapalı.
- Adım: İki kamerayı seçin ve iki `/stream2` tile'ını en az 15 dakika açık tutun.
- Beklenen: Crash, sürekli yeniden başlatılan player veya diğer tile'ı kapatan
  tekil hata oluşmaz.
- Kanıt: Başlangıç/bitiş saati, tam süre, crash sayısı ve her tile'ın son durumu.

### 5. Fiziksel D-pad odağı

- Önkoşul: İki tile duvarda görünür ve Mi Stick kumandası bağlı.
- Adım: Yön tuşlarıyla iki tile ve duvar eylemleri arasında dolaşın.
- Beklenen: Odak çerçevesi her zaman görünür, öngörülebilir ve dokunma gerektirmez.
- Kanıt: İzlenen odak sırası; seri numarası içermeyen fotoğraf isteğe bağlıdır.

### 6. C500 `/stream1` tam ekran

- Önkoşul: C500 tile'ı canlı ve odakta.
- Adım: Kronometreyi başlatıp OK ile tam ekranı açın; ilk karede durdurun ve
  Back ile dönün.
- Beklenen: C500 yüksek kaliteli `/stream1` ile tek player olarak açılır; Back
  aynı C500 tile odağını duvarda geri yükler.
- Kanıt: `C500 fullscreen: PASS/FAIL`, ilk kare süresi ve geri dönen odak adı.

### 7. C510W `/stream1` tam ekran

- Önkoşul: C510W tile'ı canlı ve odakta.
- Adım: Kronometreyi başlatıp OK ile tam ekranı açın; ilk karede durdurun ve
  Back ile dönün.
- Beklenen: C510W yüksek kaliteli `/stream1` ile tek player olarak açılır; Back
  aynı C510W tile odağını duvarda geri yükler.
- Kanıt: `C510W fullscreen: PASS/FAIL`, ilk kare süresi ve geri dönen odak adı.

### 8. Arka plan ve ön plan toparlanması

- Önkoşul: İki yayınlı duvar canlı.
- Adım: Home ile uygulamayı arka plana alın, 30 saniye bekleyin ve launcher'dan
  yeniden açın. İlk iki tile kararlı duruma geldiğinde kronometreyi durdurun.
- Beklenen: Arka planda player'lar serbest bırakılır; dönüşte yalnız istenen iki
  player yeniden kurulur, kopya ses/video veya crash oluşmaz.
- Kanıt: Arka plan süresi, dönüşten kararlı duruma kadar süre, önce/sonra tile
  durumları ve gözlenen player sayısı.

### 9. Yerel ağ kaybı ve geri dönüş

- Önkoşul: İki yayın canlı; router ve kameralar güvenli biçimde erişilebilir.
- Adım: Mi Stick Wi-Fi bağlantısını 30 saniye kesin, yeniden bağlayın ve iki
  tile'ın terminal durumlarına kadar geçen süreyi ölçün.
- Beklenen: Tile'lar offline olur, sıkı retry döngüsü oluşmaz ve ağ dönünce
  kontrollü biçimde yeniden bağlanır.
- Kanıt: Kesinti süresi, toparlanma süresi ve crash/retry özeti.

### 10. Yanlış parola davranışı

- Önkoşul: Doğru hesabın çalıştığı daha önce kanıtlanmış.
- Adım: Geçici olarak yanlış parola girip yalnız bir kamerayı test edin; ardından
  doğru bilgiyi uygulama içinde yeniden girin.
- Beklenen: Güvenli kimlik doğrulama hatası gösterilir, sonsuz otomatik retry
  yapılmaz ve diğer tile etkilenmez.
- Kanıt: Hata sınıfı, retry'nin durduğu süre ve diğer tile durumu; yanlış veya
  doğru parolayı yazmayın.

### 11. Force-stop, yeniden başlatma ve seçim kalıcılığı

- Önkoşul: İki kamera seçilmiş, güvenli profiller kaydedilmiş ve duvar kararlı.
- Adım: Önce gizli olmayan kamera adlarını/sırasını not edin. Ardından gerçek bir
  süreç sonlandırma ve yeniden başlatmayı ölçün:

  ```powershell
  $beforePid = (& $adb -s $deviceSerial shell pidof $appId | Out-String).Trim()
  & $adb -s $deviceSerial shell am force-stop $appId
  Start-Sleep -Seconds 2
  $stoppedPid = (& $adb -s $deviceSerial shell pidof $appId | Out-String).Trim()
  if ($stoppedPid) { throw 'Force-stop sonrası süreç hâlâ çalışıyor.' }
  $relaunchTimer = [System.Diagnostics.Stopwatch]::StartNew()
  $launchOutput = & $adb -s $deviceSerial shell am start -W `
      -a android.intent.action.MAIN `
      -c android.intent.category.LEANBACK_LAUNCHER `
      -p $appId
  $launchExitCode = $LASTEXITCODE
  $launchOutput
  if ($launchExitCode -ne 0) { throw 'Uygulama yeniden başlatılamadı.' }
  Read-Host 'Kaydedilmiş duvar kararlı olduğunda Enter'
  $relaunchTimer.Stop()
  $afterPid = (& $adb -s $deviceSerial shell pidof $appId | Out-String).Trim()
  if (-not $afterPid) { throw 'Yeniden başlatma sonrası süreç bulunamadı.' }
  'Duvar toparlanma süresi: {0:N1} saniye' -f $relaunchTimer.Elapsed.TotalSeconds
  Remove-Variable -Name beforePid, stoppedPid, relaunchTimer, launchOutput, launchExitCode, afterPid
  ```

- Beklenen: Force-stop sonrası PID yoktur; yeniden başlatma exit code 0 verir ve
  yeni süreç oluşur. Seçimler, özel kamera adları ve sıralama korunur; kaydedilmiş
  duvar izin hazır olduğunda güvenli biçimde açılır.
- Kanıt: `force-stop sonrası PID: yok`, `yeniden süreç: var`, `am start TotalTime`,
  duvar toparlanma süresi ve seçim/sıra karşılaştırması. PID değerlerini yazmak
  gerekmez.

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
  & $adb -s $deviceSerial logcat -c
  & $adb -s $deviceSerial logcat
  ```

  İnceleme bitince `Ctrl+C` kullanın.
- Beklenen: Kullanıcı adı, parola, credential taşıyan tam RTSP URI, ev IP'si veya
  hassas anahtar malzemesi görünmez.
- Kanıt: Yalnız `sensitive matches: 0` veya redakte edilmiş hata sınıfı; ham logu
  commit etmeyin.

### 14. Bellek, decoder ve tekil hata izolasyonu

- Önkoşul: İki yayınlı duvar en az 15 dakika çalışmış; diğer RTSP istemcileri
  kapalı.
- Adım: Belleği yerel olarak gözlemleyin; mümkünse bir kamerayı kısa süre kapatın:

  ```powershell
  & $adb -s $deviceSerial shell dumpsys meminfo $appId
  ```

- Beklenen: Bellek kontrolsüz büyümez; bir tile hatası diğer canlı tile'ı
  kapatmaz; decoder yetersizliği güvenli hata olarak görünür.
- Kanıt: Başlangıç/15. dakika redakte edilmiş toplam bellek özeti, tile durumları
  ve crash sayısı.

### 15. Üç yayınlı grid (isteğe bağlı)

- Önkoşul: Üçüncü güvenli gerçek veya yerel fake RTSP yayını fiziksel cihazdan
  erişilebilir; üretim arayüzüne manuel IP aracı eklenmez; diğer RTSP istemcileri
  kapalı.
- Adım: Üç tile'ı açıp D-pad ile tümüne gidin.
- Beklenen: 2×2 yerleşimde son satır ortalanır ve odak yolu kararlı kalır.
- Kanıt: Tile sayısı, odak sırası ve `PASS/FAIL`; önkoşul yoksa `BLOCKED`.

### 16. API 37 yerel ağ iznini çalışma sırasında iptal etme

- Önkoşul: `& $adb -s $deviceSerial shell getprop ro.build.version.sdk` sonucu en
  az `37`; duvar veya tam ekran gerçek bir yerel yayın gösteriyor.
- Adım: Home ile sistem ayarlarına geçin, CamGrid TV debug uygulamasının **Yerel
  ağ** iznini kapatın ve uygulamaya geri dönün. İzin ekranı kararlı olduktan sonra
  sistem ayarlarından izni yeniden verip tekrar uygulamaya dönün.
- Beklenen: İzin kapatılınca tüm discovery/RTSP işi ve player'lar durur, uygulama
  izin açıklamalı keşif ekranına döner ve izinsizken otomatik yeniden bağlantı
  başlamaz. İzin yeniden verilince keşif güvenli biçimde devam eder. Sistem izin
  değişiminde prosesi öldürdüyse, normal soğuk başlangıç politikası daha önce
  kaydedilmiş ve yapılandırılmış kameraların duvarını yalnız izin yeniden
  verildikten sonra açabilir; izin yokken hiçbir LAN işi başlamaz ve crash oluşmaz.
- Kanıt: SDK seviyesi, iptalden izin ekranına kadar süre, player'ın durduğu
  gözlemi ve yeniden izin sonrası keşif durumu. Cihaz seri numarası, IP veya
  credential kaydetmeyin. API 37 cihazı yoksa `BLOCKED` yazın.

## Test sonu

Eski cihazda TCP 5555 kullandıysanız `usb` komutunu hâlâ etkin olan ağ transportuna
gönderin; `adb tcpip 5555` sonrasında eski `$usbSerial` transportu çoğu cihazda
artık mevcut değildir. Aşağıdaki blok adbd'yi USB modunda yeniden başlatır; yalnız
`adb disconnect` çalıştırmak TCP 5555 dinleyicisini kapatmaz. API 33+ eşleme
akışında yalnız kablosuz transportu ayırır. Son olarak gizli değişkenleri temizleyin
ve ham log oluşturmadığınızı doğrulayın:

```powershell
if ($legacyTcpMode) {
    $cleanupErrors = [System.Collections.Generic.List[string]]::new()
    $usbModeOutput = & $adb -s $tcpTarget usb 2>&1
    $usbModeExitCode = $LASTEXITCODE
    $usbModeOutput
    if ($usbModeExitCode -ne 0) {
        $cleanupErrors.Add('adbd USB moduna alınamadı.')
    }

    Start-Sleep -Seconds 2
    $tcpProbe = [System.Net.Sockets.TcpClient]::new()
    $probeAsync = $tcpProbe.BeginConnect($tvHost, 5555, $null, $null)
    $probeCompleted = $probeAsync.AsyncWaitHandle.WaitOne(3000, $false)
    $tcp5555StillOpen = $false
    if ($probeCompleted) {
        try {
            $tcpProbe.EndConnect($probeAsync)
            $tcp5555StillOpen = $tcpProbe.Connected
        } catch {
            $tcp5555StillOpen = $false
        }
    }
    $probeAsync.AsyncWaitHandle.Close()
    $tcpProbe.Close()
    if ($tcp5555StillOpen) {
        $cleanupErrors.Add('Cihazın TCP 5555 portu hâlâ erişilebilir.')
    }

    $disconnectOutput = & $adb disconnect $tcpTarget 2>&1
    $disconnectExitCode = $LASTEXITCODE
    $disconnectOutput
    if ($disconnectExitCode -ne 0) {
        $cleanupErrors.Add('Yerel adb TCP transport kaydı ayrılamadı.')
    }
    if ($cleanupErrors.Count -ne 0) {
        throw ('TCP 5555 temizliği doğrulanamadı: ' + ($cleanupErrors -join ' '))
    }
    $legacyTcpMode = $false
    Remove-Variable -Name usbSerial, tvHost, tcpTarget, cleanupErrors, usbModeOutput,
        usbModeExitCode, tcpProbe, probeAsync, probeCompleted, tcp5555StillOpen,
        disconnectOutput, disconnectExitCode
} else {
    $disconnectOutput = & $adb disconnect $deviceSerial 2>&1
    $disconnectExitCode = $LASTEXITCODE
    $disconnectOutput
    if ($disconnectExitCode -ne 0) { throw 'Kablosuz adb transportu ayrılamadı.' }
    Remove-Variable -Name disconnectOutput, disconnectExitCode
}
Remove-Variable -Name deviceSerial, adb, apk, appId, legacyTcpMode
```

USB modu komutu ağ transportundan kullanılamıyorsa veya erişim probu portu hâlâ
açık bulursa cihazı yeniden başlatın ya da geliştirici ayarlarından ağ hata
ayıklamasını kapatın. Yalnız `adb disconnect` veya boş `adb devices` çıktısı,
cihazın 5555 portunu kapattığını kanıtlamaz. USB ile yeniden bağlanacaksanız port
erişilemez olduktan sonra veri kablosu/OTG'yi takın ve yeni yetki istemini cihaz
ekranından onaylayın.

Özellikle şu sonuçları ayrı satırlarda tutun: Mi Stick
`connectedDebugAndroidTest`, temiz başlangıç, C500 keşif, C510W keşif, C500
`/stream2`, C510W `/stream2`, iki yayın/15 dakika, C500 `/stream1` tam ekran,
C510W `/stream1` tam ekran, Mi Stick D-pad, lifecycle, API 37 izin iptali, ağ geri
dönüşü, yanlış parola, force-stop/kalıcılık, log gizliliği ve decoder/bellek. Her
fiziksel cihaz sonucu kanıt yürütülene kadar `BLOCKED` kalır.
