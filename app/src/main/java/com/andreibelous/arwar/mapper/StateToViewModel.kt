package com.andreibelous.arwar.mapper

import com.andreibelous.arwar.feature.GameState
import com.andreibelous.arwar.view.GameViewModel

object StateToViewModel : (GameState) -> GameViewModel {

    override fun invoke(state: GameState): GameViewModel =
        GameViewModel(
            session = state.session,
            isLoading = state.isLoading,
            stage = state.stage
        )
}