package com.statistical.android.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 事件
 * 
 */
class Event {
    private static final String SEGMENTATION_KEY = "segmentation";
    private static final String KEY_KEY = "key";
    private static final String COUNT_KEY = "count";
    private static final String SUM_KEY = "sum";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String DAY_OF_WEEK = "dow";
    private static final String HOUR = "hour";

    public String key;
    public Map<String, String> segmentation;
    public int count;
    public double sum;
    public int timestamp;
    public int hour;
    public int dow;

    /**
     * @return a JSONObject containing the event data from this object
     */
    JSONObject toJSON() {
        final JSONObject json = new JSONObject();

        try {
            json.put(KEY_KEY, key);
            json.put(COUNT_KEY, count);
            json.put(TIMESTAMP_KEY, timestamp);
            json.put(HOUR, hour);
            json.put(DAY_OF_WEEK, dow);

            if (segmentation != null) {
                json.put(SEGMENTATION_KEY, new JSONObject(segmentation));
            }

            json.put(SUM_KEY, sum);
        }
        catch (JSONException e) {
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.w(Statistical.TAG, "Got exception converting an Event to JSON", e);
            }
        }

        return json;
    }

    /**
     * @param json JSON object to extract event data from
     * @return Event object built from the data in the JSON or null if the "key" value is not
     *         present or the empty string, or if a JSON exception occurs
     * @throws NullPointerException if JSONObject is null
     */
    static Event fromJSON(final JSONObject json) {
        Event event = new Event();

        try {
            if (!json.isNull(KEY_KEY)) {
                event.key = json.getString(KEY_KEY);
            }
            event.count = json.optInt(COUNT_KEY);
            event.sum = json.optDouble(SUM_KEY, 0.0d);
            event.timestamp = json.optInt(TIMESTAMP_KEY);
            event.hour = json.optInt(HOUR);
            event.dow = json.optInt(DAY_OF_WEEK);

            if (!json.isNull(SEGMENTATION_KEY)) {
                final JSONObject segm = json.getJSONObject(SEGMENTATION_KEY);
                final HashMap<String, String> segmentation = new HashMap<String, String>(segm.length());
                final Iterator nameItr = segm.keys();
                while (nameItr.hasNext()) {
                    final String key = (String) nameItr.next();
                    if (!segm.isNull(key)) {
                        segmentation.put(key, segm.getString(key));
                    }
                }
                event.segmentation = segmentation;
            }
        }
        catch (JSONException e) {
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.w(Statistical.TAG, "Got exception converting JSON to an Event", e);
            }
            event = null;
        }

        return (event != null && event.key != null && event.key.length() > 0) ? event : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof Event)) {
            return false;
        }

        final Event e = (Event) o;

        return (key == null ? e.key == null : key.equals(e.key)) &&
               timestamp == e.timestamp &&
               hour == e.hour &&
               dow == e.dow &&
               (segmentation == null ? e.segmentation == null : segmentation.equals(e.segmentation));
    }

    @Override
    public int hashCode() {
        return (key != null ? key.hashCode() : 1) ^
               (segmentation != null ? segmentation.hashCode() : 1) ^
               (timestamp != 0 ? timestamp : 1);
    }
}
