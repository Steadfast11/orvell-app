# Hal bo'lmagan savollar va keyingi capture (v0.4)

Bu fayl spec'dagi bo'shliqlarni sanab beradi. Har biri uchun holat, dalil va aniq tekshiruv yoziladi. Bo'shliq yopilmaguncha kod unga tegmaydi (funksiya kodga kirmaydi yoki ilovada "Tez orada" bo'ladi).

Belgilar: **[T]** tasdiqlangan, **[E]** ehtimol, **[N]** noma'lum.

## v0.4 holat (s4 capture, 2026-10-11)

Bu jadval v0.3 ning yangilangan holati. Har qator uchun yangi dalil [messages.md](messages.md) ning "v0.4 yangilanishi" bo'limida.

| # | Mavzu | v0.3 | v0.4 | Kodga ta'siri |
|---|---|---|---|---|
| 1 | GATT UUID va handle'lar | [T] | [T] | UUID'lar kodga kiradi |
| 2 | Vaqt `0x0001` | [N] | [N]: chiziqsiz (s4 ham) | yozilmaydi, ilovada "Tez orada" |
| 3 | Batareya | [N] / [E] | [E]: `0x1C` = `00 04 50` 11/11, foiz ekranda ko'rinmagan | foiz ko'rsatilmaydi, "Tez orada" |
| 4 | Jonli puls | oqim [T], puls [N] | oqim [T], puls [N] (s4 da o'lchov yo'q) | "Tez orada" |
| 5 | Tarix sinxroni | tuzilish [T], mapping [E] | tuzilish [T]; puls/BP mapping [N] (rad), qadam [N], SpO₂ [N], uyqu [N] | "Tez orada" |
| 6 | Bog'lash | `0x0011` [T]; `0x0013/0x0014` [N] | `0x0011 → 00` [T]; `0x0013 → 00` [T]; `0x0014 = 01` [E] (tasdiq kutilmoqda) | bog'lash oqimi kodga kiradi, qurilmada sinov kerak |
| 7 | `0x003B`/`0x003C` | ziddiyat | [N]: s4 da yozuv bor, ATT rad javobi parserda ko'rinmadi | yozilmaydi |
| 8 | `0x0017`, 6287, `d0ff` | [N] | [N] | yozilmaydi |
| 9 | Ulanish handle'lari | [T] | [T] | — |
| 10 | Sessiya oxiri | ahamiyatsiz | ahamiyatsiz | — |
| 11 | Bo'lingan payload (LEN > 244) | [T] eski, [N] | [N] (s4: LEN ≤ 122) | rad etiladi |
| 12 | `KIND=0x00`, LEN=0 | [N] | [N] | rad etiladi |
| 13 | Sarlavha va payload orasi | [T] kuzatuv | [T] (s4: 67 / 67 darhol) | — |
| 14 | Maqsad `0x0006` u32@0 = 8000 | [T] | [T] (s4 ham) | o'qish mumkin, yozish "Tez orada" |
| 15 | Maqsad u32@12 = `0x0001` | [T] tenglik | [T] (2/2) | — |
| 16 | CCCD `0100` bog'lashdan oldin | [E] | [T] (5 holat) | bog'lashdan oldin yoziladi |
| 17 | `05/0006` | [N] | [E]: 12, 33, 0 (sinxron bo'yicha) | yozilmaydi |
| 18 | `TYPE 0x04`, ID `0x0047` | yo'q | [N] (yangi) | yozilmaydi |
| 19 | `0x0044` javob | [N] | [N]: har doim `0E 75 00 01 0E 01` | yozilmaydi |

**Qaror (foydalanuvchi, v0.4 dan keyin):** ilova qanday ishlashi kerak. Tasdiqlanmagan funksiyalar (sinxron, puls, qadam, SpO₂, uyqu, batareya, vaqt, jonli o'lchov, maqsad yozish) ilovada "Tez orada". Qolgani: ulanish, bog'lash, ajratish.


## Qisqa xulosa

| # | Mavzu | Holat (v0.3) | Kodga ta'siri |
|---|---|---|---|
| 1 | GATT UUID va handle'lar | **[T]** (dumpstate), uchta handle'ning vazifasi **[N]** | UUID'lar kodga kiradi |
| 2 | Vaqt (`02/0001`, 4 bayt) | **[N]**. `0006` bayt 12–15 = oxirgi `0001`: tenglik **[T]** (3 / 3), ma'nosi **[N]**. `0024` bayt 4–5 daqiqa **[T]** (20 / 20). `0030` daqiqa **[E]** | `02/0001` yozilmaydi |
| 3 | Batareya | **[N]**: ekranda faqat ikonka, raqam yo'q (`screen_115044.png`). `0x001C` bayt 2 = `0x50` nomzod **[E]** | yozilmaydi |
| 4 | Jonli puls (`05/0024`) | oqim tuzilishi **[T]** (ikki o'lchov, 20 kadr, VLEN 11); daqiqa baytlari per-kadr **[E]**; puls bayti **[N]**; bayt 7–10 **[N]** | yozilmaydi |
| 5 | Tarix sinxroni (`05/0007`, `0008`, `0030`) | uzunlik **[T]** (har sinxronda 9 / 9 juft; jami 18 / 18); puls/BP nomzod **[E]** (1 sinxron mos, 1 mos emas); qadam **[E]** (bayt 8–13 nol; bayt 0–7 nol emas); SpO₂, uyqu **[N]** | yozilmaydi |
| 6 | Bog'lash (`0x0011`/`0x0012`, `0x0013`/`0x0014`) | `0x0011`→`00` **[T]**; `0x0014` ma'nosi **[N]** | `0x0011` kodga kiradi |
| 7 | `0x003B` / `0x003C` | ziddiyat (discovery va ATT xato) | yozilmaydi |
| 8 | `0x0017`, 6287, d0ff | logda murojaat yo'q **[N]** | yozilmaydi |
| 9 | Ulanish handle'lari (`0x0201`, `0x0202`) | **[T]**: 12:12 qismi boshqa qurilma | — |
| 10 | Sessiya oxiri (`12:02:55.513`, ACK yo'q) | ahamiyatsiz | — |
| 11 | Bo'lingan payload (LEN > 244) | **[T]** eski trafikda (14 / 520); **[N]** FitCloudPro'da yo'q | rad etiladi |
| 12 | `KIND=0x00` va LEN = 0 | **[N]**: loglarda 0 marta | rad etiladi |
| 13 | Sarlavha va payload orasida boshqa notify | **[T]** kuzatuv: 0 marta; xulq **[N]** | xavfsiz rad |

## 1. Xizmat va xarakteristika UUID'lari

**Holat: [T]** (dumpstate, `bugreport-rebind.zip` → `dumpstate-2026-10-11-12-28-00.txt`, `bta_gattc_display_explore_record`, 12:27:47). Hash farqi 12:27:46.713 da. Discovery va qayta bog'lash bind'i (12:27:47.710, `conn 0x0202`) bir sessiyada.

| Handle | UUID | Xususiyat (prop) | Holat |
|---|---|---|---|
| `0x0001–0x0006` | `1801` | `0x0003` = `2A05` (`0x20` indicate), CCCD `0x0004` | **[T]** |
| `0x0007–0x000F` | `1800` | `2A00`, `2A01`, `2A04`, `2AA6` | **[T]** |
| `0x0010–0x0017` | `000001ff-…` | `0x0012` `ff02` (write); `0x0014` `ff03` (notify), CCCD `0x0015`; `0x0017` `ff04` (read/write) | **[T]** |
| `0x0018–0x002C` | `d0ff-…` | `ffd1` … `fff5` | **[T]** UUID; murojaat yo'q **[N]** |
| `0x002D–0x0032` | `6287-…` | `6387` `0x002F`, `6487` `0x0031`, CCCD `0x0032` | **[T]** UUID; murojaat yo'q **[N]** |
| `0x0033–0x0038` | `fee7` | `fec9` `0x0035`, `fea1` `0x0037`, CCCD `0x0038` | **[T]** UUID |
| `0x0039–0xFFFF` | `3802` | `4a02` `0x003B` (prop `0x1a`), `0x003C` (discovery'da yozuv yo'q) | **[T]** UUID; ziddiyat (§7) |

## 2. Vaqt

**`02/0001` (4 bayt, telefon → soat): [N].**

Yangi dalil (v0.3):
- Qiymat log vaqti bilan chiziqli emas: 2,084 s da +2; 40,98 s da +45; 230,5 s da +242; 42 daqiqada +2973.
- Qiymatning vaqt qismi devor soatidan oldinda: `11:45:20,277` da +2 min 11,7 s; `12:27:49,758` da +9 min 15,2 s; `12:27:51,068` da +9 min 15,9 s. Sana 40 kun oldin (Unix UTC: 2026-09-01).
- `05/0024` bayt 4–5 (daqiqa): per-kadr **[E]** (11:53: 20 / 20; 14:11: 19 / 20). `05/0030` bayt 4–5: yozuv vaqti **[E]**. `02/0001` bu maydonlar bilan bir xil shakl emas.
- `02/0006` bayt 12–15 = oxirgi `02/0001` qiymati: tenglik **[T]** (3 / 3). Maydonning ma'nosi (vaqt) **[N]**.

**Kerak (capture C2):** qurilmaning o'z soat ekranini va telefon vaqtini bir vaqtda suratga olish, ikki vaqt orasidagi farqni yozish, so'ng vaqt sinxronlash (agar FitCloudPro'da alohida tugma bo'lsa) va yana bir skrinshot. Foydalanuvchi ruxsati bilan (soat vaqtini o'zgartiradi).

**Kodga ta'siri:** vaqt yuborilmaydi.

## 3. Batareya

**[N].** Ekranda faqat batareya ikonkasi ko'rinadi (`screen_115044.png`, 11:50:44: "GL-SW26 Connected", yashil ikonka, raqam yo'q), shuning uchun [T] emas. Alohida batareya so'rovi topilmadi. Davriy `0x001B` (telefon) → `0x001C` (soat, `00 04 50`) mediana 7,50 s (faqat guruhlar ichida); orada 16,9–51,9 s va 658,7 s uzilishlar (11:51:26 dan 12:02:25 gacha). Bu juftlik 11:49:33–11:51:26 (sinxron davri) va 11:50:34–49 (batareya stsenariysi) ichida ham, sessiya va qayta bog'lashda ham o'zgarmas. Batareya foizi ham uzoq vaqt o'zgarmasligi mumkin, shuning uchun bu o'zi batareyani inkor etmaydi. Nomzod **[E]**: bayt 2 = `0x50` (ehtimol 80%), tasdiqlanmagan.

**Kerak (capture C1):** FitCloudPro "Device" sahifasida batareya foizi ko'rinadigan holatda, snoop boshlanadi, sahifa ochiladi va yangilanadi (agar yangilash tugmasi bo'lsa), foiz skrinshoti vaqt bilan, snoop to'xtatiladi. Log `captures/`.

**Kodga ta'siri:** batareya ko'rsatilmaydi, "Tez orada".

## 4. Jonli puls va maqsad

### 4a. Jonli oqim (`05/0024`)

**Tuzilish: [T]**:
- Boshlash: telefon `05/0023` = `00 01 05 01` (`11:52:15.809`, `conn 0x0201`); 14:11 o'lchovi: `14:11:39.628` **[T]**.
- Kadr: 16 bayt payload (TLV `05 0024 000B` + 11 bayt), ~2,9 s, 20 kadr (`11:52:19.125`–`11:53:13`). 14:11 o'lchovi: 20 kadr, `14:11:43.159`–`14:12:37.872` **[T]**; to'xtatish `05/0023` = `00 00 05 00` (`14:12:39.632`) **[T]**. Har kadr notify'da payload bilan (sarlavha + payload).
- To'xtatish: `05/0023` = `00 00 05 00` (`11:53:15.807`).
- Bayt 0–1 `35 4B`, bayt 2–3 `00 01` (20 / 20 va 20 / 20) **[T]**, ma'nosi **[N]**.
- Bayt 4–5 (BE) = kadr vaqtining daqiqasi: per-kadr **[E]** (11:53: 20 / 20; 14:11: 19 / 20, chegara kadri farqi; birgalikda 39 / 40). Oxirgi kadr `713` = 11:53 ekran "11:53" yorlig'i bilan mos **[T]**: `712` → `713` `11:53:02` da.

**Puls maydoni: [N].** 17:32 Orvell o'lchovi (`measure78` log) spec manbasi emas (`docs/clean-room/README.md`, 4-qoida): tahlil qilinmadi; mustaqil nuqta FitCloudPro bilan tekshiriladi (C4b). Bayt 6 nomzod edi. Ekran natijasi (`screen_115324.png`, 11:53:24) bilan solishtirildi: oxirgi kadr mos kelmadi (ekran natijasi kadr qiymatlaridan farq qiladi, yoki natija boshqa manbadan). 14:11 o'lchovida ham ekran qiymatiga teng bitta kadr (o'rtada), oxirgi kadr mos emas. Bayt 7–10: **[N]**. Hali bitta o'lchov bo'yicha tekshiruv yo'q, shuning uchun puls bayti [N].

**Kerak (capture C4):** 3 ta o'lchov, har biri 30 s, orasida 1 daqiqa. Har o'lchovdan oldin va keyin ekran skrinshoti (qiymat va vaqt), snoop bilan birga. Qiymatlar repoga yozilmaydi, faqat bayt joyi va mos kelish natijasi.

**Kodga ta'siri:** jonli puls ko'rsatilmaydi, "Tez orada". TLV qatlami (`05 0024 000B`) kodga kirmaydi.

### 4b. Maqsad (`06`, 18 bayt)

**Qisman: [T]** u32@0 = 8000 (qadam maqsadi, ekran tasdiqlagan, `screen_115024.png`). Qolgan maydonlar (u32@4, u32@8, u16@16) **[N]**. u32@12 = `0x0001` qiymatiga teng (uch holatda, tenglik **[T]**; ma'nosi **[N]**).

**Kerak (capture C5):** maqsadni 8000 dan 9000 ga o'zgartirish (ekranda), frame'ni solishtirish, so'ng 8000 ga qaytarish. Foydalanuvchi ruxsati bilan.

**Kodga ta'siri:** 18 bayt to'liq yozilishi kerak, qolgani taxmin. Maqsad funksiyasi kodga kirmaydi.

## 5. Tarix sinxroni (`05/0007`, `05/0008`, `05/0030`)

**Yozuv tuzilishi: [T]** (6 yozuv, 2 sinxron): `05/0007` = `05/0008` = N (yozuv uzunligi), keyin `05/0030` (N bayt). Tuzilish `messages.md` da. Hisoblagich `01`, `03`, `05` da N > 0.

**Daqiqa maydoni (`05/0030` bayt 4–5): [E]**: 705 = 11:45, 709 = 11:49, sinxrondan oldingi daqiqalar (ekranda yozuv vaqti ko'rinmagan).

**Puls va qon bosimi mapping: [E].** 11:49 sinxronida yozuvlar ekran bilan mos (puls, qon bosimi). 11:51 sinxronida yozuvlar ekran (11:51:46) bilan mos emas. Bitta mos keldi, bitta yo'q. Sabablar: ekran qiymati sinxrondan keyingi boshqa o'lchovdan bo'lishi mumkin, yoki mapping noto'g'ri.

**Ochiq:** SpO₂ **[N]** (ekranda raqam bor, lekin uning qiymati hech bir sinxron yozuvida yo'q). Uyqu **[N]** (ekranda "No sleep data last night", yozuv yo'q). Qadam **[E]** (ekranda 0; 14 baytli yozuvda bayt 8–13 nol; bayt 0–7 nol emas). Puls tarixi **[E]** (`05/0030`).

**Kerak (capture C3):** har tur uchun alohida sinxron, har biridan oldin va keyin ekran skrinshoti (vaqt bilan), orasida 1–2 daqiqa pauza:
1. Faqat qadam (Home sahifasini pastga tortish, qadam ekranida ko'rinishi).
2. Faqat yurak urishi tarixi (Heart Rate sahifasi).
3. Faqat SpO₂ (Oxygen sahifasi).
4. Faqat uyqu (agar tunda ma'lumot bo'lsa).
Har sinxronni yozib, snoop bilan birga.

**Kodga ta'siri:** tarix sinxroni yo'q, ilovada "Tez orada". **Bu eng katta funksional bo'shliq** ([qaror](#qaror-kerak) pastda).

## 6. Bog'lash javoblari

**Yopilgan: [T]**:
- `0x0011 → 0x0012 = 00`: ikki marta (`11:45:19.180`, `12:27:49.053`). Qayta bog'lashda ilova "Bind Success" ko'rsatgan.
- Ajratish `0x0015 → 0x0016 = 00` (`12:02:55.052`).

**Ochiq: [N].**
- `0x0013 → 0x0014`: `01` (`11:44:53.293`) va `00` (`11:46:00.618`). Ma'nosi (birinchi bog'lash, tasdiq kutilmoqda, qayta bog'lash) noma'lum.
- Ilova o'z hisob satrini yuborganda soat qabul qiladimi. Eski Orvell trafigi manba emas.
- **Xavf:** `0x0011` faqat qayta bog'lashda (soat allaqachon tanish) tasdiqlangan. Yangi qurilmaga birinchi bog'lash `0x0011` bilan ishlashi tasdiqlanmagan. Kod hozir faqat `0x0011` yuboradi. Qurilmada sinov foydalanuvchi ruxsati bilan.

**Kerak (capture C6):** ajratish, so'ng boshidan bog'lash. Har qadamdan oldin va keyin soat ekrani va telefon ekrani skrinshoti. Foydalanuvchi ruxsati bilan (soat holatini o'zgartiradi).

**Kodga ta'siri:** `0x0011`/`0x0012` kodga kiradi (tasdiqlangan). `0x0013`/`0x0014` kodga kirmaydi. Hisob satri parametr sifatida olinadi, repoda hech qachon qattiq yozilmaydi.

## 7. Handle `0x003B` va `0x003C`

**Ziddiyat.** FitCloudPro oynasida telefon `0x003B` ga 4 marta `AA BB …` yozdi; soat har safar ATT `0x0A` (Attribute Not Found) bilan rad etdi (raw ATT xato, `conn 0x0201`). Qayta bog'lashda 2 marta. `0x003C` ga `0100` yozilgan, rad etilmagan.

Ziddiyat: GATT discovery `0x003B` ni `4a02` (prop `0x1a`, write bor) deydi. Yozish rad etilishi bilan mos emas.

**Kerak:** discovery'ni qayta olish (GATT DB o'zgargan bo'lishi mumkin) va `0x003C` ning ma'nosi. Foydalanuvchi ruxsati bilan.

**Kodga ta'siri:** `AA BB` kadrlari kodga kirmaydi.

## 8. Logda murojaat yo'q handle'lar

`0x0017` (`ff04`), 6287 xizmati (`0x002F`, `0x0031`), `d0ff` xizmati (`0x0018`–`0x002C`): FitCloudPro va qayta bog'lash loglarida na yozuv, na notify. Ma'nosi **[N]**.

**Kerak:** ilovada bu funksiyalardan foydalanadigan stsenariy bo'lsa, shu stsenariy bilan capture.

## 9. Ulanish handle'lari va boshqa qurilma

**Yopilgan: [T].** Rebind logidagi `conn 0x0201` (12:12) boshqa MAC'li qurilma. Soat `conn 0x0202` da (12:27:47 dan). FitCloudPro oynasida (11:44:52–12:02:56) faqat `0x0201` (613 qator), soat.

## 10. Sessiya oxiri

Log `12:02:55.513` da tugaydi. Telefon SEQ `0x006B` frame'iga soat ACK'i ushlanmagan. Ahamiyatsiz.

## 11. Bo'lingan payload (LEN > 244)

**[T]** eski trafikda 14 / 520 (`final` log), **[N]** FitCloudPro oynasida. 14 ta ma'lumot sarlavhasi LEN = 261 bilan kelgan: 8 ta 2026-10-09, 3 ta 2026-10-10, 3 ta 2026-10-11 00:43 (hammasi FitCloudPro'dan oldin, Orvell davri, spec manbasi emas). Ular 244 + 17 bayt notify'larga bo'lingan, birlashtirilganda CRC mos. FitCloudPro oynasida (11:44–12:02) bunday frame yo'q. Eng katta bitta notify ma'lumoti 122 bayt.

**Kerak:** tarix sinxroni (C3) katta frame berishini kuzatish. Agar LEN > 244 bo'lsa, bo'lish qoidasi FitCloudPro logida tasdiqlanishi kerak.

**Kodga ta'siri:** kod bunday frame'ni rad etadi (birinchi notify hajmi LEN ga teng emas). Bo'lish yig'ilmaydi.

## 12. `KIND = 0x00` va LEN = 0

**[N].** Barcha loglarda (993 soat sarlavhasi, shu jumladan eski trafik) bunday sarlavha 0 marta. Shu sababli kod uni rad etadi.

**Kerak:** tarix sinxroni yoki boshqa capture'da bunday sarlavha paydo bo'lsa, u holda qaysi so'rovdan keyin ekanini yozish.

## 13. Sarlavha va payload orasida boshqa notify

**[T] kuzatuv, xulq [N].** Ma'lumot sarlavhasidan keyin ACK yoki boshqa notify kelgan holat hech qayerda kuzatilmagan:
- `final` log: 520 ma'lumot sarlavhasi. 506 tasida keyingi notify aynan LEN bayt payload. 14 tasi (LEN = 261) 244 + 17 bayt bo'lingan, birlashtirilganda CRC mos. Ikkala holatda ham orada boshqa notify yo'q.
- `rebind` log: 29 ma'lumot sarlavhasi, 29 / 29 darhol payload, CRC xato 0.
- ACK sarlavhalari (473 + 38) faqat sarlavha sifatida keladi, ma'lumot payload'i orasiga tushmagan.

**Kerak:** bunday holatni qurilmada kuzatish shart emas, lekin capture'da paydo bo'lsa, qaysi so'rovdan keyin ekanini yozish.

**Kodga ta'siri:** payload kutilayotganda keladigan notify payload deb tekshiriladi. Hajm yoki CRC mos bo'lmasa `WireFormatException`, sarlavha tashlanadi (xavfsiz rad).

## Kuzatuvlar (da'vo emas)

Bular T2 hisobotidagi kuzatuvlar. Spec da'vosi sifatida ishlatilmaydi, kod va tahlil bularga tayanmaydi.

- **O1.** Qayta bog'lash sessiyasida (`12:27:51`–`12:27:57`, `conn 0x0202`) to'liq hisoblagich ketma-ketligi bor: 9 ta `05/0001`, 9 juft `05/0007`/`05/0008` (hammasi nol), `05/0030` yo'q, keyin `05/0021` va `05/0022`. Bu sessiya sinxron deb tavsiflanmagan. "N > 0 da `01`/`03`/`05`" qoidasi shu yerda ishlamaydi.
- **O2.** FitCloudPro oynasida jadvalda yo'q TLV'lar: `02/0018` (5 ta, `11:47:48`–`11:48:37`, uzunlik 2) va `02/003B` (2 ta, `11:48:44` va `11:48:55`, uzunlik 0). Ma'nosi noma'lum.
- **O3.** `11:45:22` atrofida `05/0005` → `05/0006` → `05/0021` → `05/0022` almashuvi (hisoblagichsiz). Spec'da alohida tavsif yo'q.

## Capture rejasi (foydalanuvchi ruxsati bilan)

Har bir stsenariy: vaqtni yozish, skrinshotlar, snoop boshidan oxirigacha. Log `captures/` ga, repoga emas. Foydalanuvchi har stsenariydan oldin tasdiqlaydi.

| ID | Nima | Nega | Soat holatini o'zgartiradimi |
|---|---|---|---|
| C1 | Batareya: "Device" sahifasida foiz, snoop | §3, batareya | yo'q |
| C2 | Vaqt: soat ekrani va telefon vaqti bir vaqtda; so'ng vaqt sinxroni (agar bor) | §2, vaqt | ha (vaqt sinxroni) |
| C3 | Tarix: qadam, puls tarixi, SpO₂, uyqu alohida-alohida | §5, tarix | yo'q (sinxron) |
| C4 | Jonli puls: 3 o'lchov, 30 s, orasida 1 daqiqa | §4a, puls | yo'q |
| C4b | Puls mustaqil tekshiruvi: FitCloudPro orqali o'lchov; soat ekrani va FitCloudPro ekrani bir vaqtda suratga olinadi, snoop bilan. Qiymatlar repoga yozilmaydi. Orvell (eski kod) orqali o'lchov spec manbasi emas | §4a, puls | yo'q |
| C5 | Maqsad 8000 → 9000 → 8000 | §4b, maqsad | ha (sozlama) |
| C6 | Ajratish, so'ng boshidan bog'lash | §6, bog'lash | ha (bog'lash holati) |

**Xavf:** C2, C5, C6 soat holatini o'zgartiradi. Har biri oldindan foydalanuvchi tasdig'i bilan.

<a id="qaror-kerak"></a>

## Qaror kerak (foydalanuvchi)

Hozirgi holatda yangi kodek tasdiqlangan qismlarni qamraydi: frame, CRC, ACK, TLV, bog'lash (`0x0011`/`0x0012`), ajratish (`0x0015`/`0x0016`). Qolgan funksiyalar (tarix sinxroni, qadam, puls, SpO₂, uyqu, batareya, vaqt, jonli puls, maqsad) tasdiqlanmagan. Hozirgi ilova bularga tayangan.

Ulash (WatchManager/BleClient) ilova funksiyalarini kamaytiradi. Uchta yo'l:

1. Yangi kodek ulanadi, tasdiqlanmagan funksiyalar ilovada "Tez orada" bo'ladi (tarix, batareya, vaqt, puls). Eng toza clean-room, lekin hozirgi qadam/puls ko'rinishi yo'qoladi.
2. Eski kodek vaqtincha qoladi (faqat ma'lumot olish uchun), yangi kodek bog'lash va ajratish uchun. Ikki kodek bir vaqtda; eski kodni o'chirish keyinga qoladi. Clean-room to'liq emas.
3. Avval capture (C1–C4), keyin ulash. Ulash eng keyin.
