package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import org.conscrypt.Conscrypt
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.injectLazy
import java.security.Security

open class App : Application(), LifecycleObserver {

    private val preferences: PreferencesHelper by injectLazy()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        Injekt.importModule(AppModule(this))

        setupNotificationChannels()

        LocaleHelper.updateConfiguration(this, resources.configuration)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.updateConfiguration(this, newConfig, true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Suppress("unused")
    fun onAppBackgrounded() {
        if (preferences.lockAppAfter().get() >= 0) {
            SecureActivityDelegate.locked = true
        }
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }
}
