# Spec qo'shimchasi: sessiya 3 (FitCloudPro referens, tarix, o'lchov, ciferblat)

Sana: 2026-10-12 (qurilma vaqti 00:00–00:40). Manba: `captures/s5-sync/` (bugreport 1–4, `timeline.txt`).
Bu fayl repoda: faqat protokol baytlari va tuzilma. MAC manzil, hisob satri, sog'liq qiymatlari va joy matni yozilmaydi.

Vaqt eslatmasi: `tools/snoop/parse_btsnoop.py` chiqishidagi vaqt qurilma vaqtidan 5 soat oldinda ko'rinadi. Tahlilda 5 soat ayriladi.

Belgilar: [T] tasdiqlangan, [E] taxmin, [N] noma'lum. [N] kodga kirmaydi.

## 1. Tarix sinxroni (TYPE 0x05)

| Band | Holat | Belgi |
|---|---|---|
| Ketma-ketlik: `05/0005` → `05/0006` → har hisoblagich `05/0001` → `05/0007`, `05/0008` → (N > 0 bo'lsa) `05/0030` → `05/0020` (keyingi hisoblagichdan oldin va `05/0021` dan oldin) → `05/0021` | 6 ta sinxronda kuzatildi (FitCloudPro: 00:07, 00:09, 00:14, 00:21; Orvell: 23:57; s4 dan oldin) | [T] |
| `05/0006` (4 bayt BE) = kutilayotgan jami bayt. 00:14 da 46 = 14+13+9+10; 00:21 da 42 = 20+10+12 | 2 ta sinxronda [T] | [T] |
| `05/0007` va `05/0008` teng (yozuv hajmi N) | 9 ta hisoblagich juftligidan 8 tasida teng. Istisno: 00:07 sinxroni, hisoblagich 01: `0007` = 28, `0008` = 0, `05/0030` kelmadi, lekin FitCloudPro baribir `05/0020` yubordi | [T] / istisno [N] |
| `05/0030` boshi: `0001 354C` yoki `0002 354C`, keyin daqiqa (BE, 2 bayt), keyin `0005` (yoki `000x`), keyin qiymat baytlari | boshi [T], qiymat ma'nosi [N] | [T] / [N] |
| Hisoblagich 83, 84, 85 ham xuddi shunday (N = 0 yoki yozuv) | [T] (kuzatuv) | [T] |
| Yakundan keyin `05/0022` (30 bayt) davriy keladi, har gal yangi SEQ bilan. Ilova unga ACK yubormaydi | [N] (ma'nosi) | [N] |
| Orvell sinxroni 23:57: pending 46, lekin hamma hisoblagich N = 0 va `05/0030` yo'q. Bir xil sinxron 00:14 da FitCloudPro bilan yozuvlar bilan keldi | sabab [N] | [N] |

## 2. Jonli o'lchov (TYPE 0x05, ID 0x0023 va 0x0024)

| Band | Holat | Belgi |
|---|---|---|
| Boshlash `0x0023` = `00 01 05 01` (yurak urishi), `00 02 05 01` (SpO₂), `00 04 05 01` (qon bosimi). To'xtatish `00 00 05 00` | 3 rejim, har biri kuzatilgan | [T] |
| `0x0024` kadr: VLEN 11, bayt 0–1 `35 4C`, bayt 2–3 `00 01`, bayt 4–5 daqiqa (BE) | tuzilma [T], daqiqa [E] | [T] / [E] |
| Oraliq taxminan 2,9 s. Boshlashdan 2–3 s keyin birinchi kadr. Boshida bir necha kadr nol (sensor isinadi) | [T] | [T] |
| Bayt 6: yurak urishi nomzodi (yurak rejimi). Yakuniy kadr 1 ta o'lchovda ekrandagi yakuniy qiymatga teng | 1 ta o'lchov | [E] |
| Bayt 7: SpO₂ nomzodi (SpO₂ rejimi). Qiymat o'zgaradi, yakuniy kadr ekranga yaqin, lekin 1 kadr farq bor | 1 ta o'lchov | [E] |
| Bayt 8–9: qon bosimi nomzodi (qon bosimi rejimi). Juftlik, ekrandagi yakuniy qiymat bilan aniq teng emas | 1 ta o'lchov | [E] |

## 3. Ciferblat yuborish (kanal 0x002F va 0x0031)

| Band | Holat | Belgi |
|---|---|---|
| "My Dial"dan "PUSH" (771,8 KB) telefon tomonidan `0x002F` ga `write_cmd` bilan 2214 ta bo'lak bilan yuborildi. Bo'lak hajmi 240, 216 yoki 128 bayt | soni va hajm [T] | [T] |
| Boshqarish kanali `0x0031`: telefon `write_req` 5 bayt (`0a 00 08 xx xx`), soat `notify` 7 bayt (`10 0a 01 00 xx xx 00`). 497 kadr | kadr shakli [T], ma'nosi [N] | [T] / [N] |
| Yakuniy soat xabari `0x0014` 59 bayt (00:29:07) | ma'nosi [N] | [N] |
| UI: "Select the position to be replaced". Tanlangan o'rin (1,0 MB) almashtiriladi, eski ciferblat "My Dial" ro'yxatida qoladi | UI kuzatuvi | [T] |

Eslatma: yuborilgan ciferblatning soat ekranida ko'rinishi foydalanuvchi tomonidan tasdiqlanishi kerak. Bu kuzatuv hali dekod qilinmagan.

## 4. FitCloudPro ulanishda yuborgan yozuvlar (kodga kirmaydi)

- `02/0006` (18 bayt): maqsad yozuvi, har ulanishda yuboriladi. Uzunlik [T], ma'nosi [N] (spec v0.4). Orvell bu yozuvni yubormaydi.
- `02/0036` (62 bayt): joy matni. Saqlanmaydi, kodga kirmaydi.
- `02/0044` javobi (6 bayt): [N].
- `02/0001` (vaqt): har ulanishda, [T] (spec v0.5).

## 5. Ochiq savollar

1. `0x0020` soat tomonida nima qiladi: tarixni o'chiradimi? [N]. Orvell hozir yozuvni DB ga yozgandan keyin yuboradi.
2. 23:57 dagi pending 46 bilan N = 0 nima? [N].
3. Yurak urishi, SpO₂ va qon bosimi mapping: [E]. [T] bo'lishi uchun har bir ko'rsatkich bo'yicha 3+ juft (ekran qiymati va vaqt) kerak.
4. Ciferblat yuborish oqimi va `0x0031` boshqarish kadrlarining ma'nosi [N]. Tahlil uchun to'liq kadr ketma-ketligi kerak.
5. Maqsad (`02/0006`) va monitoring intervali: sozlamani o'zgartirib, yozuvni yozib olish kerak. [N].

## 6. Ulanish paytidagi tarix yozuvlari (qurilma sinovi, 2026-10-12 00:49 va 01:05)

| Band | Holat | Belgi |
|---|---|---|
| Daqiqa maydoni (bayt 4–5, BE) kun daqiqasi (0–1439). Ikki ulanishda (00:49:50 va 01:05:14) yozuv daqiqasi ulanish daqiqasiga mos | 2 ta holat | [E] |
| Har ulanishda yangi yozuvlar paydo bo'ladi. Qiymatlar ulanishdan-ulanishga o'zgaradi (doimiy kesh emas) | 2 ta ulanish | [E] |
| Ulanish boshida hisoblagich 01 yozuvi nol (qadam hisoblagichi ulanishda nolga tushadi, sessiya ichida 0 dan boshlanadi) | 1 ta holat | [E] |
| Hisoblagich 03 yozuvi: 1 bayt, qiymat yurak urishiga o'xshash. Hisoblagich 05: 2 bayt juftlik, qon bosimiga o'xshash | jonli kadr bilan 2 juftlik mos keldi (BP oynasi) | [E] |
| Qiymatlar ekran o'lchovi bilan hali tasdiqlanmagan. Keyingi tajriba: foydalanuvchi soatda o'lchov qiymatini aytadi, keyin yangi ulanishda sinxron | — | [N] |

Eslatma: FitCloudPro yangi juftlik so'rovlarini (GL-SW26, bind jarayonidan tashqari) qabul qilinmadi. Ular rad etildi (`Bekor qilish`), `Bloklash` bosilmadi.

## 7. Qadam hisoblagichi, ulanish va juftlik (2026-10-12, 01:00–01:25)

| Band | Holat | Belgi |
|---|---|---|
| Hisoblagich 01 yozuvida bayt 8–9 (BE, u16) qadam sonidir. 2 ta sinxronda ekrandagi qadam soniga mos keldi (sessiya boshidan) | [E] (1 ta ekran tasdig'i) |
| Yangi ulanishda qadam hisoblagichi nolga tushadi. Uzilishdan oldingi qadamlar yozuvlarda topilmadi | [T] (kuzatuv) |
| Yozuvning daqiqa maydoni sessiya boshlangan daqiqa (ulanish), sinxron vaqti emas. 3 ta holatda mos | [E] |
| Uzilish (status 8), keyin avtomatik qayta ulanish. Qayta ulanishda `0x0012 = 00` ko'pincha darhol keladi, ✓ so'ralmaydi | [T] |
| Soat tomonidan boshlangan juftlik (INCOMING_SSP) 01:01:54 va 01:02:25 da. Tasdiq vaqtida berilmagani uchun AUTH_FAILURE bilan tugadi. Bu soatdagi ✓/X so'rovi | [T] (Bluetooth stek logi) |
| Juftlik holati o'zgarib turadi: 00:58:39 da BONDED, keyin AUTH_FAILURE. Sabab: kalitlarning mos kelmasligi (taxmin) | [E] |

## 8. Juftlikni qayta o'rnatish (2026-10-12, 01:27)

| Band | Holat | Belgi |
|---|---|---|
| Telefon va soat juftligi yo'q edi (Bluetooth sozlamalarida GL-SW26 "Mavjud qurilmalar" da). Bog'lash qayta qilindi: telefonda `Bog‘lash`, BOND_STATE_BONDED (HCI_SUCCESS) | [T] |
| Shundan keyingi Orvell qayta ulanishi: `0x0012 = 00` darhol, yangi `INCOMING_SSP` yo'q | [T] (1 ta sinov, uzoqlik testi hali emas) |

## 9. ✓ so'rovi sababi va profil sozlamasi (2026-10-12, 01:29–01:37)

| Band | Holat | Belgi |
|---|---|---|
| Bluetooth 01:29:44 da o'chirilib-yoqildi (`USER_TURN_OFF`). Ilova GATT klienti uzildi (ACL `reason=22`, mahalliy uzilish). Keyingi ulanishda soat tasdiq so'radi | [T] (stek logi) |
| Juftlikdan keyin telefon `HFP` (qo'ng'iroq) profilini ulab yubordi. Profil o'chirilganda HFP uzildi va ACL `reason=22` bilan tugadi. Keyingi ulanishda ✓ so'raldi | [T] (stek logi) |
| Uzilish sababi `status=8` (masofa) bo'lsa, qayta ulanishda ✓ so'ralmadi (01:16) | [T] (kuzatuv) |
| Telefonda GL-SW26 uchun qo'ng'iroq va kontakt profillari o'chirildi (HEADSET 0, PBAP 0). Audio o'chiq | [T] |
| Shundan keyin 2 ta qayta ulanish: 5 soniyada ulandi, ✓ so'ralmadi, yangi juftlik so'rovi yo'q | [T] (2 sinov) |
| Qadam hisoblagichi har ulanishda nolga tushadi (01:08, 01:16, 01:30 kuzatuvlari). Uzilishgacha bo'lgan qadamlar yozuvda topilmadi | [T] (kuzatuv); sabab [N] |
