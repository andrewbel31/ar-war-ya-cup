package com.andreibelous.arwar.view

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.andreibelous.arwar.*
import com.andreibelous.arwar.data.model.Player
import com.badoo.mvicore.modelWatcher
import io.reactivex.functions.Consumer

class GameOverlayView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Consumer<GameOverlayModel> {

    var applyClickListener: ((String, String) -> Unit)? = null

    init {
        inflate(context, R.layout.layout_game_overlay, this)
        layoutTransition = LayoutTransition()
        isClickable = true
        isFocusable = true
    }

    private var dialog: AlertDialog? = null

    private val inputCard = findViewById<View>(R.id.user_data_card).apply {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {

            override fun getOutline(view: View, outline: Outline) {
                val radius = context.dp(24f)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }

        setBackgroundColor(Color.WHITE)
    }

    private val buttonAction = findViewById<TextView>(R.id.label_action).apply {
        val radii = context.dp(24f)
        val stroke = context.dp(2f)
        val radiiArr = floatArrayOf(radii, radii, radii, radii, radii, radii, radii, radii)
        background =
            RippleDrawable(
                ColorStateList.valueOf(Color.BLACK),
                GradientDrawable().apply {
                    setStroke(stroke.toInt(), Color.GRAY)
                    cornerRadii = radiiArr
                },
                ShapeDrawable(RoundRectShape(radiiArr, null, null))
            )
    }

    private fun fieldsAreValid(name: String, session: String): Boolean {
        if (name.length < 3) {
            return false
        }

        if (session.isNotEmpty() && session.length != 6) {
            return false
        }

        return true
    }

    private val editName = findViewById<EditText>(R.id.edit_name)
    private val editSession = findViewById<EditText>(R.id.edit_session_id)
    private val textCongrats = findViewById<TextView>(R.id.label_congrats)

    init {
        editSession.addTextChangedListener(object : DefaultTextWatcher() {

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()

                if (text.isNotEmpty()) {
                    buttonAction.text = "Присоединиться"
                } else {
                    buttonAction.text = "Создать новую"
                }
            }
        })

        buttonAction.setOnClickListener {
            val name = editName.text.toString()
            val session = editSession.text.toString()
            if (fieldsAreValid(name, session)) {
                applyClickListener?.invoke(name, session)
            } else {
                dialog?.dismiss()
                AlertDialog.Builder(context)
                    .setTitle("Неверные данные")
                    .setMessage("Имя должно быть от 3х до 10 знаков, а сессия ровно 6 цифр")
                    .setPositiveButton("OK") { _, _ ->
                        dismissDialog()
                    }
                    .setCancelable(true)
                    .create()
                    .also { dialog = it }
                    .show()
            }
        }
    }

    override fun accept(viewModel: GameOverlayModel) {
        modelWatcher(viewModel)
    }

    private val modelWatcher = modelWatcher<GameOverlayModel> {
        type<GameOverlayModel.Init> {
            watch({ it }) {
                hideKeyboard()
                inputCard.visible()
                setOverlayColor(Color.BLACK)
            }
        }

        type<GameOverlayModel.InProgress> {
            watch({ it }) { inProgress ->
                inputCard.gone()
                hideKeyboard()
                if (inProgress.isMeAlive) {
                    setBackgroundColor(0)
                } else {
                    setOverlayColor(Color.RED)
                }
            }
        }

        type<GameOverlayModel.Finished> {
            watch({ it }) { finished ->
                inputCard.visible()
                hideKeyboard()
                if (finished.isMe) {
                    setOverlayColor(Color.GREEN)
                    textCongrats.text = "Поздравляем с победой!"
                } else {
                    setOverlayColor(Color.RED)
                    textCongrats.text = "Повезет еще"
                }

            }
        }
    }

    private fun setOverlayColor(@ColorInt color: Int, alpha: Float = 0.3f) {
        setBackgroundColor(ColorUtils.setAlphaComponent(color, (255 * alpha).toInt()))
    }

    override fun onDetachedFromWindow() {
        dismissDialog()
        super.onDetachedFromWindow()
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }
}

sealed interface GameOverlayModel {

    object Init : GameOverlayModel
    data class InProgress(val isMeAlive: Boolean) : GameOverlayModel
    data class Finished(
        val winner: Player?,
        val isMe: Boolean
    ) : GameOverlayModel
}