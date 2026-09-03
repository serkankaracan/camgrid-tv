# Fiziksel cihaz test planı

## Mevcut koşunun durumu

Bu plandaki gerçek C500, C510W ve Mi Stick senaryolarının tamamı **BLOCKED**
durumundadır. Fiziksel kameralara, Mi Stick'e, emülatöre veya bağlı bir adb
cihazına erişilemedi. Fake, JVM, derleme veya port erişimi sonucu fiziksel
doğrulama yerine kullanılmaz.

Akış Windows 11 ve PowerShell içindir. Proje Android SDK komut satırı araçlarını
kullanır; Android Studio gerekli değildir ve kurulu olduğu varsayılmaz.

## USB gerekli mi? Önce bağlantı yöntemini seçin

APK'yı kurmak ve tam fiziksel kabul testini çalıştırmak için bilgisayarın Mi
Stick'e **adb** ile erişmesi gerekir. USB her durumda zorunlu değildir:

1. Mi Stick'te **Kablosuz hata ayıklama** ve **Eşleme koduyla cihaz eşleştir**
   menüsü varsa `WirelessPairing` yolunu kullanın. Bilgisayar ile Mi Stick aynı
   güvenilir yerel ağdayken USB kablosu gerekmez.
2. Kablosuz eşleme menüsü yoksa ve cihaz bilgisayara veri taşıyan USB/OTG
   bağlantısıyla bağlı kalabiliyorsa `DirectUsb` yolunu kullanın. Mi Stick'in tek
   güç/veri portu için üreticiyle uyumlu, güç geçişli bir OTG/Y adaptörü veya
   powered hub gerekebilir. İki güç kaynağını birbirine bağlayan USB-A erkek–erkek
   kablo kullanmayın ve konnektörleri zorlamayın.
3. Kablosuz eşleme menüsü yoksa ve USB/OTG yalnız ilk yetkilendirme için
   kullanılabiliyorsa, aşağıdaki kanonik `LegacyTcp5555` yoluyla geçici ağ adb'si
   açın. Test sonunda TCP 5555'i doğrulayarak kapatmak zorunludur.
4. Yalnız kimlik bilgisiz kurulum/açılış denemesi için USB belleğe kopyalama
   kullanılabilir; bu yöntem `connectedDebugAndroidTest`, logcat ve diğer adb
   kanıtlarını sağlayamaz. Kamera Hesabı veya gerçek yayın senaryolarına geçmeyin;
   bu yöntem tam fiziksel kabul testi sayılmaz.

ADB yalnız kurulum, otomatik cihaz testi ve tanılama içindir. APK kurulduktan
sonra normal kamera izleme sırasında bilgisayarın veya adb bağlantısının açık
kalması gerekmez; Mi Stick ile kameraların aynı güvenilir LAN'da kalması gerekir.

Bu belgede yalnız **bir** bağlantı dalını çalıştırın ve dalın atadığı
`$connectionKind` ile `$deviceSerial` değişkenlerini test sonuna kadar koruyun.

## Hangi APK kullanılacak?

| Dosya | Amaç | Elle kurulsun mu? |
| --- | --- | --- |
| `app\build\outputs\apk\debug\app-debug.apk` | Fiziksel Mi Stick'e kurulacak debug uygulaması | **Evet** |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | Instrumented testlerin yardımcı paketi; Gradle yönetir | Hayır |
| `app\build\outputs\apk\release\app-release-unsigned.apk` | İmzalanmamış release çıktısı | Hayır; imzalanmadan kurulamaz |

Bu çalışma ağacında kurulacak APK'nın tam yolunu, boyutunu ve özetini depo
kökünde doğrulayın. `Resolve-Path` dosyayı bulamıyorsa önce kalite kapısını
çalıştırıp APK'yı yeniden üretin:

```powershell
if (-not (Test-Path -LiteralPath '.\settings.gradle.kts' -PathType Leaf)) {
    throw 'Önce VS Code PowerShell terminalinde camgrid-tv depo köküne gidin.'
}
$debugApkPath = '.\app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $debugApkPath -PathType Leaf)) {
    throw 'Debug APK bulunamadı; önce kalite kapısını çalıştırın.'
}
$debugApk = Get-Item -LiteralPath $debugApkPath
$debugApk | Select-Object FullName, Length, LastWriteTime
Get-FileHash -LiteralPath $debugApk.FullName -Algorithm SHA256
Remove-Variable -Name debugApkPath, debugApk
```

Kurulum için yalnız tablonun ilk satırındaki debug APK'yı kullanın. Hazır APK'yı
başka bilgisayara aktarıyorsanız boyut/özet değerini `docs/TEST_REPORT.md` ve
kaynak bilgisayardaki değerle karşılaştırın. Aynı revision ikinci bilgisayarda
yeniden derlenirse o bilgisayarın debug imza anahtarı özeti değiştirebilir; bu
durumda ikinci bilgisayardaki kalite kapısını çalıştırıp yeni özeti koşu kanıtı
olarak kaydedin.

## Başka bir Windows 11 bilgisayarda test

Mi Stick'in adb yetkisi bilgisayar bazındadır. Testi başka bilgisayarda
yapacaksanız o bilgisayarı ayrıca eşleştirin veya TV'deki RSA istemiyle ayrıca
yetkilendirin; eski eşleme kodunu yeniden kullanmayın. Yeni bilgisayar, Mi Stick
ve kameralar aynı güvenilir özel ağda olmalıdır.

Önce aşağıdaki iki yöntemden birini seçin. **Tam fiziksel test** için birinci
yöntem önerilir; yalnız APK'yı kurup kimlik bilgisiz açılışı denemek için ikinci
yöntem yeterlidir.

### Tam test: depoyu aynı revision ile hazırlama

Bu yöntem `connectedDebugAndroidTest`, logcat ve tüm kanıt adımlarını sağlar.
Kaynak bilgisayarda değişikliklerin GitHub'a gönderilmiş olduğundan emin olun ve
test edilecek revision'ı alın:

```powershell
if ((git status --porcelain).Count -ne 0) {
    throw 'Önce kaynak bilgisayardaki değişiklikleri commit edip GitHub sunucusuna gönderin.'
}
$revision = (git rev-parse HEAD | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $revision -notmatch '^[0-9a-f]{40}$') {
    throw 'Test edilecek Git revision okunamadı.'
}
$revision
Get-FileHash -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk' -Algorithm SHA256
```

Revision ve APK SHA-256 değerleri gizli değildir; bunları not edebilirsiniz.
Başka bilgisayarda Git, JDK 17, PowerShell ve resmî Android SDK Command-line
Tools'u hazırlayın. Android Studio gerekmez. VS Code PowerShell terminalinde:

```powershell
git clone https://github.com/serkankaracan/camgrid-tv.git
if ($LASTEXITCODE -ne 0) { throw 'Depo klonlanamadı.' }
Set-Location -LiteralPath '.\camgrid-tv'
$revision = Read-Host 'Kaynak bilgisayardaki 40 karakterli Git revision'
if ($revision -notmatch '^[0-9a-fA-F]{40}$') { throw 'Revision biçimi geçersiz.' }
git switch --detach $revision
if ($LASTEXITCODE -ne 0) { throw 'İstenen revision açılamadı.' }
$actualRevision = (git rev-parse HEAD | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $actualRevision -cne $revision.ToLowerInvariant()) {
    throw 'Açılan revision istenen revision ile aynı değil.'
}

$javaHomeInput = Read-Host 'JDK 17 klasörünün tam yolu'
if ([string]::IsNullOrWhiteSpace($javaHomeInput)) {
    throw 'JDK 17 klasörü boş bırakılamaz.'
}
$javaCandidate = Join-Path $javaHomeInput 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaCandidate -PathType Leaf)) {
    throw 'Girilen klasörde bin\java.exe bulunamadı.'
}
$env:JAVA_HOME = (Resolve-Path -LiteralPath $javaHomeInput).Path
$javaVersionOutput = & $javaCandidate -version 2>&1
$javaVersionExitCode = $LASTEXITCODE
$javaVersionText = ($javaVersionOutput | Out-String).Trim()
$javaVersionOutput
if ($javaVersionExitCode -ne 0 -or $javaVersionText -notmatch '\bversion\s+"17(?:[.\-"]|$)') {
    throw 'Seçilen Java kurulumu çalışan bir JDK 17 değil.'
}
$sdkManagerInput = Read-Host 'Çıkardığınız sdkmanager.bat dosyasının tam yolu'
if (-not (Test-Path -LiteralPath $sdkManagerInput -PathType Leaf)) {
    throw 'sdkmanager.bat bulunamadı.'
}
.\scripts\setup-android-sdk.ps1 `
    -SdkManagerPath $sdkManagerInput `
    -JavaHomePath $env:JAVA_HOME `
    -AcceptLicenses
