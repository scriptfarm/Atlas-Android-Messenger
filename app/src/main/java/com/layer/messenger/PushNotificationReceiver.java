package com.layer.messenger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.layer.messenger.util.Log;
import com.layer.messenger.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerQueryRequest;
import com.layer.sdk.messaging.Channel;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagingPattern;
import com.layer.sdk.messaging.PushNotificationPayload;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.Queryable;
import com.layer.sdk.query.SortDescriptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PushNotificationReceiver extends BroadcastReceiver {
    public final static int MESSAGE_ID = 1;
    private final static AtomicInteger sPendingIntentCounter = new AtomicInteger(0);
    private static Notifications sNotifications;

    public final static String ACTION_PUSH = "com.layer.sdk.PUSH";
    public final static String ACTION_CANCEL = BuildConfig.APPLICATION_ID + ".CANCEL_PUSH";

    public final static String LAYER_CONVERSATION_KEY = "layer-conversation-id";
    public final static String LAYER_MESSAGE_KEY = "layer-message-id";

    /**
     * Parses the `com.layer.sdk.PUSH` Intent.
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        final PushNotificationPayload payload = PushNotificationPayload.fromLayerPushExtras(extras);
        final Uri conversationId = extras.getParcelable(LAYER_CONVERSATION_KEY);
        final Uri messageId = extras.getParcelable(LAYER_MESSAGE_KEY);

        if (intent.getAction().equals(ACTION_PUSH)) {
            // New push from Layer
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Received notification for: " + messageId);
            if (messageId == null) {
                if (Log.isLoggable(Log.ERROR)) Log.e("No message to notify: " + extras);
                return;
            }
            if (conversationId == null) {
                if (Log.isLoggable(Log.ERROR)) Log.e("No conversation to notify: " + extras);
                return;
            }

            if (!getNotifications(context).isEnabled()) {
                if (Log.isLoggable(Log.VERBOSE)) {
                    Log.v("Blocking notification due to global app setting");
                }
                return;
            }

            if (!getNotifications(context).isEnabled(conversationId)) {
                if (Log.isLoggable(Log.VERBOSE)) {
                    Log.v("Blocking notification due to conversation detail setting");
                }
                return;
            }

            // Try to have content ready for viewing before posting a Notification
            // TODO Not the best way to get a LayerClient. Should retrieve via a singleton or DI framework. Else we could just recreate it here and the SDK will return an instance if it exists.
            LayerClient layerClient = ((App) context.getApplicationContext()).getLayerClient();

            if (layerClient != null) {
                layerClient.waitForContent(messageId, new LayerClient.ContentAvailableCallback() {
                    @Override
                    public void onContentAvailable(LayerClient client, @NonNull Queryable object) {
                        if (Log.isLoggable(Log.VERBOSE)) {
                            Log.v("Pre-fetched notification content");
                        }
                        getNotifications(context).add(context, client, (Message) object, payload.getText());
                    }

                    @Override
                    public void onContentFailed(LayerClient client, Uri objectId, Exception e) {
                        if (Log.isLoggable(Log.ERROR)) {
                            Log.e("Failed to fetch notification content");
                        }
                        getNotifications(context).notifyOnContentFailure(context, conversationId, messageId, payload.getText());
                    }
                });
            }

        } else if (intent.getAction().equals(ACTION_CANCEL)) {
            // TODO Not the best way to get a LayerClient. Should retrieve via a singleton or DI framework. Else we could just recreate it here and the SDK will return an instance if it exists.
            LayerClient layerClient = ((App) context.getApplicationContext()).getLayerClient();

            // User swiped notification out
            if (Log.isLoggable(Log.VERBOSE)) {
                Log.v("Cancelling notifications for: " + conversationId);
            }
            getNotifications(context).clear(layerClient, conversationId);
        } else {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e("Got unknown intent action: " + intent.getAction());
            }
        }
    }

    public static synchronized Notifications getNotifications(Context context) {
        if (sNotifications == null) {
            sNotifications = new Notifications(context);
        }
        return sNotifications;
    }

    /**
     * Notifications manages notifications displayed on the user's device.  Notifications are
     * grouped by Conversation, where a Conversation's notifications are rolled-up into single
     * notification summaries.
     */
    public static class Notifications {
        private static final String KEY_ALL = "all";
        private static final String KEY_POSITION = "position";
        private static final String KEY_TEXT = "text";

        private final int MAX_MESSAGES = 5;
        // Contains black-listed conversation IDs and the global "all" key for notifications
        private final SharedPreferences mDisableds;

        // Contains positions for message IDs
        private final SharedPreferences mPositions;
        private final SharedPreferences mMessages;
        private final NotificationManager mManager;

        public Notifications(Context context) {
            mDisableds = context.getSharedPreferences("notification_disableds", Context.MODE_PRIVATE);
            mPositions = context.getSharedPreferences("notification_positions", Context.MODE_PRIVATE);
            mMessages = context.getSharedPreferences("notification_messages", Context.MODE_PRIVATE);
            mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public boolean isEnabled() {
            return !mDisableds.contains(KEY_ALL);
        }

        public boolean isEnabled(Uri conversationId) {
            if (conversationId == null) {
                return isEnabled();
            }
            return !mDisableds.contains(conversationId.toString());
        }

        public void setEnabled(boolean enabled) {
            if (enabled) {
                mDisableds.edit().remove(KEY_ALL).apply();
            } else {
                mDisableds.edit().putBoolean(KEY_ALL, true).apply();
                mManager.cancelAll();
            }
        }

        public void setEnabled(Uri conversationId, boolean enabled) {
            if (conversationId == null) {
                return;
            }
            if (enabled) {
                mDisableds.edit().remove(conversationId.toString()).apply();
            } else {
                mDisableds.edit().putBoolean(conversationId.toString(), true).apply();
                mManager.cancel(conversationId.toString(), MESSAGE_ID);
            }
        }

        public void clear(LayerClient layerClient, final Conversation conversation) {
            if (conversation == null) return;
            clear(layerClient, conversation.getId());
        }

        /**
         * Called when a Conversation is opened or message is marked as read
         * Clears messages map; sets position to greatest position
         *
         * @param conversationId Conversation whose notifications should be cleared
         */
        public void clear(final LayerClient layerClient, final Uri conversationId) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (conversationId == null) return;
                    String key = conversationId.toString();
                    long maxPosition = getMaxPosition(layerClient, conversationId);
                    mMessages.edit().remove(key).commit();
                    mPositions.edit().putLong(key, maxPosition).commit();
                    mManager.cancel(key, MESSAGE_ID);
                }
            }).start();
        }

        /**
         * Called when a new message arrives
         *
         * @param message Message to add
         * @param text    Notification text for added Message
         */
        protected void add(Context context, LayerClient layerClient, Message message, String text) {
            MessagingPattern messagingPattern = message.getMessagingPattern();
            String key = messagingPattern.getId().toString();
            long currentPosition = mPositions.getLong(key, Long.MIN_VALUE);

            // Ignore older messages
            if (message.getPosition() <= currentPosition) return;

            String currentMessages = mMessages.getString(key, null);

            try {
                JSONObject messages = currentMessages == null ? new JSONObject() : new JSONObject(currentMessages);
                String messageKey = message.getId().toString();

                // Ignore if we already have this message
                if (messages.has(messageKey)) return;

                JSONObject messageEntry = new JSONObject();
                messageEntry.put(KEY_POSITION, message.getPosition());
                messageEntry.put(KEY_TEXT, text);
                messages.put(messageKey, messageEntry);

                mMessages.edit().putString(key, messages.toString()).commit();
            } catch (JSONException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
                return;
            }
            update(context, layerClient, messagingPattern, message);
        }

        private void update(Context context, LayerClient layerClient, MessagingPattern messagingPattern,
                Message message) {
            String messagesString = mMessages.getString(messagingPattern.getId().toString(), null);
            if (messagesString == null) return;

            // Get current notification texts
            Map<Long, String> positionText = new HashMap<Long, String>();
            try {
                JSONObject messagesJson = new JSONObject(messagesString);
                Iterator<String> iterator = messagesJson.keys();
                while (iterator.hasNext()) {
                    String messageId = iterator.next();
                    JSONObject messageJson = messagesJson.getJSONObject(messageId);
                    long position = messageJson.getLong(KEY_POSITION);
                    String text = messageJson.getString(KEY_TEXT);
                    positionText.put(position, text);
                }
            } catch (JSONException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
                return;
            }

            // Sort by message position
            List<Long> positions = new ArrayList<Long>(positionText.keySet());
            Collections.sort(positions);

            // Construct notification
            // TODO We'd need to handle updates if the authenticated user isn't in the cache

            String conversationTitle;
            if (messagingPattern instanceof Channel) {
                conversationTitle = ((Channel) messagingPattern).getName();
            } else {
                conversationTitle = Util.getConversationItemFormatter().getConversationTitle(layerClient.getAuthenticatedUserLive().getValue(),
                        ((Conversation) messagingPattern));
            }
            
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle().setBigContentTitle(conversationTitle);
            int i;
            if (positions.size() <= MAX_MESSAGES) {
                i = 0;
                inboxStyle.setSummaryText(null);
            } else {
                i = positions.size() - MAX_MESSAGES;
                inboxStyle.setSummaryText(context.getString(R.string.notifications_num_more, i));
            }
            while (i < positions.size()) {
                inboxStyle.addLine(positionText.get(positions.get(i++)));
            }

            String collapsedSummary = positions.size() == 1 ? positionText.get(positions.get(0)) :
                    context.getString(R.string.notifications_new_messages, positions.size());

            // Construct notification
            // TODO: use large icon based on avatars
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(conversationTitle)
                    .setContentText(collapsedSummary)
                    .setAutoCancel(true)
                    .setLights(context.getResources().getColor(R.color.layer_ui_action_bar_background), 100, 1900)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                    .setStyle(inboxStyle);

            // Intent to launch when clicked
            PendingIntent clickPendingIntent = createNotificationClickIntent(context, messagingPattern.getId(), message.getId());
            mBuilder.setContentIntent(clickPendingIntent);

            // Intent to launch when swiped out
            PendingIntent deleteIntent = createNotificationDeleteIntent(context, messagingPattern.getId(), message.getId());
            mBuilder.setDeleteIntent(deleteIntent);

            // Show the notification
            mManager.notify(messagingPattern.getId().toString(), MESSAGE_ID, mBuilder.build());
        }

        private void notifyOnContentFailure(Context context, Uri conversationId, Uri messageId, String text) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(context.getString(R.string.push_notification_no_content_title))
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setLights(context.getResources().getColor(R.color.layer_ui_action_bar_background), 100, 1900)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);

            // Intent to launch when clicked
            PendingIntent clickPendingIntent = createNotificationClickIntent(context, conversationId, messageId);
            mBuilder.setContentIntent(clickPendingIntent);

            // Intent to launch when swiped out
            PendingIntent deleteIntent = createNotificationDeleteIntent(context, conversationId, messageId);
            mBuilder.setDeleteIntent(deleteIntent);

            // Show the notification
            mManager.notify(conversationId.toString(), MESSAGE_ID, mBuilder.build());
        }

        /**
         * Returns the current maximum Message position within the given Conversation, or
         * Long.MIN_VALUE if no messages are found.
         *
         * @param conversationId Conversation whose maximum Message position to return.
         * @return the current maximum Message position or Long.MIN_VALUE.
         */
        private long getMaxPosition(LayerClient layerClient, Uri conversationId) {

            Query<Message> query = Query.builder(Message.class)
                    .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversationId))
                    .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.DESCENDING))
                    .limit(1)
                    .build();

            final LiveData<LayerQueryRequest<?>> liveData = layerClient.executeQueryForObjectsLive(query);
            // Wait for results since this is off the main thread
            final CountDownLatch latch = new CountDownLatch(1);
            liveData.observeForever(new Observer<LayerQueryRequest<?>>() {
                @Override
                public void onChanged(@Nullable LayerQueryRequest<?> layerQueryRequest) {
                    if (layerQueryRequest != null) {
                        switch (layerQueryRequest.getRequestStatus()) {
                            case AVAILABLE:
                            case UNAVAILABLE:
                                liveData.removeObserver(this);
                                latch.countDown();
                        }
                    }
                }
            });

            try {
                // TODO Don't wait forever
                latch.await();
            } catch (InterruptedException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.d("Failed to find last message from conversation " + conversationId, e);
                }
                return Long.MIN_VALUE;
            }

            if (liveData.getValue() == null || liveData.getValue().getResults() == null
                    || liveData.getValue().getResults().isEmpty()) {
                return Long.MIN_VALUE;
            }
            return ((Message) liveData.getValue().getResults().get(0)).getPosition();
        }

        private static PendingIntent createNotificationClickIntent(Context context, Uri conversationId, Uri messageId) {
            Intent clickIntent = new Intent(context, MessagesListActivity.class)
                    .setPackage(context.getApplicationContext().getPackageName())
                    .putExtra(LAYER_CONVERSATION_KEY, conversationId)
                    .putExtra(LAYER_MESSAGE_KEY, messageId)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return PendingIntent.getActivity(
                    context, sPendingIntentCounter.getAndIncrement(),
                    clickIntent, PendingIntent.FLAG_ONE_SHOT);
        }

        private static PendingIntent createNotificationDeleteIntent(Context context, Uri conversationId, Uri messageId) {
            Intent cancelIntent = new Intent(ACTION_CANCEL)
                    .setPackage(context.getApplicationContext().getPackageName())
                    .putExtra(LAYER_CONVERSATION_KEY, conversationId)
                    .putExtra(LAYER_MESSAGE_KEY, messageId);
            return PendingIntent.getBroadcast(
                    context, sPendingIntentCounter.getAndIncrement(),
                    cancelIntent, PendingIntent.FLAG_ONE_SHOT);
        }
    }
}
