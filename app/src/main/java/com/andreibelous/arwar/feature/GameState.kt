package com.andreibelous.arwar.feature

import com.andreibelous.arwar.data.model.Player
import com.andreibelous.arwar.data.model.Session

data class GameState(
    val session: Session? = null,
    val myId: Int? = null,
    val stage: Stage = Stage.NoSession,
    val isLoading: Boolean = false
)

sealed interface Stage {

    object NoSession : Stage
    data class InProgress(val isMeAlive: Boolean) : Stage
    data class Finished(
        val winner: Player?,
        val isMe: Boolean
    ) : Stage
}