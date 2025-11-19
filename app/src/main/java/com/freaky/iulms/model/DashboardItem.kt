package com.freaky.iulms.model

import androidx.annotation.DrawableRes

data class DashboardItem(
    val title: String,
    val urlToFetch: String, // The direct URL for the data
    @DrawableRes val iconResId: Int,
    val destinationActivity: Class<*>
)