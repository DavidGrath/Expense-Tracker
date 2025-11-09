package com.davidgrath.expensetracker

class Constants {
    companion object {
        const val MAX_ITEMS_ADD_DETAILED_TRANSACTION_PAGE = 20
        const val MAX_ITEMS_ADD_DETAILED_TRANSACTION_IMAGES_PER_ITEM = 10
        const val MAX_ITEMS_ADD_DETAILED_TRANSACTION_EVIDENCE = 10
        const val MAX_NOTE_CODEPOINT_LENGTH = 500
        const val DRAFT_FILE_NAME = "add_transaction_draft.json"
        const val FOLDER_NAME_PROFILES = "profiles"
        const val FOLDER_NAME_DATA = "data"
        const val FOLDER_NAME_DRAFT = "draft"
        const val SUBFOLDER_NAME_IMAGES = "images"
        const val SUBFOLDER_NAME_DOCUMENTS = "documents"
        const val DATABASE_NAME = "expense-tracker-db"
        const val DEFAULT_PROFILE_ID = "fae075e1-d54a-4857-a76b-b3eadf88d602"
        const val DEFAULT_PREFERENCES_FILE_NAME = "ExpenseTracker"
        const val FILE_NAME_INTENT_PICTURE = "ExpenseTrackerCameraResult"

        /**
         * I'm paranoid I'll somehow pass the limit for data transfer between Activities through Intents so this exists
         */
        const val FILE_NAME_STATS_FILTER_DATA = "statistics_filter.json"
    }
    class PreferenceKeys {
        class Device {
            companion object {
                const val CURRENT_PROFILE = "currentProfile"
            }
        }
        class Profile {
            companion object {
                const val DEFAULT_ACCOUNT_ID = "defaultAccountId"
            }
        }
    }
}