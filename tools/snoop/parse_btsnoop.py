#!/usr/bin/env python3
"""Android btsnoop_hci.log fayllaridan ATT trafikini ajratib oluvchi vosita.

Faqat ochiq standartlar asosida yozilgan: btsnoop fayl formati (RFC 1761
uslubidagi snoop), Bluetooth Core spec (HCI, L2CAP, ATT). Hech qanday soat
protokoli bilimi yo'q: --frames rejimidagi qoida to'liq CLI orqali beriladi.

Faqat Python 3 standart kutubxonasi.
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import BinaryIO, Iterable, Iterator

# --- btsnoop ----------------------------------------------------------------

BTSNOOP_MAGIC = b"btsnoop\x00"
# btsnoop vaqti: 0000-01-01 yarim tundan beri mikrosekund; Unix epoch bilan farq.
BTSNOOP_EPOCH_DELTA_US = 0x00DCDDB30F2F8000

DATALINK_HCI = 1001  # H1: tip bayti yo'q, tip flags'dan aniqlanadi
DATALINK_H4 = 1002  # HCI UART (H4): har paket oldida tip bayti

H4_COMMAND, H4_ACL, H4_SCO, H4_EVENT, H4_ISO = 0x01, 0x02, 0x03, 0x04, 0x05

PHONE_TO_WATCH = "phone->watch"
WATCH_TO_PHONE = "watch->phone"


class SnoopError(Exception):
    pass


@dataclass
class Record:
    ts_us: int  # Unix epoch, mikrosekund
    flags: int
    orig_len: int
    data: bytes

    @property
    def truncated(self) -> bool:
        return len(self.data) < self.orig_len

    @property
    def direction(self) -> str:
        # flags bit0: 0 = host yubordi (telefon -> soat), 1 = host qabul qildi
        return WATCH_TO_PHONE if self.flags & 1 else PHONE_TO_WATCH


def read_btsnoop(f: BinaryIO, stats: "Stats | None" = None) -> tuple[int, Iterator[Record]]:
    """Sarlavhani tekshiradi; (datalink, yozuvlar iteratori) qaytaradi."""
    header = f.read(16)
    if len(header) < 16 or header[:8] != BTSNOOP_MAGIC:
        raise SnoopError("not a btsnoop file (bad magic)")
    version, datalink = struct.unpack(">II", header[8:16])
    if version != 1:
        raise SnoopError(f"unsupported btsnoop version {version}")
    if datalink not in (DATALINK_HCI, DATALINK_H4):
        raise SnoopError(f"unsupported datalink type {datalink}")

    def records() -> Iterator[Record]:
        while True:
            rh = f.read(24)
            if not rh:
                return
            if len(rh) < 24:
                if stats:
                    stats.warn("file ends inside a record header")
                return
            orig_len, incl_len, flags, _drops, ts = struct.unpack(">IIIIq", rh)
            data = f.read(incl_len)
            if len(data) < incl_len:
                if stats:
                    stats.warn("file ends inside a record body")
                return
            yield Record(ts - BTSNOOP_EPOCH_DELTA_US, flags, orig_len, data)

    return datalink, records()


def hci_packets(datalink: int, records: Iterable[Record]) -> Iterator[tuple[Record, int, bytes]]:
    """(yozuv, H4 paket tipi, tip baytisiz paket) juftliklari."""
    for rec in records:
        if datalink == DATALINK_H4:
            if not rec.data:
                continue
            yield rec, rec.data[0], rec.data[1:]
        else:
            # H1: flags bit1 = 1 -> buyruq (yuborilgan) yoki hodisa (qabul qilingan)
            if rec.flags & 2:
                ptype = H4_EVENT if rec.flags & 1 else H4_COMMAND
            else:
                ptype = H4_ACL  # H1 da ACL va SCO ajratib bo'lmaydi; ACL deb olamiz
            yield rec, ptype, rec.data


# --- HCI / L2CAP / ATT ------------------------------------------------------

EVT_CONN_COMPLETE = 0x03
EVT_DISCONN_COMPLETE = 0x05
EVT_LE_META = 0x3E
LE_CONN_COMPLETE_SUBEVENTS = (0x01, 0x0A, 0x29)  # oddiy, Enhanced, Enhanced v2

L2CAP_CID_ATT = 0x0004

ATT_ERROR_RSP = 0x01
ATT_READ_REQ = 0x0A
ATT_READ_RSP = 0x0B
ATT_READ_BLOB_REQ = 0x0C
ATT_READ_BLOB_RSP = 0x0D
ATT_WRITE_REQ = 0x12
ATT_NOTIFY = 0x1B
ATT_INDICATE = 0x1D
ATT_MULTI_NOTIFY = 0x23
ATT_WRITE_CMD = 0x52
ATT_SIGNED_WRITE_CMD = 0xD2

ATT_OP_NAMES = {
    ATT_READ_RSP: "read_rsp",
    ATT_READ_BLOB_RSP: "read_blob_rsp",
    ATT_WRITE_REQ: "write_req",
    ATT_NOTIFY: "notify",
    ATT_INDICATE: "indicate",
    ATT_MULTI_NOTIFY: "multi_notify",
    ATT_WRITE_CMD: "write_cmd",
    ATT_SIGNED_WRITE_CMD: "signed_write_cmd",
}


def opposite(direction: str) -> str:
    return WATCH_TO_PHONE if direction == PHONE_TO_WATCH else PHONE_TO_WATCH


def format_mac(le_bytes: bytes) -> str:
    # HCI manzilni little-endian yuboradi
    return ":".join(f"{b:02X}" for b in reversed(le_bytes))


def normalize_mac(mac: str) -> str:
    hexs = mac.replace(":", "").replace("-", "").strip()
    if len(hexs) != 12:
        raise ValueError(f"bad MAC address: {mac}")
    int(hexs, 16)
    return ":".join(hexs[i:i + 2] for i in range(0, 12, 2)).upper()


@dataclass
class AttEvent:
    ts_us: int
    direction: str
    conn: int
    mac: str | None
    handle: int | None
    opcode: int
    value: bytes


@dataclass
class Stats:
    warnings: dict[str, int] = field(default_factory=dict)

    def warn(self, msg: str) -> None:
        self.warnings[msg] = self.warnings.get(msg, 0) + 1


class AttExtractor:
    """HCI paketlaridan ATT PDU'larini yig'adi (L2CAP qayta yig'ish bilan)."""

    def __init__(self, stats: Stats):
        self.stats = stats
        self.macs: dict[int, str] = {}
        # (conn, yo'nalish) -> yig'ilayotgan L2CAP bufer
        self.reasm: dict[tuple[int, str], bytearray] = {}
        # (conn, so'rovchi yo'nalishi) -> oxirgi Read/Read Blob so'rovidagi handle
        self.pending_read: dict[tuple[int, str], int] = {}

    def feed(self, rec: Record, ptype: int, pkt: bytes) -> list[AttEvent]:
        if ptype == H4_EVENT:
            self._event(pkt)
            return []
        if ptype != H4_ACL:
            return []
        if rec.truncated:
            # Qisqartirilgan (filtered) yozuv: qayta yig'ishni buzmaslik uchun tashlaymiz
            self.stats.warn("truncated ACL record skipped (snoop log in filtered mode?)")
            if len(pkt) >= 2:
                conn = struct.unpack_from("<H", pkt)[0] & 0x0FFF
                self.reasm.pop((conn, rec.direction), None)
            return []
        return self._acl(rec, pkt)

    def _event(self, pkt: bytes) -> None:
        if len(pkt) < 2:
            return
        code, plen = pkt[0], pkt[1]
        p = pkt[2:2 + plen]
        if code == EVT_LE_META and len(p) >= 12 and p[0] in LE_CONN_COMPLETE_SUBEVENTS:
            # subevent, status, handle(2), role, peer_addr_type, peer_addr(6)
            if p[1] == 0:
                conn = struct.unpack_from("<H", p, 2)[0] & 0x0FFF
                self._connected(conn, format_mac(p[6:12]))
        elif code == EVT_CONN_COMPLETE and len(p) >= 9:
            # status, handle(2), bd_addr(6), ...
            if p[0] == 0:
                conn = struct.unpack_from("<H", p, 1)[0] & 0x0FFF
                self._connected(conn, format_mac(p[3:9]))
        elif code == EVT_DISCONN_COMPLETE and len(p) >= 3:
            if p[0] == 0:
                self._forget(struct.unpack_from("<H", p, 1)[0] & 0x0FFF)

    def _connected(self, conn: int, mac: str) -> None:
        self._forget(conn)
        self.macs[conn] = mac

    def _forget(self, conn: int) -> None:
        self.macs.pop(conn, None)
        for d in (PHONE_TO_WATCH, WATCH_TO_PHONE):
            self.reasm.pop((conn, d), None)
            self.pending_read.pop((conn, d), None)

    def _acl(self, rec: Record, pkt: bytes) -> list[AttEvent]:
        if len(pkt) < 4:
            self.stats.warn("short ACL packet")
            return []
        hf, dlen = struct.unpack_from("<HH", pkt)
        conn, pb = hf & 0x0FFF, (hf >> 12) & 0x3
        data = pkt[4:4 + dlen]
        key = (conn, rec.direction)
        if pb == 0b01:  # davomi
            buf = self.reasm.get(key)
            if buf is None:
                self.stats.warn("continuation fragment without start")
                return []
            buf += data
        else:  # 00, 10, 11: yangi L2CAP PDU boshi
            if key in self.reasm:
                self.stats.warn("incomplete L2CAP PDU dropped")
            buf = self.reasm[key] = bytearray(data)
        if len(buf) < 4:
            return []
        l2len, cid = struct.unpack_from("<HH", buf)
        if len(buf) < 4 + l2len:
            return []
        if len(buf) > 4 + l2len:
            self.stats.warn("extra bytes after L2CAP PDU ignored")
        del self.reasm[key]
        if cid != L2CAP_CID_ATT:
            return []
        return self._att(rec, conn, bytes(buf[4:4 + l2len]))

    def _att(self, rec: Record, conn: int, pdu: bytes) -> list[AttEvent]:
        if not pdu:
            return []
        op, d = pdu[0], rec.direction

        def ev(handle: int | None, opcode: int, value: bytes) -> AttEvent:
            return AttEvent(rec.ts_us, d, conn, self.macs.get(conn), handle, opcode, value)

        if op in (ATT_READ_REQ, ATT_READ_BLOB_REQ) and len(pdu) >= 3:
            self.pending_read[(conn, d)] = struct.unpack_from("<H", pdu, 1)[0]
            return []
        if op == ATT_ERROR_RSP:
            self.pending_read.pop((conn, opposite(d)), None)
            return []
        if op in (ATT_READ_RSP, ATT_READ_BLOB_RSP):
            # Javobda handle yo'q: qarshi tomonning oxirgi so'rovidan olamiz
            return [ev(self.pending_read.pop((conn, opposite(d)), None), op, pdu[1:])]
        if op in (ATT_WRITE_REQ, ATT_WRITE_CMD, ATT_NOTIFY, ATT_INDICATE) and len(pdu) >= 3:
            return [ev(struct.unpack_from("<H", pdu, 1)[0], op, pdu[3:])]
        if op == ATT_SIGNED_WRITE_CMD and len(pdu) >= 15:
            return [ev(struct.unpack_from("<H", pdu, 1)[0], op, pdu[3:-12])]
        if op == ATT_MULTI_NOTIFY:
            out, i = [], 1
            while i + 4 <= len(pdu):
                h, ln = struct.unpack_from("<HH", pdu, i)
                out.append(ev(h, op, pdu[i + 4:i + 4 + ln]))
                i += 4 + ln
            return out
        return []


def extract(f: BinaryIO, stats: Stats) -> Iterator[AttEvent]:
    datalink, records = read_btsnoop(f, stats)
    ex = AttExtractor(stats)
    for rec, ptype, pkt in hci_packets(datalink, records):
        yield from ex.feed(rec, ptype, pkt)


# --- Filtrlar ---------------------------------------------------------------

@dataclass
class Filters:
    handles: set[int] | None = None
    direction: str | None = None
    since_us: int | None = None
    until_us: int | None = None
    mac: str | None = None

    def match(self, e: AttEvent) -> bool:
        if self.handles is not None and e.handle not in self.handles:
            return False
        if self.direction is not None and e.direction != self.direction:
            return False
        if self.since_us is not None and e.ts_us < self.since_us:
            return False
        if self.until_us is not None and e.ts_us > self.until_us:
            return False
        if self.mac is not None and e.mac != self.mac:
            return False
        return True


# --- Ilova kadrlariga guruhlash (--frames) ----------------------------------

@dataclass
class FrameRule:
    """Umumiy kadr qoidasi: hech qanday protokol bilimi yo'q, hammasi CLI'dan.

    Kadr `start` bayti bilan boshlanadi. Uzunlik maydoni kadr boshidan
    `len_offset` baytda, `len_size` bayt, `len_endian` tartibida. Kadrning
    to'liq uzunligi:
      len_base == "after": len_offset + len_size + qiymat + len_adjust
      len_base == "start": qiymat + len_adjust
    """
    start: int
    len_offset: int
    len_size: int = 2
    len_endian: str = "big"
    len_base: str = "after"
    len_adjust: int = 0
    max_len: int = 65536
    timeout_us: int = 2_000_000

    @property
    def header_len(self) -> int:
        return self.len_offset + self.len_size

    def total_len(self, buf: bytes) -> int | None:
        if len(buf) < self.header_len:
            return None
        n = int.from_bytes(buf[self.len_offset:self.header_len], self.len_endian)
        if self.len_base == "after":
            return self.header_len + n + self.len_adjust
        return n + self.len_adjust


@dataclass
class Frame:
    ts_us: int
    direction: str
    conn: int
    mac: str | None
    handle: int | None
    opcode: int
    value: bytes
    fragments: int
    status: str  # "complete" | "incomplete" | "unframed" | "invalid_length"


@dataclass
class _FrameState:
    buf: bytearray
    first: AttEvent
    last_ts: int
    fragments: int


class FrameGrouper:
    """Bir handle'dagi ketma-ket qiymatlarni (conn, yo'nalish, handle bo'yicha) kadrga yig'adi."""

    def __init__(self, rule: FrameRule):
        self.rule = rule
        self.state: dict[tuple, _FrameState] = {}

    def _mk(self, st: _FrameState, value: bytes, status: str) -> Frame:
        e = st.first
        return Frame(e.ts_us, e.direction, e.conn, e.mac, e.handle, e.opcode, bytes(value), st.fragments, status)

    def feed(self, e: AttEvent) -> list[Frame]:
        key = (e.conn, e.direction, e.handle)
        out: list[Frame] = []
        st = self.state.get(key)
        if st is not None and e.ts_us - st.last_ts > self.rule.timeout_us:
            out.append(self._mk(st, st.buf, "incomplete"))
            st = None
        if st is None:
            st = _FrameState(bytearray(), e, e.ts_us, 0)
            self.state[key] = st
        st.buf += e.value
        st.last_ts = e.ts_us
        st.fragments += 1
        out += self._drain(key, st, e)
        return out

    def _drain(self, key: tuple, st: _FrameState, e: AttEvent) -> list[Frame]:
        out: list[Frame] = []
        r = self.rule
        while st.buf:
            if st.buf[0] != r.start:
                out.append(self._mk(st, st.buf, "unframed"))
                st.buf.clear()
                break
            need = r.total_len(bytes(st.buf))
            if need is None:
                break
            if need < r.header_len or need > r.max_len:
                out.append(self._mk(st, st.buf, "invalid_length"))
                st.buf.clear()
                break
            if len(st.buf) < need:
                break
            out.append(self._mk(st, st.buf[:need], "complete"))
            del st.buf[:need]
            # Qoldiq joriy fragmentdan boshlangan kadr hisoblanadi
            st.first, st.fragments = e, 1
        if not st.buf:
            del self.state[key]
        return out

    def flush(self) -> list[Frame]:
        out = [self._mk(st, st.buf, "incomplete") for st in self.state.values() if st.buf]
        self.state.clear()
        return out


# --- Chiqarish --------------------------------------------------------------

def iso_time(ts_us: int, tz: timezone | None) -> str:
    dt = datetime(1970, 1, 1, tzinfo=timezone.utc) + timedelta(microseconds=ts_us)
    return dt.astimezone(tz).isoformat(timespec="microseconds")


def _hex16(v: int | None) -> str | None:
    return None if v is None else f"0x{v:04x}"


def to_dict(item: AttEvent | Frame, tz: timezone | None) -> dict:
    d = {
        "timestamp": iso_time(item.ts_us, tz),
        "direction": item.direction,
        "conn": _hex16(item.conn),
        "mac": item.mac,
        "handle": _hex16(item.handle),
        "opcode": f"0x{item.opcode:02x}",
        "op": ATT_OP_NAMES.get(item.opcode, "unknown"),
    }
    if isinstance(item, Frame):
        d["fragments"] = item.fragments
        d["status"] = item.status
    d["length"] = len(item.value)
    d["value"] = item.value.hex()
    return d


class TableWriter:
    """Odam o'qishi uchun ustunli chiqish."""

    def __init__(self, out, frames: bool):
        self.out = out
        self.cols = [
            ("timestamp", 32, lambda d: d["timestamp"]),
            ("dir", 3, lambda d: "->" if d["direction"] == PHONE_TO_WATCH else "<-"),
            ("conn", 6, lambda d: d["conn"]),
            ("handle", 6, lambda d: d["handle"] or "?"),
            ("op", 16, lambda d: d["op"]),
            ("len", 4, lambda d: str(d["length"])),
        ]
        if frames:
            self.cols += [("frags", 5, lambda d: str(d["fragments"])),
                          ("status", 14, lambda d: d["status"])]
        self._line([name for name, _, _ in self.cols], "value")

    def _line(self, cells: list[str], value: str) -> None:
        line = "  ".join(f"{c:<{w}}" for c, (_, w, _) in zip(cells, self.cols))
        self.out.write(f"{line}  {value}".rstrip() + "\n")

    def write(self, d: dict) -> None:
        v = d["value"]
        self._line([get(d) for _, _, get in self.cols], " ".join(v[i:i + 2] for i in range(0, len(v), 2)))


# --- CLI --------------------------------------------------------------------

def _int(s: str) -> int:
    return int(s, 0)


def _parse_time(s: str, tz: timezone | None) -> int:
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=tz) if tz else dt.astimezone()
    return (dt - datetime(1970, 1, 1, tzinfo=timezone.utc)) // timedelta(microseconds=1)