if ($LASTEXITCODE -ne 0) { throw 'Proje-yerel Android SDK hazırlanamadı.' }
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath "$PWD\.android-sdk"
if ($LASTEXITCODE -ne 0) { throw 'Başka bilgisayardaki kalite kapısı başarısız oldu.' }
Get-FileHash -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk' -Algorithm SHA256
Remove-Variable -Name revision, actualRevision, javaHomeInput, javaCandidate,
    javaVersionOutput, javaVersionExitCode, javaVersionText, sdkManagerInput
```

Her bilgisayarın Gradle debug imza anahtarı farklı olabilir; bu nedenle ikinci
bilgisayarda yeniden üretilen APK'nın SHA-256 değeri, aynı kaynak revision'ına
rağmen kaynak bilgisayardaki hazır APK'dan farklı çıkabilir. İkinci bilgisayarda
tam testi yapacaksanız o bilgisayarın ürettiği `app-debug.apk` ile devam edin ve
aynı Mi Stick'e iki bilgisayardan üretilmiş debug APK'ları sırayla kurmayın.
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` alırsanız aşağıdaki veri kaybı uyarısını
okumadan mevcut uygulamayı kaldırmayın.

Bu hazırlıktan sonra belgeyi **Mi Stick'te geliştirici seçeneklerini hazırlama**
bölümünden normal sırayla sürdürün.

### Hızlı yol: hazır APK + Platform-Tools ile kimlik bilgisiz smoke test

Yalnız kurulum, launcher görünürlüğü ve kimlik bilgisiz açılış smoke testi
yapacaksanız depoyu, JDK'yı veya Android SDK Command-line Tools paketinin tamamını
kurmanız gerekmez. Kaynak bilgisayardan yalnız `app-debug.apk` dosyasını güvenilir
bir aktarım ortamıyla taşıyın; `.gradle` önbelleklerini, `local.properties`,
`.android` klasörünü veya `debug.keystore` dosyasını taşımayın. APK'yı Git'e commit
etmeyin.

Başka bilgisayarda Google'ın [resmî SDK Platform-Tools
ZIP'ini](https://developer.android.com/tools/releases/platform-tools) indirip ayrı
bir klasöre çıkarın. Aktarılan dosyanın SHA-256 değerini kaynak bilgisayarda
aldığınız değerle birebir karşılaştırın ve çalışma değişkenlerini hazırlayın:

```powershell
$adbInput = Read-Host 'Başka bilgisayardaki adb.exe dosyasının tam yolu'
$apkInput = Read-Host 'Aktarılan app-debug.apk dosyasının tam yolu'
if (-not (Test-Path -LiteralPath $adbInput -PathType Leaf)) {
    throw 'adb.exe bulunamadı.'
}
if (-not (Test-Path -LiteralPath $apkInput -PathType Leaf)) {
    throw 'app-debug.apk bulunamadı.'
}
$adb = (Resolve-Path -LiteralPath $adbInput).Path
$apk = (Resolve-Path -LiteralPath $apkInput).Path
$appId = 'io.github.serkankaracan.camgridtv.debug'
$connectionKind = $null
& $adb version
if ($LASTEXITCODE -ne 0) { throw 'adb çalıştırılamadı.' }
Get-FileHash -LiteralPath $apk -Algorithm SHA256
Remove-Variable -Name adbInput, apkInput
```

Hash eşleşmiyorsa APK'yı kurmayın; yeniden aktarın. Eşleşiyorsa geliştirici
seçeneklerini hazırlayın, bu belgedeki bağlantı dallarından birini tamamlayın ve
doğrulanmış `install -r` bloğuyla kurun. Bu hızlı yolda **PowerShell ile adb
hazırlığı**, kalite kapısı ve `connectedDebugAndroidTest` bölümlerini atlayın;
bunları sonuçta `BLOCKED` yazın. Uygulamayı başlatıp launcher banner'ını, ilk
ekranı, D-pad odağını ve crash olmamasını kimlik bilgisi girmeden kontrol
edebilirsiniz. Kamera Hesabı girmeyin ve gerçek yayın senaryolarına geçmeyin.
Tam cihaz testleri için birinci yönteme dönün.

Her iki yöntemde de test bitince yeni bilgisayarın kablosuz eşleşmesini TV'den
unutun veya USB hata ayıklama yetkisini kapatın; **Test sonu** bölümünü atlamayın.

## Mi Stick'te geliştirici seçeneklerini hazırlama

Menü adları üretici sürümüne göre biraz değişebilir. Kumandayla genel olarak:

1. **Ayarlar > Sistem** veya **Cihaz Tercihleri > Hakkında** bölümünü açın.
2. **Android TV OS derlemesi** ya da **Derleme** satırına geliştirici olduğunuzu
   belirten ileti görünene kadar, genellikle yedi kez, OK ile basın.
3. Geri dönüp **Geliştirici seçenekleri** bölümünü açın.
4. `WirelessPairing` için **Kablosuz hata ayıklama**yı; `DirectUsb` veya
   `LegacyTcp5555` için **USB hata ayıklama**yı açın.
5. TV'deki RSA/eşleme onayını yalnız kendi test bilgisayarınız için kabul edin.
   Bilgisayarın parmak izi gösteriliyorsa ekrandaki değerle karşılaştırın.
6. Mi Stick, bilgisayar ve kameraları aynı güvenilir özel ağa bağlayın. Misafir
   Wi-Fi ve AP/client isolation kullanmayın. Windows Güvenlik Duvarı adb için izin
   sorarsa yalnız **Özel ağlar** kapsamını seçin.

Test bitince seçtiğiniz hata ayıklama yöntemini kapatma ve gerekirse eşleşmiş
bilgisayarı unutma adımları bu belgenin **Test sonu** bölümündedir.

## Zorunlu yürütme sırası

1. Kaynak değiştiyse kalite kapısını çalıştırın. Hazır APK kullanıyorsanız özetin
   güncel `docs/TEST_REPORT.md` kanıtıyla eşleştiğini doğrulayın. Aynı revision'ı
   başka bilgisayarda yeniden derlediyseniz o bilgisayarda kalite kapısını
   çalıştırıp kendi APK özetini kaydedin.
2. Bir bağlantı dalı seçip yalnız hedef Mi Stick'i adb'de yetkili `device`
   durumuna getirin.
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
8. Sonuçları redakte biçimde kaydedip seçilen bağlantı dalının **Test sonu**
   temizliğini tamamlayın.

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

### Redakte sonuç şablonu

Her fiziksel koşudan sonra aşağıdaki şablonu `docs/TEST_REPORT.md` içine kopyalayıp
yalnız çalıştırdığınız alanları doldurun. Seri numarası, IP/port, MAC adresi,
eşleme kodu, kullanıcı adı, parola veya tam URI yazmayın:

