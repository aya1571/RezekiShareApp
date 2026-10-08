package com.example.communityfoodrescue

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Offline prototype. Mutations recheck permissions and state inside a transaction. */
class RescueDatabase(context: Context, name: String = "rezeki_share.db") : SQLiteOpenHelper(context, name, null, 2) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE people(id INTEGER PRIMARY KEY, name TEXT NOT NULL,
            role TEXT NOT NULL CHECK(role IN ('DONOR','RECIPIENT','VOLUNTEER')), eligible INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("""CREATE TABLE food(id INTEGER PRIMARY KEY AUTOINCREMENT, donor_id INTEGER NOT NULL REFERENCES people(id),
            title TEXT NOT NULL, category TEXT NOT NULL, portions INTEGER NOT NULL CHECK(portions BETWEEN 1 AND 500),
            area TEXT NOT NULL, address TEXT NOT NULL, expiry INTEGER NOT NULL, allergens TEXT NOT NULL,
            notes TEXT NOT NULL, created_at INTEGER NOT NULL, photo_path TEXT)""")
        db.execSQL("""CREATE TABLE bookings(id INTEGER PRIMARY KEY AUTOINCREMENT, food_id INTEGER NOT NULL REFERENCES food(id),
            recipient_id INTEGER NOT NULL REFERENCES people(id), volunteer_id INTEGER REFERENCES people(id),
            pickup_at INTEGER, status TEXT NOT NULL CHECK(status IN ('RESERVED','SCHEDULED','COLLECTED','CANCELLED')),
            created_at INTEGER NOT NULL, collected_at INTEGER)""")
        // A cancelled booking releases the listing. Completed listings can never be reserved again.
        db.execSQL("CREATE UNIQUE INDEX one_active_booking ON bookings(food_id) WHERE status != 'CANCELLED'")
        db.execSQL("CREATE INDEX food_expiry ON food(expiry)")
        db.execSQL("INSERT INTO people VALUES(1,'Dapur Kak Siti','DONOR',0),(2,'Aina • Keluarga B40','RECIPIENT',1),(3,'Amir • Sukarelawan','VOLUNTEER',0),(4,'Penerima belum disahkan','RECIPIENT',0),(5,'Farah • Keluarga B40','RECIPIENT',1),(6,'Ravi • Sukarelawan','VOLUNTEER',0)")
        val now = System.currentTimeMillis()
        val samples = listOf(
            arrayOf("Nasi sayur & ayam", "Makanan siap", "8", "Shah Alam", "Dapur Kak Siti, Seksyen 7, Shah Alam", "Telur, soya", "Disediakan hari ini. Simpan sejuk; panaskan sebelum makan."),
            arrayOf("Roti bun segar", "Roti & pastri", "12", "Petaling Jaya", "Kaunter komuniti, SS2, Petaling Jaya", "Gandum, susu, telur", "Dibungkus berasingan. Bawa beg sendiri."),
            arrayOf("Bakul buah campuran", "Buah & sayur", "6", "Shah Alam", "Pusat komuniti, Seksyen 13, Shah Alam", "Tiada diketahui", "Epal dan oren. Basuh sebelum dimakan."),
            arrayOf("Pek beras & biskut", "Barangan kering", "5", "Klang", "Pusat komuniti, Bandar Bukit Tinggi, Klang", "Gandum", "Bungkusan masih tertutup. Simpan di tempat kering.")
        )
        samples.forEachIndexed { i, a ->
            db.insertOrThrow("food", null, foodValues(1, a[0], a[1], a[2].toInt(), a[3], a[4], now + (i+1)*24*60*60*1000L, a[5], a[6], now))
        }
        seedExtraFood(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE food ADD COLUMN photo_path TEXT")
            seedExtraFood(db)
        }
    }
    private fun seedExtraFood(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val samples = listOf(
            arrayOf("Spageti sos tomato", "Mi & pasta", "6", "Gandum", "Dimasak hari ini. Simpan sejuk dan panaskan sebelum makan."),
            arrayOf("Sup sayur", "Sup & bubur", "8", "Saderi", "Dibungkus dalam bekas bertutup. Simpan sejuk."),
            arrayOf("Kek vanila", "Kuih & pencuci mulut", "10", "Gandum, susu, telur", "Potongan berasingan. Simpan dalam peti sejuk."),
            arrayOf("Yogurt buah", "Produk tenusu", "5", "Susu", "Simpan sejuk sepanjang masa dan ambil dalam beg berpenebat.")
        )
        samples.forEach { a ->
            db.insertOrThrow("food", null, foodValues(1, a[0], a[1], a[2].toInt(), "Shah Alam",
                "Dapur Kak Siti, Seksyen 7, Shah Alam", now + 24*60*60*1000L, a[3], a[4], now))
        }
    }
    private fun foodValues(donor: Long, title: String, category: String, portions: Int, area: String,
                           address: String, expiry: Long, allergens: String, notes: String, now: Long, photoPath: String? = null) = ContentValues().apply {
        put("donor_id", donor); put("title", title.trim()); put("category", category); put("portions", portions)
        put("area", area.trim()); put("address", address.trim()); put("expiry", expiry)
        put("allergens", allergens.trim()); put("notes", notes.trim()); put("created_at", now); put("photo_path", photoPath)
    }
    private fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try { val result = block(db); db.setTransactionSuccessful(); return result }
        finally { db.endTransaction() }
    }
    fun people(): List<Person> = readableDatabase.rawQuery("SELECT * FROM people ORDER BY id", null).use { c ->
        buildList { while(c.moveToNext()) add(Person(c.getLong(0),c.getString(1),Role.valueOf(c.getString(2)),c.getInt(3)==1)) }
    }
    fun person(id: Long): Person = people().firstOrNull { it.id == id } ?: error("Profil tidak dijumpai.")
    private fun Cursor.food() = Food(getLong(0), getLong(1), getString(2), getString(3), getInt(4), getString(5), getString(6), getLong(7), getString(8), getString(9), getLong(10), getString(11))
    private fun Cursor.booking() = Booking(getLong(0),getLong(1),getLong(2),if(isNull(3)) null else getLong(3),if(isNull(4)) null else getLong(4),Status.valueOf(getString(5)),getLong(6),if(isNull(7)) null else getLong(7))
    fun foods(): List<Food> = readableDatabase.rawQuery("SELECT * FROM food ORDER BY created_at DESC, id DESC", null).use { c -> buildList { while(c.moveToNext()) add(c.food()) } }
    fun food(id: Long): Food = foods().firstOrNull { it.id == id } ?: error("Makanan tidak dijumpai.")
    fun bookings(): List<Booking> = readableDatabase.rawQuery("SELECT * FROM bookings ORDER BY created_at DESC, id DESC",null).use { c -> buildList { while(c.moveToNext()) add(c.booking()) } }
    fun booking(id: Long) = bookings().firstOrNull { it.id == id } ?: error("Tempahan tidak dijumpai.")
    fun available(now: Long = System.currentTimeMillis()): List<Food> {
        val taken = bookings().filter { it.status != Status.CANCELLED }.map { it.foodId }.toSet()
        return foods().filter { it.expiry > now && it.id !in taken }
    }
    fun addFood(actor: Long, title: String, category: String, portions: Int, area: String, address: String, expiry: Long, allergens: String, notes: String, photoPath: String? = null): Long = transaction { db ->
        require(person(actor).role == Role.DONOR) { "Hanya penderma boleh menyenaraikan makanan." }
        FoodRules.validateFood(title, portions, area, address, expiry, allergens, System.currentTimeMillis())
        require(category in FoodRules.categories) { "Kategori tidak sah." }
        require(allergens.length <= 200 && notes.length <= 500) { "Maklumat alergen atau nota terlalu panjang." }
        db.insertOrThrow("food",null,foodValues(actor,title,category,portions,area,address,expiry,allergens,notes,System.currentTimeMillis(),photoPath))
    }
    fun reserve(actor: Long, foodId: Long): Long = transaction { db ->
        val user = person(actor)
        require(user.role == Role.RECIPIENT && user.eligible) { "Tempahan memerlukan profil penerima yang telah disahkan layak." }
        require(available().any { it.id == foodId }) { "Makanan telah ditempah atau telah luput." }
        db.insertOrThrow("bookings",null,ContentValues().apply {
            put("food_id",foodId); put("recipient_id",actor); put("status",Status.RESERVED.name); put("created_at",System.currentTimeMillis())
        })
    }
    fun schedule(actor: Long, bookingId: Long, pickup: Long) = transaction { db ->
        val b = booking(bookingId)
        require(b.recipientId == actor) { "Hanya pemilik tempahan boleh menetapkan jadual." }
        require(b.status == Status.RESERVED || b.status == Status.SCHEDULED) { "Tempahan ini tidak boleh dijadualkan." }
        require(b.volunteerId == null) { "Sukarelawan telah ditugaskan. Batalkan tempahan jika jadual perlu diubah." }
        FoodRules.validatePickup(pickup, food(b.foodId).expiry, System.currentTimeMillis())
        db.update("bookings", ContentValues().apply { put("pickup_at",pickup); put("status",Status.SCHEDULED.name) },"id=?", arrayOf(bookingId.toString()))
    }
    fun claim(actor: Long, bookingId: Long) = transaction { db ->
        val b = booking(bookingId)
        require(person(actor).role == Role.VOLUNTEER) { "Hanya sukarelawan boleh mengambil tugasan." }
        require(b.status == Status.SCHEDULED && b.volunteerId == null) { "Tugasan tidak lagi tersedia." }
        require(food(b.foodId).expiry > System.currentTimeMillis()) { "Makanan sudah luput. Tugasan tidak boleh diambil." }
        db.update("bookings",ContentValues().apply { put("volunteer_id",actor) },"id=?",arrayOf(bookingId.toString()))
    }
    fun collect(actor: Long, bookingId: Long) = transaction { db ->
        val b = booking(bookingId); val f = food(b.foodId); val now = System.currentTimeMillis()
        require(actor == f.donorId || actor == b.volunteerId) { "Pengesahan hanya oleh penderma atau sukarelawan yang ditugaskan." }
        require(b.status == Status.SCHEDULED) { "Tetapkan jadual pengambilan dahulu." }
        require(now >= (b.pickupAt ?: Long.MAX_VALUE)) { "Belum tiba masa pengambilan yang dijadualkan." }
        require(now < f.expiry) { "Makanan sudah luput dan tidak boleh dikutip." }
        db.update("bookings",ContentValues().apply { put("status",Status.COLLECTED.name); put("collected_at",now) },"id=?",arrayOf(bookingId.toString()))
    }
    fun cancel(actor: Long, bookingId: Long) = transaction { db ->
        val b = booking(bookingId)
        require(actor == b.recipientId || actor == food(b.foodId).donorId) { "Hanya penerima atau penderma boleh membatalkan tempahan." }
        require(b.status == Status.RESERVED || b.status == Status.SCHEDULED) { "Tempahan ini sudah ditutup." }
        db.update("bookings",ContentValues().apply { put("status",Status.CANCELLED.name) },"id=?",arrayOf(bookingId.toString()))
    }
}
