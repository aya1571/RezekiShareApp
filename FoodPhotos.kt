package com.example.communityfoodrescue

import android.widget.ImageView

/** Food photos are bundled with the app and work offline. No gallery or permissions required. */
object FoodPhotos {
    fun defaultResource(category: String): Int = when(category) {
        "Roti & pastri" -> R.drawable.food_bread
        "Buah & sayur" -> R.drawable.food_fruit
        "Barangan kering" -> R.drawable.food_rice
        "Mi & pasta" -> R.drawable.food_pasta
        "Sup & bubur" -> R.drawable.food_soup
        "Kuih & pencuci mulut" -> R.drawable.food_cake
        "Produk tenusu" -> R.drawable.food_yogurt
        else -> R.drawable.food_meal
    }
    fun show(view: ImageView, category: String) {
        view.setImageResource(defaultResource(category))
        view.scaleType=ImageView.ScaleType.CENTER_CROP
        view.contentDescription="Foto makanan: $category"
    }
}
