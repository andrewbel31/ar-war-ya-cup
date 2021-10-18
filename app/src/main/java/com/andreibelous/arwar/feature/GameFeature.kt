package com.andreibelous.arwar.feature

import com.andreibelous.arwar.OrientationDataSource
import com.andreibelous.arwar.RandomGenerator
import com.andreibelous.arwar.data.LocationDataSource
import com.andreibelous.arwar.data.SessionDataSource
import com.andreibelous.arwar.data.model.Location
import com.andreibelous.arwar.data.model.Player
import com.andreibelous.arwar.data.model.Session
import com.andreibelous.arwar.feature.GameFeature.News
import com.andreibelous.arwar.feature.GameFeature.Wish
import com.andreibelous.arwar.findPlayerInMap
import com.andreibelous.arwar.toObservable
import com.badoo.mvicore.element.Actor
import com.badoo.mvicore.element.Bootstrapper
import com.badoo.mvicore.element.NewsPublisher
import com.badoo.mvicore.element.Reducer
import com.badoo.mvicore.feature.BaseFeature
import com.badoo.mvicore.feature.Feature
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class GameFeature(
    private val sessionDataSource: SessionDataSource,
    private val locationDataSource: LocationDataSource,
    private val orientationDataSource: OrientationDataSource
) : Feature<Wish, GameState, News> by BaseFeature(
    initialState = GameState(),
    bootstrapper = BootstrapperImpl(locationDataSource),
    wishToAction = Action::ExecuteWish,
    actor = ActorImpl(sessionDataSource, locationDataSource, orientationDataSource),
    reducer = ReducerImpl(),
    newsPublisher = NewsPublisherImpl()
) {

    sealed interface Wish {

        data class StartNewSession(val userName: String) : Wish
        data class ConnectToExistingSession(val userName: String, val sessionId: String) : Wish
        object Shot : Wish
        object HandleCloseClicked : Wish
        object FinishMyGame : Wish
    }

    sealed interface News {

        object ConfirmCloseRequested : News
        object Finish : News
        data class ErrorHappened(val throwable: Throwable) : News
    }

    private sealed interface Action {

        data class ExecuteWish(val wish: Wish) : Action
        data class HandleLocationUpdated(val location: Location) : Action
    }

    private sealed interface Effect {

        data class SessionUpdated(
            val session: Session,
            val stage: Stage
        ) : Effect

        object LoadingStarted : Effect
        data class MyIdUpdated(val id: Int) : Effect
        object ConfirmCloseRequested : Effect
        object Finish : Effect
        data class ErrorHappened(val throwable: Throwable) : Effect
    }

    private class ActorImpl(
        private val sessionDataSource: SessionDataSource,
        private val locationDataSource: LocationDataSource,
        private val orientationDataSource: OrientationDataSource
    ) : Actor<GameState, Action, Effect> {

        override fun invoke(state: GameState, action: Action): Observable<out Effect> =
            when (action) {
                is Action.HandleLocationUpdated -> updateMyLocation(state, action.location)
                is Action.ExecuteWish -> executeWish(state, action.wish)
            }.onErrorReturn { Effect.ErrorHappened(it) }
                .observeOn(AndroidSchedulers.mainThread())

        private fun updateMyLocation(state: GameState, location: Location): Observable<Effect> =
            if (state.session != null && state.myId != null) {
                sessionDataSource
                    .updateLocation(state.session.id, state.myId, location)
                    .toObservable()
            } else {
                Observable.empty()
            }

        private fun executeWish(state: GameState, wish: Wish): Observable<Effect> =
            when (wish) {
                is Wish.StartNewSession -> startNewSession(wish)
                is Wish.ConnectToExistingSession -> connectToExistingSession(wish)
                is Wish.Shot -> tryShot(state)
                is Wish.HandleCloseClicked ->
                    if (state.stage is Stage.InProgress && state.stage.isMeAlive) {
                        Effect.ConfirmCloseRequested.toObservable()
                    } else {
                        Effect.Finish.toObservable()
                    }
                is Wish.FinishMyGame -> {
                    if (state.session?.id != null && state.myId != null) {
                        sessionDataSource.killPlayer(state.session.id.toString(), state.myId)
                            .andThen(Effect.Finish.toObservable())
                    } else {
                        Observable.empty()
                    }
                }

            }

        private fun startNewSession(
            wish: Wish.StartNewSession
        ): Observable<Effect> {
            val myId = RandomGenerator.generateUserId()
            val sessionId = RandomGenerator.generateSessionId()
            val me =
                Player(
                    id = myId,
                    name = wish.userName,
                    location = locationDataSource.location,
                    alive = true
                )
            val session =
                Session(
                    id = sessionId,
                    players = listOf(me),
                    active = true,
                )

            return Observable
                .concat(
                    Effect.LoadingStarted.toObservable(),
                    Effect.MyIdUpdated(myId).toObservable(),
                    sessionDataSource.updateSession(session).andThen(Observable.empty()),
                    sessionDataSource.sessionUpdates(sessionId.toString())
                        .map { Effect.SessionUpdated(it, it.fetchStage(myId)) }
                )
        }

        private fun Session.fetchStage(myId: Int?): Stage {
            val players = players
            val alivePlayers = players.filter { it.alive }
            val alivePlayersSize = alivePlayers.size

            return when {
                active -> Stage.InProgress(
                    isMeAlive = myId != null && myId in alivePlayers.toSet().map { it.id })
                players.size > 1 && alivePlayersSize == 1 ->
                    Stage.Finished(
                        winner = alivePlayers[0],
                        isMe = alivePlayers[0].id == myId
                    )
                else -> Stage.Finished(null, false)
            }
        }

        private fun connectToExistingSession(
            wish: Wish.ConnectToExistingSession
        ): Observable<Effect> {
            val myId = RandomGenerator.generateUserId()
            val newPlayer =
                Player(
                    id = myId,
                    name = wish.userName,
                    location = locationDataSource.location,
                    alive = true
                )

            return Observable.concat(
                Effect.MyIdUpdated(myId).toObservable(),
                Effect.LoadingStarted.toObservable(),
                sessionDataSource
                    .updatePlayer(wish.sessionId, newPlayer)
                    .andThen(Observable.empty()),
                sessionDataSource
                    .sessionUpdates(wish.sessionId)
                    .map { Effect.SessionUpdated(it, it.fetchStage(myId)) },
            )
        }

        private fun tryShot(state: GameState): Observable<Effect> {
            val currentAngle = orientationDataSource.heading ?: return Observable.empty()
            val playerToShot =
                findPlayerInMap(
                    heading = currentAngle,
                    myId = state.myId,
                    players = state.session?.players,
                    epsilon = 0.001
                )
            return if (playerToShot != null && state.session?.id != null) {
                sessionDataSource
                    .updatePlayer(state.session.id.toString(), playerToShot.copy(alive = false))
                    .andThen(Observable.empty())
            } else {
                Observable.empty()
            }
        }
    }

    private class ReducerImpl() : Reducer<GameState, Effect> {

        override fun invoke(state: GameState, effect: Effect): GameState =
            when (effect) {
                is Effect.SessionUpdated ->
                    state.copy(
                        session = effect.session,
                        isLoading = false,
                        stage = effect.stage
                    )
                is Effect.MyIdUpdated -> state.copy(myId = effect.id)
                is Effect.LoadingStarted -> state.copy(isLoading = true)
                is Effect.ConfirmCloseRequested,
                is Effect.Finish,
                is Effect.ErrorHappened -> state
            }
    }

    private class BootstrapperImpl(
        private val locationDataSource: LocationDataSource
    ) : Bootstrapper<Action> {

        override fun invoke(): Observable<Action> =
            locationDataSource
                .locationUpdates
                .filter { it.isPresent }
                .map { Action.HandleLocationUpdated(it.get()) }
    }

    private class NewsPublisherImpl : NewsPublisher<Action, Effect, GameState, News> {

        override fun invoke(action: Action, effect: Effect, state: GameState): News? =
            when (effect) {
                is Effect.ConfirmCloseRequested -> News.ConfirmCloseRequested
                is Effect.Finish -> News.Finish
                is Effect.ErrorHappened -> News.ErrorHappened(effect.throwable)
                else -> null
            }
    }
}