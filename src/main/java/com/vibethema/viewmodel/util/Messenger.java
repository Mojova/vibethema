package com.vibethema.viewmodel.util;

import de.saxsys.mvvmfx.utils.notifications.NotificationCenter;
import de.saxsys.mvvmfx.utils.notifications.NotificationCenterFactory;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;

/**
 * A lightweight wrapper around MVVMfx NotificationCenter to provide a 
 * consistent "Messenger" interface for cross-viewModel communication.
 */
public class Messenger {

    private static final NotificationCenter center = NotificationCenterFactory.getNotificationCenter();

    public static void publish(String messageName, Object... payload) {
        center.publish(messageName, payload);
    }

    public static void subscribe(String messageName, NotificationObserver observer) {
        center.subscribe(messageName, observer);
    }

    public static void unsubscribe(String messageName, NotificationObserver observer) {
        center.unsubscribe(messageName, observer);
    }
}
