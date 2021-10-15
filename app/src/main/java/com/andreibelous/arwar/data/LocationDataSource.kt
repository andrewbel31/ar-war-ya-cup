package com.andreibelous.arwar.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.andreibelous.arwar.cast
import com.andreibelous.arwar.data.model.Location
import com.andreibelous.arwar.plusAssign
import com.andreibelous.arwar.subscribe
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

class LocationDataSource(
    private val activity: AppCompatActivity
) : Disposable {

    private val _locationUpdates = BehaviorRelay.createDefault<Optional<Location>>(Optional.empty())
    val locationUpdates: Observable<Optional<Location>> = _locationUpdates

    val location: Location?
        get() {
            val value = _locationUpdates.value
            return if (value?.isPresent == true) {
                value.get()
            } else {
                null
            }
        }

    private val locationClient = LocationServices.getFusedLocationProviderClient(activity)
    private val settingsClient = LocationServices.getSettingsClient(activity)
    private val locationRequest =
        LocationRequest()
            .apply {
                interval = UPDATE_INTERVAL_MILLIS
                fastestInterval = FASTEST_UPDATE_INTERVAL_MILLIS
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
    private val locationSettingsRequest =
        LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

    private val disposables = CompositeDisposable()

    init {
        activity.lifecycle.subscribe(
            onStart = { startUpdates() },
            onStop = { stopUpdates() }
        )
    }

    private fun startUpdates() {
        disposables +=
            checkLocationSettings()
                .andThen(requestLocation())
                .subscribe()
    }

    private fun stopUpdates() {
        disposables.clear()
    }

    private fun checkLocationSettings(): Completable =
        Completable.create { source ->
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener { source.onComplete() }
                .addOnFailureListener { exception ->
                    source.onError(exception)
                    when (val statusCode = (exception.cast<ApiException>()).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val rae = exception as ResolvableApiException
                                rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                            } catch (sie: SendIntentException) {

                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> activity.finish()
                    }
                }
        }

    @SuppressLint("MissingPermission")
    private fun requestLocation(): Observable<Location> =
        Observable.create { _ ->
            val locationCallback = object : LocationCallback() {

                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    _locationUpdates.accept(
                        Optional.of(
                            Location(
                                location.latitude,
                                location.longitude
                            )
                        )
                    )
                }
            }

            locationClient
                .requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
        }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS ->
                when (resultCode) {
                    Activity.RESULT_OK -> Unit
                    Activity.RESULT_CANCELED -> activity.finish()
                }
        }
    }

    override fun dispose() {
        disposables.dispose()
    }

    override fun isDisposed(): Boolean = disposables.isDisposed

    private companion object {

        private const val UPDATE_INTERVAL_MILLIS = 1000L
        private const val FASTEST_UPDATE_INTERVAL_MILLIS = 500L
        private const val REQUEST_CHECK_SETTINGS = 1001
    }
}