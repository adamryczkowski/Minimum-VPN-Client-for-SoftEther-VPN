package kittoku.mvc.di

import android.content.Context
import android.net.ConnectivityManager
import kittoku.mvc.autoconnect.AutoConnectSettings
import kittoku.mvc.autoconnect.NetworkMonitor
import kittoku.mvc.autoconnect.TrustedNetworkChecker
import kittoku.mvc.autoconnect.VpnConnectionStarter
import kittoku.mvc.autoconnect.VpnConnectionStarterImpl
import kittoku.mvc.autoconnect.WifiSsidProvider
import kittoku.mvc.autoconnect.WifiSsidProviderImpl
import kittoku.mvc.connection.ConnectionStateManager
import kittoku.mvc.database.AppDatabase
import kittoku.mvc.notification.VpnNotificationManager
import kittoku.mvc.repository.ProfileRepository
import kittoku.mvc.repository.ProfileRepositoryImpl
import kittoku.mvc.repository.VpnConnectionRepository
import kittoku.mvc.service.ProfileImportExportService
import kittoku.mvc.service.ProfileImportExportServiceImpl
import kittoku.mvc.service.VpnConnectionManager
import kittoku.mvc.statistics.ConnectionTimer
import kittoku.mvc.statistics.StatisticsRepository
import kittoku.mvc.statistics.TrafficCounter
import kittoku.mvc.viewmodel.HomeViewModel
import kittoku.mvc.viewmodel.ProfileViewModel
import kittoku.mvc.viewmodel.StatisticsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin module for the VPN application.
 *
 * This module defines all the dependencies that can be injected throughout the app.
 * Dependencies are organized by layer:
 * - Database layer: Room database
 * - Service layer: VPN service components
 * - Repository layer: Data access
 * - ViewModel layer: UI state management
 */
val appModule =
    module {
        // === Database Layer ===
        // Room database (singleton)
        single { AppDatabase.getInstance(androidContext()) }

        // VPN Profile DAO
        single { get<AppDatabase>().vpnProfileDao() }

        // Connection Session DAO
        single { get<AppDatabase>().connectionSessionDao() }

        // === Repository Layer ===
        // VPN connection state repository (singleton)
        single { VpnConnectionRepository(androidContext()) }

        // Profile repository (singleton)
        single<ProfileRepository> { ProfileRepositoryImpl(get()) }

        // Statistics repository (singleton)
        single { StatisticsRepository(get()) }

        // === Service Layer ===
        // Connection state manager (singleton)
        single { ConnectionStateManager() }

        // VPN notification manager (singleton)
        single { VpnNotificationManager(androidContext()) }

        // VPN connection manager (singleton)
        single { VpnConnectionManager(androidContext(), get()) }

        // Profile import/export service (singleton)
        single<ProfileImportExportService> { ProfileImportExportServiceImpl(get()) }

        // Traffic counter (singleton for current session)
        single { TrafficCounter() }

        // Connection timer (singleton for current session)
        single { ConnectionTimer() }

        // === Auto-Connect Layer ===
        // SharedPreferences for auto-connect settings
        single {
            androidContext().getSharedPreferences(
                "auto_connect_prefs",
                Context.MODE_PRIVATE,
            )
        }

        // Auto-connect settings (singleton)
        single { AutoConnectSettings(get()) }

        // VPN connection starter (singleton)
        single<VpnConnectionStarter> { VpnConnectionStarterImpl() }

        // ConnectivityManager
        single {
            androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }

        // Network monitor (singleton)
        single { NetworkMonitor(get()) }

        // WiFi SSID provider (singleton)
        single<WifiSsidProvider> { WifiSsidProviderImpl(androidContext()) }

        // Trusted network checker (singleton)
        single { TrustedNetworkChecker(get(), get(), get()) }

        // === ViewModel Layer ===
        // Home screen ViewModel
        viewModel { HomeViewModel(get(), get()) }

        // Profile management ViewModel
        viewModel { ProfileViewModel(get(), get()) }

        // Statistics ViewModel
        viewModel { StatisticsViewModel(get(), get(), get()) }
    }

/**
 * Module for network-related dependencies.
 *
 * Contains terminal implementations and network utilities.
 */
val networkModule =
    module {
        // Network terminals will be added here when refactored
        // factory { TCPTerminal(get()) }
        // factory { UDPTerminal(get()) }
        // factory { IPTerminal(get()) }
    }

/**
 * Module for protocol handlers.
 *
 * Contains SoftEther, DHCP, and ARP protocol implementations.
 */
val protocolModule =
    module {
        // Protocol handlers will be added here when refactored
        // factory { SoftEtherClient(get()) }
        // factory { DhcpClient(get()) }
        // factory { ARPClient(get()) }
    }

/**
 * All application modules combined.
 *
 * Use this when starting Koin to load all modules at once.
 */
val allModules = listOf(appModule, networkModule, protocolModule)