```text
Tarih/saat dilimi: YYYY-AA-GG / Europe-Istanbul
Git revision: <kısa commit>
Debug APK SHA-256: <64 hex karakter>
APK kaynağı: Kaynak bilgisayardan aktarıldı | Bu bilgisayarda derlendi
Bağlantı yöntemi: WirelessPairing | DirectUsb | LegacyTcp5555 | UsbStorageOnly
Mi Stick Android SDK seviyesi: <sayı | bilinmiyor>
connectedDebugAndroidTest: PASS | FAIL | BLOCKED — <redakte kısa kanıt>
Ölçümlü temiz başlangıç: PASS | FAIL | BLOCKED | NOT RUN — <süre>
C500 keşif: PASS | FAIL | BLOCKED | NOT RUN — <süre ve kayıt sayısı>
C510W keşif: PASS | FAIL | BLOCKED | NOT RUN — <süre ve kayıt sayısı>
C500 /stream2: PASS | FAIL | BLOCKED | NOT RUN — <ilk kare süresi>
C510W /stream2: PASS | FAIL | BLOCKED | NOT RUN — <ilk kare süresi>
İki yayın / 15 dakika: PASS | FAIL | BLOCKED | NOT RUN — <crash ve son durum>
D-pad odağı: PASS | FAIL | BLOCKED | NOT RUN — <izlenen odak sırası>
Metin alanı/ekran klavyesi: PASS | FAIL | BLOCKED | NOT RUN — <gezinme, OK, Back/Bitti sonucu>
Uyarlanabilir Doğrula → N kamerayı izle: PASS | FAIL | BLOCKED | NOT RUN — <durum geçişi>
Duvar üst çubuğu yeniden tarama: PASS | FAIL | BLOCKED | NOT RUN — <görünürlük, odak, yeni tarama>
TV arayüzü/safe area: PASS | FAIL | BLOCKED | NOT RUN — <960x540 ve 1280x720 gözlemi>
C500 /stream1 tam ekran: PASS | FAIL | BLOCKED | NOT RUN — <ilk kare süresi>
C510W /stream1 tam ekran: PASS | FAIL | BLOCKED | NOT RUN — <ilk kare süresi>
Back ile odak dönüşü: PASS | FAIL | BLOCKED | NOT RUN — <redakte kısa kanıt>
Arka plan / ön plan: PASS | FAIL | BLOCKED | NOT RUN — <toparlanma süresi>
Yerel ağ kaybı / dönüşü: PASS | FAIL | BLOCKED | NOT RUN — <toparlanma süresi>
Yanlış parola: PASS | FAIL | BLOCKED | NOT RUN — <güvenli hata sınıfı>
Force-stop / kalıcılık: PASS | FAIL | BLOCKED | NOT RUN — <süre ve seçim sonucu>
Endpoint UUID / DHCP değişimi: PASS | FAIL | BLOCKED | NOT RUN — <kayıt sayısı>
Log gizliliği: PASS | FAIL | BLOCKED | NOT RUN — sensitive matches: <sayı>
Bellek / decoder izolasyonu: PASS | FAIL | BLOCKED | NOT RUN — <redakte özet>
Üç yayınlı grid: PASS | FAIL | BLOCKED | NOT RUN — <tile ve odak özeti>
API 37 izin iptali: PASS | FAIL | BLOCKED | NOT RUN — <toparlanma özeti>
Test sonu hata ayıklama temizliği: PASS | FAIL | BLOCKED — <yöntem kapatıldı mı?>
```

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

VS Code içindeki PowerShell terminalinde önce depo köküne gidin. İstem sonunda
`camgrid-tv` klasörü görünmelidir. Proje-yerel platform-tools yolunu ve debug
paketini fail-fast denetimleriyle tanımlayın:

```powershell
$repoRootInput = Read-Host 'camgrid-tv depo kökünün tam yolu'
if ([string]::IsNullOrWhiteSpace($repoRootInput) -or
    -not (Test-Path -LiteralPath $repoRootInput -PathType Container)) {
    throw 'Girilen depo klasörü bulunamadı.'
}
$repoRoot = (Resolve-Path -LiteralPath $repoRootInput).Path
Set-Location -LiteralPath $repoRoot
if (-not (Test-Path -LiteralPath '.\settings.gradle.kts' -PathType Leaf)) {
    throw 'Bu klasör camgrid-tv depo kökü değil.'
}

$adbCandidate = Join-Path $repoRoot '.android-sdk\platform-tools\adb.exe'
$apkCandidate = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $adbCandidate -PathType Leaf)) {
    throw 'Proje-yerel adb bulunamadı; README içindeki SDK kurulumunu tamamlayın.'
}
if (-not (Test-Path -LiteralPath $apkCandidate -PathType Leaf)) {
    throw 'Debug APK bulunamadı; önce kalite kapısını çalıştırın.'
}

$adb = (Resolve-Path -LiteralPath $adbCandidate).Path
$apk = (Resolve-Path -LiteralPath $apkCandidate).Path
$appId = 'io.github.serkankaracan.camgridtv.debug'
$connectionKind = $null
& $adb version
if ($LASTEXITCODE -ne 0) { throw 'adb başlatılamadı.' }
& $adb start-server
if ($LASTEXITCODE -ne 0) { throw 'adb sunucusu başlatılamadı.' }
& $adb devices -l
if ($LASTEXITCODE -ne 0) { throw 'adb cihaz listesi okunamadı.' }
Remove-Variable -Name repoRootInput, adbCandidate, apkCandidate
```

Bu ilk listede henüz eşleme yapmadıysanız cihaz görünmemesi normaldir. Aşağıdaki
üç bağlantı dalından yalnız birini tamamlayın. Seçtiğiniz dalın sonundaki listede
Mi Stick satırı tam olarak `device` durumuna gelmezse yükleme/teste başlamayın ve
sonucu `BLOCKED` kaydedin.

### `WirelessPairing`: standart kablosuz eşleme

Mi Stick **Kablosuz hata ayıklama > Eşleme koduyla cihaz eşleştir** menüsünü
sunuyorsa bu dalı kullanın. Android sürüm numarasına bakarak varsayım yapmak
yerine menünün gerçekten bulunmasını esas alın. Ekrandaki eşleme adresi/portu ile
bağlantı adresi/portunun farklı olabileceğini dikkate alın. Değerleri yalnız
istemde girin:

```powershell
$connectionKind = 'WirelessPairing'
$pairTarget = Read-Host 'Mi Stick eşleme IP:port değeri'
$connectTarget = $null
try {
    # Çıktıyı yakalamayın: eşleme kodu istemi terminalde canlı görünmelidir.
    & $adb pair $pairTarget
    $pairExitCode = $LASTEXITCODE
    if ($pairExitCode -ne 0) {
        throw 'Mi Stick ile adb eşleştirmesi başarısız oldu.'
    }

    $connectTarget = Read-Host 'Mi Stick kablosuz hata ayıklama IP:port değeri'
    $connectOutput = & $adb connect $connectTarget 2>&1
    $connectExitCode = $LASTEXITCODE
    $connectText = ($connectOutput | Out-String).Trim()
    $connectOutput
    if ($connectExitCode -ne 0 -or $connectText -match '(?i)\b(?:failed|error)\b') {
        throw 'Mi Stick kablosuz adb bağlantısı kurulamadı.'
    }

    $deviceSerial = $connectTarget
    $deviceState = (& $adb -s $deviceSerial get-state 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $deviceState -cne 'device') {
        throw 'Kablosuz adb transportu yetkili device durumuna gelmedi.'
    }
} catch {
    if ($connectTarget) { & $adb disconnect $connectTarget | Out-Null }
    Write-Warning 'TV ayarlarından Kablosuz hata ayıklamayı kapatın; gerekirse eşleşmiş bilgisayarı unutun.'
    throw
}
& $adb devices -l
if ($LASTEXITCODE -ne 0) { throw 'adb cihaz listesi okunamadı.' }
Remove-Variable -Name pairTarget, connectTarget, pairExitCode, connectOutput,
    connectExitCode, connectText, deviceState `
    -ErrorAction SilentlyContinue
