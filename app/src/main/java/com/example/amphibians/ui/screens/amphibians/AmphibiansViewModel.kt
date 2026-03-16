package com.example.amphibians.ui.screens.amphibians

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amphibians.data.AmphibiansRepository
import com.example.amphibians.model.amphibians.Amphibian
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

/**
 * // ── One-Shot Events (fired once, uses SharedFlow) ─────────────────────
 * //
 * // These are things that HAPPEN, not things that ARE.
 * //
 * // BAD approach (using StateFlow for events):
 * //   If you put "showSnackbar: Boolean" in AmphibiansUiState (StateFlow),
 * //   then when the user rotates the phone, the new collector sees the
 * //   current state which still has showSnackbar=true → snackbar shows again!
 * //   You'd have to manually reset it with another update(), which is messy.
 * //
 * // GOOD approach: Use SharedFlow.
 * //   emit() fires the event. Whoever is collecting at that moment handles it.
 * //   Rotation creates a NEW collector → it missed the past event → no repeat.
 * */
sealed interface AmphibiansUiEvent {
    // Show a short snackbar with a message (e.g., "Copied to clipboard")
    data class ShowSnackbar(val message: String) : AmphibiansUiEvent
    // Navigate to the detail screen for the given amphibian
    data class NavigateToDetails(val amphibianId: Long) : AmphibiansUiEvent
    // Navigate back (e.g., after a delete operation completes)
    object NavigateBack : AmphibiansUiEvent
}


