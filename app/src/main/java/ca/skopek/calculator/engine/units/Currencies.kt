package ca.skopek.calculator.engine.units

import java.math.BigDecimal
import java.math.MathContext

data class CurrencyInfo(val code: String, val name: String, val flag: String) {
    val displayName: String get() = if (flag.isEmpty()) name else "$flag $name"
}

/** Known currencies. The rate provider decides which are actually available. */
object Currencies {
    const val CATEGORY_ID = "currency"
    const val BASE = "USD"

    /** Sensible starting set for the picker; the user can change it. */
    val defaultFavorites = listOf("USD", "CAD", "EUR", "GBP", "JPY", "AUD", "CHF", "CNY", "INR", "MXN")

    private fun flag(countryCode: String): String =
        countryCode.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')) }.joinToString("") { String(it) }

    private fun c(code: String, name: String, country: String? = code.substring(0, 2)) =
        CurrencyInfo(code, name, country?.let(::flag) ?: "")

    val all: List<CurrencyInfo> = listOf(
        c("AED", "UAE dirham"), c("AFN", "Afghan afghani"), c("ALL", "Albanian lek"), c("AMD", "Armenian dram"),
        c("ANG", "Netherlands Antillean guilder", "CW"), c("AOA", "Angolan kwanza"), c("ARS", "Argentine peso"),
        c("AUD", "Australian dollar"), c("AWG", "Aruban florin"), c("AZN", "Azerbaijani manat"),
        c("BAM", "Bosnia-Herzegovina mark"), c("BBD", "Barbadian dollar"), c("BDT", "Bangladeshi taka"),
        c("BGN", "Bulgarian lev"), c("BHD", "Bahraini dinar"), c("BIF", "Burundian franc"), c("BMD", "Bermudian dollar"),
        c("BND", "Brunei dollar"), c("BOB", "Bolivian boliviano"), c("BRL", "Brazilian real"), c("BSD", "Bahamian dollar"),
        c("BTN", "Bhutanese ngultrum"), c("BWP", "Botswana pula"), c("BYN", "Belarusian ruble"), c("BZD", "Belize dollar"),
        c("CAD", "Canadian dollar"), c("CDF", "Congolese franc"), c("CHF", "Swiss franc"), c("CLP", "Chilean peso"),
        c("CNY", "Chinese yuan"), c("COP", "Colombian peso"), c("CRC", "Costa Rican colón"), c("CUP", "Cuban peso"),
        c("CVE", "Cape Verdean escudo"), c("CZK", "Czech koruna"), c("DJF", "Djiboutian franc"), c("DKK", "Danish krone"),
        c("DOP", "Dominican peso"), c("DZD", "Algerian dinar"), c("EGP", "Egyptian pound"), c("ERN", "Eritrean nakfa"),
        c("ETB", "Ethiopian birr"), c("EUR", "Euro", "EU"), c("FJD", "Fijian dollar"), c("FKP", "Falkland Islands pound"),
        c("FOK", "Faroese króna"), c("GBP", "British pound"), c("GEL", "Georgian lari"), c("GGP", "Guernsey pound"),
        c("GHS", "Ghanaian cedi"), c("GIP", "Gibraltar pound"), c("GMD", "Gambian dalasi"), c("GNF", "Guinean franc"),
        c("GTQ", "Guatemalan quetzal"), c("GYD", "Guyanese dollar"), c("HKD", "Hong Kong dollar"), c("HNL", "Honduran lempira"),
        c("HRK", "Croatian kuna"), c("HTG", "Haitian gourde"), c("HUF", "Hungarian forint"), c("IDR", "Indonesian rupiah"),
        c("ILS", "Israeli new shekel"), c("IMP", "Manx pound"), c("INR", "Indian rupee"), c("IQD", "Iraqi dinar"),
        c("IRR", "Iranian rial"), c("ISK", "Icelandic króna"), c("JEP", "Jersey pound"), c("JMD", "Jamaican dollar"),
        c("JOD", "Jordanian dinar"), c("JPY", "Japanese yen"), c("KES", "Kenyan shilling"), c("KGS", "Kyrgyzstani som"),
        c("KHR", "Cambodian riel"), c("KID", "Kiribati dollar"), c("KMF", "Comorian franc"), c("KRW", "South Korean won"),
        c("KWD", "Kuwaiti dinar"), c("KYD", "Cayman Islands dollar"), c("KZT", "Kazakhstani tenge"), c("LAK", "Lao kip"),
        c("LBP", "Lebanese pound"), c("LKR", "Sri Lankan rupee"), c("LRD", "Liberian dollar"), c("LSL", "Lesotho loti"),
        c("LYD", "Libyan dinar"), c("MAD", "Moroccan dirham"), c("MDL", "Moldovan leu"), c("MGA", "Malagasy ariary"),
        c("MKD", "Macedonian denar"), c("MMK", "Myanmar kyat"), c("MNT", "Mongolian tögrög"), c("MOP", "Macanese pataca"),
        c("MRU", "Mauritanian ouguiya"), c("MUR", "Mauritian rupee"), c("MVR", "Maldivian rufiyaa"), c("MWK", "Malawian kwacha"),
        c("MXN", "Mexican peso"), c("MYR", "Malaysian ringgit"), c("MZN", "Mozambican metical"), c("NAD", "Namibian dollar"),
        c("NGN", "Nigerian naira"), c("NIO", "Nicaraguan córdoba"), c("NOK", "Norwegian krone"), c("NPR", "Nepalese rupee"),
        c("NZD", "New Zealand dollar"), c("OMR", "Omani rial"), c("PAB", "Panamanian balboa"), c("PEN", "Peruvian sol"),
        c("PGK", "Papua New Guinean kina"), c("PHP", "Philippine peso"), c("PKR", "Pakistani rupee"), c("PLN", "Polish złoty"),
        c("PYG", "Paraguayan guaraní"), c("QAR", "Qatari riyal"), c("RON", "Romanian leu"), c("RSD", "Serbian dinar"),
        c("RUB", "Russian ruble"), c("RWF", "Rwandan franc"), c("SAR", "Saudi riyal"), c("SBD", "Solomon Islands dollar"),
        c("SCR", "Seychellois rupee"), c("SDG", "Sudanese pound"), c("SEK", "Swedish krona"), c("SGD", "Singapore dollar"),
        c("SHP", "Saint Helena pound"), c("SLE", "Sierra Leonean leone"), c("SOS", "Somali shilling"), c("SRD", "Surinamese dollar"),
        c("SSP", "South Sudanese pound"), c("STN", "São Tomé and Príncipe dobra"), c("SYP", "Syrian pound"), c("SZL", "Swazi lilangeni"),
        c("THB", "Thai baht"), c("TJS", "Tajikistani somoni"), c("TMT", "Turkmenistani manat"), c("TND", "Tunisian dinar"),
        c("TOP", "Tongan paʻanga"), c("TRY", "Turkish lira"), c("TTD", "Trinidad and Tobago dollar"), c("TVD", "Tuvaluan dollar"),
        c("TWD", "New Taiwan dollar"), c("TZS", "Tanzanian shilling"), c("UAH", "Ukrainian hryvnia"), c("UGX", "Ugandan shilling"),
        c("USD", "US dollar"), c("UYU", "Uruguayan peso"), c("UZS", "Uzbekistani som"), c("VES", "Venezuelan bolívar"),
        c("VND", "Vietnamese đồng"), c("VUV", "Vanuatu vatu"), c("WST", "Samoan tālā"), c("XAF", "Central African CFA franc", null),
        c("XCD", "East Caribbean dollar", null), c("XDR", "IMF special drawing rights", null), c("XOF", "West African CFA franc", null),
        c("XPF", "CFP franc", null), c("YER", "Yemeni rial"), c("ZAR", "South African rand"), c("ZMW", "Zambian kwacha"),
        c("ZWL", "Zimbabwean dollar"),
    )

