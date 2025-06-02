package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.IOException

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 * @param S The type of state managed by this ViewModel.
 * @param mainDispatcher The dispatcher used for UI-related coroutines.
 * @param ioDispatcher The dispatcher used for IO operations.
 */
abstract class BaseViewModel<S>(
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * The current state of the ViewModel.
     */
    abstract var state: S
        protected set

    /**
     * The coroutine scope used for launching coroutines.
     */
    protected val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    /**
     * Executes an API call with proper error handling and state management.
     * @param loadingState A function that returns a new state with loading set to true.
     * @param errorState A function that returns a new state with the error message.
     * @param apiCall The API call to execute.
     * @param onSuccess A function to handle the successful response.
     */
    protected suspend fun <T> executeApiCall(
        loadingState: () -> S,
        errorState: (String) -> S,
        apiCall: suspend () -> Response<T>,
        onSuccess: suspend (T) -> S
    ) {
        state = loadingState()
        try {
            val response = withContext(ioDispatcher) {
                apiCall()
            }
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    state = onSuccess(body)
                } else {
                    state = errorState("Response body is null")
                }
            } else {
                state = errorState("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: IOException) {
            state = errorState("Network error: ${e.message}")
        } catch (e: Exception) {
            state = errorState("Exception: ${e.message}")
        }
    }

    /**
     * Launches a coroutine in the viewModelScope.
     * @param block The coroutine block to execute.
     */
    protected fun launchCoroutine(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    /**
     * Cleans up resources when the ViewModel is no longer used.
     */
    open fun onCleared() {
        viewModelScope.cancel()
    }
}
