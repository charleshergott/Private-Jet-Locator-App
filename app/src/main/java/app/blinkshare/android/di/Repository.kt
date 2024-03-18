package app.blinkshare.android.di

import app.blinkshare.android.model.AircraftResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ActivityRetainedScoped
class Repository @Inject constructor(
    private val remoteDataSource: RemoteDataSource
) : BaseApiResponse() {
    suspend fun getAircraftList(): Flow<NetworkResult<AircraftResponse>> {
        return flow<NetworkResult<AircraftResponse>> {
            emit(safeApiCall { remoteDataSource.getAircraftList() })
        }.flowOn(Dispatchers.IO)
    }
}