```

Eşleme kodunu `adb pair` komutuna argüman olarak yazmayın; adb istediğinde
etkileşimli olarak girin. TV'de gösterilen RSA/eşleme isteğini yalnız test
bilgisayarının parmak izi doğruysa kabul edin.

### `DirectUsb`: veri taşıyan USB/OTG bağlantısını koruma

Mi Stick bilgisayara veri taşıyan, güvenli ve güç geçişli bir USB/OTG düzeniyle
bağlı kalabiliyorsa bu dalı kullanın. USB bellek takmak adb bağlantısı değildir.
Windows Aygıt Yöneticisi cihazı tanımıyorsa modelle uyumlu OEM ADB sürücüsü
gerekebilir; [Android'ın resmî OEM USB sürücüsü
rehberini](https://developer.android.com/studio/run/oem-usb) izleyin ve rastgele
sürücü paketleri kurmayın. TV'deki RSA istemini kabul ettikten sonra:

```powershell
$connectionKind = 'DirectUsb'
& $adb devices -l
if ($LASTEXITCODE -ne 0) { throw 'adb cihaz listesi okunamadı.' }
$deviceSerial = Read-Host 'Yetkili USB Mi Stick adb seri değeri'
$deviceState = (& $adb -s $deviceSerial get-state 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $deviceState -cne 'device') {
    throw 'Seçilen USB transportu yetkili device durumunda değil.'
}
Remove-Variable -Name deviceState
```

Testler tamamlanana kadar USB veri/OTG bağlantısını ve güvenli güç kaynağını
koruyun. Bu dalda `adb tcpip 5555` veya `adb connect` çalıştırmayın.

### `LegacyTcp5555`: eski TV/Mi Stick'te USB/OTG'den TCP 5555'e geçiş

Kablosuz eşleme menüsü olmayan cihazı önce veri taşıyabilen USB/OTG bağlantısıyla
bağlayın, TV'deki RSA yetki istemini kabul edin ve USB satırının `device`
olduğunu doğrulayın. Klasik `adb tcpip 5555` kanalı şifrelenmemiş, yüksek yetkili
ve geçici bir debug erişimidir. Yalnız güvenilir özel LAN'da kullanın; TCP 5555'i
router'dan/internete yönlendirmeyin ve test sonunda kapatın. Sonra şu akışı
kullanın:

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
    $connectionKind = 'LegacyTcp5555'
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
    $deviceSerial = $tcpTarget
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

    Start-Sleep -Seconds 2
    $cleanupProbe = [System.Net.Sockets.TcpClient]::new()
    $cleanupProbeAsync = $null
    $tcp5555StillOpen = $false
    $cleanupProbeConfirmedClosed = $false
    $cleanupProbeFailed = $false
    try {
        $cleanupProbeAsync = $cleanupProbe.BeginConnect($tvHost, 5555, $null, $null)
        $cleanupProbeCompleted = $cleanupProbeAsync.AsyncWaitHandle.WaitOne(3000, $false)
        if (-not $cleanupProbeCompleted) {
            $cleanupProbeFailed = $true
        } else {
            try {
                $cleanupProbe.EndConnect($cleanupProbeAsync)
                $tcp5555StillOpen = $cleanupProbe.Connected
            } catch [System.Net.Sockets.SocketException] {
                if ($_.Exception.SocketErrorCode -eq
                    [System.Net.Sockets.SocketError]::ConnectionRefused) {
                    $cleanupProbeConfirmedClosed = $true
                } else {
                    $cleanupProbeFailed = $true
                }
            } catch {
                $cleanupProbeFailed = $true
            }
        }
    } catch {
        $cleanupProbeFailed = $true
    } finally {
        if ($null -ne $cleanupProbeAsync) {
            $cleanupProbeAsync.AsyncWaitHandle.Close()
        }
        $cleanupProbe.Close()
    }
    & $adb disconnect $tcpTarget | Out-Null
    if ($usbModeRestored -and $cleanupProbeConfirmedClosed -and
        -not $tcp5555StillOpen -and -not $cleanupProbeFailed) {
        $abortCleanupInstruction = 'Otomatik TCP kapanışı doğrulandı. Yine de TV geliştirici ayarlarında USB hata ayıklamayı kapatın ve gerekmezse yetkileri iptal edin.'
    } else {
        $abortCleanupInstruction = 'TCP 5555 kapanışı doğrulanamadı. TV geliştirici ayarlarında USB/ağ hata ayıklamayı kapatın veya Mi Stick cihazını yeniden başlatın.'
    }
    Write-Warning $abortCleanupInstruction
    $abortCleanupConfirmation = Read-Host 'Yukarıdaki manuel temizliği tamamladıysanız KAPALI yazın'
    if ($abortCleanupConfirmation -cne 'KAPALI') {
        throw 'Legacy TCP 5555 hazırlığı ve manuel kapanış doğrulanamadı.'
    }
    $connectionKind = $null
    throw 'Legacy TCP 5555 hazırlığı başarısız oldu; manuel temizlik onaylandı, sonucu BLOCKED kaydedin.'
}
Remove-Variable -Name parsedTvHost, hostIsValid, hostOctets, hostIsPrivate, usbState,
    tcpipOutput, tcpipExitCode, connectOutput, connectExitCode, tcpState
```

`get-state` çıktısı `device` olduktan sonra USB/OTG bağlantısını çıkarın ve
`& $adb devices -l` ile yalnız ağ taşımasının kaldığını doğrulayın. TCP 5555
yalnız güvenilir yerel ağda kullanılmalıdır. `$usbSerial`, `$tcpTarget` ve
`$connectionKind` değişkenlerini test sonuna kadar koruyun; aşağıdaki test sonu
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

Seçtiğiniz dal `$deviceSerial` değerini atadıktan sonra tam olarak bir yetkili
cihaz kaldığını ve seçilen transportun gerçekten `device` olduğunu fail-fast
doğrulayın. Tüm sonraki manuel adb komutlarında `-s` kullanın:

```powershell
$deviceRows = @(& $adb devices -l)
if ($LASTEXITCODE -ne 0) { throw 'adb cihaz listesi okunamadı.' }
$authorizedRows = @($deviceRows | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' })
$problemRows = @($deviceRows | Where-Object { $_ -match '^\S+\s+(?:unauthorized|offline)(?:\s|$)' })
if ($authorizedRows.Count -ne 1 -or $problemRows.Count -ne 0) {
    $deviceRows
    throw 'Tam olarak bir yetkili device kalmalıdır; unauthorized/offline satırı olmamalıdır.'
}
$deviceState = (& $adb -s $deviceSerial get-state 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $deviceState -cne 'device') {
    throw 'Seçilen Mi Stick transportu device durumunda değil.'
}
$sdkLevel = (& $adb -s $deviceSerial shell getprop ro.build.version.sdk 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $sdkLevel -notmatch '^\d+$') {
    throw 'Mi Stick Android SDK seviyesi okunamadı.'
}
'Mi Stick Android SDK seviyesi: {0}' -f $sdkLevel
Remove-Variable -Name deviceRows, authorizedRows, problemRows, deviceState
```

Çıktı tam olarak `device` değilse devam etmeyin. Seri/adres değerini test
raporuna kopyalamayın.

## Derleme, kurulum ve cihaz testleri

Yalnız mevcut doğrulanmış debug APK'yı kuracaksanız yeniden derleme zorunlu
değildir. Kaynak değiştiyse veya tam kabul koşusu yapıyorsanız JDK 17 ile güncel
kalite kapısını çalıştırın:

```powershell
if (-not $env:JAVA_HOME) {
    throw 'JAVA_HOME tanımlı değil; JDK 17 klasörünü tanımlayın.'
}
$javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw 'JAVA_HOME geçerli bir JDK 17 klasörünü göstermiyor.'
}
& $javaExe -version
if ($LASTEXITCODE -ne 0) { throw 'JDK çalıştırılamadı.' }
$sdkRoot = (Resolve-Path -LiteralPath '.\.android-sdk').Path
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath $sdkRoot
if ($LASTEXITCODE -ne 0) { throw 'Kalite kapısı başarısız oldu.' }
Remove-Variable -Name javaExe, sdkRoot
```

Güncel debug APK'yı `-r` ile kurun. `-r`, aynı imzalı mevcut debug kurulumunu
güncellerken uygulama verisini korur. Çıktı `Success` değilse ilerlemeyin:

```powershell
$installOutput = & $adb -s $deviceSerial install -r $apk 2>&1
$installExitCode = $LASTEXITCODE
$installText = ($installOutput | Out-String).Trim()
$installOutput
if ($installExitCode -ne 0 -or $installText -notmatch '(?m)^Success\s*$') {
    throw 'Debug APK kurulamadı; yukarıdaki adb hatasını çözmeden ilerlemeyin.'
}
$packagePath = & $adb -s $deviceSerial shell pm path $appId 2>&1
$packagePathExitCode = $LASTEXITCODE
$packagePathText = ($packagePath | Out-String).Trim()
$packagePath
if ($packagePathExitCode -ne 0 -or $packagePathText -notmatch '(?m)^package:') {
    throw 'Kurulan debug paketi cihazda doğrulanamadı.'
}
Remove-Variable -Name installOutput, installExitCode, installText, packagePath,
    packagePathExitCode, packagePathText
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
$uninstallOutput = & $adb -s $deviceSerial uninstall $appId 2>&1
$uninstallExitCode = $LASTEXITCODE
$uninstallOutput
if ($uninstallExitCode -ne 0 -or ($uninstallOutput | Out-String) -notmatch '(?m)^Success\s*$') {
    throw 'Uygulama kaldırılamadı.'
}
$installOutput = & $adb -s $deviceSerial install $apk 2>&1
$installExitCode = $LASTEXITCODE
$installOutput
if ($installExitCode -ne 0 -or ($installOutput | Out-String) -notmatch '(?m)^Success\s*$') {
    throw 'APK kurulamadı.'
}
Remove-Variable -Name eraseApproval
Remove-Variable -Name uninstallOutput, uninstallExitCode, installOutput, installExitCode
```

### Kimlik bilgilerinden önce zorunlu instrumented test

Mi Stick açık, uyanık ve kilitsizken bağlantıyı sabit tutun. Bu görev JDK 17,
proje-yerel Android SDK ve listede yalnız bir yetkili `device` gerektirir. Gradle
uygulama ile Android-test APK'larını gerektiğinde kendisi kurar; Android-test
APK'sını elle kurmayın veya başlatmayın. Henüz hiçbir Kamera Hesabı bilgisi
girmeden:

