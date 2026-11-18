package com.rudi.audioplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupStallFormatTest {

    @Test
    fun `nsToMs membagi sejuta dan membuang sisa`() {
        assertEquals(258L, StartupStallFormat.nsToMs(258_400_000L))
        assertEquals(0L, StartupStallFormat.nsToMs(999_999L))
    }

    @Test
    fun `messageLine membuang awalan Looper dan memuat durasi serta waktu`() {
        val raw = ">>>>> Dispatching to Handler (android.app.ActivityThread\$H) {abc} null: 159"
        assertEquals(
            "Pesan thread utama lambat: 233ms (selesai t=+2294ms sejak proses mulai) | " +
                "Handler (android.app.ActivityThread\$H) {abc} null: 159",
            StartupStallFormat.messageLine(2294, 233, raw)
        )
    }

    @Test
    fun `messageLine memotong teks mentah yang panjang`() {
        val raw = StartupStallFormat.DISPATCH_PREFIX + "x".repeat(300)
        val line = StartupStallFormat.messageLine(1, 100, raw)
        assertTrue(line.endsWith("x".repeat(200) + "..."))
        assertFalse(line.contains("x".repeat(201)))
    }

    @Test
    fun `frameLine memuat semua fase dalam milidetik`() {
        val sample = FrameSample(
            totalNs = 258_000_000,
            vsyncDelayNs = 190_000_000,
            inputNs = 1_000_000,
            animationNs = 2_000_000,
            layoutNs = 30_000_000,
            drawNs = 25_000_000,
            syncNs = 5_000_000,
            issueNs = 3_000_000,
            swapNs = 2_000_000,
            unknownNs = 0,
            firstDraw = true,
            dropCount = 31
        )
        assertEquals(
            "Frame lambat: total=258ms (t=+2452ms sejak proses mulai) frame_pertama=ya drop=31 | " +
                "tunda_vsync=190ms input=1ms animasi=2ms layout=30ms gambar=25ms sinkron=5ms " +
                "perintah=3ms swap=2ms tak_terhitung=0ms",
            StartupStallFormat.frameLine(2452, sample)
        )
    }

    @Test
    fun `tanpa stall besar tidak ada baris yang ditulis`() {
        val collector = StallCollector()
        collector.add(100, StartupStallFormat.MESSAGE_TRIGGER_MS, "pesan sedang")
        collector.add(140, StartupStallFormat.FRAME_TRIGGER_MS, "frame sedang")
        assertTrue(collector.drain().isEmpty())
    }

    @Test
    fun `entri di bawah ambang minimum tidak disimpan`() {
        val collector = StallCollector()
        collector.add(59, StartupStallFormat.MESSAGE_TRIGGER_MS, "kecil")
        collector.add(200, StartupStallFormat.MESSAGE_TRIGGER_MS, "besar")
        assertEquals(listOf("besar"), collector.drain())
    }

    @Test
    fun `ada stall besar maka seluruh entri keluar berurutan`() {
        val collector = StallCollector()
        collector.add(70, StartupStallFormat.MESSAGE_TRIGGER_MS, "1")
        collector.add(160, StartupStallFormat.MESSAGE_TRIGGER_MS, "2")
        collector.add(90, StartupStallFormat.FRAME_TRIGGER_MS, "3")
        assertEquals(listOf("1", "2", "3"), collector.drain())
    }

    @Test
    fun `batas baris dihormati tetapi stall besar tetap tersimpan`() {
        val collector = StallCollector()
        repeat(StartupStallFormat.MAX_LINES) { collector.add(70, StartupStallFormat.MESSAGE_TRIGGER_MS, "n$it") }
        collector.add(75, StartupStallFormat.MESSAGE_TRIGGER_MS, "biasa-lewat-batas") // dibuang: batas penuh
        assertTrue(collector.drain().isEmpty()) // belum ada pemicu
        collector.add(300, StartupStallFormat.MESSAGE_TRIGGER_MS, "besar-lewat-batas")
        val out = collector.drain()
        assertEquals(StartupStallFormat.MAX_LINES + 1, out.size)
        assertEquals("besar-lewat-batas", out.last())
        assertFalse(out.contains("biasa-lewat-batas"))
    }

    @Test
    fun `pola nyata v681 peluncuran Activity 233ms memicu dan frame pertama ikut tercatat`() {
        // v681 (pid 27485): act_mulai=2061ms -> frame1_predraw=2294ms = 233 ms; frame1_selesai=2452ms (frame ~158 ms).
        val collector = StallCollector()
        collector.add(233, StartupStallFormat.MESSAGE_TRIGGER_MS, "peluncuran")
        collector.add(158, StartupStallFormat.FRAME_TRIGGER_MS, "frame pertama")
        assertEquals(listOf("peluncuran", "frame pertama"), collector.drain())
        // v677 start dingin murni: act_mulai=89 -> frame1_predraw=202 = 113 ms (< 150) dan frame ~123 ms (< 200): senyap.
        val normal = StallCollector()
        normal.add(113, StartupStallFormat.MESSAGE_TRIGGER_MS, "peluncuran")
        normal.add(123, StartupStallFormat.FRAME_TRIGGER_MS, "frame pertama")
        assertTrue(normal.drain().isEmpty())
    }

    @Test
    fun `Batch 684 ambang frame 200 ms frame pertama normal 155 dan 175 senyap frame berat memicu`() {
        // TOTAL_DURATION terukur di log v683: frame pertama normal 155 ms (start dingin) / 175 ms (pasca-instal).
        for (normalMs in listOf(123L, 155L, 175L)) {
            val normal = StallCollector()
            normal.add(normalMs, StartupStallFormat.FRAME_TRIGGER_MS, "frame normal")
            assertTrue(normal.drain().isEmpty())
        }
        // Frame berat nyata di v683/pid 31012: 208 (pesan doFrame 206-208), 232, 240, 242 ms.
        for (heavyMs in listOf(208L, 232L, 240L, 242L)) {
            val heavy = StallCollector()
            heavy.add(heavyMs, StartupStallFormat.FRAME_TRIGGER_MS, "frame berat")
            assertEquals(listOf("frame berat"), heavy.drain())
        }
    }

    @Test
    fun `Batch 683 reset mengosongkan entri dan pemicu supaya jendela berikutnya mulai bersih`() {
        val collector = StallCollector()
        collector.add(300, StartupStallFormat.MESSAGE_TRIGGER_MS, "stall jendela proses")
        assertEquals(listOf("stall jendela proses"), collector.drain())
        collector.reset()
        assertTrue(collector.drain().isEmpty())
        collector.add(90, StartupStallFormat.MESSAGE_TRIGGER_MS, "kecil jendela Activity")
        assertTrue(collector.drain().isEmpty()) // pemicu jendela lama tidak terbawa
        collector.add(160, StartupStallFormat.MESSAGE_TRIGGER_MS, "besar jendela Activity")
        assertEquals(listOf("kecil jendela Activity", "besar jendela Activity"), collector.drain())
    }

    @Test
    fun `Batch 683 jendela proses pola v682 habis sebelum Activity dan pola v681 masih mencakup Activity`() {
        val processStart = 961_000_000L
        // v682 pid 23208: Application.onCreate ~142 ms setelah proses mulai, Activity t=+8439 ms.
        assertEquals(7_858L, StartupStallFormat.processWindowRemainingMs(processStart, processStart + 142))
        assertEquals(-439L, StartupStallFormat.processWindowRemainingMs(processStart, processStart + 8_439))
        // v681 pid 27485 (pasca-instal): Activity t=+2061 ms masih di dalam jendela 8 dtk.
        assertTrue(StartupStallFormat.processWindowRemainingMs(processStart, processStart + 2_061) > 0L)
        // v681 pid 31012: Activity 12.305.396 ms (3,4 jam) setelah proses mulai -> jauh di luar jendela proses.
        assertTrue(StartupStallFormat.processWindowRemainingMs(processStart, processStart + 12_305_396) < 0L)
    }

    @Test
    fun `Batch 683 windowLine memuat jenis saat dibuka dan saat Activity pertama`() {
        assertEquals(
            "Jendela probe: jenis=activity_pertama dibuka t=+8439ms sejak proses mulai, Activity pertama dibuat t=+8439ms",
            StartupStallFormat.windowLine(StartupStallFormat.KIND_ACTIVITY, 8_439, 8_439)
        )
        assertEquals(
            "Jendela probe: jenis=proses_mulai dibuka t=+142ms sejak proses mulai, Activity pertama belum dibuat",
            StartupStallFormat.windowLine(StartupStallFormat.KIND_PROCESS, 142, null)
        )
        assertEquals(3, setOf(
            StartupStallFormat.KIND_PROCESS,
            StartupStallFormat.KIND_PROCESS_ACTIVITY,
            StartupStallFormat.KIND_ACTIVITY
        ).size)
    }

    @Test
    fun `Batch 684 RecentKeys membuang duplikat dan melupakan kunci tertua`() {
        val keys = RecentKeys(2)
        assertTrue(keys.firstTime(10L))
        assertFalse(keys.firstTime(10L)) // frame yang sama diserahkan platform lagi
        assertTrue(keys.firstTime(20L))
        assertFalse(keys.firstTime(10L))
        assertFalse(keys.firstTime(20L))
        assertTrue(keys.firstTime(30L)) // kapasitas 2: kunci 10 tergeser
        assertTrue(keys.firstTime(10L))
    }

    @Test
    fun `Batch 684 siteOf membuang sampel menganggur dan menggabung tiga frame teratas`() {
        assertNull(StartupStallFormat.siteOf(emptyList()))
        assertNull(StartupStallFormat.siteOf(listOf("android.os.MessageQueue.nativePollOnce(Native Method)", "x.y(Z.java:1)")))
        assertEquals(
            "a.A.one(A.java:1) < b.B.two(B.java:2) < c.C.three(C.java:3)",
            StartupStallFormat.siteOf(
                listOf("a.A.one(A.java:1)", "b.B.two(B.java:2)", "c.C.three(C.java:3)", "d.D.four(D.java:4)")
            )
        )
        assertEquals("a.A.one(A.java:1)", StartupStallFormat.siteOf(listOf("a.A.one(A.java:1)")))
    }

    @Test
    fun `Batch 684 StackSampleCounter mengurutkan terbanyak dulu mencatat rentang waktu dan bisa direset`() {
        val counter = StackSampleCounter()
        counter.add("jarang", 100)
        counter.add("sering", 200)
        counter.add("sering", 230)
        counter.add("sering", 260)
        counter.add("sedang", 150)
        counter.add("sedang", 180)
        val lines = counter.topLines(2)
        assertEquals(2, lines.size)
        assertEquals(StartupStallFormat.sampleLine(3, 200, 260, "sering"), lines[0])
        assertEquals(StartupStallFormat.sampleLine(2, 150, 180, "sedang"), lines[1])
        assertEquals(
            "Sampel tumpukan thread utama: 3x (tiap ~15ms; t=+200..+260ms sejak proses mulai) | sering",
            lines[0]
        )
        counter.reset()
        assertTrue(counter.topLines(5).isEmpty())
    }

    @Test
    fun `Batch 685 topBetween mengaitkan sampel hanya ke rentang pesan lambat dan melaporkan rentang kosong`() {
        val counter = StackSampleCounter()
        counter.add("binder", 2_320) // pola v684: ISession.setMetadata di dalam pesan 111 ms
        counter.add("binder", 2_340)
        counter.add("gambar", 3_990) // di dalam pesan doFrame 200 ms (3964..4164)
        counter.add("gambar", 4_010)
        counter.add("gambar", 4_030)
        counter.add("lain", 4_040)
        counter.add("luar", 9_000)
        assertEquals(
            listOf(
                StartupStallFormat.intervalSampleLine(3_964, 4_164, 4, 3, "gambar"),
                StartupStallFormat.intervalSampleLine(3_964, 4_164, 4, 1, "lain")
            ),
            counter.topBetween(3_964, 4_164, 3)
        )
        assertEquals(
            listOf(StartupStallFormat.intervalSampleLine(2_381, 2_492, 0, 0, "(tidak ada sampel aktif)")),
            counter.topBetween(2_381, 2_492, 3)
        )
        assertEquals(1, counter.topBetween(2_300, 2_400, 1).size)
        counter.reset()
        assertEquals(
            listOf(StartupStallFormat.intervalSampleLine(0, 10_000, 0, 0, "(tidak ada sampel aktif)")),
            counter.topBetween(0, 10_000, 3)
        )
    }

    @Test
    fun `Batch 685 SlowIntervals memilih yang terpanjang lalu mengurutkan menurut waktu dan bisa direset`() {
        val slow = SlowIntervals()
        slow.add(1_859, 1_994) // 135 ms
        slow.add(2_381, 2_492) // 111 ms
        slow.add(3_964, 4_164) // 200 ms
        slow.add(100, 160) // 60 ms
        val top = slow.longest(3)
        assertEquals(listOf(1_859L, 2_381L, 3_964L), top.map { it.fromMs })
        assertEquals(listOf(1_994L, 2_492L, 4_164L), top.map { it.toMs })
        assertEquals(1, slow.longest(1).size)
        assertEquals(3_964L, slow.longest(1)[0].fromMs)
        slow.reset()
        assertTrue(slow.longest(3).isEmpty())
    }

    @Test
    fun `Batch 685 intervalSampleLine memuat rentang sampel aktif jumlah dan lokasi`() {
        assertEquals(
            "Sampel selama pesan lambat t=+3964..+4164ms (4 sampel aktif): 3x | gambar",
            StartupStallFormat.intervalSampleLine(3_964, 4_164, 4, 3, "gambar")
        )
        assertEquals(4, StartupStallFormat.SITE_DEPTH)
    }
}

