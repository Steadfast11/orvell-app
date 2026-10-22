package uz.xasan.orvell.watch

import uz.xasan.orvell.R
import uz.xasan.orvell.protocol.wire.HistoryMessages
import uz.xasan.orvell.protocol.wire.MAX_REASSEMBLED_PAYLOAD

/**
 * Bitta hisoblagich yozuvining eng katta jami hajmi (bayt). Soat uni 0x0007 da e'lon qiladi va bir nechta
 * 0x0030 bo'lagida yuboradi (kuzatilgan: 1158 bayt = 4 × 256 + 134). Bir kunlik 5 daqiqalik yozuvlar ~1,8 KB.
 */
internal const val MAX_RECORD_BYTES = 64 * 1024

/** Oxirgisidan boshqa har 0x0030 bo'lagining hajmi (kuzatilgan [E]). Boshqa hajm kelsa sinxron to'xtaydi. */
internal const val RECORD_PIECE_BYTES = 256

/** Bitta 0x0030 bo'lagining eng katta hajmi: yig'iladigan frame payload'i, TLV sarlavhasi (5 bayt) ayirilgan. */
internal const val MAX_PIECE_BYTES = MAX_REASSEMBLED_PAYLOAD - 5

/**
 * Tarix sinxroni oqimi (spec messages.md, TYPE 0x05). Sof mantiq: Android'siz, JVM testida tekshiriladi.
 * Bo'lak DB ga yozilmaguncha soatga ACK ham, 0x0020 ham yuborilmaydi (loyiha qoidasi).
 *
 * Kuzatilgan tartib (qurilma sinovi, FitCloudPro 00:14 va Orvell 00:44): 0x0007 (N), 0x0030 (bo'laklar), 0x0008 (N).
 * Shu sababli 0x0030 0x0008 dan oldin kelishi mumkin.
 *  IDLE --start()--> PENDING_WAIT (0x0005)
 *  PENDING_WAIT --0x0006 = 0--> DONE (faqat 0x0021: FitCloudPro shunday qilgan [T])
 *  PENDING_WAIT --0x0006 > 0--> COUNTER_WAIT (hisoblagich so'raladi)
 *  COUNTER_WAIT --0x0007 = N > 0--> RECORD_WAIT (0x0030 bo'laklari kutiladi, jami N bayt)
 *  RECORD_WAIT --0x0030 bo'lagi--> STORING --saqlandi--> ACK (ilova), jami N ga yetguncha yana RECORD_WAIT
 *  0x0007 va 0x0008 teng bo'lib, N > 0 bo'lsa N bayt saqlangan bo'lishi kerak:
 *      --> keyingi hisoblagich (oldin 0x0020) yoki yakun (0x0020 + 0x0021)
 *  0x0007 != 0x0008 --> shu hisoblagich bir marta qayta so'raladi (0x0001); yana mos kelmasa FAILED
 *      (0x0020 yo'q: soat yozuvni saqlab qoladi). Qayta so'rovda saqlangan bo'laklar tartib bilan takrorlansa faqat ACK.
 *  STORING --saqlanmadi--> FAILED (ACK ham, 0x0020 ham yo'q)
 *  oxirgi bo'lak aynan o'sha SEQ va baytlar bilan takrorlansa (ACK yo'qolgan): faqat ACK (Next.Ack), qayta saqlanmaydi
 *  N to'lgandan keyin boshqa bo'lak, N dan oshadigan yoki 256 baytdan boshqa oraliq bo'lak --> FAILED
 *  har bosqichda vaqt chegarasi --> FAILED
 */
internal class HistoryFlow {
    enum class Phase { IDLE, PENDING_WAIT, COUNTER_WAIT, RECORD_WAIT, STORING, DONE, FAILED }

    /** Qadamdan keyingi harakat. */
    sealed class Next {
        /** Hech narsa qilinmaydi (kutilmagan xabar yoki faza mos emas). */
        object None : Next()

        /** Takroriy bo'lak: allaqachon saqlangan, faqat ACK yuboriladi. */
        object Ack : Next()

        /** Soatga shu payload'lar tartib bilan yuboriladi. done = true: sinxron tugadi. */
        class Send(val payloads: List<ByteArray>, val done: Boolean = false) : Next()

