package com.davidgrath.expensetracker

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toFile
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.db.SellerLocationDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.views.AccountWithStats
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AccountWithStatsUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.ImageUi
import com.davidgrath.expensetracker.entities.ui.SellerLocationUi
import com.davidgrath.expensetracker.entities.ui.SellerUi
import com.davidgrath.expensetracker.entities.ui.StatisticsConfig
import com.davidgrath.expensetracker.entities.ui.StatisticsFilter
import com.davidgrath.expensetracker.entities.ui.TransactionDetailsUi
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.davidgrath.expensetracker.repositories.TransactionRepository
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.Precision
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.NumberFormat
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Currency
import java.util.Locale

private val LOGGER = LoggerFactory.getLogger(Utils::class.java)
class Utils {
    companion object {
        val CORE_CATEGORIES = setOf(
            "food",
            "rent",
            "utilities",
            "transportation",
            "healthcare",
            "debt",
            "childcare",
            "household",
            "entertainment",
            "self_care",
            "clothing",
            "education",
            "gifts_and_donations",
            "emergency",
            "savings",
            "fitness",
            "miscellaneous",
        )

        //TODO Context and string ids
        val CATEGORY_NAMES_DEFAULT = mapOf(
            "food" to "Food",
            "rent" to "Housing",
            "utilities" to "Utilities",
            "transportation" to "Transportation",
            "healthcare" to "Healthcare",
            "debt" to "Debt",
            "childcare" to "Childcare",
            "household" to "Household",
            "entertainment" to "Entertainment",
            "self_care" to "Self-care",
            "clothing" to "Clothing",
            "education" to "Education",
            "gifts_and_donations" to "Gifts and donations",
            "emergency" to "Emergency",
            "savings" to "Savings",
            "fitness" to "Fitness",
            "miscellaneous" to "Miscellaneous",
        )
        val CATEGORY_IDS_ICONS_DEFAULT = mapOf(
            "food" to "materialsymbols:restaurant",
            "rent" to "materialsymbols:home",
            "utilities" to "materialsymbols:construction",
            "transportation" to "materialsymbols:directions_car",
            "healthcare" to "materialsymbols:medical_services",
            "debt" to "materialsymbols:credit_card",
            "childcare" to "materialsymbols:child_care",
            "household" to "materialsymbols:household_supplies",
            "entertainment" to "materialsymbols:tv",
            "self_care" to "materialsymbols:self_care",
            "clothing" to "materialsymbols:checkroom",
            "education" to "materialsymbols:book",
            "gifts_and_donations" to "materialsymbols:featured_seasonal_and_gifts",
            "emergency" to "materialsymbols:emergency",
            "savings" to "materialsymbols:savings",
            "fitness" to "materialsymbols:fitness_center",
            "miscellaneous" to "materialsymbols:category",
        )
    }
}

fun loadMaterialSymbolsIcons(context: Context): Single<List<MaterialMetadata.MaterialIcon>> {
    return Single.fromCallable {


        val gson = Gson()
        val inputStreamReader = context.assets.open("material_metadata.json").bufferedReader()
        val materialMetadata = gson.fromJson(inputStreamReader, MaterialMetadata::class.java)
        inputStreamReader.close()
        val symbolsOutlined = materialMetadata.icons.filter {
            !it.unsupported_families.contains(MaterialMetadata.MaterialIconFamily.MaterialSymbolsOutlined)
        }.filter {
            it.name !in nonExistentSymbols
        }
        symbolsOutlined
    }
}
fun getMaterialResourceId(context: Context, icon: MaterialMetadata.MaterialIcon): Int {
    val resourceName = "material_symbols_${icon.name}_48px"
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    return resId
}
fun getMaterialResourceId(context: Context, iconId: String): Int {
    val split = iconId.split(':')
    val name = split[1]
    val resourceName = "material_symbols_${name}_48px"
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    return resId
}

