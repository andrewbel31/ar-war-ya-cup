package com.andreibelous.arwar.view

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.andreibelous.arwar.R
import com.andreibelous.arwar.data.model.Session
import com.andreibelous.arwar.feature.Stage
import com.andreibelous.arwar.gone
import com.andreibelous.arwar.subscribe
import com.andreibelous.arwar.view.GameView.Event
import com.andreibelous.arwar.visible
import com.badoo.mvicore.modelWatcher
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.ObservableSource
import io.reactivex.functions.Consumer

class GameView(
    private val root: AppCompatActivity,
    private val getHeading: () -> Float?,
    private val events: PublishRelay<Event> = PublishRelay.create()
) : Consumer<GameViewModel>, ObservableSource<Event> by events {

    sealed interface Event {

        data class StartNewSession(val name: String) : Event
        data class ConnectToExistingSession(val name: String, val id: String) : Event
        object ShotClicked : Event
        object CloseClicked : Event
        object FinishGameClicked : Event
        object ShowInfoClicked : Event
    }

    private val miniMapView =
        root.findViewById<MiniMapView>(R.id.mini_map_view).apply {
            headingProvider = getHeading
        }

    private val gameOverlayView =
        root.findViewById<GameOverlayView>(R.id.game_overlay_view).apply {
            applyClickListener = { name: String, session: String ->
                if (session.isEmpty()) {
                    events.accept(Event.StartNewSession(name))
                } else {
                    events.accept(Event.ConnectToExistingSession(name, session))
                }
            }
        }

    private val labelSessionId = root.findViewById<TextView>(R.id.label_session_id)

    private val buttonClose = root.findViewById<View>(R.id.button_close).apply {
        setOnClickListener { events.accept(Event.CloseClicked) }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )
    }

    private val buttonInfo = root.findViewById<View>(R.id.button_info).apply {
        setOnClickListener { events.accept(Event.ShowInfoClicked) }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.WHITE),
            null,
            ShapeDrawable(OvalShape())
        )
    }

    private val progressView = root.findViewById<View>(R.id.progress_view)
    private val buttonFire = root.findViewById<Button>(R.id.fire_button).apply {
        setOnClickListener {
            events.accept(Event.ShotClicked)
        }
    }

    private var dialog: AlertDialog? = null

    init {
        root.lifecycle.subscribe {
            dismissDialog()
        }
    }


    override fun accept(vm: GameViewModel) {
        modelWatcher(vm)
    }

    private val modelWatcher = modelWatcher<GameViewModel> {
        watch(GameViewModel::isLoading) { isLoading ->
            progressView.isVisible = isLoading
        }

        watch({ it }) {
            val stage = it.stage
            gameOverlayView.accept(stage.toGameOverlayModel())
            if (stage is Stage.InProgress && stage.isMeAlive) {
                val sessionId = it.session?.id
                val label =
                    if (sessionId != null) {
                        "Game ID - ${it.session.id}"
                    } else {
                        ""
                    }
                labelSessionId.text = label
                labelSessionId.visible()
                buttonFire.visible()
            } else {
                labelSessionId.gone()
                labelSessionId.text = ""
                buttonFire.gone()
            }
        }
    }

    private fun Stage.toGameOverlayModel(): GameOverlayModel =
        when (val stage = this) {
            is Stage.NoSession -> GameOverlayModel.Init
            is Stage.InProgress -> GameOverlayModel.InProgress(isMeAlive = stage.isMeAlive)
            is Stage.Finished -> GameOverlayModel.Finished(winner = stage.winner, isMe = stage.isMe)
        }

    fun executeAction(action: Action) {
        when (action) {
            is Action.ShowConfirmCloseDialog -> showConfirmCloseDialog()
            is Action.Close -> root.finish()
            is Action.HandleError -> {
                Toast.makeText(
                    root,
                    "Error = ${action.throwable}",
                    Toast.LENGTH_LONG
                ).show()
            }
            is Action.ShowInfoDialog -> showInfoDialog(action.session)
        }
    }

    private fun showInfoDialog(session: Session?) {
        dismissDialog()
        AlertDialog.Builder(root)
            .setTitle("Session info")
            .setMessage(session.toText())
            .setPositiveButton("закрыть") { _, _ -> }
            .setCancelable(true)
            .create()
            .also { dialog = it }
            .show()
    }

    private fun Session?.toText(): String {
        if (this == null) {
            return "Нет активной сессии"
        }

        val sb = StringBuilder()

        sb.append("ID Сессии: $id")
            .append("\n")
            .append("Количество игроков: ${players.size}")
            .append("\n")
            .append("\n")

        for (player in players) {
            sb.append("Игрок - ${player.name}")
                .append("\n")
                .append("id = ${player.id}")
                .append("\n")
                .append("alive = ${player.alive}")
                .append("\n")
                .append("location = ${player.location}")
                .append("\n")
                .append("\n")
        }

        sb.append("\n")

        return sb.toString()
    }

    private fun showConfirmCloseDialog() {
        dismissDialog()
        AlertDialog.Builder(root)
            .setTitle("Игра в процессе")
            .setMessage("Закрытие игры - проигрышь")
            .setPositiveButton("закрыть") { _, _ ->
                events.accept(Event.FinishGameClicked)
                root.finish()
                dismissDialog()
            }
            .setNegativeButton("отмена") { _, _ ->
                dismissDialog()
            }
            .setCancelable(true)
            .create()
            .also { dialog = it }
            .show()
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    sealed interface Action {

        object ShowConfirmCloseDialog : Action
        object Close : Action
        data class HandleError(val throwable: Throwable) : Action
        data class ShowInfoDialog(val session: Session?) : Action
    }
}

data class GameViewModel(
    val session: Session?,
    val isLoading: Boolean,
    val stage: Stage
)