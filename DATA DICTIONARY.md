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
* Miscellaneous

## Entities

**Profile**
* ID: Long
* Name: String
* StringID: String - Meant for the SharedPreferences file name
* CreatedAt: String - An ISO8601 DateTime at UTC
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String - A TZDB Timezone

**Financial Institution** - An entity that holds accounts
* ID: Long
* ProfileID: Long
* Name: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Account**
* ID: Long
* ProfileID: Long
* FinancialInstitutionID: Long?
* ReferenceNumber: String
* Name: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Transaction** - The basis for everything else in the app
* ID: Long
* AccountID: Long
* Amount: Double
* CurrencyCode: String
* ReferenceNumber: String?
* DebitOrCredit: Boolean
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String
* DatedAt: String
* DatedAtOffset: String - An ISO8601 Offset
* DatedAtTimezone: String

**TransactionItem**
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
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Category**
* ID: Long
* ProfileID: Long
* StringID: String?
* IsCustom: Boolean
* Name: String?
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String


**TransactionItemCategories**
* ID: Long
* TransactionItemID: Long
* CategoryID: Long
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Evidence**
* ID: Long
* TransactionID: Long
* Type: String ["link", "document"]
* SizeBytes: Long?
* SHA256: String?
* MimeType: String?
* URI: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Image**
* ID: Long
* SizeBytes: Long
* SHA256: String
* URI: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**TransactionItemImages**
* ID: Long
* TransactionItemID: Long
* ImageID: Long
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Product**
* ID: Long
* Name: String
* Brand: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**ProductImages**
* ID: Long
* ProductID: Long
* ImageID: Long
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**FinancialInstitutionCard** - Primarily for deriving the Account when using OCR
* ID: Long
* AccountID: Long
* Last4: String
* ExpMonth: Int
* ExpYear: Int
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**Seller** - An entity that sells items
* ID: Long
* ProfileID: Long
* Name: String
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String

**SellerLocation**
* ID: Long
* SellerID: Long
* Location: String
* isVirtual: Boolean
* Longitude: Double?
* Latitude: Double?
* Address: String?
* CreatedAt: String
* CreatedAtOffset: String - An ISO8601 Offset
* CreatedAtTimezone: String