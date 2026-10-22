# snoop — Bluetooth traffic analysis

`parse_btsnoop.py` reads an Android `btsnoop_hci.log` and prints the ATT traffic between the phone and the watch: writes (request/command), notifications/indications and read responses. Python 3.11+, standard library only.

Written only from public specs: the btsnoop file format and the Bluetooth Core spec (HCI, L2CAP, ATT). It contains no watch protocol knowledge.

## 1. Capture a log

1. Enable Developer options: **Settings → About phone**, tap **Build number** 7 times.
2. **Settings → System → Developer options → Enable Bluetooth HCI snoop log** → **Enabled** (not "Filtered": filtered mode cuts packet payloads; the tool warns about this).
3. Turn Bluetooth off and on (or reboot) so logging starts.
4. Reproduce what you want to capture (open the vendor app, sync, etc.). Note the time.

## 2. Get the log

```bash
adb bugreport bugreport.zip
unzip -l bugreport.zip | grep -i btsnoop
```

The log is usually at `FS/data/misc/bluetooth/logs/btsnoop_hci.log` inside the zip (some devices use `FS/data/log/bt/`). Extract it:

```bash
unzip -j bugreport.zip 'FS/data/misc/bluetooth/logs/btsnoop_hci.log' -d .
```

A log can contain personal data (device addresses, health values). Do not commit it.

## 3. Run

```bash
python3 tools/snoop/parse_btsnoop.py btsnoop_hci.log            # JSON lines
python3 tools/snoop/parse_btsnoop.py btsnoop_hci.log --table    # readable table
```

Each JSON line has `timestamp` (local ISO 8601, or UTC with `--utc`), `direction` (`phone->watch` / `watch->phone`), `conn` (HCI connection handle), `mac` (peer address from the HCI connection event, if seen), `handle` (ATT handle), `opcode`, `op`, `length`, `value` (hex). For read responses the handle comes from the matching read request (`null` if it was not captured).

Filters (combine freely):

```bash
--handle 0x000e            # repeatable
--direction phone->watch   # or watch->phone; aliases tx/out, rx/in
--since 2026-10-04T14:30 --until 2026-10-04T14:35   # naive = local time
--mac AA:BB:CC:DD:EE:FF    # needs the connection event in the log
```

### Application frames

`--frames` joins consecutive values on one (connection, direction, handle) into application frames. You describe the frame layout; nothing is built in:

```bash
python3 tools/snoop/parse_btsnoop.py btsnoop_hci.log --frames \
    --frame-start 0x7E --len-offset 1 --len-size 2 --len-endian big \
    --len-base after --len-adjust 0 --table
```

- `--frame-start`: first byte of every frame
- `--len-offset`, `--len-size`, `--len-endian`: where the length field is
- `--len-base after`: length counts bytes after the length field; `start`: length is the whole frame
- `--len-adjust`: extra bytes not counted by the length field (e.g. a trailing checksum)
- `--frame-timeout` (s), `--frame-max`: when to give up on an unfinished or bogus frame

Each frame has `fragments` and `status`: `complete`, `incomplete`, `unframed` (data not starting with the start byte) or `invalid_length`.

## Tests

```bash
python3 -m unittest discover -s tools/snoop -v
```

Tests build synthetic btsnoop files (fragmented L2CAP, both directions, both datalink types) in code; no real capture is needed.

## Limits

- Datalinks 1001 (HCI, no packet type byte) and 1002 (H4, Android default).
- Only the fixed ATT channel (CID 0x0004); Enhanced ATT over dynamic L2CAP channels is not decoded.
- `--mac` matches the address in the connection event; with a private (random) address that may differ from the watch's public address.