    private val byCode: Map<String, CurrencyInfo> = all.associateBy { it.code }

    fun info(code: String): CurrencyInfo = byCode[code] ?: CurrencyInfo(code, code, "")

    /**
     * Builds a converter category from exchange rates quoted against [BASE] (1 base = rate units of
     * currency). Only [codes] that have a rate are included, in the given order.
     */
    fun category(
        rates: Map<String, BigDecimal>,
        codes: List<String>,
        note: String?,
        preferredFrom: String? = null,
        preferredTo: String? = null,
    ): UnitCategory {
        val units = codes.mapNotNull { code ->
            val rate = rates[code] ?: return@mapNotNull null
            if (rate.signum() <= 0) return@mapNotNull null
            val info = info(code)
            ConversionUnit(
                id = code,
                name = info.displayName,
                symbol = code,
                factor = BigDecimal.ONE.divide(rate, MathContext.DECIMAL64),
            )
        }
        val ids = units.map { it.id }
        val from = preferredFrom?.takeIf { it in ids } ?: ids.firstOrNull() ?: BASE
        val to = preferredTo?.takeIf { it in ids && it != from } ?: ids.firstOrNull { it != from } ?: from
        return UnitCategory(
            id = CATEGORY_ID,
            name = "Currency",
            units = units,
            defaultFromId = from,
            defaultToId = to,
            note = note,
        )
    }
}