@HiltViewModel
class AmphibiansViewModel @Inject constructor(
    private val amphibiansRepository: AmphibiansRepository
): ViewModel() {

    /**
     * // ── How StateFlow behaves ──────────────────────────────────────────
     * //
     * //  Timeline:
     * //  ViewModel emits:  Loading ──► Success ──► Success(page2)
     * //                                              ↑
     * //  User rotates phone ─────────────────────────┘
     * //  New collector starts HERE and immediately gets "Success(page2)"
     * //  It does NOT restart from Loading.
     * //
     * //  ==>> This is called "replay = 1" (the latest value is always replayed).
     * //
     * // ── Conflation ────────────────────────────────────────────────────
     * //  If you call _amphibiansUiState.update { it.copy(...) } with a value
     *     that is EQUAL to the current value (by data class equals), NO new
     *     emission happens. Collectors are not notified. This is conflation.
     * */
    private val _amphibiansUiState = MutableStateFlow(AmphibiansUiState())

    /**
     * Expose as read-only StateFlow to the UI.
     * .asStateFlow() wraps it so no one outside can cast it back to Mutable.
     * */
    val amphibiansUiState: StateFlow<AmphibiansUiState> = _amphibiansUiState.asStateFlow()

    // cached list
    private val _amphibiansCachedList = MutableStateFlow<List<Amphibian>>(emptyList())
    val amphibiansCachedList: StateFlow<List<Amphibian>> = _amphibiansCachedList.asStateFlow()

    /**
     * // ── SharedFlow: fires one-shot events ────────────────────────────
     *     //
     *     // replay = 0
     *     //   A collector that starts AFTER an event was emitted will NOT see it.
     *     //   Perfect for navigation or snackbars — you don't want them to
     *     //   re-trigger when the screen recomposes or the user rotates.
     *     //
     *     // extraBufferCapacity = 1
     *     //   Allows the ViewModel to emit() without suspending even if the
     *     //   UI collector is momentarily busy. The event sits in the buffer
     *     //   for a moment until the collector picks it up.
     *     //
     *     // onBufferOverflow = DROP_OLDEST
     *     //   If somehow 2 events pile up before the collector processes them,
     *     //   drop the oldest one. For snackbars this is usually fine.
     *     //   For critical events (e.g., logout), prefer SUSPEND instead.
     * */
    private val _amphibiansUiEvents = MutableSharedFlow<AmphibiansUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val amphibiansUiEvents: SharedFlow<AmphibiansUiEvent> = _amphibiansUiEvents.asSharedFlow()

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
        // Update persistent state so the detail screen knows which item is shown.
        // StateFlow is correct here because the "selected ID" is persistent state
        // that the detail screen needs whenever it composes/recomposes.
        _amphibiansUiState.update {
            it.copy(
                selectedAmphibianId = amphibian.id,
            )
        }
        // Also fire a one-shot navigation event so the NavHost can respond.
        // SharedFlow is correct here because "navigate to detail" should happen
        // ONCE. If the user rotates while on the detail screen, we do NOT want
        // this event to re-fire and push another destination onto the back stack.
        viewModelScope.launch {
            _amphibiansUiEvents.emit(AmphibiansUiEvent.NavigateToDetails(amphibian.id))
        }
    }
    /**
     * // ⚠️  MINOR NOTE on NavigateToDetails event:
     * //     Your NavGraph currently handles navigation directly in onCardClick
     * //     (calls navController.navigate() right there in the lambda).
     * //     That means the SharedFlow NavigateToDetails event is emitted but
     * //     NEVER collected anywhere — it fires into the void.
     * //
     * //     This is not a bug (nothing breaks), but it's dead code.
     * //     You have two options:
     * //
     * //     OPTION A (current approach — keep it simple):
     * //       Keep doing navController.navigate() in onCardClick in the NavGraph.
     * //       Remove the NavigateToDetails emit from selectedAmphibian() since
     * //       nobody collects it.
     * //
     * //     OPTION B (pure SharedFlow approach):
     * //       Remove navController.navigate() from onCardClick in NavGraph.
     * //       Collect the SharedFlow event in NavGraph and navigate from there.
     * //       This is "cleaner" architecturally (ViewModel drives navigation)
     * //       but adds complexity. Fine either way.
     * //
     * //     We will show OPTION B below because it demonstrates SharedFlow
     * //     collection properly. You can choose whichever fits your preference.
     * */

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

    // Example: User long-presses an amphibian to copy its name
    fun onAmphibianLongPressed(amphibian: Amphibian) {
        // This is a one-shot event: show a snackbar.
        // We do NOT store "showSnackbar = true" in UiState (StateFlow)
        // because that would re-show the snackbar on rotation.
        viewModelScope.launch {
            _amphibiansUiEvents.emit(AmphibiansUiEvent.ShowSnackbar("Copied to clipboard: ${amphibian.name}"))
        }
    }



    fun loadNextPage() {

        val current = amphibiansUiState.value

        // 1) Guard: do not start another request if one is already running
        //    and do not load if there are no more pages.
        if (current.isLoadingMore || current.endReached) {
            return
        }

        // 2) Ask repository for a Flow page request
        amphibiansRepository.getAmphibiansPageFlow(
            offset = offset,
            limit = pageSize
        )
            .onStart {
                /**
                 * Runs right before upstream starts emitting.
                 *
                 * Great place to:
                 * - show loading spinner
                 * - clear old paging error
                 */
                _amphibiansUiState.update {
                    it.copy(
                        isLoadingMore = true,
                        pagingErrorMessage = null
                    )
                }
            }

            .onEach { page ->
                /**
                 * Runs every time the flow emits a value.
                 *
                 * In our current repository, the flow emits ONE AmphibiansPage.
                 * So this block runs once per call to loadNextPage().
                 */

                // 3) Append new items to the cached list
                _amphibiansCachedList.update {
                    it + page.items
                }

                // 4) Increase offset so next request starts after this page
                offset += pageSize

                // 5) Update UI state with the full combined list
                _amphibiansUiState.update {
                    it.copy(
                        dataState = AmphibiansDataState.Success(_amphibiansCachedList.value),
                        isLoadingMore = false,
                        endReached = page.endOfRecords,
                        pagingErrorMessage = null // Clear error message
                    )
                }

                Log.d(TAG, "Amphibians: ${amphibiansCachedList.value.size}")
                Log.d(TAG, "-----------------------------------------")
                Log.d(TAG, "Offset: $offset")
                Log.d(TAG, "-----------------------------------------")

            }


            .catch { e ->
                /**
                 * Runs if the upstream flow throws an exception.
                 *
                 * Examples:
                 * - network error
                 * - timeout
                 * - parsing failure
                 */
                when (e) {
                    is CancellationException -> throw e // never swallow cancellation
                    is UnknownHostException -> showError("No internet / DNS issue: \n ${e.message}")
                    is SocketTimeoutException -> showError("Request timed out: \n ${e.message}")
                    is InterruptedIOException -> showError("Connection interrupted or timed out: \n ${e.message}")
                    is HttpException -> showError("Server error: \n (HTTP ${e.code()}): ${e.message()}")
                    is SerializationException -> showError("Data error: could not read server response. \n ${e.message}")
                    is IOException -> showError("Network error: \n ${e.message}")
                    else -> showError("Unexpected error: \n ${e.message}")
                }

            }

            /**
             * launchIn(viewModelScope) starts collection.
             *
             * It is basically shorthand for:
             * viewModelScope.launch {
             *     flow.collect()
             * }
             */
            .launchIn(viewModelScope)


        /**========================== (ALTERNATIVE APPROACH) ============================================*/
        /*
        /*
         * You can also use collect inside a launch block instead of launchIn.
         * It's a bit more verbose but allows you to use withContext, etc if needed.
         *
         * Note: if you use collect, you MUST call it inside a launch block.
         * Otherwise, the flow will not start and your UI will not update.
         */
        viewModelScope.launch {
            amphibiansRepository.getAmphibiansPageFlow(offset, pageSize)
                .onStart {
                    _amphibiansUiState.update {
                        it.copy(
                            isLoadingMore = true,
                            pagingErrorMessage = null
                        )
                    }
                }
                .catch { e ->
                    when (e) {
                        is CancellationException -> throw e // never swallow cancellation
                        is UnknownHostException -> showError("No internet / DNS issue: \n ${e.message}")
                        is SocketTimeoutException -> showError("Request timed out: \n ${e.message}")
                        is InterruptedIOException -> showError("Connection interrupted or timed out: \n ${e.message}")
                        is HttpException -> showError("Server error: \n (HTTP ${e.code()}): ${e.message()}")
                        is SerializationException -> showError("Data error: could not read server response. \n ${e.message}")
                        is IOException -> showError("Network error: \n ${e.message}")
                        else -> showError("Unexpected error: \n ${e.message}")
                    }
                }
                .collect { page ->
                    _amphibiansCachedList.update {
                        it + page.items
                     }
                    offset += pageSize

                    _amphibiansUiState.update {
                        it.copy(
                            dataState = AmphibiansDataState.Success(_amphibiansCachedList.value),
                            isLoadingMore = false,
                            endReached = page.endOfRecords,
                            pagingErrorMessage = null
                        )
                    }
                }
        }
        */
        /**==========================================================================================*/

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