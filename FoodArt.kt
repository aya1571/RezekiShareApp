package com.example.communityfoodrescue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Original vector-style food illustrations; available offline at any screen density. */
class FoodArt(context: Context, private val category: String) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription = "Ilustrasi $category"; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.save(); c.scale(width/320f, height/130f)
        fun oval(color: String, l: Float, t: Float, r: Float, b: Float) { p.color=Color.parseColor(color); c.drawOval(RectF(l,t,r,b),p) }
        fun box(color: String,l:Float,t:Float,r:Float,b:Float,rad:Float=12f) { p.color=Color.parseColor(color); c.drawRoundRect(RectF(l,t,r,b),rad,rad,p) }
        box("#EEF3E6",0f,0f,320f,130f,16f)
        oval("#DCE7CE",215f,-35f,355f,105f); oval("#E3ECCC",-30f,80f,95f,185f)
        oval("#CBD8BE",87f,99f,236f,116f)
        when(category) {
            "Roti & pastri" -> {
                oval("#C58238",85f,44f,234f,107f); oval("#EFB763",85f,31f,234f,94f)
                p.color=Color.parseColor("#FFF0CB"); p.strokeWidth=7f; p.strokeCap=Paint.Cap.ROUND
                for(x in listOf(121f,153f,185f)) c.drawLine(x,44f,x+13f,70f,p)
            }
            "Buah & sayur" -> {
                oval("#E9684B",98f,48f,159f,105f); oval("#F5AB38",151f,46f,210f,106f)
                oval("#629344",127f,27f,169f,80f); oval("#315D37",113f,29f,137f,49f)
                oval("#FFE3A0",168f,56f,181f,67f)
            }
            "Barangan kering" -> {
                box("#CB9D62",105f,27f,164f,105f); box("#EAD5A9",110f,41f,159f,93f)
                box("#C67046",171f,43f,215f,105f); box("#F4DEB5",176f,55f,210f,95f)
                oval("#FFFFEB",120f,57f,150f,80f); oval("#CA9453",183f,64f,204f,85f)
            }
            else -> {
                oval("#FFFFFF",82f,39f,237f,109f); oval("#DFE8D2",94f,46f,225f,98f)
                oval("#FFF7DA",113f,43f,188f,91f); oval("#CE8151",167f,54f,212f,86f)
                for(x in listOf(104f,121f,193f)) oval("#5E9146",x,48f,x+20f,67f)
                oval("#EC7753",102f,66f,121f,83f)
            }
        }
        c.restore()
    }
}
