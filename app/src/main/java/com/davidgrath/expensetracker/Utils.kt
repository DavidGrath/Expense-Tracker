package com.davidgrath.expensetracker

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toFile
import com.davidgrath.expensetracker.di.TimeAndLocaleHandler
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.views.AccountWithStats
import com.davidgrath.expensetracker.entities.db.views.EvidenceWithTransactionDateAndOrdinal
import com.davidgrath.expensetracker.entities.db.views.ItemSumByCategory
import com.davidgrath.expensetracker.entities.db.views.TransactionWithItemAndCategory
import com.davidgrath.expensetracker.entities.ui.AccountUi
import com.davidgrath.expensetracker.entities.ui.AccountWithStatsUi
import com.davidgrath.expensetracker.entities.ui.CategoryUi
import com.davidgrath.expensetracker.entities.ui.EvidenceUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.davidgrath.expensetracker.entities.ui.ImageUi
import com.davidgrath.expensetracker.entities.ui.TransactionDetailsUi
import com.davidgrath.expensetracker.entities.ui.TransactionItemUi
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.TransactionWithItemAndCategoryUi
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.threeten.bp.Clock
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
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
        val CATEGORY_IDS_DEFAULT = mapOf(
            "food" to R.drawable.baseline_restaurant_24,
            "rent" to R.drawable.baseline_home_24,
            "utilities" to R.drawable.baseline_construction_24,
            "transportation" to R.drawable.baseline_directions_car_24,
            "healthcare" to R.drawable.baseline_medical_services_24,
            "debt" to R.drawable.baseline_credit_card_24,
            "childcare" to R.drawable.baseline_child_care_24,
            "household" to R.drawable.material_household_supplies_24dp,
            "entertainment" to R.drawable.baseline_tv_24,
            "self_care" to R.drawable.material_self_care_24dp,
            "clothing" to R.drawable.baseline_checkroom_24,
            "education" to R.drawable.baseline_book_24,
            "gifts_and_donations" to R.drawable.material_featured_seasonal_and_gifts_24dp,
            "emergency" to R.drawable.baseline_emergency_24,
            "savings" to R.drawable.baseline_savings_24,
            "fitness" to R.drawable.baseline_fitness_center_24,
            "miscellaneous" to R.drawable.baseline_category_24,
        )
    }
}

fun transactionsToTransactionItems(transactions: List<TransactionWithItemAndCategoryUi>): List<GeneralTransactionListItem> {
    val itemsList = arrayListOf<GeneralTransactionListItem>()
    var currentDate: LocalDate? = null
    var currentTransaction: TransactionUi? = null
    for(transaction in transactions) {
        val ld = transaction.transactionDatedAt
        if(currentDate != ld) {
            currentDate = ld
            itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.Date, currentDate, null, null))
        }
        if(currentTransaction?.id != transaction.transactionId) {
            val transactionUi = TransactionUi(transaction.transactionId, transaction.itemAmount, transaction.currencyCode, transaction.debitOrCredit,
                transaction.transactionCreatedAt, currentDate, transaction.transactionDatedAtTime, null, emptyList())
            currentTransaction = transactionUi
            itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.Transaction, null, transactionUi, null))
        }
        val item = TransactionItemUi(
            currentTransaction, transaction.itemAmount, transaction.description,
            transaction.category,
            null,
            transaction.itemImages
        )
        itemsList.add(GeneralTransactionListItem(GeneralTransactionListItem.Type.TransactionItem, null, null, item))

    }
    return itemsList
}

//TODO Context and string ids
fun categoryDbToCategoryUi(categoryDb: CategoryDb): CategoryUi {
    val category = if(categoryDb.isCustom) {
        val categoryIcon = R.drawable.baseline_category_24
        CategoryUi(categoryDb.id!!, null, categoryDb.name!!, categoryIcon)
    } else {
        val categoryIcon = Utils.CATEGORY_IDS_DEFAULT.get(categoryDb.stringId)!!
        CategoryUi(categoryDb.id!!, categoryDb.stringId, Utils.CATEGORY_NAMES_DEFAULT[categoryDb.stringId]!!, categoryIcon)
    }
    return category
}
//TODO Context and string ids
fun transactionWithCategoryToCategoryUi(transactionWithItemAndCategory: TransactionWithItemAndCategory): CategoryUi {
    val category = if(transactionWithItemAndCategory.categoryIsCustom) {
        val categoryIcon = R.drawable.baseline_category_24
        CategoryUi(transactionWithItemAndCategory.primaryCategoryId, null, transactionWithItemAndCategory.categoryName!!, categoryIcon)
    } else {
        val categoryIcon = Utils.CATEGORY_IDS_DEFAULT.get(transactionWithItemAndCategory.categoryStringId)!!
        CategoryUi(transactionWithItemAndCategory.primaryCategoryId, transactionWithItemAndCategory.categoryStringId, Utils.CATEGORY_NAMES_DEFAULT[transactionWithItemAndCategory.categoryStringId]!!, categoryIcon)
    }
    return category
}
//TODO Context and string ids
fun itemSumToCategoryUi(itemSumByCategory: ItemSumByCategory): CategoryUi {
    val category = if(itemSumByCategory.isCustom) {
        val categoryIcon = R.drawable.baseline_category_24
        CategoryUi(itemSumByCategory.categoryId, null, itemSumByCategory.name!!, categoryIcon)
    } else {
        val categoryIcon = Utils.CATEGORY_IDS_DEFAULT.get(itemSumByCategory.stringId)!!
        CategoryUi(itemSumByCategory.categoryId, itemSumByCategory.stringId, Utils.CATEGORY_NAMES_DEFAULT[itemSumByCategory.stringId]!!, categoryIcon)
    }
    return category
}

fun transactionDbToTransactionDetailedUi(transactionDb: TransactionDb, accountDb: AccountDb): TransactionDetailsUi {
    val createdDateTime = LocalDateTime.parse(transactionDb.createdAt)
    var datedDate = LocalDate.parse(transactionDb.datedAt)

    val datedTime = if(transactionDb.datedAtTime == null) {
        null
    } else {
        LocalTime.parse(transactionDb.datedAtTime)
    }
    val transactionUi = TransactionDetailsUi(transactionDb.id!!, accountDb.name, accountDb.currencyCode, accountDb.referenceNumber, transactionDb.amount, transactionDb.currencyCode, transactionDb.debitOrCredit, transactionDb.note, createdDateTime, datedDate, datedTime, null)
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
    val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT)
    return format.formatMeasures(measure)
}