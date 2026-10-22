# Clean-room notes: FitCloud BLE protocol

Orvell's watch protocol (`watch-protocol/`) was **written from our own Bluetooth captures**, not from third-party
FitCloud / TopStep code or documentation. Earlier versions of the project were based on the Gadgetbridge project
(AGPL-3.0); the current codec replaced that code.

- `spec/` — the protocol notes: frame layout, CRC-16/ARC, TLV messages, bind/unbind, time sync, history records, watch-face upload.
  Every value is tagged with how well it is known: **[T]** verified against our captures, **[E]** estimate, **[N]** not verified.
- `../../tools/snoop/` — the btsnoop/HCI log parser used to produce the tables.
- The raw captures themselves (they contain Bluetooth addresses and personal health values) are **not** published.

> This is a good-faith engineering process, **not** a legal guarantee that the code is free of third-party rights.
> Notes are in Uzbek and English. Rule of thumb for reading them: `[T]` is a fact, everything else is a hypothesis.
