package com.kodomo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.data.prefs.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(session: SessionPreferences) : ViewModel() {
    val session: StateFlow<SessionState?> = session.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
