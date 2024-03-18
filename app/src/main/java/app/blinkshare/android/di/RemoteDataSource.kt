package app.blinkshare.android.di

import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val dogService: ApiInterface) {
    suspend fun getAircraftList() =
        dogService.getAircraftList()
}