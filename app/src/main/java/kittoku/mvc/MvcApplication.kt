package kittoku.mvc

import android.app.Application
import kittoku.mvc.di.appModule
import kittoku.mvc.logging.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for SoftEther Connect VPN client.
 *
 * Initializes:
 * - CrashHandler for privacy-respecting crash reporting
 * - Koin dependency injection framework
 */
class MvcApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize crash handler early to catch any initialization errors
        CrashHandler.initialize(this)

        startKoin {
            // Use Android logger with ERROR level in production
            androidLogger(Level.ERROR)

            // Provide Android context
            androidContext(this@MvcApplication)

            // Load Koin modules
            modules(appModule)
        }
    }
}
