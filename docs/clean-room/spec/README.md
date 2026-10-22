# FitCloud protokoli: spec (clean-room), v0.4

Bu papkadagi har bir qiymat **bizning snoop logimizdan** yoki **ochiq Bluetooth spec'idan** olingan. Gadgetbridge yoki boshqa uchinchi tomon FitCloud hujjatlari ishlatilmagan. Har qiymat yonida manba yoziladi: log fayl, vaqt (devor vaqti, mahalliy), handle yoki frame seq, misol bayt.

## Manba

| Nima | Qayerda |
|---|---|
| Asosiy log (FitCloudPro) | `final/btsnoop_hci.log` (not published) (SHA-256 `ba8f95a1…e0cb`) |
| Qayta bog'lash log | `rebind/btsnoop_hci.log` (not published) |
| GATT discovery | `bugreport-rebind.zip` → `dumpstate-2026-10-11-12-28-00.txt`, `bta_gattc_display_explore_record`, 12:27:46–47 |
| Stsenariy vaqtlari | `captures/stsenariylar.txt` |
| Ekran suratlari | `captures/screens/` (faqat vaqt va qiymat mosligini tekshirish uchun; qiymatlar repoga yozilmaydi) |
| Tahlil | `tools/snoop/parse_btsnoop.py --utc` (ATT); frame/TLV dekod skriptlari scratchpad'da, repoda emas |

## Sessiya chegaralari (v0.2 tuzatish)

Log uch xil ulanishni o'z ichiga oladi. Har biri alohida, MAC bo'yicha ajratiladi (MAC repoga yozilmaydi):

- **FitCloudPro, soat, `conn 0x0201`**: devor vaqti **11:44:52 – 12:02:56**. Shu oynada boshqa `conn` yo'q (613 ATT qator). Spec'ning asosiy manbasi.
- **Qayta bog'lash, soat, `conn 0x0202`**: devor vaqti **12:27:46 – 12:28:06**. Bind 12:27:47.
- **Boshqa qurilma, `conn 0x0201`, boshqa MAC**: rebind logning 12:12 qismi (4 qator, `0x0028` va `0x0012` da `0x55` boshli kadrlar). Bu soat emas. v0.1 da bu qism "oldingi sessiya" deb olingan edi, noto'g'ri. Spec'da ishlatilmaydi.
- Undan oldingi yozuvlar (`conn 0x0200`, 2026-10-09 – 10-11 11:36) eski Orvell trafigi, manba emas.

## Vaqt

- Btsnoop vaqti devor vaqti bilan bir xil, lekin parser uni UTC deb belgilaydi. `--utc` chiqishi aynan devor vaqti. Tekshiruv: ajratish so'rovi `12:02:54.754`, ekran suratlari `12:02:50` va `12:02:58`.
- Default chiqish (`+05:00`) ustiga 5 soat qo'shib ko'rsatadi. Tahlilda `--utc` ishlatiladi.

## ATT ulanish

- Telefon `write request` (`0x12`) bilan `0x0012` ga yozadi. Soat `notify` (`0x1B`) bilan `0x0014` dan javob beradi.
- `0x0015` — `0x0014` ning CCCD'si (tasdiqlangan GATT jadvali bilan mos).
- ATT MTU (**tasdiqlangan**, FitCloudPro oynasi): soat `Exchange MTU Request` yuboradi (`0x00F7` = 247), telefon `Response` bilan `0x0205` = 517. Samarali MTU 247. Eng katta telefon yozuvi: 75 bayt (8 bayt sarlavha + 67 bayt payload).
- Qurilma nomi (Read By Type, `0x2A00`): `GL-SW26`.
- Har sessiyada ATT handle'lari GATT discovery'ga bog'liq. Bog'lanish handle'i (`conn`) sessiyadan sessiyaga o'zgaradi.

## Holat belgilari

- **tasdiqlangan**: log baytlari bilan qayta hisoblangan (CRC, uzunlik, seq, TLV VLEN) yoki stsenariy/skrinshot bilan mos.
- **ehtimol**: tuzilishi log'da ko'rinadi, lekin ma'nosi to'liq tekshirilmagan.
- **noma'lum**: faqat shakli ma'lum yoki ziddiyat bor.

## Spec fayllari

- [`frame.md`](frame.md): frame formati, CRC-16/ARC, seq va ACK qoidalari, test vektorlari.
- [`messages.md`](messages.md): TLV formati, bog'lash/ajratish, goal, vaqt, jonli oqim va boshqa ID'lar.
- [`open-questions.md`](open-questions.md): hal bo'lmagan qismlar va keyingi capture uchun aniq qadamlar.

## Maxfiylik

Spec, kod va testlarda quyidagilar **yo'q**: qurilma MAC manzili, hisob satri (bind payload), foydalanuvchi joyi matni, sog'liq qiymatlari (puls, qon bosimi, SpO₂, qadam va boshqalar) va ularning baytlari. Testlarda faqat protokolning tuzilishi (sarlavha, CRC, ACK, bind/unbind javob kodlari) ishlatiladi.
