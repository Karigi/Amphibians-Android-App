package com.example.amphibians.ui.screens.amphibians

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amphibians.data.AmphibiansRepository
import com.example.amphibians.model.amphibians.Amphibian
import com.example.amphibians.model.amphibians.AmphibiansPage
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
    val selectedAmphibianId: Long? = null,

    // Pagination
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,

    /**
     * If loading the NEXT page fails, we store the message here.
     * - null  -> no paging error
     * - non-null -> show Retry + message in footer
     */
    val pagingErrorMessage: String? = null
)



@HiltViewModel
class AmphibiansViewModel @Inject constructor(
    private val amphibiansRepository: AmphibiansRepository
): ViewModel() {

    private val _amphibiansUiState = MutableStateFlow(AmphibiansUiState())
    val amphibiansUiState: StateFlow<AmphibiansUiState> = _amphibiansUiState.asStateFlow()

    // cached list
    private val _amphibiansCachedList = MutableStateFlow<List<Amphibian>>(emptyList())
    val amphibiansCachedList: StateFlow<List<Amphibian>> = _amphibiansCachedList.asStateFlow()

    // Pagination
    private var offset: Int = 0
    private val pageSize: Int = 50



    init {
        refresh()
    }

    fun refresh(){
        offset = 0

        // Clear cache immediately
        _amphibiansCachedList.update { emptyList() }

        _amphibiansUiState.update{
            it.copy(
                dataState = AmphibiansDataState.Loading,
                isLoadingMore = false,
                endReached = false,
                pagingErrorMessage = null // Clear error message
            )
        }
        // Load the first page
        loadNextPage()

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





    fun loadNextPage(){
        viewModelScope.launch{

            val currentState = amphibiansUiState.value

            // Guard
            if(currentState.isLoadingMore || currentState.endReached){
                return@launch
            }

            // Show loading more
            _amphibiansUiState.update {
                it.copy(isLoadingMore = true)
            }


            try {
                val page: AmphibiansPage = withContext(Dispatchers.IO){
                    amphibiansRepository.getAmphibiansPage(offset = offset, limit = pageSize)
                }

                // Append new items to the cached list
                _amphibiansCachedList.update {
                    it + page.items
                }

                // Update offset
                offset += pageSize

                _amphibiansUiState.update{
                    it.copy(
                        dataState = AmphibiansDataState.Success(_amphibiansCachedList.value),
                        isLoadingMore = false,
                        endReached = page.endOfRecords,
                        pagingErrorMessage = null // Clear error message
                    )
                }
                Log.d(TAG, "Amphibians: ${amphibiansCachedList.value.size}")

            } catch (e: CancellationException) {
                throw e // never swallow cancellation

            } catch (e: UnknownHostException) {
                showError("No internet / DNS issue: \n ${e.message}")
            } catch (e: SocketTimeoutException) {
                showError("Request timed out: \n ${e.message}")
            } catch (e: InterruptedIOException) {
                // Optional: many timeouts can come through here depending on the stack.
                showError("Connection interrupted or timed out: \n ${e.message}")
            } catch (e: HttpException) {
                showError("Server error: \n (HTTP ${e.code()}): ${e.message()}")
            } catch (e: SerializationException) {
                showError("Data error: could not read server response. \n ${e.message}")
            } catch (e: IOException) {
                showError("Network error: \n ${e.message}")
            } catch (e: Exception) {
                showError("Unexpected error: \n ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        val hasItemsAlready = _amphibiansCachedList.value.isNotEmpty()

        _amphibiansUiState.update { current ->
            if (hasItemsAlready) {
                // Paging error: keep list visible and show retry in footer
                current.copy(
                    dataState = AmphibiansDataState.Success(_amphibiansCachedList.value),
                    isLoadingMore = false,
                    pagingErrorMessage = message
                )
            } else {
                // First page error: show full screen error
                current.copy(
                    dataState = AmphibiansDataState.Error(message),
                    isLoadingMore = false,
                    pagingErrorMessage = null
                )
            }
        }

        Log.e(TAG, message)
    }

}