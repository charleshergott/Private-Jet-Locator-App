package app.blinkshare.android.di

import app.blinkshare.android.model.AircraftResponse
import app.blinkshare.android.utills.Constants
import retrofit2.Response
import retrofit2.http.GET

interface ApiInterface {
    @GET(Constants.RANDOM_URL)
    suspend fun getAircraftList(): Response<AircraftResponse>
}