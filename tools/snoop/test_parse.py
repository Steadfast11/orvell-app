"""parse_btsnoop.py testlari: btsnoop fayllari shu yerning o'zida sintetik yasaladi."""

import contextlib
import io
import json
import os
import struct
import sys
import tempfile
import unittest
from datetime import datetime, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import parse_btsnoop as P  # noqa: E402

T0 = int(datetime(2026, 10, 4, 10, 0, 0, tzinfo=timezone.utc).timestamp()) * 1_000_000
MAC_A = "AA:BB:CC:DD:EE:01"
MAC_B = "11:22:33:44:55:66"
CONN_A = 0x0040
CONN_B = 0x0041

PB_START_HOST = 0b00  # host -> controller boshi
PB_CONT = 0b01
PB_START_CTRL = 0b10  # controller -> host boshi


# --- Sintetik paket yasovchilar ---------------------------------------------

def l2cap(cid: int, payload: bytes) -> bytes:
    return struct.pack("<HH", len(payload), cid) + payload


def att(op: int, *parts: bytes) -> bytes:
    return l2cap(P.L2CAP_CID_ATT, bytes([op]) + b"".join(parts))


def h16(v: int) -> bytes:
    return struct.pack("<H", v)


def acl(conn: int, pb: int, data: bytes) -> bytes:
    return struct.pack("<HH", conn | (pb << 12), len(data)) + data


def mac_le(mac: str) -> bytes:
    return bytes(reversed(bytes.fromhex(mac.replace(":", ""))))


def evt(code: int, params: bytes) -> bytes:
    return bytes([code, len(params)]) + params


def le_conn_complete(conn: int, mac: str, sub: int = 0x01) -> bytes:
    p = bytes([sub, 0x00]) + h16(conn) + bytes([0x00, 0x00]) + mac_le(mac)
    if sub == 0x0A:
        p += bytes(12)  # local RPA, peer RPA
    p += h16(24) + h16(0) + h16(400) + bytes([0])
    return evt(0x3E, p)


def disconn_complete(conn: int) -> bytes:
    return evt(0x05, bytes([0x00]) + h16(conn) + bytes([0x13]))


class Capture:
    """Paketlar ro'yxati; H4 (1002) yoki H1 (1001) btsnoop fayliga yoziladi."""

    def __init__(self):
        self.items = []  # (ts_us, rx, kind, payload, truncate_to)
        self.t = T0

    def _add(self, rx, kind, payload, dt_ms=10, truncate_to=None):
        self.t += dt_ms * 1000
        self.items.append((self.t, rx, kind, payload, truncate_to))
        return self

    def event(self, payload, **kw):
        return self._add(True, "evt", payload, **kw)

    def tx(self, conn, data, pb=PB_START_HOST, **kw):
        return self._add(False, "acl", acl(conn, pb, data), **kw)

    def rx(self, conn, data, pb=PB_START_CTRL, **kw):
        return self._add(True, "acl", acl(conn, pb, data), **kw)

    def tx_fragmented(self, conn, pdu, size, **kw):
        for i in range(0, len(pdu), size):
            self.tx(conn, pdu[i:i + size], PB_START_HOST if i == 0 else PB_CONT, **kw)
        return self

    def rx_fragmented(self, conn, pdu, size, **kw):
        for i in range(0, len(pdu), size):
            self.rx(conn, pdu[i:i + size], PB_START_CTRL if i == 0 else PB_CONT, **kw)
        return self

    def build(self, datalink=P.DATALINK_H4) -> bytes:
        out = bytearray(P.BTSNOOP_MAGIC + struct.pack(">II", 1, datalink))
        h4 = {"acl": P.H4_ACL, "evt": P.H4_EVENT, "cmd": P.H4_COMMAND}
        for ts, rx, kind, payload, trunc in self.items:
            flags = (1 if rx else 0) | (2 if kind in ("evt", "cmd") else 0)
            data = bytes([h4[kind]]) + payload if datalink == P.DATALINK_H4 else payload
            orig = len(data)
            if trunc is not None:
                data = data[:trunc]
            out += struct.pack(">IIIIq", orig, len(data), flags, 0, ts + P.BTSNOOP_EPOCH_DELTA_US)
            out += data
        return bytes(out)


def events(blob: bytes):
    stats = P.Stats()
    return list(P.extract(io.BytesIO(blob), stats)), stats


