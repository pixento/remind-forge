package nl.pixento.remindforge.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScheduleStateRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ScheduleStateRepositoryImpl

    private fun setUp() {
        val file = File(tempFolder.root, "test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        repository = ScheduleStateRepositoryImpl(dataStore)
    }

    @Test
    fun `no trigger is recorded before anything is scheduled`() = runTest {
        setUp()
        assertNull(repository.nextTriggerAtMillis.first())
    }

    @Test
    fun `a recorded trigger round-trips`() = runTest {
        setUp()
        repository.setNextTriggerAtMillis(1_767_265_200_000L)
        assertEquals(1_767_265_200_000L, repository.nextTriggerAtMillis.first())
    }

    @Test
    fun `null clears the recorded trigger`() = runTest {
        setUp()
        repository.setNextTriggerAtMillis(1_767_265_200_000L)
        repository.setNextTriggerAtMillis(null)
        assertNull(repository.nextTriggerAtMillis.first())
    }
}
