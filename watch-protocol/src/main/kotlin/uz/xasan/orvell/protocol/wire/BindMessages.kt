package uz.xasan.orvell.protocol.wire

/**
 * Bog'lash va ajratish xabarlari (TYPE 0x03).
 * Ilova bog'lashni 0x0011 bilan qiladi (spec v0.4.3, qurilmada tasdiqlangan; javob 00 = muvaffaqiyat [T]).
 * 0x0013/0x0014 tasdiqlash so'rovi spec v0.4.1 bo'yicha, faqat sintetik testlar bilan saqlangan: ilova uni ishlatmaydi.
 * Javob kodi 0x01 bu yerda talqin qilinmaydi: ilova qatlami uni [E] deb ishlatadi.
 */
object BindMessages {
    const val TYPE = 0x03
    const val ID_BIND_REQUEST = 0x0011
    const val ID_BIND_RESPONSE = 0x0012
    const val ID_CONFIRM_REQUEST = 0x0013
    const val ID_CONFIRM_RESPONSE = 0x0014
    const val ID_UNBIND_REQUEST = 0x0015
    const val ID_UNBIND_RESPONSE = 0x0016
    const val ACCOUNT_FIELD_SIZE = 32

    /**
     * Bog'lash so'rovi TLV payload'i (5 + 32 bayt).
     * Hisob satri faqat ASCII, 32 baytgacha; qolgani nol bilan to'ldiriladi.
     * Satr parametr sifatida keladi, repoda qattiq yozilmaydi.
     */
    fun bindRequest(account: String): ByteArray = accountRequest(ID_BIND_REQUEST, account)

    /** Tasdiqlash so'rovi (0x0013): bog'lash so'rovi bilan bir xil 32 baytli hisob maydoni. */
    fun confirmRequest(account: String): ByteArray = accountRequest(ID_CONFIRM_REQUEST, account)

    /** Ajratish so'rovi: qiymatsiz TLV (03 00 15 00 00). */
    fun unbindRequest(): ByteArray = TlvCodec.encode(TYPE, ID_UNBIND_REQUEST, ByteArray(0))

    /** Bog'lash javobini (ID 0x0012, qiymat 1 bayt) o'qiydi. */
    fun decodeBindResponse(tlv: Tlv): ResponseStatus = decodeStatus(tlv, ID_BIND_RESPONSE)

    /** Tasdiqlash javobini (ID 0x0014, qiymat 1 bayt) o'qiydi. */
    fun decodeConfirmResponse(tlv: Tlv): ResponseStatus = decodeStatus(tlv, ID_CONFIRM_RESPONSE)

    /** Ajratish javobini (ID 0x0016, qiymat 1 bayt) o'qiydi. */
    fun decodeUnbindResponse(tlv: Tlv): ResponseStatus = decodeStatus(tlv, ID_UNBIND_RESPONSE)

    /** Hisob maydonli so'rov (0x0011 yoki 0x0013): ASCII tekshiruvi, 32 bayt, nol bilan to'ldirish. */
    private fun accountRequest(id: Int, account: String): ByteArray {
        require(account.all { it.code < 0x80 }) { "Account must be ASCII only" }
        require(account.length <= ACCOUNT_FIELD_SIZE) { "Account must be at most $ACCOUNT_FIELD_SIZE bytes" }
        val field = ByteArray(ACCOUNT_FIELD_SIZE)
        account.toByteArray(Charsets.US_ASCII).copyInto(field)
        return TlvCodec.encode(TYPE, id, field)
    }

    private fun decodeStatus(tlv: Tlv, expectedId: Int): ResponseStatus {
        if (tlv.type != TYPE || tlv.id != expectedId || tlv.value.size != 1) {
            throw WireFormatException(
                "Unexpected bind response: type=${tlv.type}, id=${tlv.id}, vlen=${tlv.value.size}",
            )
        }
        return ResponseStatus.of(tlv.value[0].toInt() and 0xFF)
    }
}
