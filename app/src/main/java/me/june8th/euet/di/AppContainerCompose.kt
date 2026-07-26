package me.june8th.euet.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/** Provides the app-scoped [AppContainer] down the Compose tree. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap content in CompositionLocalProvider(LocalAppContainer provides …).")
}

/**
 * Obtains a [ViewModel] built from the current [AppContainer]. Replaces Hilt's `hiltViewModel()`:
 *
 *     val vm: ProfileViewModel = euetViewModel { ProfileViewModel(it.studentRepository) }
 */
@Composable
inline fun <reified VM : ViewModel> euetViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel { create(container) }
}
