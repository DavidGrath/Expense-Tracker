package com.davidgrath.expensetracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.davidgrath.expensetracker.Constants
import com.davidgrath.expensetracker.db.dao.AccountDao
import com.davidgrath.expensetracker.db.dao.CategoryDao
import com.davidgrath.expensetracker.db.dao.EvidenceDao
import com.davidgrath.expensetracker.db.dao.ImageDao
import com.davidgrath.expensetracker.db.dao.ProfileDao
import com.davidgrath.expensetracker.db.dao.SellerDao
import com.davidgrath.expensetracker.db.dao.SellerLocationDao
import com.davidgrath.expensetracker.db.dao.TransactionDao
import com.davidgrath.expensetracker.db.dao.TransactionItemDao
import com.davidgrath.expensetracker.db.dao.TransactionItemImagesDao
import com.davidgrath.expensetracker.entities.db.AccountDb
import com.davidgrath.expensetracker.entities.db.CategoryDb
import com.davidgrath.expensetracker.entities.db.EvidenceDb
import com.davidgrath.expensetracker.entities.db.ImageDb
import com.davidgrath.expensetracker.entities.db.ProfileDb
import com.davidgrath.expensetracker.entities.db.SellerDb
import com.davidgrath.expensetracker.entities.db.SellerLocationDb
import com.davidgrath.expensetracker.entities.db.TransactionDb
import com.davidgrath.expensetracker.entities.db.TransactionItemCategoriesDb
import com.davidgrath.expensetracker.entities.db.TransactionItemDb
import com.davidgrath.expensetracker.entities.db.TransactionItemImagesDb

@Database(version = 3,
    entities = [
        CategoryDb::class, ImageDb::class, ProfileDb::class, TransactionDb::class, TransactionItemDb::class, TransactionItemImagesDb::class,
        EvidenceDb::class, AccountDb::class, TransactionItemCategoriesDb::class, SellerDb::class, SellerLocationDb::class
    ]
)
@TypeConverters(Converters::class)
abstract class ExpenseTrackerDatabase: RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun imageDao(): ImageDao
    abstract fun profileDao(): ProfileDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun transactionItemImagesDao(): TransactionItemImagesDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun accountDao(): AccountDao
    abstract fun sellerDao(): SellerDao
    abstract fun sellerLocationDao(): SellerLocationDao

    companion object {


        val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `SellerLocationDb2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `sellerId` INTEGER NOT NULL, `location` TEXT NOT NULL, `isVirtual` INTEGER NOT NULL, `longitude` REAL, `latitude` REAL, `address` TEXT, `createdAt` TEXT NOT NULL, `createdAtOffset` TEXT NOT NULL, `createdAtTimezone` TEXT NOT NULL, FOREIGN KEY(`sellerId`) REFERENCES `SellerDb`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_SellerLocationDb_sellerId` ON `SellerLocationDb2` (`sellerId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `SellerDb2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `createdAtOffset` TEXT NOT NULL, `createdAtTimezone` TEXT NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `ProfileDb`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                db.execSQL("INSERT INTO SellerLocationDb SELECT * FROM SellerLocationDb2")
                db.execSQL("INSERT INTO SellerDb SELECT * FROM SellerDb2")
                db.execSQL("DROP TABLE SellerLocationDb")
                db.execSQL("DROP TABLE SellerDb")
                db.execSQL("ALTER TABLE SellerLocationDb2 RENAME TO SellerLocationDb")
                db.execSQL("ALTER TABLE SellerDb2 RENAME TO SellerDb")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_SellerLocationDb_sellerId` ON `SellerLocationDb` (`sellerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_SellerDb_profileId` ON `SellerDb` (`profileId`)")
            }
        }

        val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE CategoryDb ADD COLUMN `icon` STRING NOT NULL DEFAULT 'materialsymbols:category'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:restaurant' WHERE stringId='food'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:home' WHERE stringId='rent'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:construction' WHERE stringId='utilities'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:directions_car' WHERE stringId='transportation'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:medical_services' WHERE stringId='healthcare'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:credit_card' WHERE stringId='debt'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:child_care' WHERE stringId='childcare'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:household_supplies' WHERE stringId='household'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:tv' WHERE stringId='entertainment'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:self_care' WHERE stringId='self_care'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:checkroom' WHERE stringId='clothing'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:book' WHERE stringId='education'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:featured_seasonal_and_gifts' WHERE stringId='gifts_and_donations'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:emergency' WHERE stringId='emergency'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:savings' WHERE stringId='savings'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:fitness_center' WHERE stringId='fitness'")
                db.execSQL("ALTER TABLE CategoryDb SET icon='materialsymbols:category' WHERE stringId='miscellaneous'")
            }
        }
        @Volatile
        private var INSTANCE: ExpenseTrackerDatabase? = null
        fun getDatabase(context: Context): ExpenseTrackerDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.databaseBuilder(context, ExpenseTrackerDatabase::class.java, Constants.DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}