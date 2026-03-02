// android/app/src/main/java/com/leodge/LeodgeWidgetModule.kt
package com.leodge

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class LeodgeWidgetModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "LeodgeWidgetModule"

    @ReactMethod
    fun updateWidget(totalValue: String) {
        val context = reactApplicationContext
        LeodgeWidget.updateAllWidgets(context, totalValue)
    }
}
