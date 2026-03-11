package com.openplaato.keg.notification

import android.content.Context
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.preferences.AppPreferences
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PourNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PlaatoRepository,
    private val prefs: AppPreferences,
) {
    private val pouringState = mutableMapOf<String, Boolean>()

    fun start(scope: CoroutineScope) {
        scope.launch {
            repository.wsEvents.collect { event ->
                if (event !is WsEvent.KegUpdate) return@collect
                val keg = event.keg
                val wasPouring = pouringState[keg.id] ?: false
                val isPouring = keg.is_pouring?.let { it != "0" && it.isNotBlank() } ?: false

                if (wasPouring && !isPouring) {
                    val pour = keg.last_pour
                    if (pour != null && pour > 0 && prefs.pourNotificationsEnabled.first()) {
                        val (mult, unit) = when (keg.beer_left_unit) {
                            "lbs" -> 16.0 to "oz"
                            "kg" -> 1000.0 to "g"
                            "gal" -> 128.0 to "oz"
                            else -> 1000.0 to "ml"
                        }
                        val amount = (pour * mult).toInt()
                        // Ignore noise: require at least 5 oz (≈148 ml/g) to suppress false reads
                        val minAmount = if (unit == "oz") 5 else 148
                        if (amount >= minAmount) {
                            val label = keg.my_label?.takeIf { it.isNotBlank() } ?: keg.id.take(8)
                            PourNotificationHelper.showPourNotification(context, label, amount, unit)
                        }
                    }
                }
                pouringState[keg.id] = isPouring
            }
        }
    }
}