        /**
         * Bo'lakni DB ga yozish. part: hisoblagich ichidagi tartib raqami (0 dan). complete: shu bo'lak bilan
         * e'lon qilingan N to'ldi (bo'laklar birlashtirilib talqin qilinadi). Natija onStored() ga beriladi.
         */
        class Store(val counter: Int, val payload: ByteArray, val part: Int, val complete: Boolean) : Next()

        /** Sinxron to'xtadi: xabar foydalanuvchiga ko'rinadi. */
        class Fail(val messageRes: Int) : Next()
    }

    var phase: Phase = Phase.IDLE
        private set

    private var index = 0
    private var lenA: Int? = null
    private var lenB: Int? = null

    /** Shu hisoblagichda saqlangan bo'laklar (tartib bilan). */
    private val pieces = ArrayList<ByteArray>()
    private var received = 0
    private var lastSeq: Int? = null

    /** Saqlashga berilgan, natijasi kutilayotgan bo'lak. */
    private var pending: ByteArray? = null
    private var pendingSeq = -1

    /** Shu hisoblagich qayta so'ralganmi (0x0007 != 0x0008 bo'lganda bir marta). Bir hisoblagichda bir marta. */
    private var counterRetried = false

    /** Qayta so'rovdan keyin saqlangan bo'laklar takrorlanadi: keyingi kutilayotgan bo'lak raqami. */
    private var replay: Int? = null

    /** Faol sinxron bormi (vaqt chegarasi shunga qarab qo'llanadi). */
    val active: Boolean
        get() = phase == Phase.PENDING_WAIT || phase == Phase.COUNTER_WAIT ||
            phase == Phase.RECORD_WAIT || phase == Phase.STORING

    /** Sinxronni boshlash. Faol bo'lsa e'tiborsiz. */
    fun start(): Next {
        if (active) return Next.None
        phase = Phase.PENDING_WAIT
        return Next.Send(listOf(HistoryMessages.startRequest()))
    }

    /** Son javobi: 0x0006 (kutilayotgan), 0x0007 va 0x0008 (yozuv hajmi). Boshqa faza yoki ID: e'tiborsiz. */
    fun onCount(id: Int, count: Int): Next = when {
        phase == Phase.PENDING_WAIT && id == HistoryMessages.ID_PENDING -> {
            index = 0
            when {
                count < 0 -> fail(R.string.err_hist_pending)
                // Kutilayotgan hajm 0: hisoblagichlar so'ralmaydi, darhol yakun
                count == 0 -> {
                    phase = Phase.DONE
                    Next.Send(listOf(HistoryMessages.endRequest()), done = true)
                }
                else -> firstCounter()
            }
        }
        (phase == Phase.COUNTER_WAIT || phase == Phase.RECORD_WAIT) && id == HistoryMessages.ID_LENGTH -> {
            lenA = count
            when {
                count < 0 || count > MAX_RECORD_BYTES -> fail(R.string.err_hist_length)
                else -> {
                    if (count > received) phase = Phase.RECORD_WAIT
                    advance()
                }
            }
        }
        (phase == Phase.COUNTER_WAIT || phase == Phase.RECORD_WAIT) && id == HistoryMessages.ID_LENGTH_COPY -> {
            lenB = count
            advance()
        }
        else -> Next.None
    }

    /**
     * 0x0030 bo'lagi. seq: frame SEQ (takroriy yuborishni aniqlash uchun; bir xil baytli yangi bo'lak takror emas).
     * N hali kelmagan bo'lsa yoki N = 0: e'tiborsiz, ACK yo'q.
     */
    fun onRecord(payload: ByteArray, seq: Int = -1): Next {
        val a = lenA ?: return Next.None
        // COUNTER_WAIT: qayta so'rovdan keyin saqlangan bo'laklar takror kelishi mumkin (faqat ACK uchun)
        val waiting = phase == Phase.RECORD_WAIT || (phase == Phase.COUNTER_WAIT && pieces.isNotEmpty())
        if (!waiting || a == 0) return Next.None
        replay?.let { r ->
            if (r < pieces.size) {
                if (!payload.contentEquals(pieces[r])) return fail(R.string.err_hist_second)
                replay = r + 1
                return Next.Ack
            }
        }
        if (pieces.isNotEmpty() && seq == lastSeq && payload.contentEquals(pieces.last())) return Next.Ack
        val remaining = a - received
        if (remaining == 0) return fail(R.string.err_hist_second)
        val fits = payload.size == remaining || (payload.size < remaining && payload.size == RECORD_PIECE_BYTES)
        if (!fits || payload.size > MAX_PIECE_BYTES) return fail(R.string.err_hist_size)
        phase = Phase.STORING
        pending = payload
        pendingSeq = seq
        return Next.Store(HistoryMessages.COUNTERS[index], payload, part = pieces.size, complete = payload.size == remaining)
    }

