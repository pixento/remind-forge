package nl.pixento.remindforge

import android.app.Application

class RemindForgeApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