def run_cli(blob: bytes, *args):
    with tempfile.NamedTemporaryFile(suffix=".log", delete=False) as f:
        f.write(blob)
        path = f.name
    try:
        out, err = io.StringIO(), io.StringIO()
        code = P.main([path, "--utc", *args], stdout=out, stderr=err)
        return code, out.getvalue(), err.getvalue()
    finally:
        os.unlink(path)


def json_lines(text: str):
    return [json.loads(line) for line in text.splitlines() if line]


# --- Asosiy ssenariy ----------------------------------------------------------

NOTIFY_VALUE = bytes(range(0x20, 0x20 + 60))  # 3 ta ACL fragmentiga bo'linadi


def basic_capture() -> Capture:
    c = Capture()
    c.event(le_conn_complete(CONN_A, MAC_A))
    c.tx(CONN_A, att(P.ATT_WRITE_REQ, h16(0x0010), b"\x01\x02"))
    c.rx(CONN_A, att(0x13))  # Write Response: chiqmasligi kerak
    c.tx(CONN_A, att(P.ATT_WRITE_CMD, h16(0x0012), b"\xab\xcd"))
    c.rx_fragmented(CONN_A, att(P.ATT_NOTIFY, h16(0x0014), NOTIFY_VALUE), 27)
    c.rx(CONN_A, att(P.ATT_INDICATE, h16(0x0016), b"\x09"))
    c.tx(CONN_A, att(P.ATT_READ_REQ, h16(0x0003)))
    c.rx(CONN_A, att(P.ATT_READ_RSP, b"Orvell"))
    c.tx(CONN_A, l2cap(0x0005, b"\x12\x01\x00\x00"))  # LE signaling: e'tiborsiz
    return c


