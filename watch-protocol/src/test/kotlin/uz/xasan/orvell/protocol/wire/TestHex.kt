package uz.xasan.orvell.protocol.wire

/** Test uchun: hex matnni baytlarga aylantiradi. Bo'shliqlar e'tiborga olinmaydi. */
internal fun hex(text: String): ByteArray =
    text.filterNot { it.isWhitespace() }
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
