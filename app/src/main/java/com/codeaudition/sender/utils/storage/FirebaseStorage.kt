package com.codeaudition.sender.utils.storage

import android.location.Location
import com.codeaudition.sender.data.FirebaseLocation
import com.codeaudition.sender.utils.extensions.DateExtensions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

object FirebaseStorage {
    private val date = DateExtensions.appDateFormat.format(Date(System.currentTimeMillis()))
    private val database = Firebase.database.getReference("locations/$date")

    fun write(location: Location) {
        val timeStamp = System.currentTimeMillis()

        with(database) {
            val fireBaseLocation = FirebaseLocation(location.latitude, location.longitude)
            child(timeStamp.toString()).setValue(fireBaseLocation)
        }
    }
}