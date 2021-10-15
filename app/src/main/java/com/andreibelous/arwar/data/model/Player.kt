package com.andreibelous.arwar.data.model

data class Player(
    val id: Int,
    val name: String,
    val location: Location?,
    val alive: Boolean
)