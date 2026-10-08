package com.example.communityfoodrescue

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoUpgradeTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun upgradePreservesExistingFoodAndBooking() {
        val name="upgrade-test.db"
        context.deleteDatabase(name)
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),null).use { legacy ->
                legacy.execSQL("CREATE TABLE people(id INTEGER PRIMARY KEY,name TEXT,role TEXT,eligible INTEGER)")
                legacy.execSQL("CREATE TABLE food(id INTEGER PRIMARY KEY AUTOINCREMENT,donor_id INTEGER,title TEXT,category TEXT,portions INTEGER,area TEXT,address TEXT,expiry INTEGER,allergens TEXT,notes TEXT,created_at INTEGER)")
                legacy.execSQL("CREATE TABLE bookings(id INTEGER PRIMARY KEY AUTOINCREMENT,food_id INTEGER,recipient_id INTEGER,volunteer_id INTEGER,pickup_at INTEGER,status TEXT,created_at INTEGER,collected_at INTEGER)")
                legacy.execSQL("INSERT INTO people VALUES(1,'Donor','DONOR',0),(2,'Recipient','RECIPIENT',1)")
                legacy.execSQL("INSERT INTO food VALUES(10,1,'Old food','Makanan siap',3,'Klang','Alamat lama',9999999999999,'Susu','Nota lama',100)")
                legacy.execSQL("INSERT INTO bookings VALUES(20,10,2,NULL,NULL,'RESERVED',100,NULL)")
                legacy.version=1
            }
            RescueDatabase(context,name).use { db ->
                assertEquals("Old food",db.food(10).title)
                assertNull(db.food(10).photoPath)
                assertEquals(Status.RESERVED,db.booking(20).status)
                assertEquals(5,db.foods().size)
                assertEquals(2,db.readableDatabase.version)
            }
            RescueDatabase(context,name).use { assertEquals(5,it.foods().size) }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun everyCategoryHasBundledPhoto() {
        assertEquals(8,FoodRules.categories.size)
        FoodRules.categories.forEach {
            val image=BitmapFactory.decodeResource(context.resources,FoodPhotos.defaultResource(it))
            assertNotNull(image); image.recycle()
        }
    }
}
