package com.codeaudition.sender

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.viewbinding.BuildConfig
import com.codeaudition.sender.databinding.ActivityMainBinding
import com.codeaudition.sender.services.LocationService
import com.codeaudition.sender.utils.storage.hawkGetServiceSubscribedLocationUpdates
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var locationService: LocationService? = null
    private var locationServiceBound = false


    private val locationServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocationServiceBinder
            locationService = binder.service
            locationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationService = null
            locationServiceBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupUI()
    }


    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, LocationService::class.java)
        bindService(serviceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE)
    }


    override fun onStop() {
        if (locationServiceBound) {
            unbindService(locationServiceConnection)
            locationServiceBound = false
        }
        super.onStop()
    }


    private fun setupUI() {
        binding.startLocationButton.setOnClickListener {
            val isSubscribed = hawkGetServiceSubscribedLocationUpdates()
            if (isSubscribed) {
                locationService?.unsubscribeToLocationUpdates()
            } else {
                if (permissionApproved()) {
                    locationService?.subscribeToLocationUpdates()
                } else {
                    requestPermissions()
                }
            }
        }
    }

    private fun requestPermissions() {
        val isPermissionsApproved = permissionApproved()

        if (isPermissionsApproved) {
            Snackbar.make(binding.root, "Approve permissions?", Snackbar.LENGTH_LONG)
                .setAction("Ok") {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {

            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> when (PackageManager.PERMISSION_GRANTED
            ) {
                grantResults[0] -> locationService?.subscribeToLocationUpdates()
                else -> {
                    Snackbar.make(binding.root, "permissions denied", Snackbar.LENGTH_SHORT)
                        .setAction("Test") {

                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.LIBRARY_PACKAGE_NAME,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun permissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 34
    }
}