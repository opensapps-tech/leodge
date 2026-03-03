package com.example.leodge

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * MainActivity - Entry point of the React Native Android application.
 *
 * Extends ReactActivity which sets up the React Native bridge
 * and renders the JavaScript bundle.
 */
class MainActivity : ReactActivity() {

    /**
     * Returns the name of the main component registered in JavaScript.
     * Used to schedule rendering of the component.
     */
    override fun getMainComponentName(): String = "Leodge"

    /**
     * Creates the React Activity Delegate.
     * Fabric (new renderer) is enabled via fabricEnabled flag from gradle.properties.
     */
    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
