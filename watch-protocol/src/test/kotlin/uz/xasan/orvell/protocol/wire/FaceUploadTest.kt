package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Ketma-ketlik o'z logimizdan (spec watchface.md §1); fayl sun'iy.
class FaceUploadTest {
    private val file = ByteArray(4500) { (it * 31 + 7).toByte() }

    private fun ack(offset: Int) = byteArrayOf(0x10, 0x0A, 0x01, offset.toByte(), (offset ushr 8).toByte(), (offset ushr 16).toByte(), (offset ushr 24).toByte())

    @Test
    fun checksumIsXorOfEvenAndOddBytes() {
        assertArrayEquals(hex("00 00"), FaceUpload.blockChecksum(ByteArray(2048) { 0xFF.toByte() }))
        // LE so'zlar 0x0201 ^ 0x0404 = 0x0605, BE tartibda
        assertArrayEquals(hex("06 05"), FaceUpload.blockChecksum(hex("01 02 04 04")))
    }

    @Test
    fun fullSequenceMatchesCapture() {
        val u = FaceUpload(file)
        assertArrayEquals(hex("09"), (u.start() as FaceUpload.Next.Control).bytes)
        assertArrayEquals(hex("06 A1 27"), (u.onControl(hex("10 09 01 00 08 00 00")) as FaceUpload.Next.Control).bytes)
        assertArrayEquals(hex("01 FFFFFFFFFFFFFFFFFFFFFFFF 00000000"), (u.onControl(hex("10 06 01 00000000000000000000")) as FaceUpload.Next.Control).bytes)

        val b0 = u.onControl(hex("10 01 01")) as FaceUpload.Next.Block
        assertArrayEquals(hex("02 A1 27 00000000"), b0.before)
        assertEquals(List(8) { 240 } + 128, b0.chunks.map { it.size })
        assertArrayEquals(file.copyOfRange(0, 2048), b0.chunks.reduce { a, b -> a + b })
        val s0 = FaceUpload.blockChecksum(file.copyOfRange(0, 2048))
        assertArrayEquals(byteArrayOf(0x0A, 0x00, 0x08, s0[0], s0[1]), b0.after)
        // takroriy yaratish javobi e'tiborsiz
        assertTrue(u.onControl(hex("10 01 01")) is FaceUpload.Next.None)

        val b1 = u.onControl(ack(2048)) as FaceUpload.Next.Block
        assertNull(b1.before)
        val b2 = u.onControl(ack(4096)) as FaceUpload.Next.Block
        assertEquals(404, b2.chunks.sumOf { it.size })
        assertArrayEquals(byteArrayOf(0x0A, 0x94.toByte(), 0x01) + FaceUpload.blockChecksum(file.copyOfRange(4096, 4500)), b2.after)

        assertArrayEquals(hex("03 A1 27 01"), (u.onControl(ack(4500)) as FaceUpload.Next.Control).bytes)
        assertEquals(4500, u.acked)
        assertArrayEquals(hex("04 00"), (u.onControl(hex("10 03 01")) as FaceUpload.Next.Done).bytes)
        assertEquals(FaceUpload.Phase.DONE, u.phase)
    }

    @Test
    fun rejectedStepOrWrongOffsetFails() {
        val u = FaceUpload(file)
        u.start()
        assertTrue(u.onControl(hex("10 09 02")) is FaceUpload.Next.Fail)

        val v = FaceUpload(file)
        v.start(); v.onControl(hex("10 09 01")); v.onControl(hex("10 06 01")); v.onControl(hex("10 01 01"))
        assertTrue(v.onControl(ack(1024)) is FaceUpload.Next.Fail)
        assertEquals(FaceUpload.Phase.FAILED, v.phase)
    }

    @Test
    fun validationRejectedFails() {
        val small = FaceUpload(ByteArray(100))
        small.start(); small.onControl(hex("10 09 01")); small.onControl(hex("10 06 01")); small.onControl(hex("10 01 01"))
        assertTrue(small.onControl(ack(100)) is FaceUpload.Next.Control)
        assertTrue(small.onControl(hex("10 03 00")) is FaceUpload.Next.Fail)
    }

    @Test
    fun timeoutOnlyWhileActiveAndUnrelatedNotifyIgnored() {
        val u = FaceUpload(file)
        assertTrue(u.onTimeout() is FaceUpload.Next.None)
        u.start()
        assertTrue(u.onControl(hex("AB 00")) is FaceUpload.Next.None)
        assertTrue(u.onTimeout() is FaceUpload.Next.Fail)
    }

    @Test
    fun emptyFileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { FaceUpload(ByteArray(0)) }
    }
}
