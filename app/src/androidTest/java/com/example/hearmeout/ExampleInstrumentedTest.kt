package com.example.hearmeout

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device or emulator.
 * This class verifies that the application context and environment are correctly configured.
 * * [AndroidJUnit4] is the test runner that allows the tests to interact with the Android OS.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Verifies that the app's package name matches the expected identifier.
     * This ensures the build variant and context are correctly initialized.
     */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Asserting that the package name is correct to prevent configuration errors
        assertEquals("com.example.hearmeout", appContext.packageName)
    }
}