DIRECTION_ALIASES = {
    "phone->watch": PHONE_TO_WATCH, "tx": PHONE_TO_WATCH, "out": PHONE_TO_WATCH,
    "watch->phone": WATCH_TO_PHONE, "rx": WATCH_TO_PHONE, "in": WATCH_TO_PHONE,
}


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Extract ATT writes, notifications/indications and read responses "
                    "from an Android btsnoop_hci.log.")
    p.add_argument("log", help="path to btsnoop_hci.log ('-' for stdin)")
    p.add_argument("--table", action="store_true", help="readable table instead of JSON lines")
    p.add_argument("--utc", action="store_true", help="print timestamps in UTC instead of local time")
    g = p.add_argument_group("filters")
    g.add_argument("--handle", type=_int, action="append", metavar="H",
                   help="ATT handle (e.g. 0x000e or 14); repeatable")
    g.add_argument("--direction", choices=sorted(DIRECTION_ALIASES), metavar="DIR",
                   help="phone->watch (tx/out) or watch->phone (rx/in)")
    g.add_argument("--since", metavar="ISO", help="only at/after this time (naive = local, or UTC with --utc)")
    g.add_argument("--until", metavar="ISO", help="only at/before this time")
    g.add_argument("--mac", help="only connections to this peer address (from HCI connection events)")
    f = p.add_argument_group("application frames (--frames)")
    f.add_argument("--frames", action="store_true",
                   help="group consecutive values per (connection, direction, handle) into frames")
    f.add_argument("--frame-start", type=_int, metavar="BYTE", help="frame start byte (required with --frames)")
    f.add_argument("--len-offset", type=_int, metavar="N",
                   help="offset of the length field from frame start (required with --frames)")
    f.add_argument("--len-size", type=int, choices=(1, 2, 3, 4), default=2, help="length field size in bytes (default 2)")
    f.add_argument("--len-endian", choices=("big", "little"), default="big", help="length field byte order (default big)")
    f.add_argument("--len-base", choices=("after", "start"), default="after",
                   help="'after': length counts bytes after the length field; "
                        "'start': length is the whole frame (default after)")
    f.add_argument("--len-adjust", type=int, default=0, metavar="N",
                   help="bytes added to the computed frame length, e.g. a trailing checksum (default 0)")
    f.add_argument("--frame-max", type=int, default=65536, metavar="N", help="larger lengths are reported as invalid (default 65536)")
    f.add_argument("--frame-timeout", type=float, default=2.0, metavar="SEC",
                   help="seconds of silence after which an unfinished frame is flushed (default 2)")
    return p