```powershell
if (-not $env:JAVA_HOME) { throw 'connectedDebugAndroidTest için JAVA_HOME/JDK 17 gereklidir.' }
$javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw 'JAVA_HOME geçerli bir JDK 17 klasörünü göstermiyor.'
}
$sdkRoot = (Resolve-Path -LiteralPath '.\.android-sdk').Path
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot

$deviceRows = @(& $adb devices -l)
if ($LASTEXITCODE -ne 0) { throw 'adb cihaz listesi okunamadı.' }
$authorizedRows = @($deviceRows | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' })
$problemRows = @($deviceRows | Where-Object { $_ -match '^\S+\s+(?:unauthorized|offline)(?:\s|$)' })
if ($authorizedRows.Count -ne 1 -or $problemRows.Count -ne 0) {
    $deviceRows
    throw 'Instrumented test için tam olarak bir yetkili Mi Stick bağlı olmalıdır.'
}

.\gradlew.bat --no-daemon connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { throw 'Mi Stick instrumented testleri başarısız oldu.' }
$connectedReport = Join-Path $repoRoot 'app\build\reports\androidTests\connected\debug\index.html'
$connectedResults = Join-Path $repoRoot 'app\build\outputs\androidTest-results\connected\debug'
if (-not (Test-Path -LiteralPath $connectedReport -PathType Leaf)) {
    throw 'Instrumented test HTML raporu bulunamadı.'
}
if (-not (Test-Path -LiteralPath $connectedResults -PathType Container)) {
    throw 'Instrumented test sonuç klasörü bulunamadı.'
}
$connectedReportItem = Get-Item -LiteralPath $connectedReport
$connectedResultFileCount = @(Get-ChildItem -LiteralPath $connectedResults -Recurse -File).Count
[pscustomobject]@{
    HtmlReport = $connectedReportItem.FullName
    HtmlReportLastWriteTime = $connectedReportItem.LastWriteTime
    ResultDirectory = $connectedResults
    ResultFileCount = $connectedResultFileCount
}
Remove-Variable -Name javaExe, sdkRoot, deviceRows, authorizedRows, problemRows,
    connectedReport, connectedResults, connectedReportItem, connectedResultFileCount
```

Komut, bağlı fiziksel Mi Stick üzerinde tamamlanıp exit code 0 vermelidir. Test
sayısını ve sonuç dosyasını güncel koşudan kaydedin; bir derleme başarısını cihaz
testi yerine yazmayın. Altyapı/bağlantı sorunu varsa `BLOCKED`, test assertion'ı
başarısızsa `FAIL` kaydedin ve kimlik bilgili senaryolara geçmeyin.

### Kurulumu doğrulama ve uygulamayı veri silmeden başlatma

Instrumented test PASS olduktan sonra paketin hâlâ kurulu olduğunu doğrulayıp
CamGrid TV'yi Android TV launcher'dan açabilirsiniz. Alternatif olarak şu güvenli
adb komutunu kullanın; bu akış uygulama verisini silmez:

```powershell
$packagePath = & $adb -s $deviceSerial shell pm path $appId 2>&1
$packagePathExitCode = $LASTEXITCODE
$packagePath
if ($packagePathExitCode -ne 0 -or ($packagePath | Out-String) -notmatch '(?m)^package:') {
    throw 'CamGrid TV debug paketi cihazda kurulu değil.'
}
$launchOutput = & $adb -s $deviceSerial shell am start -W `
    -a android.intent.action.MAIN `
    -c android.intent.category.LEANBACK_LAUNCHER `
    -p $appId 2>&1
$launchExitCode = $LASTEXITCODE
$launchText = ($launchOutput | Out-String).Trim()
$launchOutput
if ($launchExitCode -ne 0 -or $launchText -match '(?im)^Error:') {
    throw 'CamGrid TV başlatılamadı.'
}
Start-Sleep -Seconds 2
$appPid = (& $adb -s $deviceSerial shell pidof $appId 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or -not $appPid) {
    throw 'CamGrid TV süreci başlatma sonrasında bulunamadı.'
}
Remove-Variable -Name packagePath, packagePathExitCode, launchOutput,
    launchExitCode, launchText, appPid