    /** Saqlash natijasi. True: ACK allaqachon yuborilgan. False: sinxron to'xtaydi. */
    fun onStored(stored: Boolean): Next {
        if (phase != Phase.STORING) return Next.None
        if (!stored) return fail(R.string.err_hist_store)
        val p = pending ?: return fail(R.string.err_hist_store)
        pieces += p
        received += p.size
        lastSeq = pendingSeq
        pending = null
        phase = Phase.RECORD_WAIT
        return advance()
    }

    /** Javob kelmadi (vaqt chegarasi). */
    fun onTimeout(): Next = if (active) fail(R.string.err_hist_timeout) else Next.None

    /** Sinxron bekor qilindi yoki yozish rad etildi. */
    fun abort() {
        phase = Phase.FAILED
    }

    /** Ulanish uzilganda: holat boshiga qaytadi. */
    fun reset() {
        phase = Phase.IDLE
        index = 0
        clearCounter()
    }

    private fun clearCounter() {
        lenA = null
        lenB = null
        pieces.clear()
        received = 0
        lastSeq = null
        pending = null
        pendingSeq = -1
        counterRetried = false
        replay = null
    }

    private fun firstCounter(): Next {
        clearCounter()
        phase = Phase.COUNTER_WAIT
        return Next.Send(listOf(HistoryMessages.counterRequest(HistoryMessages.COUNTERS[index])))
    }

    /**
     * Ikkala uzunlik kelgach va (N > 0 bo'lsa) N bayt saqlangach hisoblagich tugaydi.
     * Hali kutilayotgan narsa bo'lsa Next.None (keyingi xabar davom ettiradi).
     */
    private fun advance(): Next {
        val a = lenA ?: return Next.None
        val b = lenB ?: return Next.None
        if (a != b) return mismatch()
        if (received > a) return fail(R.string.err_hist_size)
        if (received < a) {
            phase = Phase.RECORD_WAIT
            return Next.None
        }
        return finishCounter()
    }

    /**
     * 0x0007 != 0x0008: hisoblagich bir marta qayta so'raladi. Saqlangan bo'laklar saqlangan holda qoladi
     * (tartib bilan takror kelsa faqat ACK). Yana mos kelmasa sinxron to'xtaydi: ACK ham, 0x0020 ham yo'q.
     */
    private fun mismatch(): Next {
        if (counterRetried) return fail(R.string.err_hist_mismatch)
        counterRetried = true
        lenA = null
        lenB = null
        if (pieces.isNotEmpty()) replay = 0
        phase = Phase.COUNTER_WAIT
        return Next.Send(listOf(HistoryMessages.counterRequest(HistoryMessages.COUNTERS[index])))
    }

    /** Hisoblagich tugadi: 0x0020 (oldingi yozuvlar qabul qilindi), keyin keyingi hisoblagich yoki 0x0021. */
    private fun finishCounter(): Next {
        val commit = HistoryMessages.commitRequest()
        if (index + 1 < HistoryMessages.COUNTERS.size) {
            index++
            clearCounter()
            phase = Phase.COUNTER_WAIT
            return Next.Send(listOf(commit, HistoryMessages.counterRequest(HistoryMessages.COUNTERS[index])))
        }
        phase = Phase.DONE
        return Next.Send(listOf(commit, HistoryMessages.endRequest()), done = true)
    }

    private fun fail(messageRes: Int): Next {
        phase = Phase.FAILED
        return Next.Fail(messageRes)
    }
}
