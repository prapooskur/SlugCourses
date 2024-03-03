package com.pras.slugcourses.ui

import android.content.Context
import android.widget.Toast

fun shortToast(text: String, context: Context) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}