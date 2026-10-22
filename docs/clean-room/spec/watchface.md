# Siferblat (watch face) uzatish va TBUI formati (v0.1, 2026-10-13)

Manba: o'z snoop logimiz (`captures/watchface-2026-10-13/`, repoga kirmaydi). FitCloudPro'da 4 amal:
siferblat almashtirish (15:20), to'liq qizil rasm (15:24), to'rt rangli rasm (15:38), do'kondan siferblat (15:45).
Sinov rasmlari Orvell tomonidan yasalgan (`orvell_test_red.png`, `orvell_test_quad.png`, 480 × 480). Uchinchi tomon kodi yoki hujjati ishlatilmagan.

Belgilar: **[T]** tasdiqlangan (barcha kuzatuvlarda), **[E]** ehtimol, **[N]** noma'lum.

## 1. Fayl uzatish xizmati (alohida GATT, asosiy `0x0012/0x0014` kanaldan tashqari)

| Handle | Vazifa | Holat |
|---|---|---|
| `0x002F` | ma'lumot, telefon → soat, Write Command (javobsiz) | **[T]** |
| `0x0031` | boshqaruv, telefon → soat Write Request; soat → telefon Notify | **[T]** |
| `0x0032` | `0x0031` uchun CCCD (`01 00`) | **[T]** |

UUID'lar hali yozilmagan (logda discovery yo'q): **[N]**. Handle'lar shu soatda barqaror.

