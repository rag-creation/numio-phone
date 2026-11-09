package com.rr.numio.phone.data

import java.util.Locale

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/**
 * Which country a phone number is from, worked out from its calling code (+91 India,
 * +971 UAE…). Everything is in the table below: nothing is looked up online.
 */
object CallerCountry {

    /** Country (ISO code like "IN") for an international number, or null if it isn't one. */
    fun isoFor(number: String, homeIso: String = ""): String? {
        val digits = internationalDigits(number, homeIso) ?: return null
        if (digits.length < 7) return null // too short to be a real number
        if (digits.startsWith("1")) return nanp(digits.drop(1))
        if (digits.startsWith("7")) return if (digits.getOrNull(1) == '6' || digits.getOrNull(1) == '7') "KZ" else "RU"
        for (len in 3 downTo 2) {
            CODES[digits.take(len)]?.let { return it }
        }
        return null
    }

    /**
     * "🇮🇳 India" for a number from another country; null for numbers from the phone's
     * own country ([homeIso]) or numbers without a country code, so local calls stay clean.
     */
    fun label(number: String, homeIso: String, locale: Locale = Locale.getDefault()): String? {
        val iso = isoFor(number, homeIso) ?: return null
        if (iso.equals(homeIso, ignoreCase = true)) return null
        return "${flag(iso)} ${name(iso, locale)}"
    }

    /** Just the flag, for tight places like the Recents list. Same rules as [label]. */
    fun flagFor(number: String, homeIso: String): String? {
        val iso = isoFor(number, homeIso) ?: return null
        if (iso.equals(homeIso, ignoreCase = true)) return null
        return flag(iso)
    }

    /** Flag emoji from the two-letter code: each letter becomes a "regional indicator". */
    fun flag(iso: String): String {
        val up = iso.uppercase(Locale.ROOT)
        if (up.length != 2 || !up.all { it in 'A'..'Z' }) return ""
        return up.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }

    fun name(iso: String, locale: Locale = Locale.getDefault()): String {
        SHORT_NAMES[iso]?.let { return it }
        val shown = Locale("", iso).getDisplayCountry(locale)
        return shown.ifBlank { iso }
    }

    /** Digits after the international prefix (+, 00, or 011 in the US/Canada), else null. */
    fun internationalDigitsOf(number: String, homeIso: String = ""): String? = internationalDigits(number, homeIso)

    private fun internationalDigits(number: String, homeIso: String): String? {
        val t = number.trim()
        val digits = t.filter { it.isDigit() }
        return when {
            t.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("011") && homeIso.uppercase(Locale.ROOT) in setOf("US", "CA") -> digits.drop(3)
            else -> null
        }
    }

    /** +1 is shared by the US, Canada and Caribbean islands; the area code tells them apart. */
    private fun nanp(rest: String): String {
        val area = rest.take(3)
        if (area in CANADA) return "CA"
        return NANP_OTHER[area] ?: "US"
    }

    // Shorter names for the countries Numio users call most; others use the phone's language
    private val SHORT_NAMES = mapOf("AE" to "UAE", "GB" to "UK", "US" to "USA")

    private val CANADA = setOf(
        "204", "226", "236", "249", "250", "257", "263", "289", "306", "343", "354", "365", "367",
        "368", "382", "387", "403", "416", "418", "428", "431", "437", "438", "450", "460", "468",
        "474", "506", "514", "519", "548", "579", "581", "584", "587", "604", "613", "639", "647",
        "672", "683", "705", "709", "742", "753", "778", "780", "782", "807", "819", "825", "867",
        "873", "879", "902", "905", "942",
    )

    private val NANP_OTHER = mapOf(
        "242" to "BS", "246" to "BB", "264" to "AI", "268" to "AG", "284" to "VG", "340" to "VI",
        "345" to "KY", "441" to "BM", "473" to "GD", "649" to "TC", "658" to "JM", "876" to "JM",
        "664" to "MS", "670" to "MP", "671" to "GU", "684" to "AS", "721" to "SX", "758" to "LC",
        "767" to "DM", "784" to "VC", "787" to "PR", "939" to "PR", "809" to "DO", "829" to "DO",
        "849" to "DO", "868" to "TT", "869" to "KN",
    )

