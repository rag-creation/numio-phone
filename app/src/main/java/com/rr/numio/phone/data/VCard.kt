package com.rr.numio.phone.data

import java.io.ByteArrayOutputStream

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/**
 * Contacts backup as a .vcf file (vCard), the format every phone and email app can
 * read. Numio writes vCard 3.0 with names and numbers; it reads 2.1, 3.0 and 4.0,
 * including the "quoted-printable" names older Android phones export (Malayalam etc.).
 */
object VCard {

    fun write(contacts: List<Contact>): String = buildString {
        for (c in contacts) {
            append("BEGIN:VCARD\r\n")
            append("VERSION:3.0\r\n")
            append("FN:").append(escape(c.name)).append("\r\n")
            append("N:;").append(escape(c.name)).append(";;;\r\n")
            for (n in c.numbers) {
                append("TEL;TYPE=").append(typeParam(n)).append(':').append(n.number).append("\r\n")
            }
            append("END:VCARD\r\n")
        }
    }

    fun read(text: String): List<ContactDraft> {
        val out = mutableListOf<ContactDraft>()
        var name: String? = null
        var structured: String? = null
        var numbers = mutableListOf<ContactNumber>()
        var inCard = false
        for (line in unfold(text)) {
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val head = line.substring(0, colon)
            val rawValue = line.substring(colon + 1)
            val parts = head.split(';')
            val prop = parts[0].substringAfterLast('.').uppercase()
            val params = parts.drop(1).map { it.uppercase() }
            when (prop) {
                "BEGIN" -> if (rawValue.trim().equals("VCARD", ignoreCase = true)) {
                    inCard = true
                    name = null
                    structured = null
                    numbers = mutableListOf()
                }
                "END" -> if (inCard && rawValue.trim().equals("VCARD", ignoreCase = true)) {
                    inCard = false
                    val shown = name?.trim()?.takeIf { it.isNotEmpty() } ?: structured?.trim().orEmpty()
                    val draft = ContactDraft(null, shown, numbers.toList(), false)
                    if (draft.canSave) out += draft
                }
                "FN" -> if (inCard) name = value(rawValue, params)
                "N" -> if (inCard) {
                    // family;given;middle;prefix;suffix → "prefix given middle family suffix"
                    val f = splitEscaped(value(rawValue, params, unescape = false)).map { unescape(it).trim() }
                    structured = listOf(f.getOrNull(3), f.getOrNull(1), f.getOrNull(2), f.getOrNull(0), f.getOrNull(4))
                        .filter { !it.isNullOrBlank() }.joinToString(" ")
                }
                "TEL" -> if (inCard) {
                    val number = value(rawValue, params).removePrefix("tel:").trim()
                    if (number.any { it.isDigit() }) {
                        val (type, label) = typeOf(parts.drop(1))
                        numbers += ContactNumber(number, label, type)
                    }
                }
            }
        }
        return out
    }

    /** Leaves out cards already on the phone (same name and the same numbers). */
    fun newOnly(drafts: List<ContactDraft>, existing: List<Contact>): List<ContactDraft> =
        drafts.filter { d ->
            existing.none { c ->
                c.name.trim().equals(d.savedName.trim(), ignoreCase = true) &&
                    d.cleanNumbers.all { n -> c.numbers.any { PhoneNumbers.same(it.number, n.number) } }
            }
        }

    // ---- helpers ----

    private fun typeParam(n: ContactNumber): String = when (n.type) {
        PhoneType.MOBILE -> "CELL"
        PhoneType.HOME -> "HOME"
        PhoneType.WORK -> "WORK"
        PhoneType.CUSTOM -> n.label?.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }?.trim()
            ?.takeIf { it.isNotEmpty() }?.let { "\"X-$it\"" } ?: "OTHER"
        else -> "OTHER"
    }

    /** TEL params → Android phone type, plus a custom label for X-… types. */
    private fun typeOf(params: List<String>): Pair<Int, String?> {
        val types = params.flatMap { p ->
            val v = if (p.contains('=')) p.substringAfter('=') else p
            if (p.substringBefore('=').equals("TYPE", true) || !p.contains('=')) v.trim('"').split(',') else emptyList()
        }.map { it.trim() }
        types.firstOrNull { it.startsWith("X-", ignoreCase = true) && it.length > 2 }?.let {
            return PhoneType.CUSTOM to it.substring(2)
        }
        val upper = types.map { it.uppercase() }
        return when {
            "CELL" in upper || "MOBILE" in upper -> PhoneType.MOBILE to null
            "WORK" in upper -> PhoneType.WORK to null
            "HOME" in upper -> PhoneType.HOME to null
            "OTHER" in upper -> PhoneType.OTHER to null
            else -> PhoneType.MOBILE to null // no type given: almost always a mobile
        }
    }

    private fun value(raw: String, params: List<String>, unescape: Boolean = true): String {
        val qp = params.any { it == "ENCODING=QUOTED-PRINTABLE" || it == "QUOTED-PRINTABLE" }
        val decoded = if (qp) decodeQuotedPrintable(raw) else raw
        return if (unescape) unescape(decoded) else decoded
    }

    /** Joins folded lines: a line starting with a space continues the last one (3.0/4.0), and a
     *  quoted-printable line ending in "=" continues on the next (2.1). */
    private fun unfold(text: String): List<String> {
        val out = mutableListOf<String>()
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        while (i < lines.size) {
            var line = lines[i]
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.size - 1] = out.last() + line.substring(1)
                i++
                continue
            }
            val isQp = line.substringBefore(':').uppercase().contains("QUOTED-PRINTABLE")
            while (isQp && line.endsWith("=") && i + 1 < lines.size) {
                i++
                line = line.dropLast(1) + lines[i]
            }
            out += line
            i++
        }
        return out
    }

    fun decodeQuotedPrintable(s: String): String {
        val bytes = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '=' && i + 2 < s.length + 1) {
                val hex = s.substring(i + 1, minOf(i + 3, s.length))
                val b = hex.toIntOrNull(16)
                if (hex.length == 2 && b != null) {
                    bytes.write(b)
                    i += 3
                    continue
                }
            }
            val enc = c.toString().toByteArray(Charsets.UTF_8)
            bytes.write(enc, 0, enc.size)
            i++
        }
        return bytes.toString("UTF-8")
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

    private fun unescape(s: String) = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n', 'N' -> append('\n')
                    else -> append(n)
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }

    /** Splits on ";" that aren't escaped. */
    private fun splitEscaped(s: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                cur.append(c).append(s[i + 1])
                i += 2
                continue
            }
            if (c == ';') {
                out += cur.toString()
                cur.clear()
            } else {
                cur.append(c)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}
