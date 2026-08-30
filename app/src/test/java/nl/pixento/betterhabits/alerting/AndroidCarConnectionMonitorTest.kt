package nl.pixento.betterhabits.alerting

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidCarConnectionMonitorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * Stands in for the provider the Android Auto app publishes. [rows] is what a query answers
     * with, so a test can hand back a state, an empty cursor, or nothing at all.
     */
    class FakeCarConnectionProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = rows?.let { states ->
            MatrixCursor(arrayOf("CarConnectionState")).apply {
                states.forEach { addRow(arrayOf(it)) }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            args: Array<out String>?,
        ) = 0

        companion object {
            /** Null makes the provider answer with no cursor at all. */
            var rows: List<Int>? = null
        }
    }

    @After
    fun tearDown() {
        FakeCarConnectionProvider.rows = null
    }

    private fun monitorWithState(vararg states: Int): AndroidCarConnectionMonitor {
        FakeCarConnectionProvider.rows = states.toList()
        Robolectric.setupContentProvider(
            FakeCarConnectionProvider::class.java,
            "androidx.car.app.connection",
        )
        return AndroidCarConnectionMonitor(context)
    }

    @Test
    fun `projection counts as connected`() {
        assertTrue(monitorWithState(CONNECTION_TYPE_PROJECTION).isConnectedToCar())
    }

    @Test
    fun `a native head unit counts as connected too`() {
        // Automotive OS rather than projection, but the phone is just as much "in the car".
        assertTrue(monitorWithState(CONNECTION_TYPE_NATIVE).isConnectedToCar())
    }

    @Test
    fun `not connected is not connected`() {
        assertFalse(monitorWithState(CONNECTION_TYPE_NOT_CONNECTED).isConnectedToCar())
    }

    @Test
    fun `an empty cursor reads as not connected`() {
        assertFalse(monitorWithState().isConnectedToCar())
    }

    @Test
    fun `a provider that answers with no cursor reads as not connected`() {
        Robolectric.setupContentProvider(
            FakeCarConnectionProvider::class.java,
            "androidx.car.app.connection",
        )
        FakeCarConnectionProvider.rows = null

        assertFalse(AndroidCarConnectionMonitor(context).isConnectedToCar())
    }

    @Test
    fun `no provider at all reads as not connected`() {
        // What every device without Android Auto installed looks like, and what package-visibility
        // filtering would look like if the manifest ever lost its queries entry. Failing open here
        // is the difference between "no car" and "no reminders, ever".
        assertFalse(AndroidCarConnectionMonitor(context).isConnectedToCar())
    }

    private companion object {
        const val CONNECTION_TYPE_NOT_CONNECTED = 0
        const val CONNECTION_TYPE_NATIVE = 1
        const val CONNECTION_TYPE_PROJECTION = 2
    }
}