class ExtractTest(unittest.TestCase):

    def check_basic(self, evs):
        got = [(e.direction, e.conn, e.mac, e.handle, e.opcode, e.value) for e in evs]
        self.assertEqual(got, [
            (P.PHONE_TO_WATCH, CONN_A, MAC_A, 0x0010, P.ATT_WRITE_REQ, b"\x01\x02"),
            (P.PHONE_TO_WATCH, CONN_A, MAC_A, 0x0012, P.ATT_WRITE_CMD, b"\xab\xcd"),
            (P.WATCH_TO_PHONE, CONN_A, MAC_A, 0x0014, P.ATT_NOTIFY, NOTIFY_VALUE),
            (P.WATCH_TO_PHONE, CONN_A, MAC_A, 0x0016, P.ATT_INDICATE, b"\x09"),
            (P.WATCH_TO_PHONE, CONN_A, MAC_A, 0x0003, P.ATT_READ_RSP, b"Orvell"),
        ])

    def test_h4_datalink(self):
        evs, stats = events(basic_capture().build(P.DATALINK_H4))
        self.check_basic(evs)
        self.assertEqual(stats.warnings, {})

    def test_hci_only_datalink(self):
        evs, stats = events(basic_capture().build(P.DATALINK_HCI))
        self.check_basic(evs)
        self.assertEqual(stats.warnings, {})

    def test_fragment_timestamp_is_last_fragment(self):
        evs, _ = events(basic_capture().build())
        notify = evs[2]
        # yozuvlar: ulanish, write, rsp, cmd, 3 fragment -> oxirgi fragment 7-chi
        self.assertEqual(notify.ts_us, T0 + 7 * 10_000)

    def test_interleaved_fragments_two_connections_both_directions(self):
        a_val, b_val, w_val = bytes(range(40)), bytes(range(100, 150)), bytes(range(200, 230))
        pa = att(P.ATT_NOTIFY, h16(0x0020), a_val)
        pb = att(P.ATT_NOTIFY, h16(0x0030), b_val)
        pw = att(P.ATT_WRITE_REQ, h16(0x0022), w_val)
        c = Capture()
        c.event(le_conn_complete(CONN_A, MAC_A))
        c.event(le_conn_complete(CONN_B, MAC_B, sub=0x0A))  # Enhanced
        c.rx(CONN_A, pa[:20])
        c.rx(CONN_B, pb[:20])
        c.tx(CONN_A, pw[:10])  # teskari yo'nalish, o'sha ulanish
        c.rx(CONN_A, pa[20:], pb=PB_CONT)
        c.tx(CONN_A, pw[10:], pb=PB_CONT)
        c.rx(CONN_B, pb[20:40], pb=PB_CONT)
        c.rx(CONN_B, pb[40:], pb=PB_CONT)
        evs, stats = events(c.build())
        got = [(e.conn, e.mac, e.direction, e.handle, e.value) for e in evs]
        self.assertEqual(got, [
            (CONN_A, MAC_A, P.WATCH_TO_PHONE, 0x0020, a_val),
            (CONN_A, MAC_A, P.PHONE_TO_WATCH, 0x0022, w_val),
            (CONN_B, MAC_B, P.WATCH_TO_PHONE, 0x0030, b_val),
        ])
        self.assertEqual(stats.warnings, {})

    def test_l2cap_header_split_across_fragments(self):
        pdu = att(P.ATT_NOTIFY, h16(0x0005), b"\x11\x22\x33")
        c = Capture().rx(CONN_A, pdu[:2]).rx(CONN_A, pdu[2:], pb=PB_CONT)
        evs, _ = events(c.build())
        self.assertEqual([(e.handle, e.value) for e in evs], [(0x0005, b"\x11\x22\x33")])

    def test_read_blob_and_error_response(self):
        c = Capture()
        c.tx(CONN_A, att(P.ATT_READ_BLOB_REQ, h16(0x0040), h16(22)))
        c.rx(CONN_A, att(P.ATT_READ_BLOB_RSP, b"tail"))
        c.tx(CONN_A, att(P.ATT_READ_REQ, h16(0x0041)))
        c.rx(CONN_A, att(P.ATT_ERROR_RSP, bytes([P.ATT_READ_REQ]), h16(0x0041), b"\x02"))
        c.rx(CONN_A, att(P.ATT_READ_RSP, b"x"))  # so'rovsiz javob -> handle noma'lum
        evs, _ = events(c.build())
        self.assertEqual([(e.handle, e.opcode, e.value) for e in evs], [
            (0x0040, P.ATT_READ_BLOB_RSP, b"tail"),
            (None, P.ATT_READ_RSP, b"x"),
        ])

    def test_multiple_notification(self):
        pdu = att(P.ATT_MULTI_NOTIFY, h16(0x10), h16(2), b"\x01\x02", h16(0x11), h16(1), b"\x03")
        evs, _ = events(Capture().rx(CONN_A, pdu).build())
        self.assertEqual([(e.handle, e.value) for e in evs], [(0x10, b"\x01\x02"), (0x11, b"\x03")])

    def test_disconnect_forgets_mac_and_handle_reuse(self):
        c = Capture()
        c.event(le_conn_complete(CONN_A, MAC_A))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), b"\x01"))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), b"\x02")[:5])  # tugallanmagan PDU
        c.event(disconn_complete(CONN_A))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), b"\x03"))
        c.event(le_conn_complete(CONN_A, MAC_B))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), b"\x04"))
        evs, stats = events(c.build())
        self.assertEqual([(e.mac, e.value) for e in evs],
                         [(MAC_A, b"\x01"), (None, b"\x03"), (MAC_B, b"\x04")])
        self.assertEqual(stats.warnings, {})

    def test_truncated_record_is_skipped_with_warning(self):
        c = Capture()
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), bytes(30)), truncate_to=12)
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(2), b"\x07"))
        evs, stats = events(c.build())
        self.assertEqual([(e.handle, e.value) for e in evs], [(2, b"\x07")])
        self.assertEqual(len(stats.warnings), 1)

    def test_orphan_continuation_and_cut_file(self):
        blob = Capture().rx(CONN_A, b"\x01\x02\x03", pb=PB_CONT).build()
        evs, stats = events(blob + b"\x00\x00")
        self.assertEqual(evs, [])
        self.assertIn("continuation fragment without start", stats.warnings)
        self.assertIn("file ends inside a record header", stats.warnings)

    def test_bad_header(self):
        with self.assertRaises(P.SnoopError):
            events(b"notsnoop" + bytes(8))
        with self.assertRaises(P.SnoopError):
            events(P.BTSNOOP_MAGIC + struct.pack(">II", 1, 1003))


# --- CLI, filtrlar va chiqish -------------------------------------------------

