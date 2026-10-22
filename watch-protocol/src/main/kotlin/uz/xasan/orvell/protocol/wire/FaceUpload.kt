package uz.xasan.orvell.protocol.wire

/**
 * Siferblat faylini soatga uzatish (spec watchface.md §1). Sof mantiq: Android'siz, JVM testida tekshiriladi.
 * Boshqaruv xabarlari fayl xizmatining boshqaruv kanaliga yoziladi, ma'lumot bo'laklari javobsiz yoziladi.
 *
 *  09 → 10 09 01 · 06 A1 27 → 10 06 01 · 01 FF×12 00×4 → 10 01 01
 *  02 A1 27 <offset u32 LE>, keyin har blok: bo'laklar + 0A <uzunlik u16 LE> <XOR 2 bayt> → 10 0A 01 <offset u32 LE>
 *  oxirgi blokdan keyin 03 A1 27 01 → 10 03 01, so'ng 04 00
 */
class FaceUpload(private val file: ByteArray, private val chunk: Int = CHUNK) {
    enum class Phase { IDLE, INFO, SELECT, CREATE, BLOCK, VALIDATE, DONE, FAILED }

    sealed class Next {
        object None : Next()
        /** Boshqaruv kanaliga yoziladi (javob kutiladi). */
        class Control(val bytes: ByteArray) : Next()
        /** Avval boshqaruv xabari (bo'lsa), keyin ma'lumot bo'laklari, so'ng yakunlovchi boshqaruv xabari. */
        class Block(val before: ByteArray?, val chunks: List<ByteArray>, val after: ByteArray) : Next()
        /** Uzatish tugadi: faollashtirish xabari yuboriladi. */
        class Done(val bytes: ByteArray) : Next()
        class Fail(val reason: String) : Next()
    }

    var phase = Phase.IDLE
        private set

    /** Soat tasdiqlagan bayt soni. */
    var acked = 0
        private set

    val size: Int get() = file.size

    init {
        require(file.isNotEmpty()) { "Empty face file" }
        require(chunk in 1..BLOCK) { "Bad chunk size: $chunk" }
    }

    fun start(): Next {
        if (phase != Phase.IDLE) return Next.None
        phase = Phase.INFO
        return Next.Control(byteArrayOf(OP_INFO.toByte()))
    }

    /** Boshqaruv kanalidan kelgan bildirishnoma. */
    fun onControl(value: ByteArray): Next {
        if (value.size < 3 || value[0] != RESPONSE) return Next.None
        val op = value[1].toInt() and 0xFF
        val ok = value[2].toInt() == 1
        return when {
            phase == Phase.INFO && op == OP_INFO -> if (ok) { phase = Phase.SELECT; Next.Control(select()) } else fail("info", value)
            phase == Phase.SELECT && op == OP_SELECT -> if (ok) { phase = Phase.CREATE; Next.Control(create()) } else fail("select", value)
            phase == Phase.CREATE && op == OP_CREATE -> if (ok) { phase = Phase.BLOCK; block(0, withStart = true) } else fail("create", value)
            phase == Phase.BLOCK && op == OP_BLOCK -> onBlockAck(ok, value)
            phase == Phase.VALIDATE && op == OP_VALIDATE ->
                if (ok) { phase = Phase.DONE; Next.Done(byteArrayOf(OP_ACTIVATE.toByte(), 0)) } else fail("validate", value)
            // Takroriy javob (kuzatilgan: 10 01 01 ikki marta) yoki boshqa faza: e'tiborsiz
            else -> Next.None
        }
    }

    fun onTimeout(): Next = if (phase in ACTIVE) fail("timeout", null) else Next.None

    private fun onBlockAck(ok: Boolean, value: ByteArray): Next {
        if (!ok || value.size < 7) return fail("block", value)
        val offset = u32le(value, 3)
        val expected = minOf(acked + BLOCK, file.size)
        if (offset != expected) return fail("offset $offset != $expected", value)
        acked = offset
        if (acked == file.size) {
            phase = Phase.VALIDATE
            return Next.Control(byteArrayOf(OP_VALIDATE.toByte(), OBJECT_LO, OBJECT_HI, 1))
        }
        return block(acked, withStart = false)
    }

    private fun block(offset: Int, withStart: Boolean): Next {
        val end = minOf(offset + BLOCK, file.size)
        val data = file.copyOfRange(offset, end)
        val chunks = (data.indices step chunk).map { data.copyOfRange(it, minOf(it + chunk, data.size)) }
        val sum = blockChecksum(data)
        val after = byteArrayOf(OP_BLOCK.toByte(), (data.size and 0xFF).toByte(), (data.size ushr 8).toByte(), sum[0], sum[1])
        return Next.Block(if (withStart) start(offset) else null, chunks, after)
    }

    private fun fail(what: String, value: ByteArray?): Next {
        phase = Phase.FAILED
        return Next.Fail(what + (value?.let { " " + it.joinToString("") { b -> "%02X".format(b) } } ?: ""))
    }

    companion object {
        /** Blok hajmi (soat 0x09 javobida e'lon qiladi: 0x0800). */
        const val BLOCK = 2048
        /** Kuzatilgan bo'lak hajmi: 8 × 240 + 128 = 2048. */
        const val CHUNK = 240

        private const val RESPONSE: Byte = 0x10
        private const val OP_CREATE = 0x01
        private const val OP_START = 0x02
        private const val OP_VALIDATE = 0x03
        private const val OP_ACTIVATE = 0x04
        private const val OP_SELECT = 0x06
        private const val OP_INFO = 0x09
        private const val OP_BLOCK = 0x0A

        /** Siferblat obyekti ID 0x27A1 (LE). */
        private const val OBJECT_LO: Byte = 0xA1.toByte()
        private const val OBJECT_HI: Byte = 0x27

        private val ACTIVE = setOf(Phase.INFO, Phase.SELECT, Phase.CREATE, Phase.BLOCK, Phase.VALIDATE)

        fun select() = byteArrayOf(OP_SELECT.toByte(), OBJECT_LO, OBJECT_HI)

        fun create() = ByteArray(17).also { it[0] = OP_CREATE.toByte(); for (i in 1..12) it[i] = 0xFF.toByte() }

        fun start(offset: Int) = byteArrayOf(
            OP_START.toByte(), OBJECT_LO, OBJECT_HI,
            offset.toByte(), (offset ushr 8).toByte(), (offset ushr 16).toByte(), (offset ushr 24).toByte(),
        )

        /** Blok tekshiruvi: 16 bitli LE so'zlar XOR'i, BE tartibda (toq o'rinli baytlar XOR'i, keyin juftlarniki). */
        fun blockChecksum(block: ByteArray): ByteArray {
            var even = 0
            var odd = 0
            for (i in block.indices) if (i % 2 == 0) even = even xor (block[i].toInt() and 0xFF) else odd = odd xor (block[i].toInt() and 0xFF)
            return byteArrayOf(odd.toByte(), even.toByte())
        }

        private fun u32le(b: ByteArray, at: Int) =
            (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or
                ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)
    }
}
