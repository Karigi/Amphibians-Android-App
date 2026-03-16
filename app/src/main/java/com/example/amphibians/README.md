# WHAT IS DAO?
- DAO → Data Access Object
- An interface or abstract class where you define methods to access the database.
- When to use: When you add local persistence using Room.
- A Room DAO can emit updates automatically when the database changes.

## Recommended for this app is suspend function, the use of flow is just for practice

# Learning Path:
- Flow ✅
- StateFlow ✅
- SharedFlow
- stateIn / shareIn
- Room DAO + Flow
- Derived UI state pipelines

→ suspend fun - get one result once 

→ Flow<T> - a stream of values over time

→ StateFlow<T> - a current state holder that always has the latest value

→ SharedFlow<T> - a broadcast stream for multiple collectors, often used for events

# StateFlow vs SharedFlow
## StateFlow
- A state holder that always has the latest value
- StateFlow emits the current and new state updates to collectors,
  and its current value can be read through the *value* property.

→ state always exists, while events are transient and happen.

- Use StateFlow when you are representing what the screen looks like right now. 
- Examples: 
  - *loading/success/error*
  - *current list of amphibians*
  - *selected amphibian id*
  - *search text currently shown in the ui*
  - *AmphibiansUiSate*
  - *cached list state*

- **replay** : means how many past emissions are replayed to new collectors
- `replay = 1` : means the latest value is always replayed 

  ## Why it fits UI state
- Because screen state always exists.
- Even if nothing is happening, the screen is still in some state:
  - Loading
  - showing data
  - showing an error

- _*State always exists, while events are transient and happen*_

## SharedFlow
- Use SharedFlow when you want to broadcast values/events to one or more collectors, 
  especially when the thing is not really “the current state of the screen.”

→ **Rule of thumb**: "Should this trigger something once and never repeat?"

→ **When a new subscriber joins? - Use SharedFlow**

- `replay = 0` : means no old values are replayed to new collectors -> This is the concept behind SharedFlow
- Contrary from `replay = 1 (StateFlow)` which means the latest value is always replayed
  - `replay = 0 (SharedFlow)` is perfect for one-shot events, e.g. _**you don't want 'navigate to detail screen' re-firing on rotation.**_

    ## Example of creating a SharedFlow in a ViewModel
    `

        private val _events = MutableSharedFlow<UiEvent>(
          replay = 0,              // ← no replay: late collectors miss past events
          extraBufferCapacity = 1, // ← buffer 1 event so emit() never suspends in a fast ViewModel even if UI is briefly slow
          onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    `
- extraBufferCapacity = means how many emissions to buffer when collectors are slow. Default is 0
- onBufferOverflow = what to do when buffer is full: DROP_OLDEST or DROP_LATEST, or SUSPEND(default)


- **Example use cases**:
    - Show snack bar
    - Navigate to details
    - Open dialog once
    - Refresh was requested
    - A new message arrived
    - A new notification arrived
    - Trigger a vibration/sound
    - Log out the user

→ *SharedFlow as a Flow API for emitting values to multiple consumers.*

## StateFlow = what is
## SharedFlow = what happened

## Use SharedFlow for:
    -> showing a one-time snackbar like "copied to clipboard".
    -> navigation events.
    -> toasts.
    -> one-shot "scroll to top".
    -> one-off retry prompts.

# SharedFlow
## _Lesson 1:- **Don't store one-time events in StateFlow**_
 eg: `
        data class UiState (
          val snackbarMessage: String? = null
          )
     `
- An example of a Snack bar message is page load failed message:
  - The message is not really screen state
  - After rotation or recollection, it may still be there
  - You must remember to clear it

→ *StateFlow*: **for persistent screen state**
→ *SharedFlow*: **for one-time events**

## _Lesson 2:- **Basic SharedFlow example**_
`
    
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import kotlinx.coroutines.flow.MutableSharedFlow
    import kotlinx.coroutines.flow.SharedFlow
    import kotlinx.coroutines.flow.asSharedFlow
    import kotlinx.coroutines.launch
    
    /**
    * This sealed class represents one-time UI events.
      *
      * These are NOT persistent screen state.
      * They are things that should happen once.
        */
        sealed interface AmphibiansUiEvent {
        data class ShowSnackbar(val message: String) : AmphibiansUiEvent
        data class NavigateToDetails(val amphibianId: Long) : AmphibiansUiEvent
        }
    
    class AmphibiansViewModel : ViewModel() {
    
        /**
         * MutableSharedFlow is the writable event stream inside the ViewModel.
         *
         * replay = 0 means:
         * - do not automatically replay old events to new collectors
         * - this is usually what you want for one-time UI events
         */
        private val _events = MutableSharedFlow<AmphibiansUiEvent>(replay = 0)
    
        /**
         * Expose read-only SharedFlow to the UI.
         */
        val events: SharedFlow<AmphibiansUiEvent> = _events.asSharedFlow()
    
        fun onAmphibianClicked(id: Long) {
            viewModelScope.launch {
                _events.emit(AmphibiansUiEvent.NavigateToDetails(id))
            }
        }
    
        fun onPagingFailed(message: String) {
            viewModelScope.launch {
                _events.emit(AmphibiansUiEvent.ShowSnackbar(message))
            }
        }
    }
`
# How to collect in compose:
## StateFlow
    - collectAsStateWithLifeCycle()
        -> Gives you a compose State<T>
        -> Compose recomposes when value changes

