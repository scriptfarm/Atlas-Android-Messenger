package com.layer.messenger.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.layer.messenger.App;
import com.layer.messenger.ConversationsListActivity;
import com.layer.messenger.MessagesListActivity;
import com.layer.messenger.PushNotificationReceiver;
import com.layer.messenger.R;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;

public class CrossfitHomeActivity extends Activity {

    private static final String CROSSFIT_ACTIVITY_SHARED_PREFERNCES = "CROSSFIT_ACTIVITY_SHARED_PREFERNCES";
    private static final String CROSSFIT_CONVERSATION_ID = "CROSSFIT_CONVERSATION_ID";

    private Button mNewBookingButton;
    private Identity me;
    private LayerClient mLayerClient;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crossfit_home);
        mNewBookingButton = (Button) findViewById(R.id.button_new_booking);
        mLayerClient = App.getLayerClient();
        mSharedPreferences = getSharedPreferences(CROSSFIT_ACTIVITY_SHARED_PREFERNCES, Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLayerClient!=null && mLayerClient.isAuthenticated() && me == null) {
            me = mLayerClient.getAuthenticatedUser();
            mNewBookingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri conversationId;

                    if (!mSharedPreferences.contains(CROSSFIT_CONVERSATION_ID)) {
                        Conversation conversation = mLayerClient.newConversation(me);
                        conversationId = conversation.getId();
                        mSharedPreferences.edit().putString(CROSSFIT_CONVERSATION_ID, conversationId.toString()).commit();
                    } else {
                        String storedConversationId = mSharedPreferences.getString(CROSSFIT_CONVERSATION_ID, null);
                        conversationId = Uri.parse(storedConversationId);
                    }

                    Intent intent = new Intent(CrossfitHomeActivity.this, MessagesListActivity.class);
                    intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, conversationId);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        me = null;
        mNewBookingButton.setOnClickListener(null);
    }
}
