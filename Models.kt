package com.example.communityfoodrescue

/** Stable IDs are stored in SQLite; labels can be translated without changing records. */
enum class Role(val label: String) { DONOR("Penderma"), RECIPIENT("Penerima"), VOLUNTEER("Sukarelawan") }
enum class Status(val label: String) {
    RESERVED("Ditempah"), SCHEDULED("Dijadualkan"), COLLECTED("Selesai"), CANCELLED("Dibatalkan")
}
data class Person(val id: Long, val name: String, val role: Role, val eligible: Boolean)
data class Food(
    val id: Long, val donorId: Long, val title: String, val category: String,
    val portions: Int, val area: String, val address: String, val expiry: Long,
    val allergens: String, val notes: String, val createdAt: Long, val photoPath: String? = null
)
data class Booking(
    val id: Long, val foodId: Long, val recipientId: Long, val volunteerId: Long?,
    val pickupAt: Long?, val status: Status, val createdAt: Long, val collectedAt: Long?
)
object FoodRules {
    val categories = listOf("Makanan siap", "Roti & pastri", "Buah & sayur", "Barangan kering",
        "Mi & pasta", "Sup & bubur", "Kuih & pencuci mulut", "Produk tenusu")
    fun validateFood(title: String, portions: Int, area: String, address: String,
                     expiry: Long, allergens: String, now: Long) {
        require(title.trim().length in 3..80) { "Nama makanan mesti 3 hingga 80 aksara." }
        require(portions in 1..500) { "Kuantiti mesti antara 1 dan 500 hidangan." }
        require(area.trim().length in 2..60) { "Masukkan kawasan (2 hingga 60 aksara)." }
        require(address.trim().length in 5..200) { "Masukkan alamat pengambilan yang lengkap." }
        require(expiry > now) { "Tarikh luput mesti pada masa akan datang." }
        require(allergens.trim().isNotEmpty()) { "Nyatakan alergen, atau tulis 'Tiada diketahui'." }
    }
    fun validatePickup(pickup: Long, expiry: Long, now: Long) {
        require(pickup > now) { "Pilih masa pengambilan pada masa akan datang." }
        require(pickup < expiry) { "Pengambilan mesti sebelum makanan luput." }
    }
}
