package com.layer.messenger;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.messenger.databinding.ActivityConversationSettingsBinding;
import com.layer.messenger.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.LayerDataObserver;
import com.layer.sdk.LayerDataRequest;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.LayerObject;
import com.layer.ui.identity.IdentityItemsListView;
import com.layer.ui.identity.IdentityItemsListViewModel;
import com.layer.ui.recyclerview.OnItemClickListener;

import java.util.HashSet;
import java.util.Set;

public class ConversationSettingsActivity extends AppCompatActivity implements LayerDataObserver {
    private EditText mConversationName;
    private Switch mShowNotifications;
    private IdentityItemsListView mParticipantRecyclerView;
    private Button mLeaveButton;
    private Button mAddParticipantsButton;

    private Conversation mConversation;
    private IdentityItemsListViewModel mItemsListViewModel;

    private LayerClient mLayerClient;
    private Identity mAuthenticatedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityConversationSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation_settings);

        setTitle(R.string.title_conversation_details);

        mConversationName = binding.conversationName;
        mShowNotifications = binding.showNotificationsSwitch;
        mParticipantRecyclerView = binding.participants;
        mLeaveButton = binding.leaveButton;
        mAddParticipantsButton = binding.addParticipantButton;

        mLayerClient = ((App) getApplication()).getLayerClient();

        mItemsListViewModel = new IdentityItemsListViewModel(this, mLayerClient,
                Util.getImageCacheWrapper(((App) getApplication())));

        // Get authenticated user
        mLayerClient.getAuthenticatedUserLive().observe(this, new Observer<Identity>() {
            @Override
            public void onChanged(@Nullable Identity identity) {
                mAuthenticatedUser = identity;
                refresh();
            }
        });

        // Get Conversation from Intent extras
        Uri conversationId = getIntent().getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);

        mItemsListViewModel = new IdentityItemsListViewModel(this, mLayerClient,
                Util.getImageCacheWrapper(((App) getApplication())));

        mLayerClient.getLive(conversationId, Conversation.class).observe(this, new Observer<Conversation>() {
            @Override
            public void onChanged(@Nullable Conversation layerObject) {
                mConversation = layerObject;
                refresh();
            }
        });

        mItemsListViewModel.setItemClickListener(new OnItemClickListener<Identity>() {
            @Override
            public void onItemClick(final Identity item) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ConversationSettingsActivity.this)
                        .setMessage(Util.getIdentityFormatter(getApplicationContext()).getDisplayName(item));

                if (mConversation.getParticipants().size() > 2) {
                    // TODO Handle null conversations (pending load)
                    builder.setNeutralButton(R.string.alert_button_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mConversation.removeParticipants(item);
                        }
                    });
                }

                // TODO Blocking support
//                final Policy blockPolicy = getBlockPolicy(App.getLayerClient(), item);
//
//                builder.setPositiveButton(blockPolicy == null ? R.string.alert_button_block : R.string.alert_button_unblock,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                if (blockPolicy == null) {
//                                    // Block
//                                    Policy policy = new Policy.Builder(Policy.PolicyType.BLOCK).sentByUserId(item.getUserId()).build();
//                                    App.getLayerClient().addPolicy(policy);
//                                } else {
//                                    App.getLayerClient().removePolicy(blockPolicy);
//                                }
//                            }
//                        }).setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                }).show();
            }

            @Override
            public boolean onItemLongClick(Identity item) {
                return false;
            }
        });

        binding.setViewModel(mItemsListViewModel);

        mConversationName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String title = ((EditText) v).getText().toString().trim();
                    // TODO Handle null conversations (pending load)
                    Util.getConversationItemFormatter().setMetaDataTitleOnConversation(mConversation, title);
                    Toast.makeText(v.getContext(), R.string.toast_group_name_updated, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        mShowNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Handle null conversations (pending load)
                PushNotificationReceiver.getNotifications(ConversationSettingsActivity.this)
                        .setEnabled(mConversation.getId(), isChecked);
            }
        });

        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(false);
                // TODO Handle null conversations (pending load)
                // TODO Handle cases where authenticated user is null
                mConversation.removeParticipants(mAuthenticatedUser);
                refresh();
                Intent intent = new Intent(ConversationSettingsActivity.this, ConversationsListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setEnabled(true);
                ConversationSettingsActivity.this.startActivity(intent);
            }
        });

        mAddParticipantsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                Toast.makeText(v.getContext(), "Coming soon", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO Blocking
//        mLayerClient.registerPolicyListener(this);
        mLayerClient.registerDataObserver(this);
        setEnabled(true);
        refresh();
    }

    @Override
    protected void onPause() {
        // TODO Blocking
//        mLayerClient.unregisterPolicyListener(this);
        mLayerClient.unregisterDataObserver(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mParticipantRecyclerView != null) {
            mParticipantRecyclerView.onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuResId = R.menu.menu_conversation_details;
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
                LayerClient.sendLogs(mLayerClient, this);
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
            actionBar.setDisplayHomeAsUpEnabled(true);
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
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(titleId);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    public void setEnabled(boolean enabled) {
        mShowNotifications.setEnabled(enabled);
        mLeaveButton.setEnabled(enabled);
    }

    private void refresh() {
        if (!mLayerClient.isAuthenticated()) return;
        // TODO This should cause placeholder views to appear
        if (mConversation == null || mAuthenticatedUser == null) return;

        Set<Identity> participants = mConversation.getParticipants();
        participants.remove(mAuthenticatedUser);

        mItemsListViewModel.setIdentities(participants);

        mConversationName.setText(Util.getConversationItemFormatter().getConversationMetadataTitle(mConversation));
        mShowNotifications.setChecked(PushNotificationReceiver.getNotifications(this).isEnabled(mConversation.getId()));

        Set<Identity> participantsMinusMe = new HashSet<>(mConversation.getParticipants());
        participantsMinusMe.remove(mAuthenticatedUser);

        if (participantsMinusMe.size() == 0) {
            // I've been removed
            mConversationName.setEnabled(false);
            mLeaveButton.setVisibility(View.GONE);
        } else if (participantsMinusMe.size() == 1) {
            // 1-on-1
            mConversationName.setEnabled(false);
            mLeaveButton.setVisibility(View.GONE);
        } else {
            // Group
            mConversationName.setEnabled(true);
            mLeaveButton.setVisibility(View.VISIBLE);
        }

        mItemsListViewModel.setIdentities(participantsMinusMe);
    }

    // TODO blocking support
//    private Policy getBlockPolicy(LayerClient client, Identity identity) {
//        for (Policy policy : client.getPolicies()) {
//            if (policy.getPolicyType() == Policy.PolicyType.BLOCK
//                    && policy.getSentByUserID().equals(identity.getUserId())) {
//                return policy;
//            }
//        }
//
//        return null;
//    }
//
//    @Override
//    public void onPolicyListUpdate(LayerClient layerClient, List<Policy> list, List<Policy> list1) {
//        refresh();
//    }


    @Override
    public void onDataChanged(LayerChangeEvent event) {
        refresh();
    }

    @Override
    public void onDataRequestCompleted(LayerDataRequest request, LayerObject object) {
    }
}