val nonExistentSymbols = hashSetOf(
    "antigravity",
    "arrows_left_right_circle",
    "arrows_up_down_circle",
    "b_circle",
    "bookmark_stacks",
    "cards_stack",
    "chevron_line_up",
    "circle_circle",
    "computer_sound",
    "dashboard_2_edit",
    "dashboard_2_gear",
    "detection_and_zone_off",
    "eyebrow",
    "format_image_back",
    "format_image_break_left",
    "format_image_break_right",
    "format_image_front",
    "format_image_inline_left",
    "format_image_inline_right",
    "game_bumper_left",
    "game_bumper_right",
    "game_button_l",
    "game_button_l1",
    "game_button_l2",
    "game_button_r",
    "game_button_r1",
    "game_button_r2",
    "game_button_zl",
    "game_button_zr",
    "game_stick_l3",
    "game_stick_left",
    "game_stick_r3",
    "game_stick_right",
    "game_trigger_left",
    "game_trigger_right",
    "gamepad_circle_down",
    "gamepad_circle_left",
    "gamepad_circle_right",
    "gamepad_circle_up",
    "gamepad_down",
    "gamepad_left",
    "gamepad_right",
    "gamepad_up",
    "graph_8",
    "hourglass_check",
    "lips",
    "mic_gear",
    "mobile_unlock",
    "music_note_2",
    "notification_audio",
    "notification_audio_off",
    "passport",
    "person_text",
    "rectangle_add",
    "square_circle",
    "sticker",
    "sticker_add",
    "thermometer_alert",
    "triangle_circle",
    "undereye",
    "video_template",
    "voice_chat_off",
    "watch_lock",
    "widget_menu",
    "x_circle",
    "y_circle"
)

data class MaterialMetadata(val host: String, val asset_url_pattern: String, val families: Set<MaterialIconFamily>, val icons: List<MaterialIcon>) {
    enum class MaterialIconFamily() {
        @SerializedName("Material Icons")
        MaterialIcons,
        @SerializedName("Material Icons Outlined")
        MaterialIconsOutlined,
        @SerializedName("Material Icons Round")
        MaterialIconsRound,
        @SerializedName("Material Icons Sharp")
        MaterialIconsSharp,
        @SerializedName("Material Icons Two Tone")
        MaterialIconsTwoTone,
        @SerializedName("Material Symbols Outlined")
        MaterialSymbolsOutlined,
        @SerializedName("Material Symbols Rounded")
        MaterialSymbolsRounded,
        @SerializedName("Material Symbols Sharp")
        MaterialSymbolsSharp
    }

    data class MaterialIcon(val name: String, val version: Int, val popularity: Int, val codePoint: Int, val unsupported_families: Set<MaterialIconFamily>, val categories: Set<String>, val tags: Set<String>, val sizes_px: Set<Int>) {

    }
}

fun transactionsToTransactionItems(transactions: List<TransactionWithItemAndCategoryUi>): List<GeneralTransactionListItem> {
    val itemsList = arrayListOf<GeneralTransactionListItem>()
    var currentDate: LocalDate? = null
    var currentTransaction: TransactionUi? = null

    val length = transactions.size
    var currentTransactionId = -1L
    transactions.forEachIndexed { index, transaction ->
        currentTransactionId = transaction.transactionId
        val ld = transaction.transactionDatedAt
        if(currentDate != ld) {
            currentDate = ld
            itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.Date, currentDate, null, null))
        }
        if(currentTransaction?.id != transaction.transactionId) {
            val transactionUi = TransactionUi(transaction.transactionId, transaction.transactionTotal, transaction.currencyCode, transaction.debitOrCredit,
                transaction.transactionCreatedAt, currentDate!!, transaction.transactionDatedAtTime, null, emptyList())
            currentTransaction = transactionUi
            itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.Transaction, null, transactionUi, null))
        }
        val next = index + 1
        val isLast: Boolean
        if(next >= length) {
            isLast = true
        } else {
            val nextItem = transactions[next]
            if(nextItem.transactionId != currentTransactionId) {
                isLast = true
            } else {
                isLast = false
            }
        }
        val item = TransactionItemUi(
            currentTransaction!!, transaction.itemAmount, transaction.description,
            transaction.category,
            isLast, null,
            transaction.itemImages
        )
        itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.TransactionItem, null, null, item))

    }
    return itemsList
}

