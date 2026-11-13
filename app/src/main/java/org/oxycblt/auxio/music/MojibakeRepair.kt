/*
 * Copyright (c) 2026 Auxio Project
 * MojibakeRepair.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.oxycblt.auxio.music

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

private val UTF8_CHARSET = StandardCharsets.UTF_8
private val GBK_CHARSET = Charset.forName("GBK")
private val BIG5_CHARSET = runCatching { Charset.forName("Big5") }.getOrNull()
private val WINDOWS_1252_CHARSET = Charset.forName("windows-1252")

/**
 * Repairs high-confidence Latin-1/Windows-1252 mojibake for display. Source tags are never changed.
 * Valid CJK and ordinary Latin names are returned unchanged.
 */
internal fun repairMojibakeForDisplay(value: String): String {
    if (value.isEmpty() || value.any(::isCjk)) return value
    if (value.none { it.code in 0x80..0xFF }) return value

    val byteSequences = buildList {
        latin1Bytes(value)?.let(::add)
        windows1252Bytes(value)?.let { candidate ->
            if (none { it.contentEquals(candidate) }) add(candidate)
        }
    }
    val decoders = preferredDecoders()
    return byteSequences.firstNotNullOfOrNull { bytes ->
        decoders.firstNotNullOfOrNull { charset ->
            decodeStrict(bytes, charset)?.takeIf(::isPlausibleCjkRepair)
        }
    } ?: value
}

private fun preferredDecoders(): List<Charset> {
    val locale = Locale.getDefault()
    val traditionalChinese = locale.language == "zh" && locale.country in setOf("TW", "HK", "MO")
    return buildList {
        add(UTF8_CHARSET)
        if (traditionalChinese) BIG5_CHARSET?.let(::add)
        add(GBK_CHARSET)
        if (!traditionalChinese) BIG5_CHARSET?.let(::add)
    }
}

private fun latin1Bytes(value: String): ByteArray? {
    if (value.any { it.code > 0xFF }) return null
    return ByteArray(value.length) { index -> value[index].code.toByte() }
}

private fun windows1252Bytes(value: String): ByteArray? =
    try {
        val buffer =
            WINDOWS_1252_CHARSET
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(value))
        ByteArray(buffer.remaining()).also { buffer.get(it) }
    } catch (_: CharacterCodingException) {
        null
    }

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? =
    try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

private fun isPlausibleCjkRepair(value: String): Boolean {
    if (value.any { it == '�' || (it.isISOControl() && !it.isWhitespace()) }) return false
    val cjkCharacters = value.count(::isCjk)
    val commonCharacters = value.count { it in COMMON_CHINESE_CHARACTERS }
    val lettersAndDigits = value.count { it.isLetterOrDigit() }
    return cjkCharacters >= 2 &&
        commonCharacters >= 2 &&
        lettersAndDigits > 0 &&
        cjkCharacters.toFloat() / lettersAndDigits >= 0.45f
}

private fun isCjk(char: Char): Boolean =
    char.code in 0x3400..0x4DBF ||
        char.code in 0x4E00..0x9FFF ||
        char.code in 0xF900..0xFAFF ||
        char.code in 0x3040..0x30FF ||
        char.code in 0x31F0..0x31FF ||
        char.code in 0xAC00..0xD7AF

private const val COMMON_CHINESE_CHARACTERS =
    "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面方后多定行学法所民得经十进着等部度家电力里如水化高自二理起小物现实加量都体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反明看原又么利比但质气第向道命变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指区强放决西被干做必战先回则任取据处队南给色光门海边来信" +
        "國這為時來們與後學會發說經進應開還從業當現實體間關係聲車適廣愛點寫東樂書畫氣風無難"
