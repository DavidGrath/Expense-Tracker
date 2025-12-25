package com.davidgrath.expensetracker

import com.google.gson.Gson
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.Precision
import com.ibm.icu.text.DecimalFormatSymbols
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import systems.uom.unicode.CLDR.BYTE
import tech.units.indriya.format.NumberDelimiterQuantityFormat
import tech.units.indriya.format.SimpleQuantityFormat
import tech.units.indriya.format.SimpleUnitFormat
import tech.units.indriya.quantity.Quantities
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import javax.measure.BinaryPrefix

@RunWith(JUnit4::class)
class UtilsTest {

    @Test
    fun formatTest() {

//        val unit = BinaryPrefix.KIBI(BYTE)
        val unit = BYTE
        val quantity = Quantities.getQuantity(1_000.23, unit)
        val format = SimpleQuantityFormat.getInstance()
        val quantityFormat = NumberDelimiterQuantityFormat.getInstance(NumberFormat.getNumberInstance(Locale.getDefault()), SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII))
        println(quantityFormat.format(quantity))

        println(1023L.formatBytes(Locale.US))
        println(4096L.formatBytes(Locale("en", "IN")))
        println(MB.formatBytes(Locale.GERMANY))
        println(GB.formatBytes(Locale.CHINA))
    }

    @Test
    fun decimalFormatTest() {
        val bigDecimal = BigDecimal("1234567.891")
        val german = Locale("de", "DE")
        val indian = Locale("en", "IN")
        val numberFormatterSettings = NumberFormatter.with().grouping(NumberFormatter.GroupingStrategy.ON_ALIGNED).precision(Precision.fixedFraction(2))
        var numberFormatter = numberFormatterSettings.locale(german)
//        val germanFormatted = numberFormatter.format(bigDecimal).toString()
        val germanFormatted = formatDecimal(bigDecimal, german)
        assertEquals("1.234.567,89", germanFormatted)
        assertEqualsBD(bigDecimal.setScale(2, RoundingMode.HALF_UP), parseDecimal(germanFormatted, german))

        numberFormatter = numberFormatterSettings.locale(indian)
//        val indianFormatted = numberFormatter.format(bigDecimal).toString()
        val indianFormatted = formatDecimal(bigDecimal, indian)
        assertEquals("12,34,567.89", indianFormatted)
        assertEqualsBD(bigDecimal.setScale(2, RoundingMode.HALF_UP), parseDecimal(indianFormatted, indian))
    }

    //They've served their purpose, not sure where else to put them for now
//    @Test
    fun materialScratchPaperTest() {
        val classLoader = UtilsTest::class.java.classLoader!!
        val gson = Gson()
        val inputStreamReader =
            classLoader.getResourceAsStream("material_metadata.json").bufferedReader()
        val materialMetadata = gson.fromJson(inputStreamReader, MaterialMetadata::class.java)
        val symbolsOutlined = materialMetadata.icons.filter {
            !it.unsupported_families.contains(MaterialMetadata.MaterialIconFamily.MaterialSymbolsOutlined)
        }
        val symbolsOutlinedNameCount = symbolsOutlined.map { it.name }.distinct()
        val nonDistinctCategories = symbolsOutlined.filter { it.categories.size > 1 }
        val grouped =
            symbolsOutlined.groupBy { it.categories.first() }.map { it.key to it.value.size }
        val doesNotHave48Px = symbolsOutlined.filter { !it.sizes_px.contains(48) }
        println("Top 10 outlined: ${symbolsOutlined.take(10)}")
        println("Top 10 distinct: ${symbolsOutlinedNameCount.take(10)}")
        println("Number of outlined: ${symbolsOutlined.size}")
        println("Number of distinct: ${symbolsOutlinedNameCount.size}")
        println("Grouped: ${grouped}")
        println("NonDistinct size: ${nonDistinctCategories.size}")
        println("No 48 pixels : ${doesNotHave48Px.size}")
    }

    fun copyAssetsToProject() {
        val classLoader = UtilsTest::class.java.classLoader!!
        val gson = Gson()
        val inputStreamReader =
            classLoader.getResourceAsStream("material_metadata.json").bufferedReader()
        val materialMetadata = gson.fromJson(inputStreamReader, MaterialMetadata::class.java)
        val symbolsOutlined = materialMetadata.icons.filter {
            !it.unsupported_families.contains(MaterialMetadata.MaterialIconFamily.MaterialSymbolsOutlined)
        }

        val materialRepositoryRoot: File = File("") //git clone git@github.com:google/material-design-icons - roughly 20 GiB
        val projectRepoRoot: File = File("")
        val symbolsAndroidPath = file(materialRepositoryRoot, "symbols", "android")
        var count = 0
        var skippedCount = 0
        var copiedCount = 0
        for(icon in symbolsOutlined) {
            val iconPath = file(symbolsAndroidPath, icon.name, "materialsymbolsoutlined", "${icon.name}_48px.xml")
            if(!iconPath.exists()) {
                println("${iconPath.path} does not exist")
                skippedCount++
                continue
            }
            val targetPath = file(projectRepoRoot, "materialDrawables", "src", "main", "res", "drawable", "material_symbols_${icon.name}_48px.xml")
            iconPath.copyTo(targetPath)
            copiedCount++
            count = skippedCount + copiedCount
            if(count % 100 == 0) {
                println("count progress: $count")
            }
        }
        println("Non-existent: $skippedCount")
        println("Successfully copied: $copiedCount")
        println("Done")

    }

//    @Test
    fun checkNonExistent() {
        val classLoader = UtilsTest::class.java.classLoader!!
        val gson = Gson()
        val inputStreamReader =
            classLoader.getResourceAsStream("material_metadata.json").bufferedReader()
        val materialMetadata = gson.fromJson(inputStreamReader, MaterialMetadata::class.java)
        val symbolsOutlined = materialMetadata.icons.filter {
            !it.unsupported_families.contains(MaterialMetadata.MaterialIconFamily.MaterialSymbolsOutlined)
        }
        val materialRepositoryRoot: File = File("")  //git clone git@github.com:google/material-design-icons - roughly 20 GiB
        val symbolsAndroidPath = file(materialRepositoryRoot, "symbols", "android")
        var count = 0
        var skippedCount = 0
        var copiedCount = 0
        val nonExistentSymbols = mutableListOf<MaterialMetadata.MaterialIcon>()
        for(icon in symbolsOutlined) {
            val iconPath = file(symbolsAndroidPath, icon.name, "materialsymbolsoutlined", "${icon.name}_48px.xml")
            if(!iconPath.exists()) {
                println("${iconPath.path} does not exist")
                nonExistentSymbols += icon
            }
        }
        println("Non-existent: ${nonExistentSymbols.map { it.name }.joinToString( System.lineSeparator() )}")

    }
}