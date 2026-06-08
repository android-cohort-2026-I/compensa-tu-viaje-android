package com.compensatuviaje.tracker.feature.database

import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseModuleTest {

    @Test
    fun placeholder_passes() {
        val module = DatabaseModule()
        assertNotNull(module)
    }

    @Test
    fun testDatabaseCompanionAccess() {
        assertNotNull(DatabaseModule.Companion)
    }
}