```

`pm path` paketi bulamazsa test altyapısı uygulamayı kaldırmış olabilir. Henüz
Kamera Hesabı girmeden yukarıdaki doğrulanmış `install -r` bloğunu bir kez daha
çalıştırın ve sonra bu doğrulama/başlatma bloğunu yineleyin.

Launcher'dan açmak için **Uygulamalar > CamGrid TV** banner'ını seçin. Banner hemen
görünmezse launcher'ı yenileyin veya Mi Stick'i bir kez yeniden başlatın; APK'yı
tekrar tekrar kaldırıp kurmayın.

### Yalnız kimlik bilgisiz deneme için USB bellek fallback'i

ADB bağlantısı kurulamıyor ama Mi Stick USB depolamayı destekliyorsa, yalnız
kimlik bilgisiz kurulum/açılış denemesi için:

1. Bilgisayarda yalnız `app-debug.apk` dosyasının SHA-256 özetini bu belgenin APK
   bölümündeki komutla doğrulayın.
2. Dosyayı güvenilir ve temiz bir USB belleğe Windows Dosya Gezgini ile kopyalayın
   ve belleği **Güvenle Kaldır** ile çıkarın.
3. Mi Stick'e üreticiyle uyumlu OTG/powered hub üzerinden takın. Güvenilir bir
   yerel dosya yöneticisinde `app-debug.apk` dosyasını açın.
4. Android isterse **Bilinmeyen uygulamaları yükle** iznini yalnız o dosya
   yöneticisi için geçici olarak verin. Kurulum tamamlanınca izni tekrar kapatın.
5. Launcher'dan **CamGrid TV**'yi açıp banner, ilk ekran, D-pad odağı ve crash
   olmamasını kontrol edin. Kamera Hesabı girmeyin veya gerçek yayına başlamayın.

USB bellekte `app-debug-androidTest.apk` veya `app-release-unsigned.apk` kurmayın.
Bu fallback adb sağlamadığı için instrumented test, `pm path`, logcat, force-stop
kanıtı ve bellek ölçümü yapılamaz; bunları `BLOCKED`, bağlantı yöntemini raporda
`UsbStorageOnly` yazın. Kimlik bilgili tüm kamera/yayın senaryoları da `BLOCKED`
kalır. Tam fiziksel kabul için daha sonra adb dallarından birini kurmak gerekir.

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

## Ekran ekran hızlı uygulama testi

Bu bölüme yalnız `connectedDebugAndroidTest` PASS olduktan sonra geçin. Test
`FAIL` veya `BLOCKED` ise Kamera Hesabı girmeyin; kimlik bilgili tüm kamera/yayın
senaryolarını `BLOCKED` bırakın. Kamera bağlantısından önce VLC, ffplay, NVR
önizlemesi, tarayıcı yayını ve üretici uygulamasındaki canlı görünümü tamamen
kapatın.

1. Her kamera için Tapo uygulamasında **Live View > Settings > Advanced Settings
   > Camera Account** yolundan ayrı yerel Kamera Hesabı oluşturulduğunu
   doğrulayın. Bu hesap Tapo bulut hesabı, Wi-Fi veya modem parolası değildir.
2. CamGrid TV'yi launcher'dan açın. `$sdkLevel` en az `37` ve **Yerel ağ izni
   gerekli** ekranı görünürse **Yerel ağa izin ver** adımını onaylayın. İzin daha
   önce verilmişse ekranın yeniden görünmemesi normaldir ve `FAIL` değildir. Daha
   eski SDK seviyelerinde bu runtime izin ekranını beklemeyin; doğrudan keşfe
   geçilmesi normaldir.
3. **Kameraları bul** ile taramayı başlatın. Tarama zaten otomatik başladıysa
   bitmesini bekleyin. Sonuç yoksa aynı ağ, misafir Wi-Fi/AP isolation ve ONVIF
   ayarlarını kontrol edip **Kameraları yeniden tara**yı kullanın.
4. C500 ve C510W kartlarını kumandanın D-pad/OK tuşlarıyla ayrı ayrı seçin.
   Seçili sayaçta beklediğiniz kamera sayısını doğrulayın; yinelenen kamera kaydı
   olmamalıdır.
5. Tarama bittikten sonra **Hesap kurulumuna devam et** düğmesiyle kurulum
   ekranına geçin. Kamera adlarını gerekirse IP içermeyen ayırt edilebilir adlarla
   düzenleyin.
6. D-pad ile **Kullanıcı adı** alanına gelin. Yalnız odaklandığında ekran
   klavyesinin kapalı kaldığını ve yön tuşlarının metin imlecine takılmadan komşu
   kontrollere geçtiğini doğrulayın. OK ile düzenleme/klavyeyi açın; Back veya
   klavyedeki **Bitti** ile gezinme moduna dönün. Aynısını **Parola** alanında
   tekrarlayın; parolanın maskeli kaldığını doğrulayın ve bilgileri yalnız TV
   arayüzünde girin. Kameralar gerçekten aynı yerel hesabı kullanmıyorsa **Bu
   hesabı seçili tüm kameralarda kullan** seçeneğini açmayın.
7. Sağ paneldeki tek tam genişlikte ana eylem başlangıçta **Bağlantıyı doğrula**
   olarak görünür. Eksik kullanıcı adı/parola taslağında devre dışı, iki alan
   tamamlandığında etkin olmalıdır. Buna veya ilgili kameradaki **Bağlantıyı test
   et**e basın. Bulunmuş olmak yalnız ONVIF keşfinin başarılı olduğunu gösterir;
   izleme için kimlik doğrulamalı `/stream2` önizlemesinin **Canlı** olması
   gerekir. Parola/URI/IP bilgisini fotoğrafa, sohbete veya rapora almayın.
8. En az bir başarılı testten ve seçili her kameraya hesap profili
   kaydedildikten sonra aynı ana eylemin **N kamerayı izle**ye dönüştüğünü
   doğrulayın ve seçin. Bu C500/C510W koşusunda beklenen metin **2 kamerayı izle**
   olmalıdır; iki `/stream2` kutucuğunun açıldığını doğrulayın. Ayrı hesap modu
   kullanılıyorsa her hedef kamera için profil kaydedilmeden bu geçiş oluşmaz.
9. D-pad ile iki tile arasında gezinip OK ile `/stream1` tam ekranı açın. Back ile
   aynı tile odağına dönüldüğünü her iki kamera için kontrol edin.
10. Ardından aşağıdaki 15 dakikalık dayanıklılık, lifecycle, ağ, yanlış parola,
    gizlilik ve decoder senaryolarını sırayla yürütün.

## Gerçek kabul senaryoları

### 1. C500 ve C510W otomatik keşfi

- Önkoşul: İki kamera açık, aynı LAN'da ve ONVIF etkin; temiz başlangıç PASS.
- Adım: Yalnız `$sdkLevel` en az `37` ve Yerel Ağ izin ekranı görünürse izni
  verin. İzin zaten verilmişse ekranın yokluğunu `FAIL` saymayın. Daha eski
  sürümde izin ekranını beklemeden PowerShell kronometresiyle taramayı başlatın ve
  terminal duruma ulaştığında süreyi durdurun.
- Beklenen: C500 ve C510W birer kez, gerçek IP rapora yazılmadan ayırt edilebilir
  adlarla görünür; yinelenen ProbeMatch satırları ikinci cihaz oluşturmaz.
- Kanıt: Model başına görünen kayıt sayısı, izin ekranı çıktıysa ona kadar geçen
  süre (çıkmadıysa `önceden verilmiş/uygulanamaz`) ve taramanın tamamlanma süresi;
  adresleri redakte edin.

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
- Adım: Yön tuşlarıyla iki tile ve duvar eylemleri arasında dolaşın. Yeniden
  tarama eylemi görünüyorsa bir tile'dan Yukarı ile üst çubuktaki eyleme gidin.
- Beklenen: Odak çerçevesi her zaman görünür, öngörülebilir ve dokunma gerektirmez.
- Kanıt: İzlenen odak sırası; seri numarası içermeyen fotoğraf isteğe bağlıdır.

### 5a. Metin alanı ve ekran klavyesi

- Önkoşul: Kamera kurulum ekranı açık ve fiziksel kumanda bağlı.
- Adım: D-pad ile Kullanıcı adı, Parola ve kamera adı alanlarına ayrı ayrı gelin;
  gezinme modunda dört yönü deneyin. Her alanda klavye kapalıyken OK'a basın;
  düzenlemeden önce Back'i, başka bir tekrarda klavyedeki **Bitti**yi kullanın.
- Beklenen: Yalnız D-pad odağı ekran klavyesini açmaz ve oklar metin imlecine
  takılmaz; OK düzenleme modunu ve klavyeyi açar; Back ile Bitti düzenlemeyi
  kapatıp uygulamadan çıkmadan gezinmeye döndürür. Parola açık metin görünmez.
- Kanıt: Alan başına gezinme/OK/Back/Bitti sonucu; girilen değerleri veya klavye
  önerilerini fotoğrafa ve rapora almayın.

### 5b. Uyarlanabilir doğrulama ve izleme eylemi

- Önkoşul: En az iki kamera seçili ve kurulum ekranı açık.
- Adım: Boş, tek alanı dolu ve iki alanı dolu hesap taslaklarında ana eylemi
  gözleyin. Doğrulamayı başlatın ve `/stream2` Canlı durumuna ulaşmasını bekleyin.
- Beklenen: Eksik taslakta **Bağlantıyı doğrula** devre dışıdır; geçerli kayıtlı
  profil veya tamamlanmış taslakla etkinleşir. İşlem sürerken durum açıkça
  gösterilir ve tamamlanınca odak aynı ana eyleme döner. Seçili her hedefte profil
  ve en az bir başarılı canlı test olduğunda metin **N kamerayı izle** olur.
- Kanıt: Kimlik bilgisi içermeyen eylem metni, durum sırası ve odak sonucu.

### 5c. Duvar üst çubuğundan yeniden tarama

- Önkoşul: Kamera duvarı açık; sağlıklı ve hata durumu ayrı ayrı üretilebilir.
- Adım: Tüm yayınlar canlıyken üst çubuğu gözleyin. Sonra bir yayını Offline,
  Retrying veya PlaybackFailed durumuna getirip D-pad Yukarı ile **Kameraları
  yeniden tara** eylemine ulaşın ve OK'a basın.
- Beklenen: Eylem sağlıklı duvarda gizli, belirtilen hata durumlarında görünür ve
  kamera kutularıyla çakışmadan odaklanabilir. OK keşif ekranına döndürür ve yeni
  taramayı başlatır.
- Kanıt: Sağlıklı/hatalı görünürlük, odak yolu ve yeni taramanın başladığı; özel
  ağ veya kamera bilgisi kaydedilmez.

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
- Beklenen: Tile'lar offline olur, üst çubukta yeniden tarama eylemi görünür,
  sıkı retry döngüsü oluşmaz ve ağ dönünce kontrollü biçimde yeniden bağlanır.
  Toparlanma ölçümü bitene kadar yeniden tarama eylemini çalıştırmayın; eylem
  geçişini Senaryo 5c'de ayrı sınayın.
- Kanıt: Kesinti süresi, toparlanma süresi ve crash/retry özeti.

Wi-Fi kesintisi `WirelessPairing` veya `LegacyTcp5555` adb transportunu da
düşürebilir. Sonraki force-stop/logcat/bellek adımlarından **önce** güncel TV
ayarındaki bağlantı hedefiyle adb'yi yeniden doğrulayın. `DirectUsb` için mevcut
seri korunur; kablosuz yöntemlerde eski çevrimdışı kaydı yeni bağlantı başarıyla
kurulduktan sonra ayırın:

```powershell
$previousDeviceSerial = $deviceSerial
switch ($connectionKind) {
    'DirectUsb' {
        # Ağ kesintisi USB adb seri değerini değiştirmemelidir.
    }
    'WirelessPairing' {
        $currentWirelessTarget = Read-Host 'TV ana Kablosuz hata ayıklama ekranındaki güncel IP:port'
        $reconnectOutput = & $adb connect $currentWirelessTarget 2>&1
        $reconnectExitCode = $LASTEXITCODE
        $reconnectText = ($reconnectOutput | Out-String).Trim()
        $reconnectOutput
        if ($reconnectExitCode -ne 0 -or $reconnectText -match '(?i)\b(?:failed|error)\b') {
            throw 'Kablosuz adb yeniden bağlanamadı.'
        }
        $deviceSerial = $currentWirelessTarget
        Remove-Variable -Name currentWirelessTarget
    }
    'LegacyTcp5555' {
        $currentTvHost = Read-Host 'Mi Stick güncel yerel IPv4 adresi'
        $parsedCurrentHost = $null
        $currentHostIsValid = [System.Net.IPAddress]::TryParse(
            $currentTvHost,
            [ref]$parsedCurrentHost
        ) -and
            $parsedCurrentHost.AddressFamily -eq
                [System.Net.Sockets.AddressFamily]::InterNetwork -and
            $parsedCurrentHost.ToString() -ceq $currentTvHost
        if (-not $currentHostIsValid) {
            throw 'Güncel Mi Stick adresi kanonik bir IPv4 değeri olmalıdır.'
        }
        $currentOctets = $parsedCurrentHost.GetAddressBytes()
        $currentHostIsPrivate = $currentOctets[0] -eq 10 -or
            ($currentOctets[0] -eq 172 -and $currentOctets[1] -ge 16 -and
                $currentOctets[1] -le 31) -or
            ($currentOctets[0] -eq 192 -and $currentOctets[1] -eq 168)
        if (-not $currentHostIsPrivate) {
            throw 'Güncel Mi Stick adresi RFC1918 yerel ağda olmalıdır.'
        }
        $currentTcpTarget = '{0}:5555' -f $currentTvHost
        $reconnectOutput = & $adb connect $currentTcpTarget 2>&1
        $reconnectExitCode = $LASTEXITCODE
        $reconnectText = ($reconnectOutput | Out-String).Trim()
        $reconnectOutput
        if ($reconnectExitCode -ne 0 -or $reconnectText -match '(?i)\b(?:failed|error)\b') {
            throw 'Legacy TCP 5555 adb yeniden bağlanamadı.'
        }
        $tvHost = $currentTvHost
        $tcpTarget = $currentTcpTarget
        $deviceSerial = $currentTcpTarget
        Remove-Variable -Name currentTvHost, parsedCurrentHost, currentHostIsValid,
            currentOctets, currentHostIsPrivate, currentTcpTarget
    }
    default {
        throw 'Bu senaryo yalnız doğrulanmış adb bağlantısıyla çalıştırılabilir.'
    }
}

