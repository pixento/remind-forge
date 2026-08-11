package nl.pixento.remindforge

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import nl.pixento.remindforge.alerting.AlertPlayer
import nl.pixento.remindforge.alerting.AndroidAlertPlayer
import nl.pixento.remindforge.data.ScheduleStateRepository
import nl.pixento.remindforge.data.SettingsRepository
import nl.pixento.remindforge.data.datastore.AlertModeMigration
import nl.pixento.remindforge.data.datastore.ScheduleStateRepositoryImpl
import nl.pixento.remindforge.data.datastore.SettingsRepositoryImpl
import nl.pixento.remindforge.domain.ReminderScheduleCoordinator
import nl.pixento.remindforge.domain.TriggerReminderUseCase
import nl.pixento.remindforge.scheduling.AlarmScheduler
import nl.pixento.remindforge.scheduling.AndroidAlarmScheduler

private const val SETTINGS_DATASTORE_FILE_NAME = "reminder_settings.preferences_pb"

/** Manual composition root: wires the repository/scheduler/alertPlayer/use cases together. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            migrations = listOf(AlertModeMigration),
            produceFile = { File(appContext.filesDir, "datastore/$SETTINGS_DATASTORE_FILE_NAME") },
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(settingsDataStore) }
    val scheduleStateRepository: ScheduleStateRepository by lazy {
        ScheduleStateRepositoryImpl(settingsDataStore)
    }
    val alarmScheduler: AlarmScheduler by lazy { AndroidAlarmScheduler(appContext) }
    val alertPlayer: AlertPlayer by lazy { AndroidAlertPlayer(appContext) }

    val triggerReminderUseCase: TriggerReminderUseCase by lazy {
        TriggerReminderUseCase(
            settingsRepository,
            scheduleStateRepository,
            alertPlayer,
            alarmScheduler,
        )
    }

    val scheduleCoordinator: ReminderScheduleCoordinator by lazy {
        ReminderScheduleCoordinator(settingsRepository, scheduleStateRepository, alarmScheduler)
    }
}
