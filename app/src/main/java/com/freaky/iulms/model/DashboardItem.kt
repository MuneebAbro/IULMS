package com.freaky.iulms.model

import androidx.annotation.DrawableRes

data class DashboardItem(
    val title: String, // The text to display in the UI
    val dataKey: String, // The key used to fetch data from the map
    @DrawableRes val iconResId: Int,
    val destinationActivity: Class<*>
)