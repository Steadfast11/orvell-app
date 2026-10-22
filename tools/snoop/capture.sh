#!/usr/bin/env bash
# Telefondagi Bluetooth HCI snoop logini yoqadi va olib keladi (adb orqali, root kerak emas).
# Ishlatish:
#   tools/snoop/capture.sh start     # logni yoqadi va Bluetooth'ni qayta ishga tushiradi
#   tools/snoop/capture.sh pull OUT  # bugreport orqali btsnoop_hci.log ni OUT papkaga chiqaradi
set -euo pipefail
cmd="${1:-}"
case "$cmd" in
  start)
    adb shell settings put secure bluetooth_hci_log 1
    adb shell settings put global bluetooth_hci_log 1 2>/dev/null || true
    adb shell svc bluetooth disable; sleep 3; adb shell svc bluetooth enable; sleep 4
    echo "HCI snoop yoqildi (qiymat: $(adb shell settings get secure bluetooth_hci_log))."
    echo "Endi kerakli amalni bajaring; tugagach: $0 pull <papka>"
    ;;
  pull)
    out="${2:?papka nomi kerak}"; mkdir -p "$out"
    tmp="$(mktemp -d)"
    # bugreport shaxsiy ma'lumot saqlaydi: vaqtincha papka chiqishda o'chiriladi, faqat btsnoop log qoladi
    trap 'rm -rf "$tmp"' EXIT
    adb bugreport "$tmp/bugreport.zip" >/dev/null
    unzip -o -q "$tmp/bugreport.zip" -d "$tmp/br" || true
    f="$(find "$tmp/br" -name 'btsnoop_hci*' | head -n 1)"
    if [ -z "$f" ]; then echo "btsnoop topilmadi: bugreport ichida yo'q (log yoqilmagan yoki Bluetooth qayta ishga tushmagan)"; exit 1; fi
    cp "$f" "$out/btsnoop_hci.log"; echo "Saqlandi: $out/btsnoop_hci.log ($(wc -c < "$out/btsnoop_hci.log") bayt)"
    ;;
  *) echo "ishlatish: $0 start | pull <papka>"; exit 2 ;;
esac
