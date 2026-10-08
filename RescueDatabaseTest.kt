package com.example.communityfoodrescue

import android.content.ContentValues
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RescueDatabaseTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: RescueDatabase
    private val name="rescue-test.db"
    @Before fun before() { context.deleteDatabase(name); db=RescueDatabase(context,name); db.writableDatabase }
    @After fun after() { db.close(); context.deleteDatabase(name) }
    private fun rejection(action: () -> Unit) { assertThrows(IllegalArgumentException::class.java) { action() } }
    @Test fun completeWorkflowSurvivesReopen() {
        val food=db.addFood(1,"Nasi ujian","Makanan siap",4,"Shah Alam","Kaunter Seksyen 7",System.currentTimeMillis()+600000,"Telur","Simpan sejuk")
        val b=db.reserve(2,food)
        db.schedule(2,b,System.currentTimeMillis()+120000)
        db.claim(3,b)
        rejection { db.collect(3,b) } // Cannot collect before scheduled time.
        db.writableDatabase.update("bookings",ContentValues().apply { put("pickup_at",System.currentTimeMillis()-1000) },"id=?",arrayOf(b.toString()))
        db.collect(3,b)
        db.close(); db=RescueDatabase(context,name)
        assertEquals(Status.COLLECTED,db.booking(b).status)
        assertNotNull(db.booking(b).collectedAt)
        assertEquals(3L,db.booking(b).volunteerId)
        assertFalse(db.available().any { it.id==food })
        rejection { db.reserve(5,food) }
    }
    @Test fun eligibilityAndDoubleBookingEnforced() {
        val food=db.available().first().id
        rejection { db.reserve(4,food) }; rejection { db.reserve(1,food) }
        db.reserve(2,food); rejection { db.reserve(5,food) }
    }
    @Test fun cancelReleasesAndPreservesHistory() {
        val food=db.available().first().id
        val b=db.reserve(2,food); db.cancel(2,b)
        assertEquals(Status.CANCELLED,db.booking(b).status)
        assertTrue(db.available().any { it.id==food })
        val second=db.reserve(5,food); assertNotEquals(b,second)
        assertEquals(2,db.bookings().count { it.foodId==food })
    }
    @Test fun rolesOwnershipAndAssignmentEnforced() {
        val b=db.reserve(2,db.available().first().id)
        rejection { db.schedule(5,b,System.currentTimeMillis()+60000) }
        rejection { db.claim(3,b) }; rejection { db.collect(1,b) }
        db.schedule(2,b,System.currentTimeMillis()+60000)
        rejection { db.claim(2,b) }; db.claim(3,b); rejection { db.claim(6,b) }
        rejection { db.collect(6,b) }; rejection { db.cancel(5,b) }
        rejection { db.schedule(2,b,System.currentTimeMillis()+120000) }
    }
    @Test fun expiredFoodBlockedAtEveryStage() {
        val f=db.available().first(); val b=db.reserve(2,f.id)
        rejection { db.schedule(2,b,f.expiry) }
        db.schedule(2,b,System.currentTimeMillis()+60000)
        db.writableDatabase.update("food",ContentValues().apply { put("expiry",System.currentTimeMillis()-1) },"id=?",arrayOf(f.id.toString()))
        rejection { db.claim(3,b) }; rejection { db.collect(1,b) }
        db.cancel(2,b); rejection { db.reserve(2,f.id) }
    }
    @Test fun donorCanConfirmSelfPickupButNotTwice() {
        val b=db.reserve(2,db.available().first().id)
        db.schedule(2,b,System.currentTimeMillis()+60000)
        db.writableDatabase.update("bookings",ContentValues().apply { put("pickup_at",System.currentTimeMillis()-1000) },"id=?",arrayOf(b.toString()))
        db.collect(1,b); rejection { db.collect(1,b) }; rejection { db.cancel(2,b) }
    }
}
