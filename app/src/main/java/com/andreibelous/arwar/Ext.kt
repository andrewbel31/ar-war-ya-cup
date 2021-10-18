package com.andreibelous.arwar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.andreibelous.arwar.data.model.Location
import com.andreibelous.arwar.data.model.Player
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlin.math.*

inline fun <reified T> Any.cast() = this as T

inline fun <reified T> T.toObservable() = Observable.just(this)

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    this.add(disposable)
}

fun Lifecycle.subscribe(
    onCreate: (() -> Unit)? = null,
    onStart: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onDestroy: (() -> Unit)? = null
) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onCreate?.invoke()
        }

        override fun onStart(owner: LifecycleOwner) {
            onStart?.invoke()
        }

        override fun onResume(owner: LifecycleOwner) {
            onResume?.invoke()
        }

        override fun onPause(owner: LifecycleOwner) {
            onPause?.invoke()
        }

        override fun onStop(owner: LifecycleOwner) {
            onStop?.invoke()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onDestroy?.invoke()
        }
    })
}

fun Drawable.toBitmap(): Bitmap? {
    if (this is BitmapDrawable) {
        val bitmapDrawable = this
        if (bitmapDrawable.bitmap != null) {
            return bitmapDrawable.bitmap
        }
    }
    var bitmap: Bitmap? = null
    bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(
            this.intrinsicWidth,
            this.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}

fun Context.dp(value: Float): Float {
    return if (value == 0f) {
        0f
    } else ceil(resources.displayMetrics.density * value)
}

abstract class DefaultTextWatcher : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {

    }

}

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun findPlayerInMap(
    heading: Float?,
    myId: Int?,
    players: List<Player>?,
    epsilon: Double
): Player? {
    heading ?: return null
    val me = players?.firstOrNull { it.id == myId } ?: return null

    players.forEach { other ->
        val myLoc = me.location ?: return null
        val otherLoc = other.location ?: return null
        if (other != me) {
            val diff =
                getDistanceFromLatLonInMeters(
                    lat1 = myLoc.lat,
                    lat2 = otherLoc.lat,
                    lon1 = myLoc.lng,
                    lon2 = otherLoc.lng
                )

            val dx = diff * cos(heading) / 111_111
            val dy = diff * sin(heading) / 111_111

            val newPoint =
                Location(
                    lat = myLoc.lat + dy,
                    lng = myLoc.lng + dx
                )

            if (
                abs(otherLoc.lat - newPoint.lat) <= epsilon
                && abs(otherLoc.lng - newPoint.lng) <= epsilon
            ) {
                return other
            }
        }
    }

    return null
}

private fun getDistanceFromLatLonInMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val R = 6371; // Radius of the earth in km
    val dLat = deg2rad(lat2 - lat1)
    val dLon = deg2rad(lon2 - lon1)
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
                cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a));
    return R * c * 1000
}

fun deg2rad(deg: Double): Double {
    return deg * (Math.PI / 180)
}

inline fun <reified T> ((T) -> Unit).asConsumer(): Consumer<T> =
    Consumer { t -> this@asConsumer.invoke(t) }