# Expense Tracker
A basic app for me to track my expenses

## Some Notes
* `AlertDialog`'s don't seem to behave well with Robolectric and so any tests involving those should use `androidTest`
* Entities in the UI layer and DB layer are suffixed with `Ui` and `Db` respectively
* Instrumented tests are suffixed with `InstrumentedTest`
* `./data` for the main app files, `./draft` for the `AddDetailedTransaction` screen, under each of them, `./images` for all images, `./documents` for all documents
* Tests that open other activities should use `androidTest`: https://github.com/robolectric/robolectric/issues/5104

## To-Do
* Jenkins Testing Pipeline
* Jenkins Staging Pipeline
* Ansible Install SDK
* Ansible Setup Emulators

## Simple Testing Guideline
Test any non-trivial or potentially confusing or complex or vague code at any layer of the feature you want to commit, and also the trivial case in case you feel like it.
* `now` is mocked to be June 30 2025 at 8 am, or `2025-06-30T08:00:00`