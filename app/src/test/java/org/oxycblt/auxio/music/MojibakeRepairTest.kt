package org.oxycblt.auxio.music

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class MojibakeRepairTest {
    @Test
    fun repairsGbKBytesMisreadAsLatin1() {
        val expected = "海边来信"
        val corrupted =
            String(expected.toByteArray(Charset.forName("GBK")), StandardCharsets.ISO_8859_1)

        assertEquals(expected, repairMojibakeForDisplay(corrupted))
    }

    @Test
    fun repairsUtf8BytesMisreadAsLatin1() {
        val expected = "在没有尽头的海岸线"
        val corrupted =
            String(expected.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)

        assertEquals(expected, repairMojibakeForDisplay(corrupted))
    }

    @Test
    fun preservesAlreadyCorrectChineseAndLatinNames() {
        val chinese = "海边来信"
        val latin = "Björk - Jóga"

        assertEquals(chinese, repairMojibakeForDisplay(chinese))
        assertEquals(latin, repairMojibakeForDisplay(latin))
    }
}
