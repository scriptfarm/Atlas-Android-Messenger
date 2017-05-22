package com.layer.messenger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.layer.ui.ConversationsRecyclerView;
import com.layer.ui.adapters.ConversationsAdapter;
import com.layer.ui.messagetypes.location.LocationCellFactory;
import com.layer.ui.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.ui.messagetypes.text.TextCellFactory;
import com.layer.ui.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.ui.util.views.SwipeableItem;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

public class ConversationsListActivity extends BaseActivity {

    private ConversationsRecyclerView mConversationsList;

    public ConversationsListActivity() {
        super(R.layout.activity_conversations_list, R.menu.menu_conversations_list, R.string.title_conversations_list, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        mConversationsList = (ConversationsRecyclerView) findViewById(R.id.conversations_list);

        // Atlas methods
        mConversationsList.init(getLayerClient(), getPicasso())
                .setInitialHistoricMessagesToFetch(20)
                .setOnConversationClickListener(new ConversationsAdapter.OnConversationClickListener() {
                    @Override
                    public void onConversationClick(ConversationsAdapter adapter, Conversation conversation) {
                        Intent intent = new Intent(ConversationsListActivity.this, MessagesListActivity.class);
                        if (Log.isLoggable(Log.VERBOSE)) {
                            Log.v("Launching MessagesListActivity with existing conversation ID: " + conversation.getId());
                        }
                        intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, conversation.getId());
                        startActivity(intent);
                    }

                    @Override
                    public boolean onConversationLongClick(ConversationsAdapter adapter, Conversation conversation) {
                        return false;
                    }
                })
                .addCellFactories(new TextCellFactory(),
                        new ThreePartImageCellFactory(getLayerClient(), getPicasso()),
                        new SinglePartImageCellFactory(getLayerClient(), getPicasso()),
                        new LocationCellFactory(getPicasso()))
                .setOnConversationSwipeListener(new SwipeableItem.OnSwipeListener<Conversation>() {
                    @Override
                    public void onSwipe(final Conversation conversation, int direction) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ConversationsListActivity.this)
                                .setMessage(R.string.alert_message_delete_conversation)
                                .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO: simply update this one conversation
                                        mConversationsList.getAdapter().notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton(R.string.alert_button_delete_all_participants, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        conversation.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                                    }
                                });
                        // User delete is only available if read receipts are enabled
                        if (conversation.isReadReceiptsEnabled()) {
                            builder.setNeutralButton(R.string.alert_button_delete_my_devices, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    conversation.delete(LayerClient.DeletionMode.ALL_MY_DEVICES);
                                }
                            });
                        }
                        builder.show();
                    }
                });

        findViewById(R.id.floating_action_button)
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(ConversationsListActivity.this, MessagesListActivity.class));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConversationsList != null) {
            mConversationsList.onDestroy();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, AppSettingsActivity.class));
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(getLayerClient(), this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
