package com.example.myapplication.util

import android.content.Context
import java.util.Properties
import java.io.IOException

/**
 * Utility class for managing environment configuration
 * Reads values from config.properties file in assets folder
 */
class ConfigUtil {
    companion object {
        private var properties: Properties? = null

        /**
         * Initialize the properties from the config file
         * Call this method in your Application class onCreate
         */
        fun init(context: Context) {
            if (properties == null) {
                properties = Properties()
                try {
                    val inputStream = context.assets.open("config.properties")
                    properties?.load(inputStream)
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Get a property value from the config file
         */
        fun getProperty(key: String, defaultValue: String = ""): String {
            return properties?.getProperty(key) ?: defaultValue
        }

        /**
         * Get the base URL for API calls
         */
        fun getRootUrl(): String {
            return getProperty("ROOT_URL", "http://10.0.1.13:8080")
        }
    }
}
