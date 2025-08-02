# Expense Tracker
A basic app for me to track my expenses

## Some Notes
* `AlertDialog`'s don't seem to behave well with Robolectric and so any tests involving those should use `androidTest`
* Entities in the UI layer and DB layer are suffixed with `Ui` and `Db` respectively
* Instrumented tests are suffixed with `InstrumentedTest`
* `./data` for the main app files, `./draft` for the `AddDetailedTransaction` screen, under each of them, `./images` for all images, `./documents` for all documents

## To-Do
* Jenkins Testing Pipeline
* Jenkins Staging Pipeline
* Ansible Install SDK
* Ansible Setup Emulators