class CliTest(unittest.TestCase):

    def two_conn_capture(self) -> bytes:
        c = Capture()
        c.event(le_conn_complete(CONN_A, MAC_A))
        c.event(le_conn_complete(CONN_B, MAC_B))
        c.tx(CONN_A, att(P.ATT_WRITE_REQ, h16(0x10), b"\x01"), dt_ms=1000)  # 10:00:01.02
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x12), b"\x02"), dt_ms=1000)  # 10:00:02.02
        c.rx(CONN_B, att(P.ATT_NOTIFY, h16(0x12), b"\x03"), dt_ms=1000)  # 10:00:03.02
        c.tx(CONN_B, att(P.ATT_WRITE_CMD, h16(0x10), b"\x04"), dt_ms=1000)  # 10:00:04.02
        return c.build()

    def values(self, *args):
        code, out, err = run_cli(self.two_conn_capture(), *args)
        self.assertEqual(code, 0, err)
        return [r["value"] for r in json_lines(out)]

    def test_json_fields(self):
        code, out, err = run_cli(basic_capture().build())
        self.assertEqual(code, 0)
        self.assertEqual(err, "")
        rows = json_lines(out)
        self.assertEqual(len(rows), 5)
        self.assertEqual(rows[0], {
            "timestamp": "2026-10-04T10:00:00.020000+00:00",
            "direction": "phone->watch",
            "conn": "0x0040",
            "mac": MAC_A,
            "handle": "0x0010",
            "opcode": "0x12",
            "op": "write_req",
            "length": 2,
            "value": "0102",
        })
        self.assertEqual(rows[2]["op"], "notify")
        self.assertEqual(rows[2]["value"], NOTIFY_VALUE.hex())

    def test_local_timestamp_has_offset(self):
        blob = basic_capture().build()
        with tempfile.NamedTemporaryFile(suffix=".log", delete=False) as f:
            f.write(blob)
        try:
            out = io.StringIO()
            P.main([f.name], stdout=out, stderr=io.StringIO())
        finally:
            os.unlink(f.name)
        ts = datetime.fromisoformat(json_lines(out.getvalue())[0]["timestamp"])
        self.assertIsNotNone(ts.tzinfo)
        self.assertEqual(int(ts.timestamp() * 1_000_000), T0 + 20_000)

    def test_filters(self):
        self.assertEqual(self.values(), ["01", "02", "03", "04"])
        self.assertEqual(self.values("--handle", "0x12"), ["02", "03"])
        self.assertEqual(self.values("--handle", "16", "--handle", "18"), ["01", "02", "03", "04"])
        self.assertEqual(self.values("--direction", "phone->watch"), ["01", "04"])
        self.assertEqual(self.values("--direction", "rx"), ["02", "03"])
        self.assertEqual(self.values("--mac", "11-22-33-44-55-66"), ["03", "04"])
        self.assertEqual(self.values("--mac", MAC_A.lower()), ["01", "02"])
        self.assertEqual(self.values("--since", "2026-10-04T10:00:02"), ["02", "03", "04"])
        self.assertEqual(self.values("--until", "2026-10-04T10:00:02.5"), ["01", "02"])
        self.assertEqual(self.values("--since", "2026-10-04T15:00:02+05:00",
                                     "--until", "2026-10-04T10:00:03.5Z"), ["02", "03"])
        self.assertEqual(self.values("--mac", MAC_B, "--direction", "in"), ["03"])

    def test_table(self):
        code, out, _ = run_cli(basic_capture().build(), "--table", "--handle", "0x10")
        lines = out.splitlines()
        self.assertEqual(code, 0)
        self.assertEqual(lines[0].split(), ["timestamp", "dir", "conn", "handle", "op", "len", "value"])
        self.assertEqual(lines[1].split(), ["2026-10-04T10:00:00.020000+00:00", "->", "0x0040",
                                            "0x0010", "write_req", "2", "01", "02"])
        self.assertEqual(len(lines), 2)

    def test_bad_file_exit_code(self):
        code, out, err = run_cli(b"garbage-garbage-garbage")
        self.assertEqual(code, 2)
        self.assertIn("bad magic", err)

    def test_warnings_go_to_stderr(self):
        blob = Capture().rx(CONN_A, att(P.ATT_NOTIFY, h16(1), bytes(30)), truncate_to=12).build()
        code, out, err = run_cli(blob)
        self.assertEqual((code, out), (0, ""))
        self.assertIn("warning: truncated ACL record", err)


