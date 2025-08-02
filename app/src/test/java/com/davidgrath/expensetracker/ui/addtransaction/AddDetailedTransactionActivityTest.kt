package com.davidgrath.expensetracker.ui.addtransaction

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Ignore
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class AddDetailedTransactionActivityTest {
    @Test
    @Ignore("Not ready yet")
    fun givenIntentHasArgsWhenStartActivityThenDetailsArePopulated() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenItemsAreAllValidWhenDoneThenTransactionAdded() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenAnyAmountNonPositiveOrEmptyWhenDoneThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenAnyDescriptionEmptyWhenDoneThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    //Should be impossible with UI restrictions but I'm paranoid
    fun givenUseCustomTimestampAndTimestampInFutureWhenSubmitThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenUseSellerAndSellerNotSuppliedWhenSubmitThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenEvidenceThresholdLimitReachedWhenAddMoreThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenImageThresholdReachedWhenAddMoreThenFail() {

    }

    @Test
    @Ignore("Not ready yet")
    fun twoDecimalPlacesTest() {

    }

    @Test
    @Ignore("Not ready yet")
    fun givenDetailedTransactionInDraftWhenStartActivityThenTransactionEditRestored() {
        //Open Add Detailed Transaction Screen
        //Add An Item with all details
        //Close App
        //Open App
        //Assert Screen is present with same details
        //This might end up becoming a toggleable preference like: "restoreDraftTransactionWhenReopenApplication"
    }

    @Test
    @Ignore("Not ready yet")
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {
        //Open Image from system
        //Open same image
    }


    @Test
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsTThenImageOnlyCopiedOnceToInternalStorage() {
        //Open Image from system
        //Add new item
        //Open same
    }
}