## SharedFlow
    - collectAsStateWithLifeCycle()
        -> Not ideal for SharedFlow because it holds onto the latest value, which may not be what you want for one-time events.
    - collectLatest or collect in LaunchedEffect + repeatOnLifecycle(STARTED)
        -> Better for SharedFlow because it allows you to react to events as they come without holding onto the latest value.
        -> NOT a State<T>, just an action trigger.


# Why not collect SharedFlow with collectAsStateWithLifecycle()?:
    - *collectAsStateWithLifecycle()* returns a State<T>, which requires an
        initial value.
    - SharedFlow has no stored value, so you'd have to pass
        null or some sentinel as the initial value, then check for it.
        That's awkward. More importantly, State<T> is for VALUES you read in
        the UI tree. Events are not values you read — they're triggers for
        side effects. LaunchedEffect is designed exactly for side effects.

# Do I need to cancel the SharedFlow collection manually?
    - No, if you use collectLatest or collect in a LaunchedEffect with repeatOnLifecycle(STARTED),
        the collection will automatically start and stop with the lifecycle of the composable. 
        You don't need to manually cancel it.

# *collect vs collectLatest*
    - collect: Process EVERY emission, never skip and never cancel
    - collectLatest: Only care about the LATEST emission. If a new one arrives 
        while I'm still processing the old one, CANCEL the old work and start fresh 
        with the new one.

- *In Compose, you must NEVER call side effects **(navigation,
    logging, network calls)** directly in the body of a composable.
    Composable bodies can re-run many times (recomposition).
    If you call navigate() directly in the body, it could fire
    multiple times per frame, pushing duplicate screens.*

- LaunchedEffect(key) restarts its block whenever `key` changes.
- LaunchedEffect(Unit) → Unit never changes → block runs exactly ONCE
  per time this LaunchedEffect enters the composition.

- _**The rule of thumb**_:
  "If the USER triggers the navigation (button tap, swipe, system back),
  handle it directly in the UI. NavController is a UI concern."
  "If the VIEWMODEL triggers navigation *(after an async operation
  completes, after a delete, after a login)*, use SharedFlow so
  the ViewModel can tell the UI to navigate without holding a
  navController reference."

# IS repeatOnLifecycle ONLY FOR VIEW-BASED SYSTEMS?
    - In COMPOSE, for StateFlow, you use collectAsStateWithLifecycle() which
        internally uses the same lifecycle-aware mechanism but is more ergonomic.
        For SharedFlow in Compose, you still use LaunchedEffect +
        repeatOnLifecycle because collectAsStateWithLifecycle() is for STATE
        (needs an initial value), not for one-shot events.

# CONFLATION
- If a new value arrives before the previous one was processes, skip the previous one and only deliver the latest
- StateFlow is conflated by DESIGN: 
  - If you emit a value that is EQUAL (by. equal()) to the current value no emission happens.
  - If the collector is slow and multiple values are emitted before it processes one, the collector only gets the LATEST value.


# BUFFER MODEL OF SharedFlow
## extraBufferCapacity:
- Stores emissions that are waiting to be collected by slow collectors(extraBufferCapacity = M)
## Replay cache:
- Stores last N emissions for new collectors(replay = N)
## TOTAL CAPACITY = replay + extraBufferCapacity

# TOAST
- A small floating message that appears briefly on screen, the automatically disappears

## Characteristics of Toast
    ✅ Shown by the Android SYSTEM — appears ABOVE your app's UI
    ✅ Requires a Context to create (not just a composable)
    ✅ Auto-dismisses — you cannot add buttons to it
    ✅ Cannot be interacted with — no click, no dismiss button
    ✅ Works even if the app goes to the background (OS controls it)
    ✅ Two durations only: Toast.LENGTH_SHORT (~2s) / Toast.LENGTH_LONG (~3.5s)
    ❌ Cannot be styled (color, font, shape) on Android 11+ (API 30+)
        Android 11 forced all toasts to use the system style.
        Custom-view toasts are deprecated and ignored on API 30+.
    ❌ No action button (that's what Snackbar is for)
    ❌ Not tied to your app's lifecycle — can outlive your screen
    ❌ Cannot be cancelled once shown (well, technically you can call
        cancel() but the timing is unreliable)

-Use Toast when:
    • The message is purely informational with NO action needed
    • You want the simplest possible implementation
    • The message should show even if the user navigates away
    • Examples: "Saved!", "Copied!", "Connected to Wi-Fi"

Use Snackbar when:
    • You need an action button ("UNDO", "RETRY", "DISMISS")
    • You want the message to respect your app's theme/style
    • You want it tied to the screen lifecycle (goes away with screen)
    • Examples: "Item deleted [UNDO]", "Failed to load [RETRY]"

In modern Android (2024+):
    Google recommends Snackbar over Toast for in-app messages because
    Snackbar is more flexible, styleable, and Compose-friendly.
    Toast is still fine for simple "fire and forget" messages.
