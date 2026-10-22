# Xabar formati (TLV) va ID katalogi (v0.4)

Har bir frame payload'i bitta TLV yozuvi:

| Offset | Hajm | Maydon |
|---|---|---|
| 0 | 1 | `TYPE` (oila) |
| 1 | 2 | `ID` (BE) |
| 3 | 2 | `VLEN` (BE), qiymat uzunligi |
| 5 | `VLEN` | `VALUE` |

**Tasdiqlangan (v0.2):** FitCloudPro oynasidagi barcha 243 ma'lumot payload'ida `VLEN = payload_uzunligi − 5` (telefon 125 + soat 118).

**Rad etish (kod):** payload 5 bayt dan kichik yoki `VLEN ≠ payload − 5` bo'lsa, TLV rad etiladi. Qoida kuzatuvdan (243 / 243) kelib chiqadi, rad jadvalida alohida qator yo'q.

**Javob ID'si:** tasdiqlangan juftliklarda javob `ID + 1` da keladi: `0x0011 → 0x0012`, `0x0013 → 0x0014`, `0x0015 → 0x0016`. Boshqa juftliklar tajriba bilan belgilanadi.

**`TYPE` ma'nosi:** `0x03` — bog'lash/ajratish (tasdiqlangan). `0x02` va `0x05` — taxmin; ichki guruhlash tasdiqlanmagan.

Har qiymat yonidagi belgi: **[T]** tasdiqlangan, **[E]** ehtimol, **[N]** noma'lum.

## Bog'lash va ajratish (`TYPE 0x03`)

| Yo'nalish | ID | Vaqt (`conn`) | Qiymat | Holat |
|---|---|---|---|---|
| P → W | `0x0013` | `11:44:53.214` (`0x0201`) | 32 bayt hisob satri | **[T]** so'rov. Javob `0x0014`. |
| W → P | `0x0014` | `11:44:53.293` (`0x0201`) | `01` | **[N]** birinchi javob |
| P → W | `0x0011` | `11:45:17.048` (`0x0201`) | 32 bayt, o'sha satr | **[T]** so'rov (qayta bog'lash). Javob `0x0012`. |
| W → P | `0x0012` | `11:45:19.180` (`0x0201`) | `00` | **[T]** javob kodi `00` |
| P → W | `0x0013` | `11:46:00.536` (`0x0201`) | 32 bayt, o'sha satr | **[T]** takror so'rov |
| W → P | `0x0014` | `11:46:00.618` (`0x0201`) | `00` | **[N]** ziddiyat: birinchi marta `01`, keyin `00` |
| P → W | `0x0011` | `12:27:47.710` (`0x0202`) | 32 bayt, o'sha satr | **[T]** qayta bog'lash (boshqa `conn`) |
| W → P | `0x0012` | `12:27:49.053` (`0x0202`) | `00` | **[T]** javob kodi `00`. Ilova "Bind Success" ko'rsatgan (12:27:50). |
| P → W | `0x0015` | `12:02:54.754` (`0x0201`) | bo'sh | **[T]** ajratish so'rovi |
| W → P | `0x0016` | `12:02:55.052` (`0x0201`) | `00` | **[T]** ajratish javobi (kadr V4, [frame.md](frame.md)). "UNBIND OK" ekrani `12:02:58` da (skrinshot). |

**Muhim xulosalar:**
- Har bog'lash so'rovi oldidan CCCD `0100` yoziladi (pastda). Qayta ulanish (stsenariy 2, `11:45`) va qayta bog'lash (`12:27`) ham bog'lash so'rovi bilan boshlanadi. Alohida login kadri topilmadi.
- Hisob satri: 32 bayt ASCII raqamlar, qolgani nol bilan to'ldirilgan. Satr shaxsiy, repoga yozilmaydi. FitCloudPro'da 32 bayt ASCII raqamlar (kuzatuv). Kod har qanday ASCII satrni (32 bayt bilan cheklangan) qabul qiladi (input validation). Soat qaysi satrlarni qabul qilishi: **[N]**.
- Ilova o'z hisob satrini yuborganda soat uni qabul qiladimi: **[N]**. Bu faqat qurilmada sinaladi (bog'lash foydalanuvchi ruxsati bilan).
- Javob kodi `0x0014` da `01` va `00` ziddiyati: **[N]**. `0x0012` da esa `00` har doim keldi.
- `00` dan boshqa javob kodlari (masalan `05`): ma'nosi **[N]**. Kod xom qiymatni `OtherCode` sifatida qaytaradi, ma'no bermaydi.

## CCCD va ulanish ketma-ketligi (ATT `0x0015`)

FitCloudPro oynasi (`conn 0x0201`), telefon yozuvlari (vaqt → qiymat):