//TODO Context and string ids
fun categoryDbToCategoryUi(context: Context, categoryDb: CategoryDb): CategoryUi {
    val category = if(categoryDb.isCustom) {
        val categoryIcon = getMaterialResourceId(context, categoryDb.icon)
        CategoryUi(categoryDb.id!!, null, categoryDb.name!!, categoryIcon)
    } else {
        val categoryIcon = getMaterialResourceId(context, categoryDb.icon)
        CategoryUi(categoryDb.id!!, categoryDb.stringId, Utils.CATEGORY_NAMES_DEFAULT[categoryDb.stringId]!!, categoryIcon)
    }
    return category
}
//TODO Context and string ids
fun transactionWithCategoryToCategoryUi(context: Context, transactionWithItemAndCategory: TransactionWithItemAndCategory): CategoryUi {
    val category = if(transactionWithItemAndCategory.categoryIsCustom) {
        val categoryIcon = R.drawable.baseline_category_24
        CategoryUi(transactionWithItemAndCategory.primaryCategoryId, null, transactionWithItemAndCategory.categoryName!!, categoryIcon)
    } else {
        val categoryIcon = getMaterialResourceId(context, transactionWithItemAndCategory.categoryIcon)
        CategoryUi(transactionWithItemAndCategory.primaryCategoryId, transactionWithItemAndCategory.categoryStringId, Utils.CATEGORY_NAMES_DEFAULT[transactionWithItemAndCategory.categoryStringId]!!, categoryIcon)
    }
    return category
}
//TODO Context and string ids
fun itemSumToCategoryUi(context: Context, itemSumByCategory: ItemSumByCategory): CategoryUi {
    val category = if(itemSumByCategory.isCustom) {
        val categoryIcon = R.drawable.baseline_category_24
        CategoryUi(itemSumByCategory.categoryId, null, itemSumByCategory.name!!, categoryIcon)
    } else {
        val categoryIcon = getMaterialResourceId(context, itemSumByCategory.categoryIcon)
        CategoryUi(itemSumByCategory.categoryId, itemSumByCategory.stringId, Utils.CATEGORY_NAMES_DEFAULT[itemSumByCategory.stringId]!!, categoryIcon)
    }
    return category
}

fun transactionDbToTransactionDetailedUi(transactionDb: TransactionDb, accountDb: AccountDb, sellerDb: SellerDb?, sellerLocationDb: SellerLocationDb?): TransactionDetailsUi {
    val createdDateTime = LocalDateTime.parse(transactionDb.createdAt)
    val datedDate = LocalDate.parse(transactionDb.datedAt)

    val datedTime = if(transactionDb.datedAtTime == null) {
        null
    } else {
        LocalTime.parse(transactionDb.datedAtTime)
    }
    val sellerUi = if(sellerDb == null) {
        null
    } else {
        sellerDbToSellerUi(sellerDb)
    }
    val sellerLocationUi = if(sellerLocationDb == null) {
        null
    } else {
        sellerLocationDbToSellerLocationUi(sellerLocationDb)
    }
    val transactionUi = TransactionDetailsUi(transactionDb.id!!, accountDb.name, accountDb.currencyCode, accountDb.referenceNumber, transactionDb.amount, transactionDb.currencyCode, transactionDb.debitOrCredit, transactionDb.note, createdDateTime, datedDate, datedTime, transactionDb.mode, sellerUi, sellerLocationUi)
    return transactionUi
}


val instantFormatter = DateTimeFormatter.ISO_INSTANT

fun imageDbToImageUi(image: ImageDb): ImageUi {
    val utcDateTime = LocalDateTime.parse(image.createdAt)
    val offset = ZoneOffset.of(image.createdAtOffset)
    val offsetDateTime = utcDateTime.atOffset(offset)
    val localDateTime = offsetDateTime.toLocalDateTime()
    val uri = Uri.parse(image.uri)
    return ImageUi(image.id!!, image.sizeBytes, image.sha256, image.mimeType, uri, localDateTime)
}