def main(argv: list[str] | None = None, stdout=None, stderr=None) -> int:
    stdout = stdout or sys.stdout
    stderr = stderr or sys.stderr
    p = build_parser()
    a = p.parse_args(argv)
    tz = timezone.utc if a.utc else None

    try:
        filters = Filters(
            handles=set(a.handle) if a.handle else None,
            direction=DIRECTION_ALIASES[a.direction] if a.direction else None,
            since_us=_parse_time(a.since, tz) if a.since else None,
            until_us=_parse_time(a.until, tz) if a.until else None,
            mac=normalize_mac(a.mac) if a.mac else None,
        )
    except ValueError as e:
        p.error(str(e))

    grouper = None
    if a.frames:
        if a.frame_start is None or a.len_offset is None:
            p.error("--frames needs --frame-start and --len-offset")
        if not 0 <= a.frame_start <= 0xFF or a.len_offset < 1:
            p.error("--frame-start must be a byte and --len-offset >= 1")
        grouper = FrameGrouper(FrameRule(
            start=a.frame_start, len_offset=a.len_offset, len_size=a.len_size,
            len_endian=a.len_endian, len_base=a.len_base, len_adjust=a.len_adjust,
            max_len=a.frame_max, timeout_us=int(a.frame_timeout * 1_000_000)))

    table = TableWriter(stdout, a.frames) if a.table else None

    def emit(item) -> None:
        d = to_dict(item, tz)
        if table:
            table.write(d)
        else:
            stdout.write(json.dumps(d) + "\n")

    stats = Stats()
    try:
        f = sys.stdin.buffer if a.log == "-" else open(a.log, "rb")
    except OSError as e:
        p.error(str(e))
    try:
        for e in extract(f, stats):
            if not filters.match(e):
                continue
            if grouper:
                for fr in grouper.feed(e):
                    emit(fr)
            else:
                emit(e)
        if grouper:
            for fr in grouper.flush():
                emit(fr)
    except SnoopError as e:
        stderr.write(f"error: {e}\n")
        return 2
    finally:
        if f is not sys.stdin.buffer:
            f.close()

    for msg, n in stats.warnings.items():
        stderr.write(f"warning: {msg} (x{n})\n")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except BrokenPipeError:  # masalan `| head`
        sys.exit(0)
