package com.andreibelous.arwar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.andreibelous.arwar.data.LocationDataSource
import com.andreibelous.arwar.data.SessionDataSource
import com.andreibelous.arwar.feature.GameFeature
import com.andreibelous.arwar.mapper.NewsToViewAction
import com.andreibelous.arwar.mapper.StateToViewModel
import com.andreibelous.arwar.mapper.UiEventToWish
import com.andreibelous.arwar.view.GameView
import com.badoo.binder.Binder
import com.badoo.binder.using
import com.badoo.mvicore.android.lifecycle.CreateDestroyBinderLifecycle
import io.reactivex.disposables.CompositeDisposable

class CameraActivityDelegate(
    private val activity: AppCompatActivity,
    private val permissionGrantedCallback: () -> Unit
) {

    private val disposables = CompositeDisposable()
    private val locationDataSource by lazy { LocationDataSource(activity) }
    private val orientationDataSource by lazy { OrientationDataSource(activity) { locationDataSource.location } }
    private val sessionDataSource = SessionDataSource()
    private val lifecycle = activity.lifecycle.apply {
        subscribe(
            onDestroy = {
                disposables.dispose()
                dismissDialog()
            }
        )
    }
    private var gameFeature: GameFeature? = null
    private var dialog: AlertDialog? = null
    private val permissions =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

    init {

        val wm = activity.getSystemService(Context.WINDOW_SERVICE)?.cast<WindowManager>()
        orientationDataSource.currentScreenOrientation = wm?.defaultDisplay?.orientation ?: 0

        val feature = GameFeature(sessionDataSource, locationDataSource, orientationDataSource)
            .also { gameFeature = it }
        val view = GameView(activity, { orientationDataSource.heading })

        Binder(CreateDestroyBinderLifecycle(lifecycle)).apply {
            bind(view to feature using UiEventToWish)
            bind(feature to view using StateToViewModel)
            bind(feature.news to view::executeAction.asConsumer() using NewsToViewAction)
        }

        disposables += feature
        disposables += locationDataSource

        if (!hasPermissions(permissions)) {
            startRequesting()
        } else {
            permissionGrantedCallback.invoke()
        }
    }

    private fun startRequesting() {
        val shouldShowRationale =
            permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

        if (shouldShowRationale) {
            dialog?.dismiss()
            AlertDialog.Builder(activity)
                .setTitle("Нет необходимых разрешений")
                .setMessage("Без этих разрешений приложение не сможет работать :(")
                .setPositiveButton("дать разрешения") { _, _ ->
                    dismissDialog()
                    requestPermissions()
                }
                .setNegativeButton("отмена") { _, _ ->
                    dismissDialog()
                    activity.finish()
                }
                .setCancelable(true)
                .create()
                .also { dialog = it }
                .show()

            return
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST_CODE)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = mutableSetOf<String>()
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    granted += permission
                }
            }
        }

        if (granted.containsAll(permissions.toList())) {
            permissionGrantedCallback.invoke()
        } else {
            activity.finish()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat
                .checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        locationDataSource.onActivityResult(requestCode, resultCode, data)
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    // runs in bg thread
    fun tryGuessPlayerName(): String? {
        val state = gameFeature?.state ?: return null
        val visiblePlayer =
            findPlayerInMap(
                heading = orientationDataSource.heading,
                myId = state.myId,
                players = state.session?.players,
                epsilon = 0.0001
            )
        return visiblePlayer?.name
    }

    private companion object {

        private const val PERMISSIONS_REQUEST_CODE = 101
    }
}