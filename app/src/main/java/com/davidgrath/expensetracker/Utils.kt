package com.davidgrath.expensetracker

import android.net.Uri
import com.davidgrath.expensetracker.entities.ui.TransactionUi
import com.davidgrath.expensetracker.entities.ui.GeneralTransactionListItem
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

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

fun transactionsToTransactionItems(transactions: List<TransactionUi>): List<GeneralTransactionListItem> {
    val itemsList = arrayListOf<GeneralTransactionListItem>()
    for(transaction in transactions) {
        itemsList.add(GeneralTransactionListItem(true, transaction, null))
        for(item in transaction.items) {
            itemsList.add(GeneralTransactionListItem(false, null, item))
        }
    }
    return itemsList
}

fun getSha256(inputStream: InputStream): String {
    val bufSize = DEFAULT_BUFFER_SIZE
    val bufferedStream = inputStream.buffered()
    val md = MessageDigest.getInstance("SHA256")
    var array = ByteArray(bufSize)
    var len = 0
    while(len >= 0) {
        len = bufferedStream.read(array)
        if(len >= 0) {
            md.update(array, 0, len)
        }
    }
    val hash = md.digest()
    return BigInteger(1, hash).toString(16)
}

class UriTypeAdapter: TypeAdapter<Uri>() {
    override fun write(out: JsonWriter?, value: Uri?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): Uri {
        return Uri.parse(`in`?.nextString())
    }
}