| Vaqt | Qiymat | Nima |
|---|---|---|
| `11:44:53.142` | `0100` | notify yoqish |
| `11:44:53.214` | (`0x0013` bog'lash) | |
| `11:44:53.293` | (javob `0x0014 = 01`) | |
| `11:44:53.404` | `0000` | notify o'chirish (javobdan keyin) |
| `11:45:16.948` | `0100` | notify qayta yoqish |
| `11:45:17.048` | (`0x0011` bog'lash) | |
| `11:46:00.440` | `0100` | notify yoqish |
| `11:46:00.536` | (`0x0013` bog'lash) | |

- `11:44:53.404` dan `11:45:16.948` gacha soat kadr yubormadi (frame'lar bo'yicha tekshirildi). Notify faqat `0100` yoqilganda keladi: **[E]**, bu oraliq kuzatuviga asoslangan, boshqa holatda sinalmagan.
- Qayta bog'lash (`0x0202`): `12:27:47.593` da `0100`, `12:27:47.710` da bog'lash.
- Ajratishdan oldin alohida CCCD yozuvi yo'q (oxirgi `0100` `11:46:00.440`).

## Umumiy oila (`TYPE 0x02`)

### Tasdiqlangan

| Yo'nalish | ID | Vaqt | Qiymat | Holat |
|---|---|---|---|---|
| P → W | `0x0006` | `11:45:20.804`, `11:46:02.441`, `12:27:50.825` | 18 bayt (pastda) | **[T]** maqsad (goal) maydoni |

**Goal (`0x0006`, 18 bayt):**

| Offset | Hajm | Qiymat (misol) | Holat |
|---|---|---|---|
| 0 | u32 BE | `8000` | **[T]** qadam maqsadi. Ekran: "Goal 8000" (`screen_115024.png`, 11:50:24; FitCloudPro "Today" sahifasi). |
| 4 | u32 BE | `500000` | **[N]** |
| 8 | u32 BE | `240000` | **[N]** |
| 12 | u32 BE | vaqt (ma'nosi **[N]**) | **[T]** tenglik: o'sha paytdagi `0x0001` qiymatiga teng (3 / 3) |
| 16 | u16 BE | `0` | **[N]** |

Maqsadni yozish uchun 18 bayt kerak. Offset 4, 8, 16 ma'nosi tasdiqlanmagan. Shuning uchun kod "faqat qadam maqsadi" deb yoza olmaydi: [open-questions.md](open-questions.md) §4.

### Vaqt (`0x0001`, 4 bayt, telefon → soat)

| Log vaqti | `conn` | Qiymat (hex) | Unix sifatida (UTC) |
|---|---|---|---|
| `11:45:20.277` | `0x0201` | `6A96BB54` | 2026-09-01 11:47:32 |
| `11:45:22.361` | `0x0201` | `6A96BB56` | 2026-09-01 11:47:34 |
| `11:46:01.255` | `0x0201` | `6A96BB81` | 2026-09-01 11:48:17 |
| `11:49:51.753` | `0x0201` | `6A96BC73` | 2026-09-01 11:52:19 |
| `11:51:35.631` | `0x0201` | `6A96BCE3` | 2026-09-01 11:54:11 |
| `12:27:49.758` | `0x0202` | `6A96C6F1` | 2026-09-01 12:37:05 |
| `12:27:51.068` | `0x0202` | `6A96C6F3` | 2026-09-01 12:37:07 |

- Holat: **[N]**. Epoch va vaqt zonasi tasdiqlanmagan. Unix (UTC) sifatida sana 2026-09-01 (40 kun oldin). Vaqt qismi devor soatidan oldinda: `11:45:20,277` da +2 min 11,7 s; `12:27:49,758` da +9 min 15,2 s; `12:27:51,068` da +9 min 15,9 s (siljish o'sadi).
- Solishtirish (v0.3, T1): `05/0024` bayt 4–5 (daqiqa): per-kadr **[E]** (11:53 da 20 / 20, 14:11 da 19 / 20; birgalikda 39 / 40). `05/0030` bayt 4–5 (705 = 11:45, 709 = 11:49): yozuv vaqti **[E]**. `02/0001` ning 4 bayti bu maydonlar bilan bir xil shakl emas. Ikki maydon bir xil vaqt manbai emas.
- `02/0006` (goal) ning bayt 12–15 maydoni oxirgi `02/0001` qiymatiga teng: 3 / 3 holat (`11:45:20.804`, `11:46:02.441`, `12:27:50.825`). Tenglik **[T]** (ikkinchi hisob: TLV maydonlari). Maydonning ma'nosi (vaqt) **[N]**.
- Log vaqti bilan chiziqli emas: 2,084 s oraliqda qiymat +2; 40,98 s da +45; 230,5 s da +242; 42 daqiqada (`11:45:20` → `12:27:49`) +2973. Soat tezligi doimiy emas, shuning uchun oddiy "2000 yildan" yoki "UTC+5" tuzatishi ishlamaydi.
- Stsenariy 4 (vaqt sinxronlash, alohida tugma yo'q) logda aniq topilmadi. Ekranda soat qiymati bilan solishtirish qilinmagan: **[N]**.
- `0x0006` ichidagi vaqt (offset 12) shu qiymatning nusxasi.

### Noma'lum yoki qisman

| Yo'nalish | ID | Soni (FitCloudPro) | Vaqt | Shakl va izoh |
|---|---|---|---|---|
| P → W | `0x0005` | 2 | `11:45:19.886`, `11:46:00.821` | 4 bayt `9AAF0C60`. **[N]** |
| P → W | `0x0050` | 2 | `11:45:19.802`, `11:46:00.732` | 4 bayt `00000007`. **[N]** |
| P → W | `0x0020` | 2 | `11:45:19.985`, `11:46:00.919` | 0 bayt. **[N]** |
| P → W | `0x0012` | 2 | `11:45:21.721`, `11:46:02.853` | 4 bayt `FFFFFFFF`. **[N]** |
| P → W | `0x003A` | 2 | `11:45:20.513`, `11:46:01.952` | 4 bayt `44000000`. **[N]** |
| P → W | `0x0093` | 2 | `11:45:22.898`, `11:46:03.681` | 7 bayt nol. **[N]** |
| P → W | `0x00E7` | 2 | `11:45:20.182`, `11:46:01.186` | 0 bayt. **[N]** |
| P → W | `0x0086` | 1 | `11:46:01.392` | 0 bayt. **[N]** |
| P → W | `0x0043` | 3 (v0.1) | `11:45:21.250`, `11:46:09.978`, `11:47:25.557` | 0 bayt. Javob `0x0044`. |
| W → P | `0x0044` | 3 (v0.1) | `11:45:21.619`, `11:46:10.048`, `11:47:25.873` | 6 bayt, sessiya ichida bir xil shakl. **[N]** |
| P → W | `0x0039` | 1 | `11:51:35.308` | 0 bayt. **[N]** |
| P → W | `0x001B` | ~40 (v0.1) | mediana 7,50 s (faqat guruhlar ichida); orada 16,9–51,9 s va 658,7 s uzilishlar | 0 bayt. Davriy so'rov. Javob `0x001C`. |
| W → P | `0x001C` | ~39 (v0.1) | mediana 7,50 s (faqat guruhlar ichida); orada 16,9–51,9 s va 658,7 s uzilishlar | 3 bayt `00 04 50`. Sessiyada va qayta bog'lashda o'zgarmadi. **[N]: batareya tasdiqlanmagan; foiz nomzodi `0x50` [E]** (pastda). |
| W → P | `0x0084` | 2 | `11:45:19.279`, `11:46:01.460` | 6 bayt: soat Bluetooth manzili. **Repoga yozilmaydi.** |
| W → P | `0x0021` | 2 + 1 (qayta bog'lash) | `11:45:20.061`, `11:46:01.006`, `12:27:49.543` | 122 bayt, uch sessiyada bir xil. Maydonlari **[N]**. Hex repoga yozilmaydi (shaxsiy sozlamalar bo'lishi mumkin). |
| W → P | `0x0022` | 3 (FitCloudPro), 1 (qayta bog'lash) | `11:45:22.791`, `11:49:59.521`, `11:51:42.886` | 30 bayt, hammasi nol. **[N]** |
| P → W | `0x0036` | 2 (v0.1) | `11:45:23.634`, `11:47:01.357` | 62 bayt UTF-8 matn (joy nomi). **Mazmuni repoga yozilmaydi.** Mazmuni **[N]**. |

- FitCloudPro oynasida jadvalda yo'q qo'shimcha ID'lar (T2): `02/0018` (5 ta, `11:47:48`–`11:48:37`, 2 bayt) va `02/003B` (2 ta, `11:48:44`, `11:48:55`, bo'sh). Ma'nosi **[N]**.
- Qayta bog'lash sessiyasida (`12:27:51`–`12:27:57`, `conn 0x0202`) hisoblagich ketma-ketligi bor, lekin barcha `05/0007`/`05/0008` qiymatlari nol (9 / 9 juft), `05/0030` yo'q. Qayta bog'lashda `05/0006` = `0x57` (FitCloudPro'da `0x21`). Bu sessiya sinxron emas: "N > 0 da 01/03/05" qoidasi faqat FitCloudPro sinxronlari uchun **[T]**.
- `11:45:22` atrofida `05/0005` → `05/0006` → `05/0021` → `05/0022` ketma-ketligi (hisoblagichsiz). Ma'nosi **[N]**.

### Batareya: tasdiqlanmadi

- `0x001B → 0x001C` (`00 04 50`) davriy almashinuv. Batareya stsenariysi vaqtida (`11:50:34`, `11:50:41`, `11:50:49`) boshqa so'rov yo'q, javob bir xil `000450`.
- Qiymat 42 daqiqa davomida o'zgarmadi (`11:45`, `11:50`, `12:27`, `12:28`). Batareya foizi ham uzoq vaqt o'zgarmasligi mumkin, shuning uchun bu faktning o'zi batareyani inkor etmaydi va tasdiqlamaydi.
- Alohida batareya so'rovi (boshqa ID) topilmadi.
- Ekranda faqat batareya ikonkasi ko'rinadi: `screen_115044.png` (11:50:44), "GL-SW26 Connected", yashil ikonka, raqam yo'q. Shuning uchun foiz **[N]**. Raqam ko'rinmagani uchun [T] yozilmaydi.
- Nomzod **[E]** (taxmin, tasdiqlanmagan): `0x001C` ning 3-bayti `0x50` (ehtimol 80%). Tasdiqlash uchun raqamli foiz ekrani kerak (C1).

## Tarix sinxroni (`TYPE 0x05`), v0.3

Tekshiruv: ikki sinxron, FitCloudPro (`conn 0x0201`), `11:49:51` va `11:51:35` dan boshlab.

Ketma-ketlik (ikkala sinxronda bir xil):
1. Telefon `02/0039` (bo'sh), `02/0001` (vaqt, 4 bayt).
2. Telefon `05/0005` (bo'sh); soat `05/0006` (4 bayt, `00000021` = 33) bilan javob beradi.
3. Telefon `05/0001` hisoblagichni yuboradi: `01`, `02`, `03`, `83`, `04`, `84`, `05`, `85`, `10`. `05/0020` (`00`) 02–10 so'rovlaridan oldin (8 ta) va `05/0021` dan oldin (1 ta) keladi. `01` oldida yo'q. Sinxronda jami 9 ta. Ma'nosi **[N]**.
4. Har `05/0001` ga soat `05/0007` (4 bayt, BE) va `05/0008` (4 bayt) yuboradi. Ikkalasi har doim teng **[T]** (sinxronda 9 juft).
5. `05/0007` = N > 0 bo'lsa, undan keyin `05/0030` (N bayt) keladi **[T]** (6 / 6 holat). Hisoblagich `01`, `03`, `05` da N > 0 (ikkala sinxronda ham), qolganlarida 0 **[T]**.

`05/0030` yozuvi (6 ta yozuv, 2 sinxron):

| Offset | Hajm | Qiymat | Holat |
|---|---|---|---|
| 0 | 2 | `00 01` | **[T]** (6 / 6), ma'nosi **[N]** |
| 2 | 2 | `35 4B` | **[T]** (6 / 6), ma'nosi **[N]** |
| 4 | 2 | BE, daqiqa-sanog'i (0..1439): `705` = 11:45, `709` = 11:49 | **[E]**: yozuvning vaqti (ekranda ko'rinmagan; sinxrondan oldingi daqiqalar) |
| 6 | 2 | `00 05` | **[T]** (6 / 6), ma'nosi **[N]** |
| 8 | 1 yoki 2 | yozuv qiymati | **[E]**: 9 baytli yozuvda bayt 8 — puls nomzodi; 10 baytli yozuvda bayt 8–9 — qon bosimi nomzodi (sistolik, diastolik). Bitta sinxronda ekranga mos, ikkinchisida mos emas. |

Tekshiruv natijasi (qiymatlar repoga yozilmaydi):
- 11:49 sinxroni: ikkala yozuv (puls va qon bosimi) ekrandagi 11:50:24 qiymatlari bilan **mos** keldi (`screen_115024.png`).
- 11:51 sinxroni: yozuvlar ekrandagi 11:51:46 qiymatlari bilan **mos emas** (`screen_115146.png`). Shu sababli bayt 8–9 mapping'i **[E]**, tasdiqlanmagan.
- 14 baytli yozuvlarda bayt 8–13 nol, bayt 0–7 nol emas (ikkala sinxronda).
- Hisoblagich va yozuv turi **[E]**: `01` → 14 baytli yozuv (bayt 8–13 nol), `03` → 9 baytli, `05` → 10 baytli (ikkala sinxronda bir xil). Qadam ekranida 0 (`screen_115146.png`), shuning uchun 14 baytli nol yozuv qadam bo'lishi mumkin **[E]**.

Qayd: `05/0030` ichidagi `00 01 35 4B` va jonli oqim (`05/0024`) ning boshidagi `35 4B` bir xil bayt juftligi. Tartibi boshqa (`0024` da `35 4B 00 01`). Bog'liqlik **[N]**.

### `05/0030` bloklari (v0.5, 2026-10-13)

Manba: Orvell `watch_raw` jadvali (o'z qurilmamiz, 2026-10-12 00:50 – 2026-10-13 00:38, 69 yozuv) va foydalanuvchi soat ekranida ko'rgan qadam soni. Uchinchi tomon manbasi ishlatilmagan.

Yozuv bir yoki bir nechta blokdan iborat, bloklar ketma-ket keladi:

| Offset | Hajm | Maydon | Holat |
|---|---|---|---|
| 0 | 2 | yozuvlar soni K (BE) | **[T]**: hisoblagich `01` va `03` dagi 43 / 43 yozuv shu tuzilish bilan qoldiqsiz ajraladi |
| 2 | 2 | sana: bitlar `yyyyyyy mmmm ddddd` (yil − 2000, oy, kun) | **[T]**: `35 4B` = 11-okt, `35 4C` = 12-okt, `35 4D` = 13-okt; yarim tundan keyingi blok keyingi kun bilan keladi |
| 4 | 2 | birinchi yozuv daqiqasi (0..1439, BE, soat mahalliy vaqti) | **[T]** (v0.4 dagi daqiqa-sanog'i) |
| 6 | 2 | yozuvlar oralig'i, daqiqa (kuzatilgan qiymat doim `5`) | **[E]** |
| 8 | K × R | yozuvlar; i-yozuv vaqti = daqiqa + i × oraliq | **[T]** |

Yozuv hajmi R hisoblagichga bog'liq:

| Hisoblagich | R | Maydonlar | Holat |
|---|---|---|---|
| `01` | 6 | qadam (2, BE), masofa sm (2, BE), kaloriya, kkal × 1000 (2, BE) | qadam **[T]**: kunlik yig'indi soat ekranidagi son bilan ikki marta teng chiqdi (13-okt, yarim tundan keyin va kunduzi). Masofa **[E]**: qadamga nisbati barcha nol bo'lmagan yozuvlarda 0,70–0,79 m. Kaloriya **[E]**: qadamga nisbati barqaror, birligi soat ekrani bilan tekshirilmagan |
| `03` | 1 | puls, zarba/daq; `0` = o'lchov yo'q | **[E]**: qiymatlar dam olish pulsi oralig'ida, soat ekrani bilan tekshirilmagan |
| `05` | 2 | ikki bayt (v0.4: qon bosimi nomzodi) | **[N]**: talqin qilinmaydi |
| `02` | 5 | `[sana:2][daqiqa:2][1 bayt]` | **[N]**: talqin qilinmaydi |

v0.4 jadvalidagi "0: `00 01`" — K (yozuvlar soni), "2: `35 4B`" — sana. "6: `00 05`" — oraliq.

Ilova: `watch-protocol/.../HistoryRecords.kt` (`HistoryRecordsTest`), `app/.../data/WatchHistory.kt` (`WatchHistoryTest`). `01` va `03` talqin qilinadi, qolgan hisoblagichlar faqat xom saqlanadi.

#### Katta yozuv: bo'laklar (v0.5, 2026-10-13 14:41–14:45 qurilma logi)

- `0x0007` = N — hisoblagich yozuvining **jami** hajmi. N > 256 bo'lsa soat uni bir nechta `0x0030` da yuboradi: `01` uchun N = 1158 → 256, 256, 256, 256, 134; `03` uchun 289 → 256, 33; `05` uchun 458 → 256, 202 **[T]** (3 / 3).
- Bo'lak chegarasi blok chegarasiga mos emas: ikkinchi bo'lak yozuv o'rtasidan boshlanadi. Talqin faqat bo'laklar birlashtirilgandan keyin **[T]**.
- 261 baytli frame (TLV 5 + 256) ikki notify'da keladi: 244 + 17 (frame.md) **[T]**.
- Har bo'lakka frame ACK yuborilgach soat keyingisini yuboradi; oxirgi bo'lakdan keyin `0x0008` = N **[T]**.
- Oraliq bo'lak hajmi doim 256 **[E]** (3 hisoblagich, bitta sinxron). Ilova boshqa hajmni qabul qilmaydi (sinxron ACK'siz to'xtaydi).

## Sinxron va jonli oila (`TYPE 0x05`)

### Jonli o'lchov (tasdiqlangan oqim, [N] ma'no)

- Boshlash: telefon `0x0023` = `00 01 05 01` (`11:52:15.809`; 14:11 o'lchovi: `14:11:39.628`, `0x0201`) **[T]**.
- Oqim: soat `0x0024` ni yuboradi. Har kadr 16 bayt payload (TLV: `05 0024 000B` + 11 bayt qiymat). Oralig'i ~2,9 s. `11:52:19.125` dan `11:53:13.838` gacha **20 kadr** **[T]** (birinchi kadr `0x0023` boshlashdan 3,3 s keyin). 14:11 o'lchovi: `14:11:43.159` dan `14:12:37.872` gacha 20 kadr, VLEN 11 **[T]** (birinchi kadr 3,53 s keyin).
- To'xtatish: telefon `0x0023` = `00 00 05 00` (`11:53:15.807`; 14:11 o'lchovi: `14:12:39.632`, START'dan 60 s) **[T]**.
- 11 baytning tuzilishi: bayt 0–1 `35 4B`, bayt 2–3 `00 01` — 20 / 20 kadrda bir xil **[T]**.
- Bayt 4–5 (BE) = kadr vaqtining daqiqa-sanog'i (`0..1439`): per-kadr **[E]**: 11:53 o'lchovida 20 / 20, 14:11 o'lchovida 19 / 20 (bitta chegara kadri `14:12:00.447` da maydon `14:11`, keyingi kadr `14:12:03.334` da `14:12`; ehtimol ~3 s qabul kechikishi). Birgalikda 39 / 40, shuning uchun [T] emas. Oxirgi kadr `713` = 11:53 ekrandagi (`screen_115324.png`) "Previous: 11-okt 2026 11:53" yozuvi bilan mos **[T]**. 14:11 o'lchovida oxirgi kadr daqiqasi `14:12` ekran yorlig'iga mos **[E]** (yorliq ko'rilmagan). `712` (11:52) dan `713` (11:53) ga `11:53:02` da o'tdi. Shakli `05/0030` yozuvining bayt 4–5 bilan bir xil.
- Bayt 6–10 kadrdan kadrga o'zgaradi.
- Puls maydoni **[N]**. Ekran natijasi (`screen_115324.png`, 11:53:24) bilan solishtirildi: bayt 6–10 ning hech birida oxirgi kadr ekran qiymatiga teng emas. Bayt 6 da ekran qiymatiga teng bitta kadr bor (11:52:36), lekin bu tasodif bo'lishi mumkin va o'rtacha ham mos emas. 14:11 o'lchovida ham shunday: ekran qiymatiga teng bitta kadr (o'rtada), oxirgi kadr mos emas. Bayt 7–10: **[N]** (ikki o'lchovda ekran qiymatiga teng kadr yo'q). Qiymatlar repoga yozilmaydi.

### Sinxron so'rovlari

| Yo'nalish | ID | Soni | Qiymat | Holat |
|---|---|---|---|---|
| P → W | `0x0001` | 9 (`11:51:35`–`11:51:42`) | 1 bayt hisoblagich: `01`, `02`, `03`, `83`, `04`, `84`, `05`, `85`, `10` | **[T]** ketma-ketlik. Ma'nosi **[N]**. |
| P → W | `0x0020` | 9 | 1 bayt `00` | **[N]** |
| P → W | `0x0005` | 1 | bo'sh | **[N]** |
| P → W | `0x0021` | 1 | bo'sh | **[N]** |
| W → P | `0x0007`, `0x0008` | juftlik, sinxron paytida (v0.1) | 4 bayt | **[N]**. Qayta bog'lashda nollar. |
| W → P | `0x0006` | 2 (har sinxronda 1) | 4 bayt (`00000021`, FitCloudPro) | **[N]** |
| W → P | `0x0030` | 6 (har sinxronda 3: 14, 9, 10 bayt) | 14, 9, 10 bayt | **[N]** (tuzilish: yuqorida) |
| W → P | `0x0022` | yuqoridagiga qarang | 30 bayt, nollar | **[N]** |

Sinxron vaqtida (`11:51:35` – `11:51:42`) telefon `0x0001` hisoblagichni va `0x0020` ni navbat bilan yuboradi, soat `0x0007`/`0x0008` bilan javob beradi. Qadam, SpO₂ va uyqu uchun alohida yozuv topilmadi. SpO₂ ekranda raqam ko'rsatgan, lekin uning qiymati hech bir sinxron yozuvida yo'q: **[N]**. Uyqu: ekranda "No sleep data last night", yozuv yo'q: **[N]**. Qadam: ekranda 0, 14 baytli yozuv (bayt 8–13 nol) **[E]** (yuqorida). Puls tarixi: `05/0030` **[E]** (yuqorida). Bu ([open-questions.md](open-questions.md) §5).

## Boshqa handle'lar (ATT)

| Handle | UUID (GATT discovery) | Holat |
|---|---|---|
| `0x0012` (`ff02`, write) | `0000ff02` | **[T]** telefon yozuvlari shu yerga |
| `0x0014` (`ff03`, notify) | `0000ff03` | **[T]** soat javoblari |
| `0x0015` (CCCD `0x0014` uchun) | `2902` | **[T]** `0100` / `0000` |
| `0x0017` (`ff04`, read/write) | `0000ff04` | **[N]** logda hech qanday murojaat yo'q |
| `0x0028` (`fff3`, d0ff xizmati) | `0000fff3` | **[N]** logda soat murojaati yo'q. 12:12 dagi `0x0028` notify boshqa qurilmaniki (MAC farqli). |
| `0x002F`, `0x0031` (6287 xizmati) | `6387`, `6487` | **[N]** logda murojaat yo'q |
| `0x003B` (`4a02`, 3802 xizmati) | `00004a02` (prop `0x1a`) | **[T]** telefon 4 marta `AA BB …` yozdi, soat 4 marta ATT `0x0A` (Attribute Not Found) bilan rad etdi (raw ATT xato, `conn 0x0201`). Qayta bog'lashda ham 2 marta. **[N]**: discovery `0x003B` ni mavjud deydi, lekin soat rad etadi. Ziddiyat. |
| `0x003C` | (discovery'da yozuv yo'q) | **[N]** telefon `0100` yozdi (2 marta, FitCloudPro; 1 marta, qayta bog'lash). Soat rad etmadi. Maqsadi noma'lum. |

`AA BB` kadrlari (`0x003B`): `aa bb 08 00` + 4 bayt, va `aa bb 01 00`. 4 bayt yozuvdan-yozuvga o'zgaradi. Ma'nosi **[N]**, kodga kirmaydi.

`0x0003` (Service Changed, `indicate`): FitCloudPro oynasida yo'q. Faqat eski sessiyalarda, telefon GATT stack'i. Soat protokoli emas.

## Log yozuvlari (tasdiqlash uchun)

| Frame | Vaqt | Nima |
|---|---|---|
| Telefon ajratish so'rovi | `12:02:54.754` | `ab000005c455006a0300150000` |
| Soat ajratish javobi (sarlavha) | `12:02:55.052` | `ab000006eb051b9e` + `030016000100` |
| Telefon ACK | `12:02:55.062` | `ab10000000001b9e` |
| Telefon poll so'rovi | `12:02:25.552` | `ab000005c709006602001b0000` |
| Soat ACK | `12:02:25.917` | `ab10000000000066` |
| Soat bind javobi (sarlavha) | `11:44:53.293` | `ab00000693c51b29` + `030014000101` |
| Jonli boshlash | `11:52:15.809` | telefon `0x0023`, `00010501` |
| Jonli to'xtatish | `11:53:15.807` | telefon `0x0023`, `00000500` |

---

## v0.4 yangilanishi: FitCloudPro capture s4 (2026-10-11, 17:58:51–18:08)

Manba: `captures/s4-indep/bugreport-s4-1.zip` → `FS/data/log/bt/btsnoop_hci.log` (1,3 MB). Oyna: **17:58:51** (Orvell `force-stop`, shundan keyin faqat FitCloudPro) dan **18:08:00** gacha, `conn 0x0201`. Orvell trafigi bu oynada yo'q deb hisoblanadi (Orvell 17:58:51 da to'xtatilgan). Oynada: telefon 152 frame (85 ma'lumot + 67 ACK), soat 152 frame (67 ma'lumot + 85 ACK); barcha ma'lumot frame'larining CRC mos. Tahlil skripti repoda emas (scratchpad). Hisob satri, MAC va sog'liq qiymatlari repoga yozilmaydi.

### Bog'lash va ajratish (v0.4)

| Vaqt | Yo'nalish | ID | Qiymat | Holat |
|---|---|---|---|---|
| `17:59:44.209` | P → W | `0x0015` (CCCD) | `0100` | **[T]** bog'lashdan oldin notify yoqiladi |
| `17:59:44.341` | P → W | `0x0011` | 32 bayt hisob satri | **[T]** |
| `17:59:45` | W → P | `0x0012` | `00` | **[T]** muvaffaqiyat (bog'lash OK) |
| `18:05:12.448` | P → W | `0x0015` (CCCD) | `0100` | **[T]** |
| `18:05:12` | P → W | `0x0013` | 32 bayt | **[T]** |
| `18:05:12` | W → P | `0x0014` | `00` | **[T]** muvaffaqiyat |
| `11:44:53.293` (s1 oynasi) | W → P | `0x0014` | `01` | **[T]** qiymat; ma'nosi **[E]**: soat tasdig'ini kutmoqda |
| `11:45:19.180` (s1 oynasi) | W → P | `0x0012` | `00` | **[T]** |

Xulosa (kod uchun):
1. Bog'lashdan oldin `0x0015 = 0100` yoziladi (beshta holat: 11:44, 11:45, 11:46, 17:59, 18:05) **[T]**.
2. Bog'lash so'rovi `0x0011` (32 bayt) → javob `0x0012`. `00` = muvaffaqiyat **[T]**.
3. Soat bog'lanmagan bo'lsa, `0x0013` (32 bayt) → javob `0x0014`. `00` = muvaffaqiyat **[T]**. `01` = soat ekranida tasdiqlash kutilmoqda **[E]** (11:44 da `01`, keyin tasdiqdan so'ng `0x0011` → `00`).
4. `01` dan keyin FitCloudPro notify ni o'chirib (`0000`), keyin qayta yoqib (`0100`), `0x0011` yuboradi. Qayta urinish ketma-ketligi **[T]** (s1), lekin uni takrorlash shart emasligi **[N]**.
5. Ajratish: `0x0015` (bo'sh) → `0x0016` = `00` **[T]** (12:02, oldingi oyna). s4 oynasida ajratish yo'q; qayta tekshirish kerak (bugreport#2).

Kod uchun taklif (**[E]**, qurilmada sinov kerak): avval `0x0011`; javob `00` bo'lmasa `0x0013`. `0x0014 = 01` da ilova "soat ekranida tasdiqlang" holatiga o'tadi va `0x0011` ni davriy qayta yuboradi. Soat ekranidagi tasdiqni foydalanuvchi bajaradi.

### Bog'lashdan keyingi sozlama (FitCloudPro, `TYPE 0x02` va `0x04`)

Bog'langandan keyin FitCloudPro quyidagilarni yuborgan (s4, 17:59:46–17:59:53). Ma'nolari **[N]**, kodga kirmaydi:

| ID | Soni | Qiymat | Izoh |
|---|---|---|---|
| `0x0050` | 1 | `00000007` | **[N]** |
| `0x0005` | 1 | `9AAF0C60` | **[N]** (oldingi `9AAF0C60` bilan bir xil) |
| `0x0020`, `0x00E7`, `0x0086`, `0x003A` | 1 har biri | bo'sh yoki 4 bayt | **[N]** |
| `0x0012` | 1 | `FFFFFFFF` | **[N]** |
| `0x0093` | 1 | 7 bayt | **[N]** |
| `0x0043` → `0x0044` | 1 (s4 da 5 marta) | javob 6 bayt, har doim `0E 75 00 01 0E 01` | **[N]** (javob doimiy) |
| `0x0036` | 1 | UTF-8 matn (joy nomi) | **[N]**, mazmuni repoga yozilmaydi |
| `0x0084` (soat → telefon) | 2 | soat Bluetooth manzili | repoga yozilmaydi |
| `TYPE 0x04`, `ID 0x0047` (telefon) | 1 | 3 bayt `00 00 00` | **[N]**, `TYPE 0x04` birinchi marta ko'rildi |

### Maqsad (`0x0006`, 18 bayt)

**[T]** (s4, 2 marta, bir xil maqsad): `u32@0` = `00001F40` = **8000** (FitCloudPro ekrani "Goal 8000"). `u32@12` = o'sha paytdagi `0x0001` qiymati (2 / 2 **[T]**, ma'nosi **[N]**). `u32@4` (`0007A120` = 500000), `u32@8` (`0003A980` = 240000), `u16@16` (`0000`) **[N]**. Kodda faqat maqsad o'qiladi/yoziladi, 18 bayt to'liq yozish uchun qolgan maydonlar noma'lum, shuning uchun maqsadni yozish funksiyasi **[N]**: ilovada "Tez orada".

### Vaqt (`0x0001`, 4 bayt)

**[N].** Oyna ichidagi namunalar (telefon soati bo'yicha, `btsnoop` vaqti):

| Vaqt | Qiymat (hex) | Oldingi nuqtadan farq (qiymat / soat) |
|---|---|---|
| `17:59:46` | `6A971EEE` | — |
| `17:59:47` | `6A971EEF` | +1 / +1 s (mos) |
| `18:02:54` | `6A9720B6` | +455 / +187 s (mos emas) |
| `18:05:13` | `6A97214D` | +151 / +139 s (mos emas) |
| `18:05:14` | `6A97214E` | +1 / +1 s (mos) |

Qiymat soat bilan chiziqli emas: ba'zi oraliqlarda 1:1, boshqalarida 2,4:1 va 1,1:1. Unix (UTC, UTC+5 ni hisobga olgan holda) yoki boshqa oddiy kodlash tasdiqlanmadi. **Kodga kirmaydi.** Ilovada vaqt sinxronlash "Tez orada".

### Batareya (`0x001B` → `0x001C`)

**[E].** Oyna ichida `0x001B` 11 marta, `0x001C` 11 marta, hammasi `00 04 50` (`0x50` = 80). Soat ekrani foizni ko'rsatmagan (FitCloudPro'da raqam yo'q), shuning uchun foiz `[T]` emas. FitCloudPro'ning 17:56 ekranida "80%" ko'ringan, ammo o'sha vaqt oynasi Orvell trafigi bilan aralashgan (17:56–17:58:51), shuning uchun spec manbasi emas. Ilovada batareya foizi "Tez orada" (yoki ko'rsatilmaydi).

### Tarix sinxroni (`TYPE 0x05`)

Uch sinxron (s4, FitCloudPro):

| Sinxron | Vaqt | `05/0006` | Yozuvlar | Izoh |
|---|---|---|---|---|
| 1 | `17:59:47` | `0000000C` (12) | 9 juft `05/0007`/`05/0008` = `0`, `05/0030` yo'q | **[T]** ketma-ketlik; `05/0006` ma'nosi **[E]** (v0.3 da `0021` = 33 kuzatilgan) |
| 2 | `18:03:00` | `00000021` (33) | 3 yozuv: N = 14 (hisoblagich `01`), 9 (`03`), 10 (`05`); `05/0007` = `05/0008` = N | **[T]** tuzilish; daqiqa maydoni (bayt 4–5) = `0437` = 1079 = 17:59 **[T]** (bu sinxron ichida) |
| 3 | `18:05:14` | `00000000` | hisoblagich yo'q, yozuv yo'q (faqat `05/0005` va `05/0021`, bo'sh) | **[T]** |

Yozuv tuzilishi (oldingi v0.3 bilan bir xil, **[T]**): bayt 0–1 `0001`, bayt 2–3 `354B`, bayt 4–5 daqiqa (BE), bayt 6–7 `0005`, bayt 8+ qiymat. Ikkinchi sinxrondagi uch yozuv:
- 14 baytli yozuv: bayt 8–13 nol (qadam nolga teng bo'lishi mumkin, **[N]** mapping).
- 9 baytli yozuv: bayt 8 nol emas. Shu bayt puls nomzodi **[E]**. Ekrandagi qiymat bilan **mos emas** (ekran 18:03 da boshqacha, shu sababli mapping rad etildi).
- 10 baytli yozuv: bayt 8–9 nol emas. Qon bosimi nomzodi **[E]**. Ekran bilan **mos emas**.

Shu sababli: puls va qon bosimi mapping (`05/0030` bayt 8+) **[N]** (bitta sinxronda mos, ikkinchisida mos emas: v0.3 va s4 birgalikda). Qadam **[N]** (s4 da qadam 0, yozuv nol, lekin nol qiymat ham qadam bo'lishi mumkin). SpO₂ va uyqu **[N]** (s4 da SpO₂ yozuvi yo'q; uyqu "No sleep data").

Sinxron #2 Home'ni pastga tortish (`18:02:54`) bilan, sinxron #3 ilovani qayta ochish (`18:05:12`) bilan bir vaqtda boshlangan **[T]** (vaqt mosligi). Sinxron #1 bog'lashdan keyin (`17:59:47`). Bu oynada Home sahifasida "Sync" tugmasi ko'rinmadi (17:56 ekranida ko'ringan). Sinxronni qo'lda ishga tushirish usuli: **[N]**.

### CCCD va ATT yozuvlari (s4)

- `0x0015` (CCCD) `0100`: bog'lashdan oldin (ikki bog'lash).
- `0x003C` `0100`: har bog'lashdan keyin (17:59:46.640, 18:05:13.346) **[T]** yozuv, ma'nosi **[N]**.
- `0x003B` `AA BB 08 00 …` va `AA BB 01 00 …`: har bog'lashdan keyin ikki yozuv (17:59:46.813, 46.907). Ma'nosi **[N]**, kodga kirmaydi.
- Oyna ichida ATT xato javoblari ko'rinmadi (parser faqat yozuv va notify ni chiqaradi, ATT xatolari chiqmaydi; bu tekshiruv cheklovi).

### Qisqa xulosa (v0.4 o'zgarishlari)

| Band | v0.3 | v0.4 |
|---|---|---|
| Bog'lash `0x0011 → 0x0012 = 00` | [T] | [T] (yana bir marta) |
| `0x0013 → 0x0014` | 01 va 00 ziddiyat [N] | 00 [T]; 01 = tasdiq kutilmoqda [E] |
| CCCD `0100` bog'lashdan oldin | [E] | [T] (5 holat) |
| Maqsad `0x0006` u32@0 = 8000 | [T] | [T]; u32@12 = `0x0001` [T] (2/2) |
| Vaqt `0x0001` | [N] | [N] (chiziqsiz, aniq formula yo'q) |
| Batareya `0x001C` bayt 2 = 0x50 | [E] | [E] (11/11 bir xil, foiz ekranda ko'rinmagan) |
| Tarix yozuvi tuzilishi | [T] | [T] (3 sinxron yana) |
| Puls/BP mapping (`05/0030` bayt 8+) | [E] | [N] (mos emas: 9-bayt va 10-bayt yozuvlari) |
| Qadam | [E] | [N] (s4 da qadam 0, mapping uchun nol emas qiymat yo'q) |
| `TYPE 0x04` | yo'q | [N] (yangi) |

### v0.4.1 tuzatish: bog'lash tartibi (s4-2 qurilma sinovi, 2026-10-11 18:40–18:42)

Yangi ilova (wire `0x0011` bilan) soat ulanganda, CCCD `0100` yozilgach, `0x0011` (32 bayt) yubordi. Manba: `captures/s4-indep/bugreport-s4-2.zip` → `FS/data/log/bt/btsnoop_hci.log`, `conn 0x0201`, 18:40:27.121 va 18:42:02.149.

| Vaqt | Yo'nalish | Nima | Holat |
|---|---|---|---|
| `18:40:27.121` | P → W | `0x0015` (CCCD) `0100` | **[T]** |
| `18:40:27.136` | P → W | `0x0011` (45 bayt kadr, SEQ `0x0001`) | **[T]** yuborildi |
| `18:40:27.146` | W → P | ACK (`ab 10 00 00 00 00 00 01`) | **[T]** soat kadrni tasdiqladi |
| `18:40:27`–`18:42:02` | W → P | `0x0012` javobi **kelmadi** | **[T]** kuzatuv |
| `18:42:02.149` | P → W | `0x0015` `0100`, keyin `0x0011` | **[T]** qayta urinish, xuddi shunday javob: faqat ACK |

Xulosa (**[E]**, kodga ta'siri bor):
1. `0x0011` faqat avval bog'langan hisob uchun javob beradi (`17:59:45` va `12:27:49` da `00`, [T]). Yangi (tanilmagan) hisob uchun javob kelmaydi (18:40, 18:42, [T] kuzatuv).
2. `0x0013` (32 bayt hisob) bu holatda ham bog'langan, ham yangi hisob uchun ishlaydi: tanilgan hisobda `00` (`18:05:12`, [T]); yangi hisobda `01` (`11:44:53`, [T] qiymat, soat tasdig'i kutiladi) va tasdiqdan keyin `00` (`11:46:00`, [T]).
3. Shu sababli bog'lash **`0x0013` bilan boshlanadi**. `0x0011` kodda ishlatilmaydi (wire funksiyasi testlar uchun qoladi).

Bog'lash oqimi (v0.4.1, kod uchun):
- CCCD `0100` yoziladi (`[T]`), keyin `0x0013` yuboriladi.
- `0x0014 = 00` → bog'langan (`[T]`).
- `0x0014 = 01` → soat ekranida tasdiq kutilmoqda (`[E]`). 15 soniyadan keyin `0x0013` qayta yuboriladi (`[E]`, FitCloudPro ham tasdiqdan keyin qayta yuborgan: `11:46:00`).
- Boshqa kod → xato, xom bayt ko'rsatiladi (ma'nosi berilmaydi).
- Ajratish (`0x0015` → `0x0016 = 00`) o'zgarmaydi, `[T]`.

Eslatma: v0.4 dagi "avval `0x0011`" taklifi shu tuzatish bilan bekor qilinadi.

### v0.4.2 qurilma tasdiqlari: ilova bog'lash/ajratish (2026-10-11, 18:52–18:59)

Manba: `captures/s4-indep/bugreport-s4-3.zip` → `FS/data/log/bt/btsnoop_hci.log` (`conn 0x0201`, devor vaqti). Ilova: 0x0013 bilan boshlanadigan versiya (commit `34dadff`).

| Vaqt | Yo'nalish | Nima | Holat |
|---|---|---|---|
| `18:52:31` | P → W | `0x0013` (32 bayt hisob), SEQ `0x0001` | **[T]** |
| `18:52:31` | W → P | `0x0014` = `00` | **[T]** muvaffaqiyat (ilova "Connected") |
| `18:54:45` | P → W | `0x0015` (bo'sh), SEQ `0x0002` | **[T]** |
| `18:54:45` | W → P | `0x0016` = `00` | **[T]** ajratish muvaffaqiyatli |
| `18:55:14` | P → W | `0x0013`, SEQ `0x0001` (yangi ulanish) | **[T]** |
| `18:55:14` | W → P | `0x0014` = `01` | **[T]** qiymat; soat ekranida "pairing request — tap ✓" (ilova ekrani) |
| `18:55:29`, `:44`, `:59` | P → W | `0x0013` qayta, har 15 s | **[T]** |
| `18:55:29`, `:44`, `:59` | W → P | `0x0014` = `01` har safar | **[T]** |

Xulosa:
1. Bog'lash `0x0013` bilan ishlaydi: tanilgan hisobda (ajratishdan oldin) `00` [T]. Ajratishdan keyin yangi bog'lashda `01` [T], soat tasdig'i kerak.
2. `01` soat ekranida tasdiq so'rovi ekanligi [E]: soat tasdig'i (✓) bajarilgandan keyingi `0x0014` javobi hali kuzatilmagan. Kutilayotgan: `00` [E].
3. Ajratish `0x0015 → 0x0016 = 00` [T] (birinchi marta ilova orqali).
4. Har yangi ulanishda telefon SEQ `0x0001` dan boshlanadi [T], soat ACK'lari telefon kadrlariga mos [T].

### v0.4.3 tuzatish: bog'lash so'rovi 0x0011, tasdiq javobi (FitCloudPro, 19:14–19:17, bugreport-s4-4)

v0.4.1 dagi xulosa "0x0011 yangi hisobga javob bermaydi" noto'g'ri edi. O'sha "javob yo'q" holati soat ekranida tasdiq kutilayotgani bo'lgan (ya'ni prompt chiqqan, lekin tasdiqlanmagan). Shu sabab 18:40 dagi ilova `0x0011` ga javob olmagan, va biz `0x0013` ga o'tgan edik. `0x0013` esa yangi hisobda darhol `01` qaytaradi va soat prompt ko'rsatmaydi. Yangi dalil: FitCloudPro ajratgandan keyin qayta bog'landi.

| Vaqt | Yo'nalish | Nima | Holat |
|---|---|---|---|
| `19:14:50` | P → W | `0x0011` (tanilgan hisob) | **[T]** |
| `19:14:53` | W → P | `0x0012` = `00` | **[T]** tez javob |
| `19:15:59` | P → W | `0x0015` (ajratish) | **[T]** |
| `19:15:59` | W → P | `0x0016` = `00` | **[T]** |
| `19:16:11.314` | P → W | CCCD `0100` | **[T]** |
| `19:16:11` | P → W | `0x0011` (SEQ `0x0001`) | **[T]**; soat faqat ACK qaytaradi, javob yo'q |
| `19:16:25` | FitCloudPro ekrani | "Please confirm pairing on the watch" | **[T]** ekran |
| `19:16:29` | P → W | `0x0011` qayta (SEQ `0x0002`) | **[T]**; soat ACK |
| `19:16:32` | W → P | `0x0012` = `00` | **[T]** tasdiqdan keyin (~3 s) |
| `19:16:51` | FitCloudPro ekrani | "Connected" | **[T]** |

Xulosa (kod uchun):
1. Bog'lash so'rovi `0x0011` (32 bayt hisob). Javob `0x0012`, `00` = bog'langan **[T]**.
2. Yangi (ajratilgan) hisobda soat `0x0011` ga darhol javob bermaydi: ACK yuboradi va soat ekranida tasdiq so'raydi. Foydalanuvchi ✓ bossa, `0x0012 = 00` keladi **[T]**. Tasdiq kutilayotgani **[E]**: FitCloudPro ekrani shu holatni ko'rsatdi.
3. Javob kelmasa (taxminan 15 s), `0x0011` qayta yuboriladi. FitCloudPro 18 s dan keyin qayta yuborgan **[T]**. Qayta yuborish oralig'i **[E]**.
4. `0x0013` asosiy so'rov emas. Yangi hisobda `01` darhol keladi, prompt chiqmaydi (Orvell 18:55–18:58 **[T]**). Kodda ishlatilmaydi.
5. `0x0012` ning boshqa qiymatlari: ko'rilmagan **[N]**. Ilova xom baytni ko'rsatadi, ma'nosini taxmin qilmaydi.
6. v0.4.1 dagi "0x0011 faqat tanilgan hisob uchun" xulosasi bekor qilinadi.

### v0.5 vaqt (`TYPE 0x02`, `ID 0x0001`): dekodlandi [T] (2026-10-11)

Qiymat 4 bayt, BE, 32 bit. Mahalliy devor vaqti, maydonlar MSB dan:

| Bitlar | Hajm | Maydon | Oraliq |
|---|---|---|---|
| 31–26 | 6 | yil − 2000 | 0–63 (2000–2063) |
| 25–22 | 4 | oy | 1–12 |
| 21–17 | 5 | kun | 1–31 |
| 16–12 | 5 | soat | 0–23 |
| 11–6 | 6 | daqiqa | 0–59 |
| 5–0 | 6 | soniya | 0–59 |

Tekshiruv: FitCloudPro'ning 10 ta `0x0001` yozuvi (uchta oyna: s4-1 17:59–18:05, s4-4 19:14–19:16, s4-5 19:39) har biri log vaqti bilan aynan mos keldi (10 / 10). Misollar: `6A971EEE` = 17:59:46, `6A9739C5` = 19:39:05. Shuningdek 19:39:05 yozuvidan keyin soat ekranida 19:47 ko'rindi (foydalanuvchi o'qidi), bu ham mos.

- Yozuv ulanishda bog'lash tugagach keladi (FitCloudPro: 19:14:53, 19:16:32, 19:39:01). **[T]**.
- Yonida `0x0006` (maqsad) ning u32@12 maydoni shu qiymatning nusxasi **[T]** (5 / 5).
- Oldingi "chiziqsiz, Unix emas" xulosasi (v0.3–v0.4) noto'g'ri taxminga asoslangan edi. Bekor qilindi.
- Javob (ACK) kuzatiladi, lekin vaqt yozuvi uchun ma'lumot javobi kelmaydi **[T]**.

### v0.4.4 ilova o'zgarishi: bog'lash tekshiruvi va tarix qayta so'rovi (2026-10-12)

Bu bo'lim ilovaning qarorlarini yozadi (**[E]**), protokol dalili emas. Qurilma dalili alohida belgilangan.

Bog'lash (`0x0013` → `0x0014`, `0x0011` → `0x0012`):
1. Har ulanishda avval `0x0013` (hisob satri). `00` = tanilgan hisob: bog'lanadi, `0x0011` yuborilmaydi. **[T]**: qurilmada `0x0013` → `0x0014 = 00` 2026-10-12 20:48:33 da ~420 ms, ✓ so'rovi yo'q, keyin vaqt yuborildi.
2. `01` = soat ekranida tasdiq kutilmoqda. `0x0011` **bir marta** yuboriladi (prompt uchun), keyin `0x0013` bilan davriy tekshiruv (har 15 s). `0x0011` qayta yuborilmaydi: har yuborish soatda yangi ✓ so'roviga sabab bo'lishi mumkin **[E]**. Foydalanuvchi ✓ bossa, `0x0013` yoki `0x0012` = `00` keladi va ilova bog'lanadi **[E]**.
3. Tekshiruvga javob kelmasa (oxirgi `0x0013` javobsiz), `0x0011` navbatma-navbat (har 30 s) yuboriladi. Bu zaruriy holat, odatiy emas **[E]**.

Tarix (`0x0007` / `0x0008`):
4. `0x0007 != 0x0008`: shu hisoblagich **bir marta** qayta so'raladi (`0x0001`). Qayta so'rovda ham mos kelmasa sinxron to'xtaydi: ACK ham, `0x0020` ham yuborilmaydi, yozuv soatda qoladi **[E]**.
5. Qayta so'rovda oldin saqlangan yozuv takror kelsa, faqat ACK; qayta saqlanmaydi. Boshqa yozuv kelsa to'xtaydi **[E]**.
6. Qurilmada mos kelmaslik kuzatilmagan: bu band faqat JVM testlar bilan tekshirilgan (`HistoryFlowTest`). Qurilmada tarix sinxroni (2026-10-12 20:48:53, "yangi yozuv 3") mos kelmasdan o'tdi **[T]**.
