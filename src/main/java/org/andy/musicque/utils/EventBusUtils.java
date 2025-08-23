package org.andy.musicque.utils;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class EventBusUtils {

    // Singleton EventBus
    private static final EventBus EVENT_BUS = new EventBus();

    private EventBusUtils() {
        // prevent instantiation
    }

    public static void register(Object subscriber) {
        EVENT_BUS.register(subscriber);
    }

    public static void unregister(Object subscriber) {
        EVENT_BUS.unregister(subscriber);
    }

    public static void post(Object event) {
        EVENT_BUS.post(event);
    }
}
