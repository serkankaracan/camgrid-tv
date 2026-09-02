# CamGrid TV

CamGrid TV, aynı güvenilir yerel ağdaki ONVIF/RTSP kameraları bulan ve seçilen
canlı yayınları Android TV / Google TV üzerinde tek bir dinamik ızgarada gösteren
yerel-öncelikli bir uygulamadır. Backend, bulut hesabı, telemetri ve uzaktan erişim
kullanmaz.

> CamGrid TV bağımsız ve resmî olmayan bir açık kaynak projesidir. TP-Link veya
> Tapo tarafından geliştirilmez, desteklenmez ya da onaylanmaz. Üründe bu markalara
> ait logo veya görsel varlık kullanılmaz.

İlk alpha sürümünün uygulama, güvenlik ve test dokümantasyonu geliştirme boyunca
bu depoda tutulur. Derleme ve kurulum yönergeleri uygulanabilir proje iskeletiyle
birlikte tamamlanacaktır.

## Sabit ürün sınırları

- Android TV / Google TV ve yalnızca kumanda (D-pad, OK, Back)
- ONVIF WS-Discovery ile otomatik keşif
- Media3 RTSP; grid için `/stream2`, tam ekran için `/stream1`
- Kamera hesabı bilgileri cihazda Android Keystore destekli AES/GCM ile şifreli
- Yalnızca yerel ağ; port yönlendirme, cloud, kayıt, PTZ, ses ve analitik yok

Hedef kabul modelleri Tapo C500 ve C510W'dir. Fiziksel doğrulama durumu
[test raporunda](docs/TEST_REPORT.md) açıkça belirtilir; model adı doğrulanmış test
iddiası anlamına gelmez.

## Lisans

Apache License 2.0. Ayrıntılar için [LICENSE](LICENSE) dosyasına bakın.

## English summary

CamGrid TV is a local-only Android TV viewer that discovers ONVIF cameras and
shows selected RTSP streams in a remote-friendly grid. It is unofficial and is
not affiliated with or endorsed by TP-Link/Tapo.
