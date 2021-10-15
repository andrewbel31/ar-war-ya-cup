package com.andreibelous.arwar.mapper

import com.andreibelous.arwar.feature.GameFeature
import com.andreibelous.arwar.view.GameView

object UiEventToWish : (GameView.Event) -> GameFeature.Wish? {

    override fun invoke(event: GameView.Event): GameFeature.Wish =
        when (event) {
            is GameView.Event.ConnectToExistingSession ->
                GameFeature.Wish.ConnectToExistingSession(event.name, event.id)
            is GameView.Event.StartNewSession ->
                GameFeature.Wish.StartNewSession(event.name)
            is GameView.Event.ShotClicked -> GameFeature.Wish.Shot
            is GameView.Event.CloseClicked -> GameFeature.Wish.HandleCloseClicked
            is GameView.Event.FinishGameClicked -> GameFeature.Wish.FinishMyGame
        }
}