Uzatishdan oldin telefon soatdan bir nechta xususiyatni o'qiydi (`0x0022`, `0x001C`, `0x0024`, `0x002A`, `0x0026`, `0x002C`, `0x0028`).
`0x0024` va `0x002A` ichida `0x2790`–`0x279D` ID'lar ro'yxati (LE) bor; uzatiladigan obyekt ID `0x27A1`: **[E]** (sig'imlar jadvali).

### Boshqaruv ketma-ketligi (4 / 4 uzatishda bir xil) **[T]**

| # | Telefon → `0x0031` | Soat → notify | Ma'nosi |
|---|---|---|---|
| 1 | `09` | `10 09 01 00 08 00 00` | ma'lumot; `00 08` = 2048 (blok hajmi) **[E]** |
| 2 | `06 A1 27` | `10 06 01` + 10 bayt nol | obyekt `0x27A1` tanlash |
| 3 | `01` + 12 × `FF` + 4 × `00` | `10 01 01` | yaratish |
| 4 | `02 A1 27 00 00 00 00` | (javob yo'q, darhol ma'lumot) | boshlash, offset 0 |
| 5 | `0x002F`: 8 × 240 + 1 × 128 bayt = 2048 | — | blok |
| 6 | `0A <uzunlik u16 LE> <tekshiruv 2 bayt>` | `10 0A 01 <yangi offset u32 LE>` | blok tasdig'i |
| 7 | 5–6 takror, oxirgi blok qisqa (masalan `0A D0 01 …` = 464 bayt) | oxirgi offset = fayl hajmi | |
| 8 | `03 A1 27 01` | `10 03 01` | tekshirish |
| 9 | `04 00` | (javob logda yo'q) | faollashtirish **[E]** |

Javob shakli: `10 <buyruq> <holat>`; holat `01` = muvaffaqiyat **[T]**.

**Blok tekshiruvi** **[T]** (3 uzatish, 1282 / 1282 blok): blokdagi 16 bitli **LE** so'zlar XOR'i, xabarda **BE** tartibda —
avval toq o'rinli baytlar XOR'i, keyin juft o'rinlilarniki (`F7 38` ↔ LE so'zlar XOR'i = `0xF738`). To'liq `FF` blok → `00 00`.

Tezlik: 1 038 800 bayt ≈ 91 s (~11 KB/s).

## 2. Uzatiladigan fayl

`[1024 bayt uzatish sarlavhasi][TBUI tanasi]`. Fayl hajmi = 1024 + tana.

### 2.1 Uzatish sarlavhasi (1024 bayt)

| Offset | Hajm | Qiymat | Holat |
|---|---|---|---|
| 0 | 2 | `05 00` | **[T]** (3 / 3), ma'nosi **[N]** |
| 2 | 2 | `70 19` | **[T]** (3 / 3), **[N]** |
| 4 | 2 | `A1 27` — obyekt ID (`0x27A1`, LE) | **[T]** |
| 6 | 2 | tanaga bog'liq tekshiruv | **[N]**: CRC-16 katalogi (22 variant), CRC32 qismlari, XOR16, bayt yig'indisi — tana, `8..`, `12..`, nollangan sarlavha + tana oraliqlarida mos emas |
| 8 | 4 | tana hajmi, u32 LE | **[T]** |
| 12 | 16 | doimiy 16 bayt (uch faylda bir xil, shu jumladan boshqa siferblat) | **[T]** doimiy; ma'nosi **[N]** |
| 28 | 996 | `FF` | **[T]** |

### 2.2 TBUI tanasi (barcha qiymatlar LE)

| Offset | Hajm | Qiymat |
|---|---|---|
| 0 | 4 | `"TBUI"` |
| 4 | 4 | versiya `1` |
| 8 | 4 | **[N]** (`0x0101C520`) |
| 12 | 4 | tana hajmi − 4 |
| 16 | 4 | `0x40` — bo'limlar jadvali offseti **[E]** |
| 20 | 4 | `10` **[N]** |
| 28 | 4 | **[N]** (`0x00160000`) |
| 32 | 2 + 2 | ekran kengligi, balandligi: `466 × 466` **[T]** |
| 36 | 2 | `0x3002` **[N]** |

Bo'limlar (`0x40` dan): `[offset u32][soni u16][yozuv hajmi u16]`:

| Offset | Soni × hajm | Mazmun |
|---|---|---|
| `0x00A0` | 21 × 32 | elementlar |
| `0x0D20` | 182 × 12 | rasm resurslari |
| `0x1920` | 10 × 96 | **[E]** belgilar xaritasi (`'0'..'9'` → 0..9) |

**Resurs yozuvi** (12 bayt) **[T]**: `[ID u16][format u16][kenglik u16][balandlik u16][tana offseti u32]`.
Formatlar: `0x0001` — RGB565 BE, siqilmagan, `kenglik × balandlik × 2` bayt **[T]** (fon); `0x0308`, `0x0309`, `0x0008` — **[N]** (raqamlar va bezaklar).

**Fon** **[T]**: 0-element → resurs `0xB4`, format `0x0001`, 466 × 466. Telefon rasmni 466 ga o'lchab, doira niqobi bilan kesadi (ekran yumaloq).
Qizil va to'rt rangli fayllar faqat fon piksellarida va sarlavha tekshiruvida farq qiladi.

**Element yozuvi** (32 bayt): `[tartib][tur][?][?]` + koordinatalar va resurs ID'lari. Tur `00` — fon; `02`, `04` — raqam guruhlari **[E]**; `FF` — **[N]**. To'liq ma'nosi **[N]**.

### 2.3 Palitrali rasm, format `0x0308` **[T]** (49 / 49 resurs qoldiqsiz ochildi, rasmlar ko'zda tekshirildi)

`[qator offsetlari: (h + 1) × u32 LE, mutlaq tana offseti; oxirgisi = rasm oxiri][palitra soni u32][palitra: soni × RGB565 BE][qatorlar]`.
Qator: 16 bitli LE sarlavhali token oqimi — `0xF000 | n` → n piksel, qiymati keyingi bayt; aks holda `n` → keyingi n bayt indeks.
Qator oxiri 4 baytga to'ldiriladi. Indeks 0 — shaffof (fon ko'rinadi) **[E]**.
`0x0309` — boshqa kodlash (shu dekoder bilan buziladi) **[N]**; `0x0008` (312 × 312, element turi `06`) **[N]**.

### 2.4 Shriftlar (`0x1920`, yozuv 384 bayt = 96 × u32) **[T]**

`[soni u32][(belgi u16, resurs u16) × soni]`. Rasmli siferblatda 10 shrift: 5 uslub × (kichik raqamlar `0–9`, katta raqamlar `0–9` va `:`).

### 2.5 Element turlari

| Tur | b3 | Ma'nosi | Holat |
|---|---|---|---|
| `00` | — | rasm (fon), u16[4] = resurs | **[T]** |
| `06` | — | soat ro'yxatidagi kichik rasm; sarlavha `0x60` da ham shu resurs ID | **[E]** |
| `09` | — | vaqt bloki konteyneri: x, y, w, h; u16[8..12] = 5 uslub rasmi | **[E]** |
| `02` | `00` | vaqt `HH:MM`, u16[4] = shrift, u16[7] = uslub (0..4), koordinatalar konteynerga nisbatan | **[E]** |
| `02` | `01` | sana (kichik raqamlar) | **[E]** |
| `04` | — | hafta kuni: u16[4] = birinchi rasm, 14 rasm (har kun 2 til joyi) | **[T]** rasmlar ko'zda tekshirildi |
| `01` | `00/01/02` | soat / daqiqa / soniya mili (boshqa siferblatda) | **[E]** |
| `FF` | — | vaqt blokining boshqa joylashuv variantlari (sarlavha `0x60..` dagi ro'yxat bilan bir xil) | **[E]** |

### 2.6 O'z siferblatimiz — to'xtatildi (2026-10-13)

Orvell'da yasalgan TBUI fayllar soatga yetib boradi va qabul qilinadi (`10 03 01`), lekin soat ularni o'rnatmaydi:
uzatish sarlavhasidagi tekshiruv maydoni (offset 6) o'rnatishda tekshiriladi. FitCloudPro'ning asl fayllari Orvell orqali
o'rnatildi (ko'zda tasdiqlandi), piksellari o'zgartirilgani o'rnatilmadi. Bu maydon ishlab chiqaruvchining fayl
yaxlitligi himoyasi: uni aylanib o'tish qilinmaydi. Siferblat yasash va ekrani Orvell'dan olib tashlandi;
uzatish protokoli (`FaceUpload`, `FaceMessages`) qoldi. Rasmiy yo'l: ishlab chiqaruvchi SDK / hamkorlik.

## 4. Asosiy kanal xabarlari (uzatish atrofida)

| Qachon | Telefon → soat | Soat → telefon | Holat |
|---|---|---|---|
| uzatishdan oldin | `01/0001` (bo'sh) | `01/0002` = `00 00` | **[T]** (2 / 2); ma'nosi **[E]** "uzatishga tayyorlan" |
| har ulanishda | `02/0052` (bo'sh) | `02/0053`: siferblatlar ro'yxati; joriy siferblat ID u16 BE offset 4 | **[T]** (5 / 5): `BD8F` → almashtirishdan keyin `BD56` (= TBUI `0x08` quyi 16 bit) → rasmli siferblatdan keyin `C520` |
| joriy = `C520` | `02/0070` = `05 01 00` | — | **[T]** (2 / 2, faqat rasmli siferblat joriy bo'lganda); ma'nosi **[N]** (uslub/joylashuv?) |

Orvell (2026-10-13): uzatishdan oldin `01/0001` (javob 3 s kutiladi), har ulanishda `02/0052` va `C520` bo'lsa `02/0070`.
Sabab: Orvell yuborgan siferblat soatda 100% yuklanib, ekranda ko'rinmagan (foydalanuvchi kuzatuvi) — shu xabarlar yo'q edi.

## 3. Ochiq savollar

1. Sarlavha tekshiruvi (offset 6). Soat uni tekshiradimi: Orvell'dan uzatib, `10 03` holatini ko'rish kerak.
2. Resurs formatlari `0x0308`/`0x0309`/`0x0008` (alfa va siqish).
3. Element turlari (soat, daqiqa, sana) — o'z siferblatimizni yasash uchun.
4. Asosiy kanaldagi `02/0043` → `02/0044` (6 bayt) — uzatishdan oldin so'raladi: **[E]** siferblat holati.
