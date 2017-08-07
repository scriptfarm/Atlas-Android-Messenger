package com.layer.messenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.layer.messenger.databinding.ActivityConversationsListBinding;
import com.layer.messenger.util.Log;
import com.layer.messenger.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.ui.adapters.ConversationItemsAdapter;
import com.layer.ui.identity.IdentityFormatterImpl;
import com.layer.ui.conversation.ConversationItemsListView;
import com.layer.ui.conversation.ConversationItemsListViewModel;
import com.layer.ui.recyclerview.OnItemClickListener;
import com.layer.ui.util.views.SwipeableItem;

public class ConversationsListActivity extends AppCompatActivity {
    private ConversationItemsListView mConversationsList;
    private ConversationItemsListViewModel mConversationItemsListViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        int menuTitleResId = R.string.title_conversations_list;
        setTitle(menuTitleResId);

        ActivityConversationsListBinding binding = ActivityConversationsListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mConversationsList = binding.conversationsList;

        mConversationItemsListViewModel = new ConversationItemsListViewModel(this, App.getLayerClient(), Util.getConversationItemFormatter(), Util.getImageCacheWrapper(),new IdentityFormatterImpl());
        mConversationItemsListViewModel.setItemClickListener(new OnItemClickListener<Conversation>() {
            @Override
            public void onItemClick(Conversation item) {
                Intent intent = new Intent(ConversationsListActivity.this, MessagesListActivity.class);
                if (Log.isLoggable(Log.VERBOSE)) {
                    Log.v("Launching MessagesListActivity with existing conversation ID: " + item.getId());
                }
                intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, item.getId());
                startActivity(intent);
            }

            @Override
            public boolean onItemLongClick(Conversation item) {
                return false;
            }
        });

        mConversationItemsListViewModel.setItemSwipeListener(new SwipeableItem.OnItemSwipeListener<Conversation>() {
            @Override
            public void onSwipe(final Conversation conversation, int direction) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConversationsListActivity.this)
                        .setMessage(R.string.alert_message_delete_conversation)
                        .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ConversationItemsAdapter adapter = mConversationItemsListViewModel.getConversationItemsAdapter();
                                int position = adapter.getPosition(conversation);
                                adapter.notifyItemChanged(position);
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

        binding.setViewModel(mConversationItemsListViewModel);
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ConversationsListActivity.this, MessagesListActivity.class));
            }
        });

        binding.executePendingBindings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LayerClient client = App.getLayerClient();
        if (client == null) return;
        if (client.isAuthenticated()) {
            client.connect();
        } else {
            client.authenticate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConversationsList != null) {
            mConversationsList.onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuResId = R.menu.menu_conversations_list;
        getMenuInflater().inflate(menuResId, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Menu "Navigate Up" acts like hardware back button
                onBackPressed();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, AppSettingsActivity.class));
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(App.getLayerClient(), this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            super.setTitle(title);
        } else {
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void setTitle(int titleId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            super.setTitle(titleId);
        } else {
            actionBar.setTitle(titleId);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }
}