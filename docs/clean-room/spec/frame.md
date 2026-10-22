# Frame formati (v0.4)

Soat va telefon orasidagi har bir xabar ATT orqali yuboriladi. Ustida 8 baytli sarlavhali frame turadi. Barcha ko'p baytli maydonlar **big-endian**.

## Sarlavha (8 bayt)

| Offset | Hajm | Maydon | Ma'nosi |
|---|---|---|---|
| 0 | 1 | `MAGIC` | Doim `0xAB`. |
| 1 | 1 | `KIND` | `0x00` = ma'lumot frame'i, `0x10` = ACK. |
| 2 | 2 | `LEN` | Payload uzunligi (bayt). Frame jami uzunligi = `8 + LEN`. |
| 4 | 2 | `CRC` | Payload'ning CRC-16/ARC'i. `LEN = 0` bo'lsa `0x0000`. |
| 6 | 2 | `SEQ` | Yo'nalish bo'yicha hisoblagich. ACK'da — tasdiqlanayotgan frame'ning SEQ'i. |
| 8 | `LEN` | payload | [`messages.md`](messages.md) dagi TLV. |

Holat (v0.3, 2026-10-11, barcha loglar bo'yicha, `conn` ajratilgan):

| Maydon / qoida | Holat | Dalil |
|---|---|---|
| MAGIC `0xAB`, 8 bayt sarlavha | **[T]** | 993 soat sarlavhasi (`final` log, barcha ulanishlar), 0 istisno |
| KIND `0x00` yoki `0x10` | **[T]** | 520 ma'lumot + 473 ACK; boshqa KIND 0 marta |
| KIND boshqa qiymat | **[N]** | ko'rilmagan. Kod bunday sarlavhani rad etadi (xavfsiz rad, protokol ma'nosi emas). |
| ACK: LEN = 0, CRC = 0x0000 | **[T]** | 473 / 473 ACK (`final`), 38 / 38 ACK qayta bog'lashda |
| `KIND = 0x00` va `LEN = 0` | **[N]** | loglarda 0 marta (520 ma'lumot sarlavhasi ichida). Kod bunday sarlavhani rad etadi. |
| LEN = 0 bo'lsa CRC = 0x0000 | **[T]** | faqat ACK uchun kuzatilgan; ma'lumot frame'i bilan tasdiqlanmagan |
| Ma'lumot payload'i bitta notify'da (LEN ≤ 122) | **[T]** | FitCloudPro oynasida barcha 118 ma'lumot frame'i; eng katta 122 bayt |
| LEN > 244: payload bo'lingan | **[T]** eski trafikda (14 / 520); **[N]** FitCloudPro oynasida | `final` log: 14 ta ma'lumot sarlavhasi (LEN = 261) 244 + 17 bayt bo'lib kelgan, birlashtirilganda CRC mos. Sana: 8 ta 2026-10-09 (conn `0x0201`), 3 ta 2026-10-10 (conn `0x0200`), 3 ta 2026-10-11 00:43 (conn `0x0201`). Hammasi Orvell davri trafigi, spec manbasi emas. FitCloudPro oynasida (11:44–12:02) bunday frame yo'q. Kod (v0.5): to'liq hajmli (ATT_MTU − 3) notify'dan keyin davomini kutadi va LEN to'lguncha yig'adi, CRC birlashtirilgan payload bo'yicha; boshqa hajmdagi qisqa notify rad. Yig'iladigan LEN chegarasi 4096. Orvell sinxronida (2026-10-13 14:45) LEN 261 frame'lar 244 + 17 bo'lib keldi va CRC mos **[T]**. MTU kattalashsa bitta notify'da katta frame bo'lishi mumkin: **[N]**. |
| Sarlavha va payload orasida boshqa notify | **[T]** kuzatuv: 0 marta; xulq **[N]** | `final` log: 520 ma'lumot sarlavhasidan 506 tasida darhol LEN bayt payload; 14 tasi (LEN = 261) 244 + 17 bo'lingan (pastga qarang). `rebind`: 29 / 29 darhol. Sarlavha va payload orasida ACK yoki boshqa notify hech qayerda kuzatilmagan. Kod: payload kutilayotganda keladigan notify payload deb tekshiriladi; hajm yoki CRC mos bo'lmasa `WireFormatException`, sarlavha tashlanadi (xavfsiz rad). Bunday holat kuzatilmagan. |
| Payload 0xAB bilan boshlanadi | **[N]** | Kuzatilmagan. Kod: payload kutilayotganda notify sarlavha deb tahlil qilinmaydi, payload deb olinadi. |

Eslatma: "Orvell davri" trafigi spec manbasi emas (qoida). Bu faqat qanday holatlar borligini ko'rsatadi.

## CRC

**CRC-16/ARC**: poly `0x8005` (reflected `0xA001`), init `0x0000`, xorout `0x0000`, reflected kirish va chiqish. Faqat payload hisoblanadi (bayt 8 dan oxirigacha), sarlavha kirmaydi.

Tekshiruv usuli: standart CRC-16 katalogidagi 20 variant (har biri "123456789" uchun ma'lum check qiymati bilan avval tekshirildi). Faqat **CRC-16/ARC, boshlanish 8, maydon offset 4 (BE)** barcha mos frame'larda ishladi.

ARC check qiymati (`123456789`): `0xBB3D`.

### v0.2 qayta tekshiruv (2026-10-11)

| Oyna | Yo'nalish | Ma'lumot frame'lari | CRC mos | LEN mos | ACK |
|---|---|---|---|---|---|
| FitCloudPro, `conn 0x0201` | telefon → soat | 125 | 125 / 125 | 125 / 125 | 118 |
| FitCloudPro, `conn 0x0201` | soat → telefon | 118 | 118 / 118 | — | 124 |
| Qayta bog'lash, `conn 0x0202` | telefon → soat | 38 | 38 / 38 | 38 / 38 | — |
| Qayta bog'lash, `conn 0x0202` | soat → telefon | 29 | 29 / 29 | — | — |

Telefon jami 243 frame (FitCloudPro oynasi). Yetishmayotgan bitta soat ACK'i: log `12:02:55.513` da tugaydi, telefon SEQ `0x006B` tasdiqlanmagan.

## LEN

- Telefon frame'lari: `LEN = frame_jami − 8`. 243 / 243 frame.
- Soat sarlavhasi (`KIND = 0x00`): `LEN` keyingi notify'dagi payload uzunligiga teng. 118 / 118 juftlik.
- `KIND = 0x10` ACK'larda `LEN = 0`, `CRC = 0x0000`.
- Telefon eng katta payload: 67 bayt (ATT yozuvi 75 bayt).

## Yuborish tartibi

### Telefon → soat (`0x0012`, write request)

Sarlavha va payload **bitta** ATT yozuvida. Misol (log `12:02:54.754`, ajratish so'rovi):

```
ab 00 00 05 c4 55 00 6a  03 00 15 00 00
└ sarlavha (LEN=5, CRC=C455, SEQ=0x006A) ┘ └ payload ┘
```

### Soat → telefon (`0x0014`, notify)

Ikki notify: avval 8 baytli **sarlavha** (`KIND = 0x00`), keyin `LEN` baytli **payload** alohida notify'da. Payload `ab` bilan boshlanmaydi. Misol (log `12:02:55.052`, ajratish javobi):

```
ab 00 00 06 eb 05 1b 9e      ← sarlavha (LEN=6, CRC=EB05, SEQ=0x1B9E)
03 00 16 00 01 00            ← payload (keyingi notify)
```

`KIND = 0x00` va `LEN = 0` sarlavhasi loglarda uchramagan: **[N]**. Kod bunday sarlavhani rad etadi (tasdiqlanmagan holat, taxmin qilinmaydi).

### Rad etish qoidalari (kod)

| Shart | Harakat | Holat |
|---|---|---|
| MAGIC ≠ `0xAB` | rad | **[T]** (mos kelmagan, kuzatilmagan) |
| KIND ∉ {`0x00`, `0x10`} | rad | **[N]**, xavfsiz rad |
| ACK (`0x10`) da LEN ≠ 0 | rad | **[T]** (473 / 473 ACK LEN = 0) |
| LEN = 0 da CRC ≠ 0 | rad | **[T]** (ACK uchun; jadval qoidasi) |
| `KIND = 0x00` va LEN = 0 | rad | **[N]** |
| Notify hajmi ≠ LEN (payload) | rad, to'liq hajmli bo'lak bundan mustasno (yig'iladi) | **[T]** |
| Payload CRC ≠ sarlavha CRC | rad | **[T]** (118 / 118 va 29 / 29 mos) |

### Bog'lanish uzilishi

Bog'lanish uzilganda kutilayotgan sarlavha (payload kutilayotgan holat) tashlanadi. Bu ilova qoidasi (protokol emas): `FrameReader.reset()`. Log'da protokol bilan bog'liq hech narsa yo'q.

### ACK

`KIND = 0x10`, `LEN = 0`, `CRC = 0x0000`, `SEQ` = tasdiqlanayotgan frame'ning SEQ'i. Qabul qiluvchi har bir ma'lumot frame'iga ACK yuboradi.

- Telefon → soat ACK misoli (`12:02:25.969`): `ab 10 00 00 00 00 1b 9a` (soat `1B9A` seq'li frame'ni tasdiqlaydi).
- Soat → telefon ACK misoli (`12:02:25.917`): `ab 10 00 00 00 00 00 66` (telefon `0x0066` seq'li frame'ni tasdiqlaydi).

## SEQ va ACK tekshiruvi

- Telefon ma'lumot SEQ'lari `0x0001` dan boshlanadi va har frame'da 1 ga oshadi. Sessiyada uch marta `SEQ = 1` ga qaytdi: `11:44:53.214`, `11:45:17.048`, `11:46:00.536` (har gal bind so'rovi qayta yuborilganda). Soat har uchala holatda ham `SEQ = 0x0001` ni tasdiqladi.
- Soat ma'lumot SEQ'lari `0x1B29` dan boshlanadi (`11:44:53.293`) va sessiya davomida to'xtovsiz 1 ga oshadi. Telefon ACK'lari ularni aynan shu tartibda tasdiqlaydi.
- Qayta bog'lash sessiyasida (`0x0202`) soat SEQ'i `0x1B9F` dan boshlanadi (`12:27:49.053`), telefon SEQ'i `0x0001` dan.

## Qabul qilish tartibi (soat → telefon)

1. Notify `0xAB` bilan boshlanib 8 bayt bo'lsa — sarlavha. `KIND` ni tekshiring.
2. `KIND = 0x10` — ACK; `SEQ` ni tasdiqlangan deb belgilang.
3. `KIND = 0x00` va `LEN > 0` — keyingi notify'ni payload deb kuting. Uzunlik va CRC tekshirilsin.
4. Ma'lumot ma'lumotlar bazasiga yozilgandan **keyin** ACK yuboring (loyiha qoidasi). Bu spec'da telefon tomoni, kod buni `WatchManager` da hal qiladi.

## Qatlam chegarasi: kodek nimani qilmaydi

Quyidagilar kodekdan tashqarida (ilova qatlami: `WatchManager`, `BleClient`). Kodek bularni bajarmaydi va tekshirmaydi:

- **Telefon SEQ hisoblagichi.** SEQ 0x0001 dan boshlanadi, har frame'da +1. Kodek SEQ ni parametr sifatida oladi.
- **ACK yuborish siyosati.** Har ma'lumot frame'iga ACK; ACK faqat ma'lumot bazaga yozilgandan keyin (loyiha qoidasi).
- **CCCD yozish.** `0x0015` handle'ga `0100`, har bog'lash so'rovidan oldin (`messages.md`).
- **Bog'lash va qayta ulanish oqimi.** Ulanish uzilganda `FrameReader.reset()` chaqiriladi.
- **ATT yozuvi hajmi.** Sarlavha + payload ATT_MTU − 3 dan oshmasligi kerak (ochiq BLE spec, Write Request). Kuzatilgan MTU 247, demak 244 bayt. Kodek bu chegarani tekshirmaydi; ilova negotiated MTU ni biladi.

Bu qoidalar kodda yo'q, shuning uchun wire testlari ularni sinamaydi. Ular ilova testlarida tekshiriladi.

## Test vektorlari (kodga kiradi)

Quyidagi vektorlar sog'liq ma'lumoti emas. Testlar shulardan foydalanadi. Boshqa frame'lar (ma'lumot, account satri, sog'liq) testga kirmaydi.

| # | Manba (log, vaqt, handle) | Hex (sarlavha + payload) | Tekshiruv |
|---|---|---|---|
| V1 | Telefon ajratish so'rovi, `12:02:54.754`, `0x0012` | `ab 00 00 05 c4 55 00 6a` + `03 00 15 00 00` | CRC(`03 00 15 00 00`) = `C455`, LEN = 5, SEQ = `006A` |
| V2 | Telefon ACK, `12:02:25.969`, `0x0012` | `ab 10 00 00 00 00 1b 9a` | KIND = `10`, LEN = 0, SEQ = `1B9A` |
| V3 | Soat ACK, `12:02:25.917`, `0x0014` | `ab 10 00 00 00 00 00 66` | KIND = `10`, LEN = 0, SEQ = `0066` |
| V4 | Soat ajratish javobi, `12:02:55.052`, `0x0014` | `ab 00 00 06 eb 05 1b 9e` + `03 00 16 00 01 00` | CRC(`03 00 16 00 01 00`) = `EB05`, LEN = 6, SEQ = `1B9E` |
| V5 | Soat bog'lash javobi, `11:44:53.293`, `0x0014` | `ab 00 00 06 93 c5 1b 29` + `03 00 14 00 01 01` | CRC = `93C5`, LEN = 6, SEQ = `1B29` |
| V6 | Soat bog'lash javobi, qayta bog'lash, `12:27:49.053`, `0x0014` | `ab 00 00 06 db 04 1b 9f` + `03 00 12 00 01 00` | CRC = `DB04`, LEN = 6, SEQ = `1B9F` |
| V7 | CRC standart vektori | `"123456789"` (ASCII) | CRC-16/ARC = `BB3D` |

V1 va V4 da CRC log baytlaridan qayta hisoblangan (v0.2, `crc_ok = True`).

## Log yozuvlari (tasdiqlash uchun)

| Frame | Vaqt | Nima |
|---|---|---|
| Telefon ajratish so'rovi | `12:02:54.754` | `ab000005c455006a0300150000` |
| Soat ajratish javobi (sarlavha) | `12:02:55.052` | `ab000006eb051b9e` + `030016000100` |
| Telefon ACK | `12:02:55.062` | `ab10000000001b9e` |
| Telefon poll so'rovi | `12:02:25.552` | `ab000005c709006602001b0000` |
| Soat ACK | `12:02:25.917` | `ab10000000000066` |
| Soat bind javobi (sarlavha) | `11:44:53.293` | `ab00000693c51b29` + `030014000101` |


## v0.4 tekshiruvi (s4, 17:58:51–18:08)

Qayta tekshirildi: telefon 85 ma'lumot frame'i va 67 ACK; soat 67 ma'lumot frame'i va 85 ACK. Hammasi bo'lib 152 + 152 frame, ma'lumot frame'larining CRC-16/ARC mosligi 85 / 85 va 67 / 67. ACK: `KIND 0x10`, `LEN 0`, `CRC 0x0000`. Soat notify'lari: ma'lumot sarlavhasidan keyin darhol LEN bayt payload (67 / 67). Eng katta soat payload'i 122 bayt, telefon ATT yozuvi eng katta 75 bayt. Frame qoidalari o'zgarmadi.
