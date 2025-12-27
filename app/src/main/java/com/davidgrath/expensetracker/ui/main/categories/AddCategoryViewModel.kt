package com.davidgrath.expensetracker.ui.main.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.davidgrath.expensetracker.MaterialMetadata
import com.davidgrath.expensetracker.iconsListToTagMap
import org.slf4j.LoggerFactory
import org.threeten.bp.Duration

class AddCategoryViewModel(icons: List<MaterialMetadata.MaterialIcon>): ViewModel() {

    var category: String? = null
//    private val _icons = MutableLiveData<List<MaterialMetadata.MaterialIcon>>()
    private var iconName: String = "category"
    private val _iconNameLiveData = MutableLiveData(iconName)
    val iconNameLiveData: LiveData<String> = _iconNameLiveData

    private val _searchTerm = MutableLiveData<String>("")
    val map = iconsListToTagMap(icons)
    val keys = map.keys
    val iconsLiveData = _searchTerm.map { s ->
        if(s.isBlank()) {
            return@map icons.map { it.name }
        }
        val start = System.currentTimeMillis()
        val available = keys.filter { k -> k.contains(s, true) }
        val filtered = mutableSetOf<String>()
        for(key in available) {
            val iconNames = map[key]!!
            for(iconName in iconNames) {
                filtered.add(iconName)
            }
        }
        val ret = filtered.sorted()
        val end = System.currentTimeMillis()
        LOGGER.debug("Time taken: {}", Duration.ofMillis(end - start))
        return@map ret
    }

    fun search(searchTerm: String) {
        _searchTerm.postValue(searchTerm)
    }

    fun setIconName(name: String) {
        this.iconName = name
        _iconNameLiveData.postValue(iconName)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AddCategoryViewModel::class.java)
    }
}