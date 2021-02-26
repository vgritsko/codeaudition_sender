package com.codeaudition.sender.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.codeaudition.sender.MainActivity
import com.codeaudition.sender.R
import com.codeaudition.sender.utils.storage.FirebaseStorage
import com.codeaudition.sender.utils.storage.hawkSetServiceSubscribedLocationUpdates
import com.google.android.gms.location.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LocationService : Service() {


    private val localBinder = LocationServiceBinder()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var notificationManager: NotificationManager

    private var configurationChange = false
    private var isRunningInForeground = false


    override fun onCreate() {
        super.onCreate()
        Timber.d("Service OnCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {

            interval = TimeUnit.SECONDS.toMillis(100)
            fastestInterval = TimeUnit.SECONDS.toMillis(50)
            maxWaitTime = TimeUnit.MINUTES.toMillis(5)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                if (locationResult?.lastLocation != null) {
                    Timber.d(
                        "Location: %f %f",
                        locationResult.lastLocation.longitude,
                        locationResult.lastLocation.latitude
                    )
                    FirebaseStorage.write(locationResult.lastLocation)
                    if (isRunningInForeground) {
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildNotification()
                        )
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("Service OnStartCommand")
        val cancelLocationTracking =
            intent.getBooleanExtra(CANCEL_LOCATION, false)

        if (cancelLocationTracking) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        Timber.d("OnBind")
        stopForeground(true)
        isRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        stopForeground(true)
        isRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("Service OnUnbind")

        if (!configurationChange) {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            isRunningInForeground = true
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Timber.d("Subscribe on Location updates")
        hawkSetServiceSubscribedLocationUpdates(true)
        startService(Intent(applicationContext, LocationService::class.java))

        try {

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            hawkSetServiceSubscribedLocationUpdates(false)
        }
    }

    fun unsubscribeToLocationUpdates() {
        Timber.d("Unsubscribe on Location updates")
        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    stopSelf()
                }
            }
            hawkSetServiceSubscribedLocationUpdates(false)
        } catch (unlikely: SecurityException) {
            hawkSetServiceSubscribedLocationUpdates(true)
        }
    }


    private fun buildNotification(): Notification {
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, LocationService::class.java)
        cancelIntent.putExtra(CANCEL_LOCATION, true)


        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )


        val mainNotificationText = "location service"
        val titleText = getString(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }


        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)


        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_launcher_background, "Go to Activity",
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop location updates",
                servicePendingIntent
            )
            .build()
    }

    inner class LocationServiceBinder : Binder() {
        internal val service: LocationService
            get() = this@LocationService
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "location_service_channel"
        private const val CANCEL_LOCATION = "CANCEL_LOCATION"
    }

}