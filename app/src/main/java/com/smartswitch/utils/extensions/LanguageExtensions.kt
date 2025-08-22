package com.smartswitch.utils.extensions

import android.content.Context
import com.smartswitch.R
import com.smartswitch.presentation.language.LanguageModel
import com.zeugmasolutions.localehelper.Locales

fun Context.getLanguageData() = arrayListOf(
    LanguageModel(R.drawable.english, getString(R.string.english)),
    LanguageModel(R.drawable.urdu, getString(R.string.urdu)),
    LanguageModel(R.drawable.arabic, getString(R.string.arabic)),
    LanguageModel(R.drawable.hindi, getString(R.string.hindi)),
    LanguageModel(R.drawable.german, getString(R.string.german)),
    LanguageModel(R.drawable.spanish, getString(R.string.spanish)),
    LanguageModel(R.drawable.italian, getString(R.string.italian))
)

fun getMyLocales(pos: Int) = when (pos) {
    0 -> {
        Locales.English
    }

    1 -> {
        Locales.Urdu
    }

    2 -> {
        Locales.Arabic
    }

    3 -> {
        Locales.Hindi
    }

    4 -> {
        Locales.German
    }

    5 -> {
        Locales.Spanish
    }

    6 -> {
        Locales.Italian
    }


//
//    7 -> {
//        Locales.French
//    }
//
//    8 -> {
//        Locales.Japanese
//    }

    else -> Locales.English
}
