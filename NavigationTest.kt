package com.example.communityfoodrescue

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.startsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @Test fun allRolesCanNavigate() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        context.getSharedPreferences("session",0).edit().putLong("actor",1).commit()
        val activity=inst.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            onView(withText("RezekiShare")).check(matches(isDisplayed()))
            onView(withContentDescription("Derma")).perform(click())
            onView(withText("Kongsi makanan")).check(matches(isDisplayed()))
            onView(withText(startsWith("Penderma  •"))).perform(click())
            onView(withText("Penerima: Aina • Keluarga B40")).perform(click())
            onView(withContentDescription("Aktiviti")).perform(click())
            onView(withText("Tempahan saya")).check(matches(isDisplayed()))
            onView(withText(startsWith("Penerima  •"))).perform(click())
            onView(withText("Sukarelawan: Amir • Sukarelawan")).perform(click())
            onView(withContentDescription("Aktiviti")).perform(click())
            onView(withText("Tugasan pengambilan")).check(matches(isDisplayed()))
            onView(withContentDescription("Sejarah")).perform(click())
            onView(withText("Jejak kebaikan")).check(matches(isDisplayed()))
        } finally {
            inst.runOnMainSync { activity.finish() }
            context.getSharedPreferences("session",0).edit().putLong("actor",1).commit()
        }
    }
}
