package com.example.apptopicos.entity

import com.google.android.gms.maps.model.LatLng

data class NavegationStep(
    val instruction: String,
    val distance: Double,
    val duration: Double,
    val endLocation: LatLng,
    val maneuver: String?
)