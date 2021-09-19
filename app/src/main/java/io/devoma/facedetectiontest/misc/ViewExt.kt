package io.devoma.facedetectiontest.misc

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE

/**
 * Sets the receiving view's visibility to [GONE].
 */
fun View.hide() {
    visibility = GONE
}

/**
 * Sets the receiving view's visibility to [VISIBLE].
 */
fun View.show() {
    visibility = VISIBLE
}