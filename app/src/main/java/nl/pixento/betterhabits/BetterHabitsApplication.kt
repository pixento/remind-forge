package nl.pixento.betterhabits

import android.app.Application

class BetterHabitsApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
