package com.statistical.android.sdk;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * 本地的事件队列数据，并可以该事件数据转换成JSON提交到服务器。
 */
public class EventQueue {
    private final StatisticalStore countlyStore_;

    /**创建一个用于储存事件队列
     * Constructs an EventQueue.
     * @param countlyStore backing store to be used for local event queue persistence
     */
    EventQueue(final StatisticalStore countlyStore) {
        countlyStore_ = countlyStore;
    }

    /**
     * 事件队列中的事件数
     * @return the number of events in the local event queue
     */
    int size() {
        return countlyStore_.events().length;
   }

    /**
     * 删除从本地队列中的所有时事和将它们作为可以提交到连接队列的URL编码的JSON字符串。
     * URL-encoded JSON string that can be submitted to a ConnectionQueue.
     * @return URL-encoded JSON string of event data from the local event queue
     */
    String events() {
        String result;

        final List<Event> events = countlyStore_.eventsList();

        final JSONArray eventArray = new JSONArray();
        for (Event e : events) {
            eventArray.put(e.toJSON());
        }

        result = eventArray.toString();

        countlyStore_.removeEvents(events);

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen because Android guarantees UTF-8 support
        }

        return result;
    }

    /**
     * 储存一个自定义事件到本地事件队列
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation values for the custom event, may be null
     * @param count count associated with the custom event, should be more than zero
     * @param sum sum associated with the custom event, if not used, pass zero.
     *            NaN and infinity values will be quietly ignored.
     * @throws IllegalArgumentException if key is null or empty
     */
    void recordEvent(final String key, final Map<String, String> segmentation, final int count, final double sum) {
        final int timestamp = Statistical.currentTimestamp();
        final int hour = Statistical.currentHour();
        final int dow = Statistical.currentDayOfWeek();
        countlyStore_.addEvent(key, segmentation, timestamp, hour, dow, count, sum);
    }

    StatisticalStore getCountlyStore() {
        return countlyStore_;
    }
}