$reconnectedState = (& $adb -s $deviceSerial get-state 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $reconnectedState -cne 'device') {
    throw 'Güncel Mi Stick adb transportu device durumunda değil.'
}
if ($previousDeviceSerial -cne $deviceSerial) {
    & $adb disconnect $previousDeviceSerial | Out-Null
}
Remove-Variable -Name previousDeviceSerial, reconnectOutput, reconnectExitCode,
    reconnectText, reconnectedState -ErrorAction SilentlyContinue
```

Standart kablosuz eşleşme tamamen kaybolduysa yeni eşleme koduyla
`WirelessPairing` dalını yeniden çalıştırın. ADB geri getirilemiyorsa sonraki adb
kanıtlarını `BLOCKED` bırakın. Özellikle `LegacyTcp5555` için TV geliştirici
ayarlarından hata ayıklamayı kapatın veya cihazı yeniden başlatın; açık kalmış
olabilecek 5555 dinleyicisini yalnız `adb disconnect` ile kapanmış saymayın.

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
  if (-not $beforePid) { throw 'Force-stop öncesinde uygulama süreci çalışmıyor.' }
  & $adb -s $deviceSerial shell am force-stop $appId
  $forceStopExitCode = $LASTEXITCODE
  if ($forceStopExitCode -ne 0) { throw 'am force-stop başarısız oldu.' }
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
  Remove-Variable -Name beforePid, forceStopExitCode, stoppedPid, relaunchTimer,
      launchOutput, launchExitCode, afterPid
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
- Adım: Tüm cihazın log tamponunu silmeden ve diğer uygulamaların kayıtlarını
  karıştırmadan CamGrid TV sürecini yerel terminalde canlı inceleyin:

  ```powershell
  $appPid = (& $adb -s $deviceSerial shell pidof $appId 2>$null | Out-String).Trim()
  if ($LASTEXITCODE -ne 0 -or -not $appPid) {
      throw 'CamGrid TV süreci logcat incelemesi için çalışmıyor.'
  }
  & $adb -s $deviceSerial logcat --pid=$appPid -T 1
  ```

  İnceleme bitince `Ctrl+C` kullanın. Uygulama yeniden başlar ve PID değişirse
  komutu durdurup güncel PID'yi almak için bu bloğu yeniden çalıştırın.
- Beklenen: Kullanıcı adı, parola, credential taşıyan tam RTSP URI, ev IP'si veya
  hassas anahtar malzemesi görünmez.
- Kanıt: Yalnız `sensitive matches: 0` veya redakte edilmiş hata sınıfı; ham logu
  commit etmeyin.

### 14. Bellek, decoder ve tekil hata izolasyonu

- Önkoşul: İki yayınlı duvar canlı; diğer RTSP istemcileri kapalı.
- Adım: Başlangıç ölçümünü alın, kronometre çalışırken duvarı en az 15 dakika açık
  tutun ve bitiş ölçümünü alın. Bu sürede mümkünse bir kamerayı kısa süre kapatıp
  diğer tile'ın etkilenmediğini gözlemleyin:

  ```powershell
  $memoryTimer = [System.Diagnostics.Stopwatch]::StartNew()
  $memoryStartPid = (& $adb -s $deviceSerial shell pidof $appId 2>$null | Out-String).Trim()
  if ($LASTEXITCODE -ne 0 -or -not $memoryStartPid) {
      throw 'Başlangıç bellek ölçümünden önce CamGrid TV süreci bulunamadı.'
  }
  $memoryStart = & $adb -s $deviceSerial shell dumpsys meminfo $appId 2>&1
  $memoryStartExitCode = $LASTEXITCODE
  $memoryStartText = ($memoryStart | Out-String)
  $memoryStart
  if ($memoryStartExitCode -ne 0 -or $memoryStartText -match '(?i)No process found') {
      throw 'Başlangıç bellek ölçümü alınamadı.'
  }
  Read-Host 'Duvar en az 15 dakika çalıştığında Enter'
  $memoryTimer.Stop()
  if ($memoryTimer.Elapsed.TotalMinutes -lt 15) {
      throw 'Bellek gözlem süresi 15 dakikadan kısa.'
  }
  $memoryEndPid = (& $adb -s $deviceSerial shell pidof $appId 2>$null | Out-String).Trim()
  if ($LASTEXITCODE -ne 0 -or -not $memoryEndPid) {
      throw 'Bitiş bellek ölçümünden önce CamGrid TV süreci bulunamadı.'
  }
  $memoryEnd = & $adb -s $deviceSerial shell dumpsys meminfo $appId 2>&1
  $memoryEndExitCode = $LASTEXITCODE
  $memoryEndText = ($memoryEnd | Out-String)
  $memoryEnd
  if ($memoryEndExitCode -ne 0 -or $memoryEndText -match '(?i)No process found') {
      throw 'Bitiş bellek ölçümü alınamadı.'
  }
  'Bellek gözlem süresi: {0:N1} dakika' -f $memoryTimer.Elapsed.TotalMinutes
  Remove-Variable -Name memoryTimer, memoryStartPid, memoryStart,
      memoryStartExitCode, memoryStartText, memoryEndPid, memoryEnd,
      memoryEndExitCode, memoryEndText
  ```

- Beklenen: Bellek kontrolsüz büyümez; bir tile hatası diğer canlı tile'ı
  kapatmaz; decoder yetersizliği güvenli hata olarak görünür.
- Kanıt: Başlangıç/15. dakika redakte edilmiş toplam bellek özeti, tile durumları
  ve crash sayısı.

### 15. Üç yayınlı grid (isteğe bağlı)

- Önkoşul: Üçüncü gerçek ONVIF/RTSP kamera veya benzersiz bir ONVIF WS-Discovery
  endpoint'i ilan edip uyumlu H.264 `/stream2` sunan yerel test cihazı fiziksel
  Mi Stick'ten erişilebilir. Tam ekran da sınanacaksa `/stream1` sunmalıdır.
  Üretim arayüzüne manuel IP aracı eklenmez; çıplak RTSP sunucusu tek başına
  yeterli değildir ve diğer RTSP istemcileri kapalıdır.
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

Önce kullandığınız `$connectionKind` dalına göre adb transportunu güvenli biçimde
kapatın. `LegacyTcp5555` için `usb` komutu hâlâ etkin ağ transportuna gönderilir;
`adb tcpip 5555` sonrasında eski `$usbSerial` çoğu cihazda artık mevcut değildir.
Yalnız `adb disconnect` çalıştırmak cihazdaki TCP 5555 dinleyicisini kapatmaz.
`DirectUsb` için ise `adb disconnect` kullanılmaz:

```powershell
$debugSettingInstruction = $null
$automaticCleanupFailure = $null
switch ($connectionKind) {
    'LegacyTcp5555' {
        $cleanupErrors = [System.Collections.Generic.List[string]]::new()
        $usbModeOutput = & $adb -s $tcpTarget usb 2>&1
        $usbModeExitCode = $LASTEXITCODE
        $usbModeOutput
        if ($usbModeExitCode -ne 0) {
            $cleanupErrors.Add('adbd USB moduna alınamadı.')
        }

        Start-Sleep -Seconds 2
        $tcpProbe = [System.Net.Sockets.TcpClient]::new()
        $probeAsync = $null
        $probeCompleted = $false
        $probeConfirmedClosed = $false
        try {
            $probeAsync = $tcpProbe.BeginConnect($tvHost, 5555, $null, $null)
            $probeCompleted = $probeAsync.AsyncWaitHandle.WaitOne(3000, $false)
            if (-not $probeCompleted) {
                $cleanupErrors.Add('TCP 5555 port probu zaman aşımına uğradı; kapanış doğrulanamadı.')
            } else {
                try {
                    $tcpProbe.EndConnect($probeAsync)
                    if ($tcpProbe.Connected) {
                        $cleanupErrors.Add('Cihazın TCP 5555 portu hâlâ erişilebilir.')
                    } else {
                        $cleanupErrors.Add('TCP 5555 port probu belirsiz sonuç verdi.')
                    }
                } catch [System.Net.Sockets.SocketException] {
                    if ($_.Exception.SocketErrorCode -eq
                        [System.Net.Sockets.SocketError]::ConnectionRefused) {
                        $probeConfirmedClosed = $true
                    } else {
                        $cleanupErrors.Add('TCP 5555 port kapanışı ağ hatası nedeniyle doğrulanamadı.')
                    }
                } catch {
                    $cleanupErrors.Add('TCP 5555 port kapanışı doğrulanamadı.')
                }
            }
        } catch {
            $cleanupErrors.Add('TCP 5555 port probu başlatılamadı.')
        } finally {
            if ($null -ne $probeAsync) {
                $probeAsync.AsyncWaitHandle.Close()
            }
            $tcpProbe.Close()
        }
        if (-not $probeConfirmedClosed -and $cleanupErrors.Count -eq 0) {
            $cleanupErrors.Add('TCP 5555 portunun kapandığı doğrulanamadı.')
        }

        $disconnectOutput = & $adb disconnect $tcpTarget 2>&1
        $disconnectExitCode = $LASTEXITCODE
        $disconnectText = ($disconnectOutput | Out-String).Trim()
        $disconnectOutput
        $alreadyAbsentAfterClosedProbe = $probeConfirmedClosed -and
            $disconnectText -match '(?i)(?:no such device|not found|not connected)'
        if ($disconnectExitCode -ne 0 -and -not $alreadyAbsentAfterClosedProbe) {
            $cleanupErrors.Add('Yerel adb TCP transport kaydı ayrılamadı.')
        }
        if ($cleanupErrors.Count -ne 0) {
            $automaticCleanupFailure = $cleanupErrors -join ' '
            $debugSettingInstruction = 'OTOMATİK KAPANIŞ DOĞRULANAMADI: TV geliştirici ayarlarında USB/ağ hata ayıklamayı kapatın veya Mi Stick cihazını yeniden başlatın.'
        } else {
            $debugSettingInstruction = 'TV ayarlarında USB hata ayıklamayı kapatın ve gerekmezse USB hata ayıklama yetkilerini iptal edin.'
        }
        Remove-Variable -Name usbSerial, tvHost, tcpTarget, cleanupErrors,
            usbModeOutput, usbModeExitCode, tcpProbe, probeAsync, probeCompleted,
            probeConfirmedClosed, disconnectOutput, disconnectExitCode,
            disconnectText, alreadyAbsentAfterClosedProbe
    }
    'WirelessPairing' {
        $disconnectOutput = & $adb disconnect $deviceSerial 2>&1
        $disconnectExitCode = $LASTEXITCODE
        $disconnectOutput
        if ($disconnectExitCode -ne 0) {
            Write-Warning 'Yerel kablosuz adb transport kaydı ayrılamadı; TV ayarından kapatma yine zorunludur.'
        }
        $debugSettingInstruction = 'TV ayarlarında Kablosuz hata ayıklamayı kapatın ve eşleşmiş cihazlar listesinden bu test bilgisayarını unutun.'
        Remove-Variable -Name disconnectOutput, disconnectExitCode
    }
    'DirectUsb' {
        $debugSettingInstruction = 'TV ayarlarında USB hata ayıklamayı kapatın, gerekmezse yetkileri iptal edin ve ardından veri USB/OTG bağlantısını güvenle çıkarın.'
    }
    default {
        throw 'Bilinmeyen veya boş connectionKind; hangi bağlantı dalını kullandığınızı doğrulayın.'
    }
}

