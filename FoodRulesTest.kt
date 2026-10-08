package com.example.communityfoodrescue

import org.junit.Assert.assertThrows
import org.junit.Test

class FoodRulesTest {
    private val now=1_800_000_000_000L
    @Test fun validFoodAccepted() { FoodRules.validateFood("Nasi ayam",8,"Shah Alam","Seksyen 7, Shah Alam",now+1000,"Telur",now) }
    @Test fun invalidPortionsRejected() { for(n in listOf(0,-1,501)) assertThrows(IllegalArgumentException::class.java) { FoodRules.validateFood("Nasi ayam",n,"Shah Alam","Seksyen 7",now+1000,"Telur",now) } }
    @Test fun expiredListingRejected() { assertThrows(IllegalArgumentException::class.java) { FoodRules.validateFood("Nasi ayam",8,"Shah Alam","Seksyen 7",now,"Telur",now) } }
    @Test fun allergenDisclosureRequired() { assertThrows(IllegalArgumentException::class.java) { FoodRules.validateFood("Nasi ayam",8,"Shah Alam","Seksyen 7",now+1000," ",now) } }
    @Test fun pickupMustBeStrictlyWithinWindow() {
        for(time in listOf(now-1,now,now+1000,now+1001)) assertThrows(IllegalArgumentException::class.java) { FoodRules.validatePickup(time,now+1000,now) }
        FoodRules.validatePickup(now+1,now+1000,now)
    }
}