fun evidenceDbToEvidenceUi(evidence: EvidenceDb): EvidenceUi {
    val utcDateTime = LocalDateTime.parse(evidence.createdAt)
    val offset = ZoneOffset.of(evidence.createdAtOffset)
    val offsetDateTime = utcDateTime.atOffset(offset)
    val localDateTime = offsetDateTime.toLocalDateTime()
    val uri = Uri.parse(evidence.uri)
    return EvidenceUi(evidence.id!!, evidence.transactionId, evidence.sizeBytes, evidence.sha256, evidence.mimeType, uri, localDateTime)
}

fun accountDbToAccountUi(accountDb: AccountDb, locale: Locale): AccountUi {
    var currencyDisplayName: String = "Unknown currency" // TODO Context and string IDs
    try {
        val currency = Currency.getInstance(accountDb.currencyCode)
        currencyDisplayName = currency.getDisplayName(locale)
    } catch (e: IllegalArgumentException) {
        LOGGER.warn("Currency not recognized", e)
    }
    val accountUi = AccountUi(accountDb.id!!, accountDb.profileId, accountDb.currencyCode, currencyDisplayName, accountDb.name)
    return accountUi
}

fun accountWithStatsDbToAccountWithStatsUi(accountWithStats: AccountWithStats, locale: Locale): AccountWithStatsUi {
    var currencyDisplayName: String = "Unknown currency" // TODO Context and string IDs
    try {
        val currency = Currency.getInstance(accountWithStats.currencyCode)
        currencyDisplayName = currency.getDisplayName(locale)
    } catch (e: IllegalArgumentException) {
        LOGGER.warn("Currency not recognized", e)
    }
    val accountUi = AccountWithStatsUi(accountWithStats.id, accountWithStats.profileId, accountWithStats.currencyCode, currencyDisplayName, accountWithStats.name, accountWithStats.expenses, accountWithStats.income, accountWithStats.transactionCount, accountWithStats.itemCount)
    return accountUi
}

fun sellerDbToSellerUi(sellerDb: SellerDb): SellerUi {
    val sellerUi = SellerUi(sellerDb.id!!, sellerDb.name)
    return sellerUi
}

fun sellerLocationDbToSellerLocationUi(sellerLocationDb: SellerLocationDb): SellerLocationUi {
    val sellerLocationUi = SellerLocationUi(sellerLocationDb.id!!, sellerLocationDb.sellerId, sellerLocationDb.location, sellerLocationDb.isVirtual, sellerLocationDb.longitude, sellerLocationDb.latitude, sellerLocationDb.address)
    return sellerLocationUi
}

fun getSha256(inputStream: InputStream): Single<String> {
    return Single.fromCallable {
        val bufSize = DEFAULT_BUFFER_SIZE
        val bufferedStream = inputStream.buffered()
        val md = MessageDigest.getInstance("SHA256")
        var array = ByteArray(bufSize)
        var len = 0
        var total = 0L //TODO UOM/UCOM
        while (len >= 0) {
            len = bufferedStream.read(array)
            if (len >= 0) {
                total += len
                md.update(array, 0, len)
            }
        }
        val hash = md.digest()
        val hashText = String.format("%064x", BigInteger(1, hash))
        LOGGER.info("getSha256: read {} bytes", total)
        LOGGER.info("getSha256: hash: {}", hashText)
        hashText
    }.subscribeOn(Schedulers.io())
}

class UriTypeAdapter: TypeAdapter<Uri>() {
    override fun write(out: JsonWriter?, value: Uri?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): Uri {
        return Uri.parse(`in`?.nextString())
    }
}

class DayOfWeekGsonAdapter: TypeAdapter<DayOfWeek>() {
    override fun write(out: JsonWriter?, value: DayOfWeek?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): DayOfWeek {
        return try {
            DayOfWeek.valueOf(`in`?.nextString()!!)
        } catch (e: NullPointerException) {
            LOGGER.warn("Could not parse DayOfWeek", e)
            DayOfWeek.MONDAY
        } catch (e: IllegalArgumentException) {
            LOGGER.warn("Could not parse DayOfWeek", e)
            DayOfWeek.MONDAY
        }
    }
}
class LocalDateGsonAdapter: TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter?, value: LocalDate?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): LocalDate? {
        return try {
            LocalDate.parse(`in`?.nextString()!!)
        } catch (e: DateTimeParseException) {
            LOGGER.warn("Could not parse date", e)
            null
        }
    }
}
fun file(vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(fullPath)
}