$debugSettingInstruction
if ($automaticCleanupFailure) {
    Write-Warning $automaticCleanupFailure
}
$debugOffConfirmation = Read-Host 'Yukarıdaki TV ayarını tamamladıysanız KAPALI yazın'
if ($debugOffConfirmation -cne 'KAPALI') {
    throw 'Hata ayıklama ayarı kapatıldığı doğrulanmadı.'
}
if ($automaticCleanupFailure) {
    throw ('Otomatik TCP 5555 kapanışı doğrulanamadı; manuel kapatma onaylandı fakat sonucu BLOCKED kaydedin: ' + $automaticCleanupFailure)
}
Remove-Variable -Name debugSettingInstruction, debugOffConfirmation
Remove-Variable -Name automaticCleanupFailure
Remove-Variable -Name deviceSerial, adb, apk, appId, connectionKind, sdkLevel,
    repoRoot -ErrorAction SilentlyContinue
```

USB modu komutu ağ transportundan kullanılamıyorsa veya erişim probu portu hâlâ
açık bulursa cihazı yeniden başlatın ya da geliştirici ayarlarından ağ hata
ayıklamasını kapatın. Yalnız `adb disconnect` veya boş `adb devices` çıktısı,
cihazın 5555 portunu kapattığını kanıtlamaz. USB ile yeniden bağlanacaksanız port
erişilemez olduktan sonra veri kablosu/OTG'yi takın ve yeni yetki istemini cihaz
ekranından onaylayın.

Özellikle şu sonuçları ayrı satırlarda tutun: Mi Stick
`connectedDebugAndroidTest`, temiz başlangıç, C500 keşif, C510W keşif, C500
`/stream2`, C510W `/stream2`, iki yayın/15 dakika, metin alanı browse/edit ve IME,
uyarlanabilir Doğrula → N kamerayı izle, duvar üst çubuğu yeniden tarama, 960x540
ve 1280x720 safe-area/odak görünümü, C500 `/stream1` tam ekran, C510W `/stream1`
tam ekran, Mi Stick D-pad, lifecycle, API 37 izin iptali, ağ geri dönüşü, yanlış
parola, force-stop/kalıcılık, log gizliliği ve decoder/bellek. Her fiziksel cihaz
sonucu kanıt yürütülene kadar `BLOCKED` kalır.

`UsbStorageOnly` fallback'ini kullandıysanız yukarıdaki adb bloğunu çalıştırmayın.
USB belleği güvenle çıkarın, dosya yöneticisinin **Bilinmeyen uygulamaları yükle**
iznini kapatın; instrumented test ile kimlik bilgili tüm kamera/yayın senaryolarını
`BLOCKED` kaydedin.

## Yetkili kaynaklar

- [Android Debug Bridge (adb): kablosuz hata ayıklama ve APK kurma](https://developer.android.com/tools/adb)
- [Android TV uygulaması oluşturma ve Leanback launcher](https://developer.android.com/training/tv/get-started/create)
- [Komut satırından Android testleri çalıştırma](https://developer.android.com/studio/test/command-line)
- [Tapo Camera Account oluşturma](https://www.tapo.com/us/faq/76/?app=web)
- [C500/C510W ONVIF Profile S, portlar ve stream yolları](https://www.tp-link.com/nordic/support/faq/4465/)

Android'ın TV/Wear için standart kablosuz hata ayıklama belgesi API 33+ desteğini
açıklar; üretici menüsü görünmüyorsa bu özelliği var saymayın. Tapo Camera Account
yolu **Live View > Settings > Advanced Settings > Camera Account** şeklindedir ve
Tapo bulut hesabından ayrıdır. C500/C510W için ONVIF/RTSP desteği, varsayılan ONVIF
2020 ve RTSP 554 portları ile `/stream1` ve `/stream2` yolları üretici kaynağından
doğrulanmalıdır; portları internete açmayın.
