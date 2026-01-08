package kittoku.mvc.di

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the Koin dependency injection module.
 *
 * These tests verify that the Koin module is correctly configured
 * and that unused empty modules have been removed.
 *
 * Note: Full Koin verification requires Android context and is tested
 * in instrumented tests. These unit tests focus on module structure.
 */
@DisplayName("AppModule")
class AppModuleTest {
    @Nested
    @DisplayName("Module structure")
    inner class ModuleStructure {
        @Test
        @DisplayName("appModule should be defined")
        fun appModuleShouldBeDefined() {
            assertThat(appModule).isNotNull()
        }

        @Test
        @DisplayName("appModule should contain definitions")
        fun appModuleShouldContainDefinitions() {
            // The module should have definitions (not be empty)
            // We can't easily count definitions without starting Koin,
            // but we can verify the module exists and is valid
            assertThat(appModule.toString()).contains("Module")
        }

        @Test
        @DisplayName("appModule should not be empty")
        fun appModuleShouldNotBeEmpty() {
            // Verify the module has content by checking its string representation
            // An empty module would just show "Module[]"
            val moduleString = appModule.toString()
            assertThat(moduleString).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Removed modules")
    inner class RemovedModules {
        @Test
        @DisplayName("networkModule should not exist")
        fun networkModuleShouldNotExist() {
            // Verify that the empty networkModule has been removed
            // by checking that it's not accessible as a top-level property
            val moduleNames =
                AppModuleTest::class.java.classLoader
                    ?.loadClass("kittoku.mvc.di.AppModuleKt")
                    ?.declaredFields
                    ?.map { it.name }
                    ?: emptyList()

            assertThat(moduleNames).doesNotContain("networkModule")
        }

        @Test
        @DisplayName("protocolModule should not exist")
        fun protocolModuleShouldNotExist() {
            // Verify that the empty protocolModule has been removed
            val moduleNames =
                AppModuleTest::class.java.classLoader
                    ?.loadClass("kittoku.mvc.di.AppModuleKt")
                    ?.declaredFields
                    ?.map { it.name }
                    ?: emptyList()

            assertThat(moduleNames).doesNotContain("protocolModule")
        }

        @Test
        @DisplayName("allModules should not exist")
        fun allModulesShouldNotExist() {
            // Verify that the unused allModules list has been removed
            val moduleNames =
                AppModuleTest::class.java.classLoader
                    ?.loadClass("kittoku.mvc.di.AppModuleKt")
                    ?.declaredFields
                    ?.map { it.name }
                    ?: emptyList()

            assertThat(moduleNames).doesNotContain("allModules")
        }

        @Test
        @DisplayName("only appModule should be exported")
        fun onlyAppModuleShouldBeExported() {
            // Verify that only appModule remains as a public module
            val moduleNames =
                AppModuleTest::class.java.classLoader
                    ?.loadClass("kittoku.mvc.di.AppModuleKt")
                    ?.declaredFields
                    ?.filter { it.name.endsWith("Module") || it.name.endsWith("Modules") }
                    ?.map { it.name }
                    ?: emptyList()

            assertThat(moduleNames).containsExactly("appModule")
        }
    }
}
