package com.codeaudition.sender.utils.extensions

import java.text.SimpleDateFormat
import java.util.*


fun Date.currentDate(dateFormat: SimpleDateFormat): String =
    dateFormat.format(Date(System.currentTimeMillis()))

object DateExtensions {
    val appDateFormat: SimpleDateFormat get() = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val appTimeFormat: SimpleDateFormat
        get() = SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss.SSS a zzz",
            Locale.getDefault()
        )
}
