# Expense Tracker Data Dictionary

## Core Categories
* Food
* Housing
* Utilities
* Transportation
* Healthcare
* Debt
* Childcare
* Household
* Entertainment
* Self-care
* Clothing
* Education
* Gifts and donations
* Emergency
* Savings
* Fitness
* Misc

## Entities

**Profile**
* ID: Long
* Name: String
* CreatedAt: String - An ISO8601 Timestamp
* CreatedAtTimezone: String - A TZDB Timezone

**Financial Institution** - An entity that holds accounts
* ID: Long
* ProfileID: Long
* Name: String
* CreatedAt: String
* CreatedAtTimezone: String

**Account**
* ID: Long
* ProfileID: Long
* FinancialInstitutionID: Long?
* Name: String
* CreatedAt: String
* CreatedAtTimezone: String

**Transaction** - The basis for everything else in the app
* ID: Long
* AccountID: Long
* Amount: Double
* CurrencyCode: String
* ReferenceNumber: String?
* DebitOrCredit: Boolean
* CreatedAt: String
* CreatedAtTimezone: String
* RecordedAt: String
* RecordedAtTimezone: String

**PurchaseItem**
* ID: Long
* TransactionID: Long
* Amount: Double
* Brand: String?
* Quantity: Int = 1
* CurrencyCode: String
* Description: String
* Variation: String
* ReferenceNumber: String?
* PrimaryCategoryID: Long
* CreatedAt: String
* CreatedAtTimezone: String

**Category**
* ID: Long
* ProfileID: Long
* StringID: String?
* IsCustom: Boolean
* Name: String?
* CreatedAt: String
* CreatedAtTimezone: String


**PurchaseItemCategories**
* ID: Long
* PurchaseItemID: Long
* CategoryID: Long
* CreatedAt: String
* CreatedAtTimezone: String

**Evidence**
* ID: Long
* TransactionID: Long
* Type: String ["link", "document"]
* URI: String
* CreatedAt: String
* CreatedAtTimezone: String

**Image**
* ID: Long
* SizeBytes: Int
* SHA256: String
* URI: String
* CreatedAt: String
* CreatedAtTimezone: String

**PurchaseItemImages**
* ID: Long
* PurchaseItemID: Long
* ImageID: Long
* CreatedAt: String
* CreatedAtTimezone: String

**Product**
* ID: Long
* Name: String
* Brand: String
* CreatedAt: String
* CreatedAtTimezone: String

**ProductImages**
* ID: Long
* ProductID: Long
* ImageID: Long
* CreatedAt: String
* CreatedAtTimezone: String

**FICard** - Primarily for deriving the Account
* ID: Long
* AccountID: Long
* Last4: String
* ExpMonth: Int
* ExpYear: Int
* CreatedAt: String
* CreatedAtTimezone: String

**Seller** - An entity that sells items
* ID: Long
* ProfileID: Long
* Name: String
* CreatedAt: String
* CreatedAtTimezone: String

**Location**
* ID: Long
* SellerID: Long
* Location: String
* isVirtual: Boolean
* Longitude: Double?
* Latitude: Double?
* Address: String?
* CreatedAt: String
* CreatedAtTimezone: String