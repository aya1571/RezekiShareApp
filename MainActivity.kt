package com.example.communityfoodrescue

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var db: RescueDatabase
    private lateinit var page: LinearLayout
    private val prefs by lazy { getSharedPreferences("session", MODE_PRIVATE) }
    private var draft = Bundle()
    private var photoPreview: ImageView? = null
    private var photoCaption: TextView? = null
    private var actor = 1L
    private var screen = "home"
    private var query = ""
    private var category = "Semua kategori"
    private var area = "Semua kawasan"
    private val green = Color.parseColor("#245B40")
    private val ink = Color.parseColor("#20362B")
    private val muted = Color.parseColor("#59685D")
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun date(t: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("ms-MY")).format(Date(t))
    private fun user() = db.person(actor)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = RescueDatabase(this)
        draft=savedInstanceState?.getBundle("draft") ?: Bundle()
        actor = savedInstanceState?.getLong("actor") ?: prefs.getLong("actor",1)
        screen = savedInstanceState?.getString("screen") ?: "home"
        query = savedInstanceState?.getString("query") ?: ""
        category = savedInstanceState?.getString("category") ?: "Semua kategori"
        area = savedInstanceState?.getString("area") ?: "Semua kawasan"
        onBackPressedDispatcher.addCallback(this,object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if(screen != "home") { screen="home"; render() } else finish() }
        })
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("draft",draft)
        outState.putLong("actor",actor); outState.putString("screen",screen)
        outState.putString("query",query); outState.putString("category",category); outState.putString("area",area)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { db.close(); super.onDestroy() }
    private fun background(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat() }
    private fun column(parent: LinearLayout, card: Boolean = false): LinearLayout = LinearLayout(this).also {
        it.orientation=LinearLayout.VERTICAL
        it.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) }
        if(card) { it.background=background(Color.WHITE); it.setPadding(dp(18),dp(18),dp(18),dp(18)) }
        parent.addView(it)
    }
    private fun text(parent: LinearLayout, value: String, size: Float = 15f, bold: Boolean = false, color: Int = ink): TextView = TextView(this).also {
        it.text=value; it.textSize=size; it.setTextColor(color); it.setLineSpacing(dp(3).toFloat(),1f)
        if(bold) it.setTypeface(null,Typeface.BOLD)
        it.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) }
        parent.addView(it)
    }
    private fun button(parent: LinearLayout, label: String, primary: Boolean = true, action: () -> Unit): Button = Button(this).also {
        it.text=label; it.isAllCaps=false; it.textSize=14f; it.minHeight=dp(48)
        it.setTextColor(if(primary) Color.WHITE else green)
        it.backgroundTintList=android.content.res.ColorStateList.valueOf(if(primary) green else Color.parseColor("#E5EDDF"))
        it.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) }
        it.setOnClickListener { action() }; parent.addView(it)
    }
    private fun field(parent: LinearLayout, label: String, hint: String, numeric: Boolean = false): EditText {
        val heading=text(parent,label,14f,true)
        return EditText(this).also {
            it.id=View.generateViewId(); heading.labelFor=it.id; it.hint=hint; it.textSize=16f
            it.setTextColor(ink); it.setHintTextColor(muted); it.minHeight=dp(50)
            it.inputType=if(numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            it.setPadding(dp(12),dp(8),dp(12),dp(8)); it.background=background(Color.parseColor("#F0F3EA"),8)
            it.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) }; parent.addView(it)
        }
    }
    private fun spinner(parent: LinearLayout, label: String, options: List<String>, selected: String): Spinner {
        val heading=text(parent,label,14f,true)
        return Spinner(this).also {
            it.id=View.generateViewId(); heading.labelFor=it.id
            it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
            it.setSelection(options.indexOf(selected).coerceAtLeast(0)); it.minimumHeight=dp(48)
            parent.addView(it,LinearLayout.LayoutParams(-1,dp(52)))
        }
    }
    private fun runAction(success: String, next: String? = null, block: () -> Unit) {
        try { block(); if(next!=null) screen=next; render(); Toast.makeText(this,success,Toast.LENGTH_LONG).show() }
        catch(e: IllegalArgumentException) { errorDialog(e.message ?: "Semak maklumat anda.") }
        catch(e: android.database.SQLException) { errorDialog("Data tidak dapat disimpan. Cuba lagi; tempahan mungkin telah berubah.") }
    }
    private fun errorDialog(message: String) { AlertDialog.Builder(this).setTitle("Semak semula").setMessage(message).setPositiveButton("OK",null).show() }
    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Kembali",null).setPositiveButton("Sahkan") { _,_ -> action() }.show()
    }
    private fun render() {
        photoPreview=null; photoCaption=null
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F6EF")) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); insets
        }
        setContentView(root); ViewCompat.requestApplyInsets(root)
        val header=column(root).apply { setPadding(dp(20),dp(16),dp(20),0) }
        text(header,"RezekiShare",27f,true,green)
        text(header,"Lebihan makanan. Harapan baharu.",13f,false,muted)
        val me=user()
        button(header,"${me.role.label}  •  ${me.name}  ▾",false) { chooseProfile() }
        val scroll=ScrollView(this).apply { isFillViewport=true }
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(4),dp(20),dp(20)) }
        scroll.addView(page)
        when(screen) { "add" -> addScreen(); "activity" -> activityScreen(); "history" -> historyScreen(); else -> homeScreen() }
        val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(dp(8),0,dp(8),dp(6)); setBackgroundColor(Color.WHITE) }
        root.addView(nav)
        val tabs=mutableListOf("home" to "⌂\nTeroka")
        if(me.role==Role.DONOR) tabs.add("add" to "+\nDerma")
        tabs.add("activity" to "↗\nAktiviti"); tabs.add("history" to "◷\nSejarah")
        tabs.forEach { (key,label) ->
            val b=Button(this).apply {
                this.text=label; isAllCaps=false; textSize=12f; minHeight=dp(56); setPadding(0,0,0,0)
                setTextColor(if(screen==key) Color.WHITE else green)
                backgroundTintList=android.content.res.ColorStateList.valueOf(if(screen==key) green else Color.WHITE)
                contentDescription=label.substringAfter('\n')
                setOnClickListener { screen=key; render() }
            }
            nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))
        }
    }
    private fun chooseProfile() {
        val profiles=db.people()
        AlertDialog.Builder(this).setTitle("Pilih profil demo")
            .setItems(profiles.map { "${it.role.label}: ${it.name}" }.toTypedArray()) { _,index ->
                if(actor!=profiles[index].id) draft.clear()
                actor=profiles[index].id; prefs.edit().putLong("actor",actor).apply(); screen="home"; render()
            }.setNegativeButton("Tutup",null).show()
    }
    private fun homeScreen() {
        val banner=column(page,true)
        text(banner,"Kongsi rezeki,\nkurangkan pembaziran.",25f,true,green)
        val available=db.available()
        text(banner,"${available.size} tawaran tersedia  •  ${available.sumOf { it.portions }} hidangan",14f,true)
        text(banner,"Prototaip luar talian • Profil demo boleh ditukar di atas.",12f,false,muted)
        if(user().role==Role.RECIPIENT && !user().eligible) text(banner,"Profil ini belum disahkan layak untuk membuat tempahan.",14f,true,Color.parseColor("#9B3C24"))
        text(page,"Cari makanan",21f,true)
        val filters=column(page,true)
        val search=field(filters,"Carian", "Nama makanan atau kawasan").apply { setText(query) }
        val cat=spinner(filters,"Kategori",listOf("Semua kategori")+FoodRules.categories,category)
        val areas=listOf("Semua kawasan")+db.foods().map { it.area }.distinct().sorted()
        val zone=spinner(filters,"Kawasan pengambilan",areas,area)
        button(filters,"Cari & tapis") { query=search.text.toString().trim(); category=cat.selectedItem.toString(); area=zone.selectedItem.toString(); render() }
        if(query.isNotEmpty() || category!="Semua kategori" || area!="Semua kawasan") button(filters,"Kosongkan penapis",false) { query=""; category="Semua kategori"; area="Semua kawasan"; render() }
        val results=available.filter { (query.isBlank() || it.title.contains(query,true) || it.area.contains(query,true)) && (category=="Semua kategori" || it.category==category) && (area=="Semua kawasan" || it.area==area) }
        text(page,"${results.size} tawaran untuk diterokai",16f,true)
        if(results.isEmpty()) empty("Tiada makanan ditemui", "Cuba kawasan atau kategori lain. Penderma boleh menambah tawaran baharu.")
        results.forEach { f ->
            val card=foodCard(f)
            button(card,"Lihat butiran",false) { details(f) }
            if(user().role==Role.RECIPIENT && user().eligible) button(card,"Tempah ${f.portions} hidangan") {
                confirm("Tempah makanan?","Semua ${f.portions} hidangan ${f.title} akan ditempah untuk anda. Tetapkan masa pengambilan selepas ini.") {
                    runAction("Tempahan berjaya. Sila tetapkan jadual.","activity") { db.reserve(actor,f.id) }
                }
            }
        }
    }
    private fun foodCard(f: Food): LinearLayout {
        val card=column(page,true)
        val image=ImageView(this)
        FoodPhotos.show(image,f.category)
        card.addView(image,LinearLayout.LayoutParams(-1,dp(185)).apply { bottomMargin=dp(8) })
        text(card,"Foto makanan • Contoh kategori",11f,false,muted)
        text(card,f.category.uppercase(Locale.forLanguageTag("ms")),11f,true,green)
        text(card,f.title,21f,true)
        text(card,"${f.portions} hidangan  •  ${f.area}",15f,true)
        text(card,"Luput: ${date(f.expiry)}",13f,false,muted)
        return card
    }
    private fun details(f: Food) {
        AlertDialog.Builder(this).setTitle(f.title).setMessage("Penderma: ${db.person(f.donorId).name}\n\n${f.portions} hidangan • ${f.category}\n\nLokasi: ${f.address}\n\nLuput: ${date(f.expiry)}\n\nAlergen: ${f.allergens}\n\nNota: ${f.notes.ifBlank { "Tiada nota tambahan." }}\n\nSemak keadaan makanan dan alergen sebelum menerima.")
            .setPositiveButton("Tutup",null).show()
    }
    private fun addScreen() {
        if(user().role != Role.DONOR) { empty("Penderma sahaja","Tukar kepada profil penderma untuk menyenaraikan makanan."); return }
        text(page,"Kongsi makanan",25f,true)
        text(page,"Maklumat yang jelas membantu pengambilan yang selamat.",14f,false,muted)
        val form=column(page,true)
        val title=field(form,"Nama makanan *","Contoh: Nasi ayam")
        val cat=spinner(form,"Kategori *",FoodRules.categories,draft.getString("category") ?: FoodRules.categories.first())
        text(form,"Gambar makanan",14f,true)
        photoPreview=ImageView(this).also { form.addView(it,LinearLayout.LayoutParams(-1,dp(185))) }
        photoCaption=text(form,"",12f,false,muted)
        cat.onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                draft.putString("category",FoodRules.categories[position]); updatePhotoPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        updatePhotoPreview()
        val portions=field(form,"Bilangan hidangan *","Contoh: 8",true)
        val zone=field(form,"Kawasan *","Contoh: Shah Alam")
        val address=field(form,"Alamat pengambilan *","Alamat dan tempat pertemuan")
        val allergens=field(form,"Alergen *","Contoh: Susu, telur / Tiada diketahui")
        val notes=field(form,"Nota penyimpanan / pengambilan","Contoh: Simpan sejuk, bawa bekas")
        listOf("title" to title,"portions" to portions,"area" to zone,"address" to address,"allergens" to allergens,"notes" to notes).forEach { (key,input) ->
            input.setText(draft.getString(key, ""))
            input.doAfterTextChanged { draft.putString(key,it.toString()) }
        }
        var expiry=draft.getLong("expiry",0)
        val expiryText=text(form,if(expiry==0L) "Tarikh luput belum dipilih" else "Luput: ${date(expiry)}",14f,true)
        button(form,"Pilih tarikh & masa luput",false) { pickTime(System.currentTimeMillis()+86400000L) { expiry=it; draft.putLong("expiry",it); expiryText.text="Luput: ${date(it)}" } }
        val safe=CheckBox(this).apply { text="Saya telah menyemak keadaan makanan dan maklumat alergen."; setTextColor(ink); minHeight=dp(48) }
        safe.isChecked=draft.getBoolean("safe",false)
        safe.setOnCheckedChangeListener { _,checked -> draft.putBoolean("safe",checked) }
        form.addView(safe)
        button(form,"Terbitkan tawaran") {
            runAction("Tawaran makanan berjaya diterbitkan.","home") {
                require(safe.isChecked) { "Sila sahkan keadaan makanan dan maklumat alergen." }
                db.addFood(actor,title.text.toString(),cat.selectedItem.toString(),portions.text.toString().toIntOrNull() ?: 0,zone.text.toString(),address.text.toString(),expiry,allergens.text.toString(),notes.text.toString())
                draft.clear()
                query=""; category="Semua kategori"; area="Semua kawasan"
            }
        }
    }
    private fun updatePhotoPreview() {
        photoPreview?.let { image ->
            FoodPhotos.show(image,draft.getString("category") ?: FoodRules.categories.first())
            photoCaption?.text="Foto dipilih secara automatik mengikut kategori makanan."
        }
    }
    private fun pickTime(initial: Long, picked: (Long) -> Unit) {
        val cal=Calendar.getInstance().apply { timeInMillis=initial }
        DatePickerDialog(this,{ _,year,month,day ->
            cal.set(year,month,day)
            TimePickerDialog(this,{ _,hour,minute ->
                cal.set(Calendar.HOUR_OF_DAY,hour); cal.set(Calendar.MINUTE,minute); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0)
                picked(cal.timeInMillis)
            },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()
        },cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate=System.currentTimeMillis(); show()
        }
    }
    private fun relevant(b: Booking): Boolean = when(user().role) {
        Role.DONOR -> db.food(b.foodId).donorId == actor
        Role.RECIPIENT -> b.recipientId == actor
        Role.VOLUNTEER -> b.volunteerId == actor
    }
    private fun activityScreen() {
        text(page,when(user().role) { Role.DONOR -> "Urus sumbangan"; Role.RECIPIENT -> "Tempahan saya"; Role.VOLUNTEER -> "Tugasan pengambilan" },25f,true)
        button(page,"Muat semula status",false) { render() }
        val active=db.bookings().filter { it.status==Status.RESERVED || it.status==Status.SCHEDULED }
        val mine=active.filter { relevant(it) }
        if(mine.isEmpty()) empty("Tiada aktiviti aktif","Tempahan dan tugasan anda akan dipaparkan di sini.")
        mine.forEach { bookingCard(it,true) }
        if(user().role==Role.VOLUNTEER) {
            text(page,"Tugasan tersedia",21f,true)
            val open=active.filter { it.status==Status.SCHEDULED && it.volunteerId==null && db.food(it.foodId).expiry>System.currentTimeMillis() }
            if(open.isEmpty()) empty("Belum ada tugasan", "Tugasan muncul selepas penerima menetapkan masa pengambilan.")
            open.forEach { bookingCard(it,true) }
        }
    }
    private fun bookingCard(b: Booking, actionable: Boolean) {
        val f=db.food(b.foodId); val card=foodCard(f); val now=System.currentTimeMillis()
        val expired=f.expiry<=now && b.status!=Status.COLLECTED && b.status!=Status.CANCELLED
        text(card,"#${b.id}  •  ${b.status.label}${if(expired) " • Makanan luput" else ""}",16f,true,if(expired) Color.parseColor("#9B3C24") else green)
        text(card,"Penerima: ${db.person(b.recipientId).name}\nSukarelawan: ${b.volunteerId?.let { db.person(it).name } ?: "Belum ditugaskan"}",14f)
        text(card,"Pengambilan: ${b.pickupAt?.let { date(it) } ?: "Belum dijadualkan"}\nAlamat: ${f.address}",14f)
        b.collectedAt?.let { text(card,"Dikutip pada: ${date(it)}",14f,true) }
        button(card,"Butiran makanan & alergen",false) { details(f) }
        if(!actionable) return
        if(!expired && actor==b.recipientId && b.volunteerId==null) button(card,if(b.pickupAt==null) "Jadualkan pengambilan" else "Ubah jadual") {
            pickTime(maxOf(System.currentTimeMillis()+120000L,b.pickupAt ?: 0L)) { pickup -> runAction("Jadual pengambilan disimpan.") { db.schedule(actor,b.id,pickup) } }
        }
        if(!expired && user().role==Role.VOLUNTEER && b.volunteerId==null && b.status==Status.SCHEDULED) button(card,"Ambil tugasan ini") {
            confirm("Terima tugasan?","Ambil makanan di ${f.address} pada ${b.pickupAt?.let { date(it) }} untuk ${db.person(b.recipientId).name}.") {
                runAction("Tugasan telah diberikan kepada anda.") { db.claim(actor,b.id) }
            }
        }
        if(!expired && b.status==Status.SCHEDULED && (actor==f.donorId || actor==b.volunteerId)) {
            if((b.pickupAt ?: Long.MAX_VALUE)>now) text(card,"Pengesahan kutipan dibuka pada waktu pengambilan. Tekan muat semula apabila tiba masanya.",13f,false,muted)
            else button(card,"Sahkan sudah dikutip") {
                confirm("Sahkan kutipan?","Sahkan ${f.portions} hidangan telah diambil dan keadaan makanan telah diperiksa.") {
                    runAction("Kutipan selesai. Rekod disimpan dalam sejarah.","history") { db.collect(actor,b.id) }
                }
            }
        }
        if(actor==b.recipientId || actor==f.donorId) button(card,"Batalkan tempahan",false) {
            confirm("Batalkan tempahan?","Tawaran akan tersedia semula jika makanan belum luput. Rekod pembatalan kekal dalam sejarah.") { runAction("Tempahan dibatalkan.") { db.cancel(actor,b.id) } }
        }
    }
    private fun historyScreen() {
        text(page,"Jejak kebaikan",25f,true)
        val history=db.bookings().filter { relevant(it) }
        val completed=history.filter { it.status==Status.COLLECTED }
        val summary=column(page,true)
        text(summary,"${completed.sumOf { db.food(it.foodId).portions }} hidangan diselamatkan",22f,true,green)
        text(summary,"${completed.size} kutipan selesai • ${history.count { it.status==Status.CANCELLED }} dibatalkan",14f)
        text(summary,"Rekod disimpan pada peranti ini, termasuk selepas aplikasi ditutup.",13f,false,muted)
        if(user().role==Role.DONOR) {
            text(page,"Semua sumbangan saya",21f,true)
            val foods=db.foods().filter { it.donorId==actor }
            if(foods.isEmpty()) empty("Belum ada sumbangan","Tambah makanan melalui tab Derma.")
            foods.forEach { f ->
                val card=foodCard(f)
                val b=db.bookings().firstOrNull { it.foodId==f.id && it.status!=Status.CANCELLED }
                val label=if(b?.status==Status.COLLECTED) "Selesai" else if(f.expiry<=System.currentTimeMillis()) "Luput" else b?.status?.label ?: "Tersedia"
                text(card,"Status: $label • Disenaraikan ${date(f.createdAt)}",14f,true,green)
                button(card,"Butiran sumbangan",false) { details(f) }
            }
        }
        text(page,"Sejarah tempahan & kutipan",21f,true)
        if(history.isEmpty()) empty("Belum ada rekod","Sejarah akan terhasil apabila makanan ditempah.")
        history.forEach { bookingCard(it,false) }
    }
    private fun empty(title: String, message: String) { val card=column(page,true); text(card,title,18f,true); text(card,message,14f,false,muted) }
}
