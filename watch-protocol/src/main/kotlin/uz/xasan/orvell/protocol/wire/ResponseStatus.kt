package uz.xasan.orvell.protocol.wire

/** Javob holati (1 bayt). 0x00 muvaffaqiyat, qolgan qiymatlar OtherCode. */
sealed class ResponseStatus {
    /** Xom bayt qiymati. */
    abstract val raw: Int

    /** 0x00: muvaffaqiyat. */
    object Success : ResponseStatus() {
        override val raw: Int = 0x00
    }

    /** 0x00 dan boshqa javob kodi. */
    data class OtherCode(override val raw: Int) : ResponseStatus()

    companion object {
        fun of(raw: Int): ResponseStatus = if (raw == 0x00) Success else OtherCode(raw)
    }
}