fun file(file: File, vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(file, fullPath)
}

fun dateTimeOffsetZone(clock: Clock): Triple<String, String, String> {
    val date = ZonedDateTime.now(clock)
    val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
    val dateTimeString = utcDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val offset = date.offset.id
    val zone = date.zone.id
    return Triple(dateTimeString, offset, zone)
}

fun loadRenderer(uri: Uri): Maybe<PdfRenderer> {
    return Maybe.fromCallable {
        val file = uri.toFile()
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer: PdfRenderer? = try {
            PdfRenderer(fd)
        } catch (e: SecurityException) {
            LOGGER.error("loadRenderer", e)
            null
        } catch (e: IOException) {
            LOGGER.error("loadRenderer", e)
            null
        }
        return@fromCallable pdfRenderer
    }
}

const val KB = 1024L
const val MB = 1024L * 1024
const val GB = 1024L * 1024 * 1024
fun Long.formatBytes(locale: Locale): String {
    val divisor: Double
    val unit: MeasureUnit = if(this >= 0 && this < KB) {
        divisor = 1.0
        MeasureUnit.BYTE
    } else if(this >= KB && this < MB) {
        divisor = KB.toDouble()
        MeasureUnit.KILOBYTE
    } else if(this >= MB && this < GB) {
        divisor = MB.toDouble()
        MeasureUnit.MEGABYTE
    } else {
        divisor = GB.toDouble()
        MeasureUnit.GIGABYTE
    }
    val measure = Measure(this/divisor, unit)
    val numberFormat = NumberFormat.getInstance(locale).apply { maximumFractionDigits = 1; minimumFractionDigits = 1; roundingMode = RoundingMode.HALF_UP.ordinal }
    val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT, numberFormat)
    return format.formatMeasures(measure)
}

val numberFormatterSettings = NumberFormatter.with().grouping(NumberFormatter.GroupingStrategy.ON_ALIGNED).precision(
    Precision.fixedFraction(2))
fun formatDecimal(bigDecimal: BigDecimal, locale: Locale): String {
//    val numberFormatter = numberFormatterSettings.locale(locale)
//    return numberFormatter.format(bigDecimal).toString()
    return NumberFormat.getInstance(locale).apply { maximumFractionDigits = 2; minimumFractionDigits = 0; roundingMode = RoundingMode.DOWN.ordinal }.format(bigDecimal)
}

fun parseDecimal(decimal: String, locale: Locale): BigDecimal {
//    val decimalSymbols = DecimalFormatSymbols.getInstance(locale)
//    val sep = decimalSymbols.decimalSeparatorString
//    val pattern = "[^\\d$sep]"
//    val replaced = decimal.replace(Regex(pattern), "")
//    println("Rep: " + replaced)
    val numberFormat = com.ibm.icu.text.NumberFormat.getInstance(locale)
//    val number = numberFormat.parse(replaced)
    val number = numberFormat.parse(decimal)
    return BigDecimal(number.toString())
}

fun getFilteredWeekDays(profileId: Long, filter: StatisticsFilter, dateMode: StatisticsConfig.DateMode, timeAndLocaleHandler: TimeAndLocaleHandler, transactionRepository: TransactionRepository): List<String> {
    val dates = mutableListOf<String>()
    val accountIds = filter.accountIds
    if(dateMode != StatisticsConfig.DateMode.Daily) {
        if(filter.weekdays.isNotEmpty()) {
            val earliestDate =
                transactionRepository.getEarliestTransactionDate(profileId, accountIds).blockingGet()
            if (earliestDate != null) {
                val today = LocalDate.now(timeAndLocaleHandler.getClock())
                val startDate = filter.startDay ?: earliestDate
                val endDate = filter.endDay ?: today
                if (startDate <= endDate) {
                    var runningDate = startDate
                    while (runningDate <= endDate) {
                        if(runningDate.dayOfWeek in filter.weekdays) {
                            dates.add(runningDate.toString())
                        }
                        runningDate = runningDate.plusDays(1)
                    }
                }
            } else {

            }
        }
    }
    LOGGER.info("Picked out {} days from weekdays filter", dates.size)
    return dates
}