# --- --frames -------------------------------------------------------------------

FRAME_ARGS = ["--frames", "--frame-start", "0xAB", "--len-offset", "2", "--len-size", "2"]


def frame(payload: bytes, kind: int = 0x00) -> bytes:
    """Sinov uchun o'ylab topilgan format: AB, tur, uzunlik(2, BE), payload."""
    return bytes([0xAB, kind]) + len(payload).to_bytes(2, "big") + payload


class FramesTest(unittest.TestCase):

    def run_frames(self, capture: Capture, *extra):
        code, out, err = run_cli(capture.build(), *FRAME_ARGS, *extra)
        self.assertEqual(code, 0, err)
        return json_lines(out)

    def test_grouping(self):
        f1, f2, f3 = frame(b"\x01\x02\x03\x04\x05"), frame(b"\x10", 1), frame(b"", 2)
        f4 = frame(bytes(range(10)), 3)
        c = Capture()
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x14), f1[:6]))
        c.tx(CONN_A, att(P.ATT_WRITE_CMD, h16(0x12), f2))  # boshqa handle: aralashmaydi
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x14), f1[6:] + f3))  # bitta qiymatda 2 kadr oxiri
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x14), f4[:3]))  # uzunlik maydoni bo'lingan
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x14), f4[3:8]))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x14), f4[8:]))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x16), b"\x55\x66"))  # start bayti yo'q
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(0x18), frame(b"abc")[:5]))  # oxirigacha tugamaydi
        rows = self.run_frames(c)
        got = [(r["handle"], r["value"], r["fragments"], r["status"]) for r in rows]
        self.assertEqual(got, [
            ("0x0012", f2.hex(), 1, "complete"),
            ("0x0014", f1.hex(), 2, "complete"),
            ("0x0014", f3.hex(), 1, "complete"),
            ("0x0014", f4.hex(), 3, "complete"),
            ("0x0016", "5566", 1, "unframed"),
            ("0x0018", frame(b"abc")[:5].hex(), 1, "incomplete"),
        ])
        # kadr vaqti birinchi fragment vaqti
        self.assertEqual(rows[1]["timestamp"], "2026-10-04T10:00:00.010000+00:00")
        self.assertEqual(rows[1]["length"], 9)

    def test_len_options(self):
        # uzunlik little-endian, butun kadrni o'z ichiga oladi, oxirida 1 bayt nazorat yig'indisi
        body = b"\xAB\x07\x00\x01\x02\x03\x04"  # uzunlik=7 (butun kadr, nazoratsiz)
        whole = body + b"\xEE"
        c = Capture()
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), whole[:4]))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), whole[4:]))
        code, out, err = run_cli(c.build(), "--frames", "--frame-start", "0xab", "--len-offset", "1",
                                 "--len-size", "2", "--len-endian", "little", "--len-base", "start",
                                 "--len-adjust", "1")
        self.assertEqual(code, 0, err)
        rows = json_lines(out)
        self.assertEqual([(r["value"], r["status"]) for r in rows], [(whole.hex(), "complete")])

    def test_timeout_and_invalid_length(self):
        f1 = frame(b"\x01\x02\x03")
        c = Capture()
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), f1[:5]))
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(1), f1), dt_ms=5000)  # pauzadan keyin yangi kadr
        c.rx(CONN_A, att(P.ATT_NOTIFY, h16(2), b"\xAB\x00\xFF\xFF"))
        rows = self.run_frames(c, "--frame-max", "1000")
        self.assertEqual([(r["handle"], r["status"]) for r in rows], [
            ("0x0001", "incomplete"), ("0x0001", "complete"), ("0x0002", "invalid_length"),
        ])

    def test_frames_table(self):
        c = Capture().rx(CONN_A, att(P.ATT_NOTIFY, h16(1), frame(b"\x01")))
        code, out, _ = run_cli(c.build(), *FRAME_ARGS, "--table")
        header, row = out.splitlines()
        self.assertIn("frags", header.split())
        self.assertEqual(row.split()[-7:], ["1", "complete", "ab", "00", "00", "01", "01"])

    def test_frames_requires_rule(self):
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(io.StringIO()):
            P.main(["x.log", "--frames"], stdout=io.StringIO(), stderr=io.StringIO())


if __name__ == "__main__":
    unittest.main()
