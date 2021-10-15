package com.andreibelous.arwar.data.model

data class Session
@JvmOverloads constructor(
    val players: List<Player>,
    val active: Boolean,
    val id: Int
) {

    companion object {

        fun empty(id: Int) =
            Session(
                players = emptyList(),
                active = false,
                id = id
            )
    }
}