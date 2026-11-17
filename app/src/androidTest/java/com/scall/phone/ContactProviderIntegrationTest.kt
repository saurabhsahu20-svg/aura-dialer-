package com.scall.phone

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scall.phone.data.ContactManager
import com.scall.phone.data.ContactValue
import com.scall.phone.data.ManagedContact
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses only a temporary synthetic contact in an Android emulator. */
@RunWith(AndroidJUnit4::class)
class ContactProviderIntegrationTest {
    @Test
    fun realAndroidProviderCreatesEditsReadsAndDeletesContact() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, Manifest.permission.READ_CONTACTS
        )
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, Manifest.permission.WRITE_CONTACTS
        )
        val manager = ContactManager(context)
        assertTrue(manager.canRead() && manager.canWrite())
        val person = ManagedContact(
            first = "SCaller", last = "QualityTest",
            phones = listOf(ContactValue("5550102", "Mobile"), ContactValue("5550103", "Work")),
            emails = listOf(ContactValue("qa@example.invalid", "Work")),
            notes = "Temporary S Caller test contact"
        )
        val id = manager.save(person)
        assertTrue("Provider must return a real contact ID", id > 0L)
        try {
            val loaded = manager.load(id)
            assertEquals("SCaller QualityTest", loaded?.fullName)
            assertEquals(2, manager.numbers(id).size)
            assertEquals("Temporary S Caller test contact", loaded?.notes)
            val updated = loaded!!.copy(first = "UpdatedSCaller", jobTitle = "QA")
            assertEquals(id, manager.save(updated))
            val reread = manager.load(id)
            assertEquals("UpdatedSCaller QualityTest", reread?.fullName)
            assertEquals("QA", reread?.jobTitle)
            assertEquals(2, manager.numbers(id).size)
        } finally {
            assertTrue("Temporary test contact must be removed", manager.delete(id))
        }
    }
}
