package nl.pixento.betterhabits

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import nl.pixento.betterhabits.alerting.AlertPlayer
import nl.pixento.betterhabits.alerting.AndroidAlertPlayer
import nl.pixento.betterhabits.alerting.AndroidCarConnectionMonitor
import nl.pixento.betterhabits.alerting.AndroidDoNotDisturbMonitor
import nl.pixento.betterhabits.alerting.CarConnectionMonitor
import nl.pixento.betterhabits.alerting.DoNotDisturbMonitor
import nl.pixento.betterhabits.data.ScheduleStateRepository
import nl.pixento.betterhabits.data.SettingsRepository
import nl.pixento.betterhabits.data.datastore.ActiveWindowModeMigration
import nl.pixento.betterhabits.data.datastore.AlertModeMigration
import nl.pixento.betterhabits.data.datastore.ScheduleStateRepositoryImpl
import nl.pixento.betterhabits.data.datastore.SettingsRepositoryImpl
import nl.pixento.betterhabits.domain.ReminderScheduleCoordinator
import nl.pixento.betterhabits.domain.TriggerReminderUseCase
import nl.pixento.betterhabits.scheduling.AlarmScheduler
import nl.pixento.betterhabits.scheduling.AndroidAlarmScheduler

private const val SETTINGS_DATASTORE_FILE_NAME = "reminder_settings.preferences_pb"

/** Manual composition root: wires the repository/scheduler/alertPlayer/use cases together. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            migrations = listOf(AlertModeMigration, ActiveWindowModeMigration),
            produceFile = { File(appContext.filesDir, "datastore/$SETTINGS_DATASTORE_FILE_NAME") },
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(settingsDataStore) }
    val scheduleStateRepository: ScheduleStateRepository by lazy {
        ScheduleStateRepositoryImpl(settingsDataStore)
    }
    val alarmScheduler: AlarmScheduler by lazy { AndroidAlarmScheduler(appContext) }
    val alertPlayer: AlertPlayer by lazy { AndroidAlertPlayer(appContext) }
    val doNotDisturbMonitor: DoNotDisturbMonitor by lazy { AndroidDoNotDisturbMonitor(appContext) }
    val carConnectionMonitor: CarConnectionMonitor by lazy {
        AndroidCarConnectionMonitor(appContext)
    }

    val triggerReminderUseCase: TriggerReminderUseCase by lazy {
        TriggerReminderUseCase(
            settingsRepository,
            scheduleStateRepository,
            alertPlayer,
            alarmScheduler,
            doNotDisturbMonitor,
            carConnectionMonitor,
        )
    }

    val scheduleCoordinator: ReminderScheduleCoordinator by lazy {
        ReminderScheduleCoordinator(settingsRepository, scheduleStateRepository, alarmScheduler)
    }
}
