package com.openplaato.keg.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {
    fun markSeen() = viewModelScope.launch { prefs.setHasSeenOnboarding(true) }
}
