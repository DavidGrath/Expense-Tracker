package com.davidgrath.expensetracker

import android.net.Uri

class TestData {
    companion object {
        data class Images(
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
            fun fileName(fileName: String): Images {
                val copy = this.copy(fileName = fileName, uri = uriBuilder.path(fileName).build())
                return copy
            }
            companion object {
                private val uriBuilder = Uri.Builder().scheme("content").authority(TestContentProvider.AUTHORITY)
                val BREAD = Images("ad94e42e0323ccba436e70ebc3cbbfca6a54469c2058a782b5ee14c90ae637a4", "pexels-pixabay-209206.jpg")
                val DUMBBELLS_1 = Images("586e73609a7c3ca0a3a9f4674010743b479af3022bf2dac7fce6061fc5c120bb", "pexels-ivan-samkov-4164765.jpg")
                val DUMBBELLS_2 = Images("815f293f09b584afe5bfe306f4e2a0ba64ec4255baebd32a03c0ebd504c367e7", "pexels-lee-catherine-collins-1371715-2652236.jpg")
                val DUMBBELLS_3 = Images("05f83ce1f08f935388eadcfbba9d5df5b217b30f97a8b4a246f4d53b819255d5", "pexels-pixabay-260352.jpg")
                val TOOTHBRUSH = Images("cfdfde96f177a290da0c744019c038d7e62eb5a6056c8af3a23f3ee61bef4147", "pexels-rrojasfoto-3588229.jpg")
            }
        }
    }
}