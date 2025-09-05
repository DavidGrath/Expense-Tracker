package com.davidgrath.expensetracker

import android.net.Uri
import com.davidgrath.expensetracker.test.TestContentProvider

class TestData {

    data class Resource(
        val sha256: String,
        /**
         * The file name in the project's resources folder
         */
        val resourceName: String,
        /**
         * The file name that appears in the application files
         */
        val fileName: String = resourceName,
        var uri: Uri = uriBuilder.path(fileName).build()
    ) {
        /**
         * Return a copy with a new file name and Uri
         */
        fun fileName(fileName: String): Resource {
            val copy = this.copy(fileName = fileName, uri = uriBuilder.path(fileName).build())
            return copy
        }

        companion object {
            private val uriBuilder = Uri.Builder().scheme("content").authority(
                TestContentProvider.AUTHORITY
            )
        }

        class Images {
            companion object {
                val BREAD = Resource(
                    "ad94e42e0323ccba436e70ebc3cbbfca6a54469c2058a782b5ee14c90ae637a4",
                    "pexels-pixabay-209206.jpg"
                )
                val DUMBBELLS_1 = Resource(
                    "586e73609a7c3ca0a3a9f4674010743b479af3022bf2dac7fce6061fc5c120bb",
                    "pexels-ivan-samkov-4164765.jpg"
                )
                val DUMBBELLS_2 = Resource(
                    "815f293f09b584afe5bfe306f4e2a0ba64ec4255baebd32a03c0ebd504c367e7",
                    "pexels-lee-catherine-collins-1371715-2652236.jpg"
                )
                val DUMBBELLS_3 = Resource(
                    "05f83ce1f08f935388eadcfbba9d5df5b217b30f97a8b4a246f4d53b819255d5",
                    "pexels-pixabay-260352.jpg"
                )
                val TOOTHBRUSH = Resource(
                    "cfdfde96f177a290da0c744019c038d7e62eb5a6056c8af3a23f3ee61bef4147",
                    "pexels-rrojasfoto-3588229.jpg"
                )
            }
        }

        class Documents {
            companion object {
                val SIMPLE_EVIDENCE = Resource(
                    "91269153f2affd820082fe45244cf37c9dffe82a137c55c895d388e15071f2a6",
                    "simple_evidence.txt"
                )
                val EVIDENCE_IMAGE = Resource(
                    "05c30323cc6442736cc7c6f06e78195e6aa198e8149979e792b54c3bd9e1d177",
                    "simple_receipt.jpg"
                )

                /**
                 * Note: PdfRenderer.pageCount returns 0 in Robolectric/Unit tests for some reason
                 */
                val EVIDENCE_PDF = Resource(
                    "3321b272185036b1a813b8ba50eab5610638d8d9761b78c03f14162032ef4f28",
                    "simple_receipt.pdf"
                )
                val EVIDENCE_PDF_MULTIPAGE = Resource(
                    "0f600c30a1de16cb32813edf62aea04330035ffa98fad1db5c123a20b5f8f82a",
                    "simple_receipt_multipage.pdf"
                )
                val EVIDENCE_PDF_PASSWORD_PROTECTED = Resource(
                    "d8330b5b1bc2114bb5b00a6d42b06b456ca33703bb4841b79cfd50d8264d9ef6",
                    "simple_receipt_protected.pdf"
                )
                val EVIDENCE_PDF_EMPTY = Resource(
                    "e95c2836f84a0b404fce20b5304ba6cece92567fc321998b6a5005c895aee668",
                    "empty_evidence.pdf"
                )
            }
        }
    }
}