package com.codeaudition.sender.utils.storage

import com.orhanobut.hawk.Hawk

const val KEY_SUBSCRIBED = "location_service"


public fun hawkSetServiceSubscribedLocationUpdates(isSubscribed:Boolean): Boolean {
    return Hawk.put( KEY_SUBSCRIBED, isSubscribed)
}

public fun hawkGetServiceSubscribedLocationUpdates(): Boolean {
    return Hawk.get( KEY_SUBSCRIBED, false)
}