    // Calling code → country (ITU list). +1 and +7 are handled above.
    private val CODES = mapOf(
        // Africa
        "20" to "EG", "211" to "SS", "212" to "MA", "213" to "DZ", "216" to "TN", "218" to "LY",
        "220" to "GM", "221" to "SN", "222" to "MR", "223" to "ML", "224" to "GN", "225" to "CI",
        "226" to "BF", "227" to "NE", "228" to "TG", "229" to "BJ", "230" to "MU", "231" to "LR",
        "232" to "SL", "233" to "GH", "234" to "NG", "235" to "TD", "236" to "CF", "237" to "CM",
        "238" to "CV", "239" to "ST", "240" to "GQ", "241" to "GA", "242" to "CG", "243" to "CD",
        "244" to "AO", "245" to "GW", "246" to "IO", "248" to "SC", "249" to "SD", "250" to "RW",
        "251" to "ET", "252" to "SO", "253" to "DJ", "254" to "KE", "255" to "TZ", "256" to "UG",
        "257" to "BI", "258" to "MZ", "260" to "ZM", "261" to "MG", "262" to "RE", "263" to "ZW",
        "264" to "NA", "265" to "MW", "266" to "LS", "267" to "BW", "268" to "SZ", "269" to "KM",
        "27" to "ZA", "290" to "SH", "291" to "ER", "297" to "AW", "298" to "FO", "299" to "GL",
        // Europe
        "30" to "GR", "31" to "NL", "32" to "BE", "33" to "FR", "34" to "ES", "350" to "GI",
        "351" to "PT", "352" to "LU", "353" to "IE", "354" to "IS", "355" to "AL", "356" to "MT",
        "357" to "CY", "358" to "FI", "359" to "BG", "36" to "HU", "370" to "LT", "371" to "LV",
        "372" to "EE", "373" to "MD", "374" to "AM", "375" to "BY", "376" to "AD", "377" to "MC",
        "378" to "SM", "379" to "VA", "380" to "UA", "381" to "RS", "382" to "ME", "383" to "XK",
        "385" to "HR", "386" to "SI", "387" to "BA", "389" to "MK", "39" to "IT", "40" to "RO",
        "41" to "CH", "420" to "CZ", "421" to "SK", "423" to "LI", "43" to "AT", "44" to "GB",
        "45" to "DK", "46" to "SE", "47" to "NO", "48" to "PL", "49" to "DE",
        // Americas
        "500" to "FK", "501" to "BZ", "502" to "GT", "503" to "SV", "504" to "HN", "505" to "NI",
        "506" to "CR", "507" to "PA", "508" to "PM", "509" to "HT", "51" to "PE", "52" to "MX",
        "53" to "CU", "54" to "AR", "55" to "BR", "56" to "CL", "57" to "CO", "58" to "VE",
        "590" to "GP", "591" to "BO", "592" to "GY", "593" to "EC", "594" to "GF", "595" to "PY",
        "596" to "MQ", "597" to "SR", "598" to "UY", "599" to "CW",
        // Asia-Pacific
        "60" to "MY", "61" to "AU", "62" to "ID", "63" to "PH", "64" to "NZ", "65" to "SG",
        "66" to "TH", "670" to "TL", "672" to "NF", "673" to "BN", "674" to "NR", "675" to "PG",
        "676" to "TO", "677" to "SB", "678" to "VU", "679" to "FJ", "680" to "PW", "681" to "WF",
        "682" to "CK", "683" to "NU", "685" to "WS", "686" to "KI", "687" to "NC", "688" to "TV",
        "689" to "PF", "690" to "TK", "691" to "FM", "692" to "MH",
        "81" to "JP", "82" to "KR", "84" to "VN", "850" to "KP", "852" to "HK", "853" to "MO",
        "855" to "KH", "856" to "LA", "86" to "CN", "880" to "BD", "886" to "TW",
        // South Asia, Middle East, Central Asia
        "90" to "TR", "91" to "IN", "92" to "PK", "93" to "AF", "94" to "LK", "95" to "MM",
        "960" to "MV", "961" to "LB", "962" to "JO", "963" to "SY", "964" to "IQ", "965" to "KW",
        "966" to "SA", "967" to "YE", "968" to "OM", "970" to "PS", "971" to "AE", "972" to "IL",
        "973" to "BH", "974" to "QA", "975" to "BT", "976" to "MN", "977" to "NP", "98" to "IR",
        "992" to "TJ", "993" to "TM", "994" to "AZ", "995" to "GE", "996" to "KG", "998" to "UZ",
    )
}
