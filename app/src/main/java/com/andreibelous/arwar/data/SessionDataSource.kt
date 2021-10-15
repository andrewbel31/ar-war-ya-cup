package com.andreibelous.arwar.data

import com.andreibelous.arwar.cast
import com.andreibelous.arwar.data.model.Location
import com.andreibelous.arwar.data.model.Player
import com.andreibelous.arwar.data.model.Session
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.reactivex.Completable
import io.reactivex.Observable

class SessionDataSource {

    private val dataBase = Firebase.database

    fun sessionUpdates(id: String): Observable<Session> =
        Observable.create { emitter ->
            dataBase.getReference(id)
                .addValueEventListener(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {
                                val sessionId = snapshot.key!!.toInt()
                                val active =
                                    snapshot.child("active").value?.cast<Boolean>() ?: false

                                val playersSnapShot = snapshot.child("players")
                                val players = mutableListOf<Player>()
                                for (player in playersSnapShot.children) {
                                    val name = player.child("name").value!!.cast<String>()
                                    val playerId = player.child("id").value!!.cast<Long>().toInt()
                                    val alive = player.child("alive").value!!.cast<Boolean>()
                                    val locationSnapshot = player.child("location")
                                    val lat = locationSnapshot
                                        .child("lat").value?.cast<Double>()
                                        ?.toDouble() ?: 0.0
                                    val lng = locationSnapshot
                                        .child("lng").value?.cast<Double>()
                                        ?.toDouble() ?: 0.0

                                    players.add(
                                        Player(
                                            id = playerId,
                                            name = name,
                                            alive = alive,
                                            location = Location(lat, lng)
                                        )
                                    )
                                }


                                emitter.onNext(
                                    Session(
                                        id = sessionId,
                                        players = players,
                                        active = active
                                    )
                                )
                            } catch (e: Exception) {
                                emitter.onError(e)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            emitter.onError(error.toException())
                        }
                    }
                )
        }

    fun updateLocation(sessionId: Int, playerId: Int, location: Location) =
        Completable.fromAction {
            val ref = dataBase.getReference("$sessionId/players/$playerId/location")
            ref.setValue(location)
        }

    fun updatePlayer(sessionId: String, player: Player) =
        Completable.fromAction {
            val ref = dataBase.getReference("$sessionId/players/${player.id}")
            ref.setValue(player)
        }

    fun killPlayer(sessionId: String, playerId: Int) =
        Completable.fromAction {
            val ref = dataBase.getReference("$sessionId/${playerId}/alive")
            ref.setValue(false)
        }

    fun updateSession(session: Session): Completable =
        Completable.fromAction {
            val activeRef = dataBase.getReference("${session.id}/active")
            activeRef.setValue(session.active)

            for (player in session.players) {
                val playerRef = dataBase.getReference("${session.id}/players/${player.id}")
                playerRef.setValue(player)
            }
        }
}