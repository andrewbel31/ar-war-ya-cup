package com.andreibelous.arwar.mapper

import com.andreibelous.arwar.feature.GameFeature
import com.andreibelous.arwar.view.GameView

object NewsToViewAction : (GameFeature.News) -> GameView.Action {

    override fun invoke(news: GameFeature.News): GameView.Action =
        when (news) {
            is GameFeature.News.ConfirmCloseRequested -> GameView.Action.ShowConfirmCloseDialog
            is GameFeature.News.Finish -> GameView.Action.Close
            is GameFeature.News.ErrorHappened -> GameView.Action.HandleError(news.throwable)
        }
}