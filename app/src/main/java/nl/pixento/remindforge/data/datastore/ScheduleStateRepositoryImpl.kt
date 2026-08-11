package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.pixento.remindforge.data.ScheduleStateRepository

class ScheduleStateRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ScheduleStateRepository {

    override val nextTriggerAtMillis: Flow<Long?> =
        dataStore.data.map { it[PreferencesKeys.NEXT_TRIGGER_AT_MILLIS] }

    override suspend fun setNextTriggerAtMillis(millis: Long?) {
        dataStore.edit { prefs ->
            if (millis == null) {
                prefs.remove(PreferencesKeys.NEXT_TRIGGER_AT_MILLIS)
            } else {
                prefs[PreferencesKeys.NEXT_TRIGGER_AT_MILLIS] = millis
            }
        }
    }
}
