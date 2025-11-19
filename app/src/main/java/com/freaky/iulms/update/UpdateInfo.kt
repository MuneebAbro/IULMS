package com.freaky.iulms.update

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the version information fetched from the remote JSON file.
 */
data class UpdateInfo(
    @SerializedName("latestVersion")
    val latestVersion: String,

    @SerializedName("apkUrl")
    val apkUrl: String,

    @SerializedName("forceUpdate")
    val forceUpdate: Boolean
)