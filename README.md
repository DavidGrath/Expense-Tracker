# Expense Tracker
A basic app for me to track my expenses

## Some Notes
* `AlertDialog`'s don't seem to behave well with Robolectric and so any tests involving those should use `androidTest`
* Entities in the UI layer and DB layer are suffixed with `Ui` and `Db` respectively
* Instrumented tests are suffixed with `InstrumentedTest`