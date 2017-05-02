package com.layer.messenger.util;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.layer.messenger.App;
import com.layer.sdk.messaging.Conversation;

import java.util.List;

/**
 * This class loads total messages, unread messages and total conversation in a background thread
 * Use in {@link com.layer.messenger.AppSettingsActivity}
 */

public class ConversationSettingsTaskLoader extends AsyncTaskLoader<ConversationSettingsTaskLoader.Results> {
    private Results mCachedSettingsData;

    public ConversationSettingsTaskLoader(Context context) {
        super(context);
    }

    @Override
    public Results loadInBackground() {
        final List<Conversation> conversations = App.getLayerClient().getConversations();
        int totalMessages = 0;
        int totalUnread = 0;
        for (Conversation conversation : conversations) {
            totalMessages += conversation.getTotalMessageCount();
            totalUnread += conversation.getTotalUnreadMessageCount();
        }

        return new Results(totalMessages,totalUnread, conversations.size());
    }

    @Override
    protected void onStartLoading() {
        if (mCachedSettingsData == null) {
            forceLoad();
        } else {
            super.deliverResult(mCachedSettingsData);
        }
    }

    @Override
    public void deliverResult(Results data) {
        mCachedSettingsData = data;
        super.deliverResult(data);
    }

    public static class Results {

        private int mTotalMessages;
        private int mTotalUnreadMessages;
        private int mConversationCount;

        public Results(int totalMessages, int totalUnreadMessages, int conversationCount) {
            this.mTotalMessages = totalMessages;
            this.mTotalUnreadMessages = totalUnreadMessages;
            this.mConversationCount = conversationCount;
        }

        public int getTotalMessages() {
            return mTotalMessages;
        }

        public int getTotalUnreadMessages() {
            return mTotalUnreadMessages;
        }

        public int getConversationCount() {
            return mConversationCount;
        }
    }
}