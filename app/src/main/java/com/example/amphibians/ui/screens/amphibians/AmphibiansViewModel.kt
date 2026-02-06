package com.example.amphibians.ui.screens.amphibians

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amphibians.data.AmphibiansRepository
import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.utils.AmphibiansAppContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject


private const val TAG = "AmphibiansViewModel"

/** [HomeScreen UI State]  */
sealed interface AmphibiansDataState {
    data class Success(val amphibians: List<Amphibian>) : AmphibiansDataState
    data class Error(val message: String) : AmphibiansDataState
    object Loading : AmphibiansDataState
}

data class AmphibiansUiState(
    val dataState: AmphibiansDataState = AmphibiansDataState.Loading,
    val selectedAmphibianId: Int? = null,
)

/** [put details screen ui state below] */


@HiltViewModel
class AmphibiansViewModel @Inject constructor(
    private val amphibiansRepository: AmphibiansRepository
): ViewModel() {

    private val _amphibiansUiState = MutableStateFlow(AmphibiansUiState())
    val amphibiansUiState: StateFlow<AmphibiansUiState> = _amphibiansUiState.asStateFlow()

    // cached list
    private val _amphibiansCachedList = MutableStateFlow<List<Amphibian>>(emptyList())
    val amphibiansCachedList: StateFlow<List<Amphibian>> = _amphibiansCachedList.asStateFlow()


    init {
        getAmphibians()
    }

    // Selected Amphibian
    fun selectedAmphibian(amphibian: Amphibian){
        _amphibiansUiState.update {
            it.copy(
                selectedAmphibianId = amphibian.id,
            )
        }
    }

    // Clear selected Amphibian when you go back to home screen
    fun onBackFromSelectedAmphibian(){
        _amphibiansUiState.update {
            it.copy(
                selectedAmphibianId = null,
            )
        }
    }

    // getSelectedAmphibianFromCache
    fun getSelectedAmphibianFromCache(): Amphibian? {
        val id = amphibiansUiState.value.selectedAmphibianId ?: return null
        return amphibiansCachedList.value.firstOrNull { it.id == id }
    }





    fun getAmphibians(){
        viewModelScope.launch{

            // if cache already has data, don't load again
            if(amphibiansCachedList.value.isNotEmpty()) {
                _amphibiansUiState.update {
                    it.copy(dataState = AmphibiansDataState.Success(amphibiansCachedList.value))
                }
                return@launch // exit the coroutine function
            }

            _amphibiansUiState.update { it.copy(dataState = AmphibiansDataState.Loading) }

            try {
                val result: List<Amphibian> = withContext(Dispatchers.IO){
                    amphibiansRepository.getAmphibians()
                }

                // create list with ids since the api doesn't have ids
                val listWithIds = result.mapIndexed { index, amphibian ->
                    // add id to each amphibian
                    amphibian.copy(id = index)
                }

                _amphibiansCachedList.update { listWithIds }
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Success(listWithIds))
                }
                Log.d(TAG, "Amphibians: ${amphibiansCachedList.value}")

            } catch (e: CancellationException) {
                throw e // never swallow cancellation

            } catch (e: UnknownHostException) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("No internet / DNS issue: \n ${e.message}"))
                }
                Log.e(TAG, "No internet / DNS issue: \n ${e.message}")
            } catch (e: SocketTimeoutException) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Request timed out: \n ${e.message} \n Try again later"))
                }
                Log.e(TAG, "Request timed out: \n ${e.message}")
            } catch (e: InterruptedIOException) {
                // Optional: many timeouts can come through here depending on the stack.
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Connection interrupted or timed out: \n ${e.message} \n Try again later"))
                }
                Log.e(TAG, "Connection interrupted or timed out: \n ${e.message}")
            } catch (e: HttpException) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Server error: \n (HTTP ${e.code()}): ${e.message()}"))
                }
                Log.e(TAG, "Server error: \n (HTTP ${e.code()}): ${e.message()}")
            } catch (e: SerializationException) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Data error: could not read server response. \n ${e.message}"))
                }
                Log.e(TAG, "Data error: could not read server response. \n ${e.message}")
            } catch (e: IOException) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Network error: \n ${e.message} \n Please try again."))
                }
                Log.e(TAG, "Network error: \n ${e.message}")
            } catch (e: Exception) {
                _amphibiansUiState.update{
                    it.copy(dataState = AmphibiansDataState.Error("Unexpected error: \n ${e.message}"))
                }
                Log.e(TAG, "Unexpected error: \n ${e.message}")
            }
        }
    }

}