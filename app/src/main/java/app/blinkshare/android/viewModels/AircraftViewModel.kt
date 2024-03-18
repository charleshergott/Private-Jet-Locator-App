package app.blinkshare.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.blinkshare.android.di.NetworkResult
import app.blinkshare.android.di.Repository
import app.blinkshare.android.model.AircraftResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AircraftViewModel @Inject constructor
    (
    private val repository: Repository,
    application: Application
) : AndroidViewModel(application) {
    private val _response: MutableStateFlow<NetworkResult<AircraftResponse>> = MutableStateFlow(NetworkResult.Loading())
    val response: MutableStateFlow<NetworkResult<AircraftResponse>> = _response
    fun fetchAircraftResponse() = viewModelScope.launch {
        repository.getAircraftList().collect { values ->
            _response.value = values
        }
    }

    init {
        viewModelScope.launch {
            fetchAircraftResponse()
        }
    }
}