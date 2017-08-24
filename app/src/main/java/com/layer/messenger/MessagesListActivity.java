package com.layer.messenger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.layer.messenger.databinding.ActivityMessagesListBinding;
import com.layer.messenger.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerConversationException;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.ui.AddressBar;
import com.layer.ui.HistoricMessagesFetchLayout;
import com.layer.ui.TypingIndicatorLayout;
import com.layer.ui.composebar.ComposeBar;
import com.layer.ui.message.MessageItemsListViewModel;
import com.layer.ui.message.messagetypes.CellFactory;
import com.layer.ui.message.MessageItemListView;
import com.layer.ui.message.messagetypes.generic.GenericCellFactory;
import com.layer.ui.message.messagetypes.location.LocationCellFactory;
import com.layer.ui.message.messagetypes.location.LocationSender;
import com.layer.ui.message.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.ui.message.messagetypes.text.TextCellFactory;
import com.layer.ui.message.messagetypes.text.TextSender;
import com.layer.ui.message.messagetypes.threepartimage.CameraSender;
import com.layer.ui.message.messagetypes.threepartimage.GallerySender;
import com.layer.ui.message.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.ui.typingindicators.BubbleTypingIndicatorFactory;
import com.layer.ui.util.imagecache.ImageCacheWrapper;
import com.layer.ui.util.views.SwipeableItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MessagesListActivity extends AppCompatActivity {
    private UiState mState;
    private Conversation mConversation;

    private MessageItemsListViewModel mMessageItemsListViewModel;
    private AddressBar mAddressBar;
    private HistoricMessagesFetchLayout mHistoricFetchLayout;
    private MessageItemListView mMessagesList;
    private TypingIndicatorLayout mTypingIndicator;
    private ComposeBar mComposeBar;
    private IdentityChangeListener mIdentityChangeListener;

    private final int mLayoutResId;
    private final int mMenuResId;
    private final int mMenuTitleResId;
    private final boolean mMenuBackEnabled;

    public MessagesListActivity() {
        mLayoutResId = R.layout.activity_messages_list;
        mMenuResId = R.menu.menu_messages_list;
        mMenuTitleResId = R.string.title_select_conversation;
        mMenuBackEnabled = true;
    }

    private void setUiState(UiState state) {
        if (mState == state) return;
        mState = state;
        switch (state) {
            case ADDRESS:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mHistoricFetchLayout.setVisibility(View.GONE);
                mComposeBar.setVisibility(View.GONE);
                break;

            case ADDRESS_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mHistoricFetchLayout.setVisibility(View.GONE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;

            case ADDRESS_CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mHistoricFetchLayout.setVisibility(View.VISIBLE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;

            case CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.GONE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mHistoricFetchLayout.setVisibility(View.VISIBLE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        if (mMenuBackEnabled) actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mMenuTitleResId);

        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        ImageCacheWrapper imageCacheWrapper = Util.getImageCacheWrapper();

        mMessageItemsListViewModel = new MessageItemsListViewModel(this, getLayerClient(),
                imageCacheWrapper, Util.getDateFormatter(this));

        ActivityMessagesListBinding activityMessagesListBinding = DataBindingUtil.setContentView(this, R.layout.activity_messages_list);

        activityMessagesListBinding.setViewModel(mMessageItemsListViewModel);

        mAddressBar = activityMessagesListBinding.conversationLauncher
                .init(getLayerClient(), imageCacheWrapper)
                .setOnConversationClickListener(new AddressBar.OnConversationClickListener() {
                    @Override
                    public void onConversationClick(AddressBar addressBar, Conversation conversation) {
                        setConversation(conversation, true);
                        setTitle(true);
                    }
                })
                .setOnParticipantSelectionChangeListener(new AddressBar.OnParticipantSelectionChangeListener() {
                    @Override
                    public void onParticipantSelectionChanged(AddressBar addressBar, final List<Identity> participants) {
                        if (participants.isEmpty()) {
                            setConversation(null, false);
                            return;
                        }
                        try {
                            setConversation(App.getLayerClient().newConversation(new ConversationOptions().distinct(true), new HashSet<>(participants)), false);
                        } catch (LayerConversationException e) {
                            setConversation(e.getConversation(), false);
                        }
                    }
                })
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (mState == UiState.ADDRESS_CONVERSATION_COMPOSER) {
                            mAddressBar.setSuggestionsVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                })
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            setUiState(UiState.CONVERSATION_COMPOSER);
                            setTitle(true);
                            return true;
                        }
                        return false;
                    }
                });

        mHistoricFetchLayout = activityMessagesListBinding.historicSyncLayout
                .init(getLayerClient())
                .setHistoricMessagesPerFetch(20);


        List<CellFactory> cellFactories = new ArrayList<CellFactory>(Arrays.asList(
                new TextCellFactory(),
                new ThreePartImageCellFactory(getLayerClient(), imageCacheWrapper),
                new LocationCellFactory(imageCacheWrapper),
                new SinglePartImageCellFactory(getLayerClient(), imageCacheWrapper),
                new GenericCellFactory()));
        mMessageItemsListViewModel.setCellFactories(cellFactories);

        mMessagesList = activityMessagesListBinding.messagesList;

        mMessageItemsListViewModel.setOnItemSwipeListener(new SwipeableItem.OnItemSwipeListener<Message>() {
            @Override
            public void onSwipe(final Message message, int direction) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MessagesListActivity.this)
                        .setMessage(R.string.alert_message_delete_message)
                        .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: simply update this one message
                                mMessageItemsListViewModel.getMessageItemsAdapter().notifyDataSetChanged();
                                dialog.dismiss();
                            }
                        })

                        .setPositiveButton(R.string.alert_button_delete_all_participants, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                message.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                            }
                        });
                // User delete is only available if read receipts are enabled
                if (message.getConversation().isReadReceiptsEnabled()) {
                    builder.setNeutralButton(R.string.alert_button_delete_my_devices, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            message.delete(LayerClient.DeletionMode.ALL_MY_DEVICES);
                        }
                    });
                }
                builder.show();
            }
        });

        mTypingIndicator = new TypingIndicatorLayout(this)
                .init(App.getLayerClient())
                .setTypingIndicatorFactory(new BubbleTypingIndicatorFactory())
                .setTypingActivityListener(new TypingIndicatorLayout.TypingActivityListener() {
                    @Override
                    public void onTypingActivityChange(TypingIndicatorLayout typingIndicator, boolean active) {
                        mMessagesList.setFooterView(active ? typingIndicator : null);
                    }
                });

        mComposeBar = activityMessagesListBinding.composeBar;
        mComposeBar.setTextSender(new TextSender(this, App.getLayerClient()));
        mComposeBar.addAttachmentSendersToDefaultAttachmentButton(
                new CameraSender(R.string.attachment_menu_camera,
                        R.drawable.ic_photo_camera_white_24dp, this, App.getLayerClient(),
                        getApplicationContext().getPackageName() + ".file_provider"),
                new GallerySender(R.string.attachment_menu_gallery, R.drawable.ic_photo_white_24dp, this, App.getLayerClient()),
                new LocationSender(R.string.attachment_menu_location, R.drawable.ic_place_white_24dp, this, App.getLayerClient()));

        mComposeBar.setOnMessageEditTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setUiState(UiState.CONVERSATION_COMPOSER);
                    setTitle(true);
                }
            }
        });

        // Get or create Conversation from Intent extras
        Conversation conversation = null;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY)) {
                Uri conversationId = intent.getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);
                conversation = App.getLayerClient().getConversation(conversationId);
            } else if (intent.hasExtra("participantIds")) {
                String[] participantIds = intent.getStringArrayExtra("participantIds");
                try {
                    conversation = App.getLayerClient().newConversationWithUserIds(new ConversationOptions().distinct(true), participantIds);
                } catch (LayerConversationException e) {
                    conversation = e.getConversation();
                }
            }
        }
        activityMessagesListBinding.setViewModel(mMessageItemsListViewModel);
        activityMessagesListBinding.executePendingBindings();
        setConversation(conversation, conversation != null);
    }

    @Override
    protected void onResume() {
        // Clear any notifications for this conversation
        PushNotificationReceiver.getNotifications(this).clear(mConversation);
        super.onResume();
        LayerClient client = App.getLayerClient();
        if (client == null) return;
        if (client.isAuthenticated()) {
            client.connect();
        } else {
            client.authenticate();
        }

        setTitle(mConversation != null);

        // Register for identity changes and update the activity's title as needed
        mIdentityChangeListener = new IdentityChangeListener();
        App.getLayerClient().registerEventListener(mIdentityChangeListener);
    }

    @Override
    protected void onPause() {
        // Update the notification position to the latest seen
        PushNotificationReceiver.getNotifications(this).clear(mConversation);

        App.getLayerClient().unregisterEventListener(mIdentityChangeListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMessagesList != null) {
            mMessagesList.onDestroy();
        }
    }

    public void setTitle(boolean useConversation) {
        if (!useConversation) {
            setTitle(R.string.title_select_conversation);
        } else {
            setTitle(Util.getConversationItemFormatter().getConversationTitle(App.getLayerClient().getAuthenticatedUser(), mConversation));
        }
    }

    private void setConversation(Conversation conversation, boolean hideLauncher) {
        mConversation = conversation;
        mHistoricFetchLayout.setConversation(conversation);
        mMessagesList.setConversation(conversation);
        mTypingIndicator.setConversation(conversation);
        mComposeBar.setConversation(conversation, App.getLayerClient());

        // UI state
        if (conversation == null) {
            setUiState(UiState.ADDRESS);
            return;
        }

        if (hideLauncher) {
            setUiState(UiState.CONVERSATION_COMPOSER);
            return;
        }

        if (conversation.getHistoricSyncStatus() == Conversation.HistoricSyncStatus.INVALID) {
            // New "temporary" conversation
            setUiState(UiState.ADDRESS_COMPOSER);
        } else {
            setUiState(UiState.ADDRESS_CONVERSATION_COMPOSER);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_details:
                if (mConversation == null) return true;
                Intent intent = new Intent(this, ConversationSettingsActivity.class);
                intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, mConversation.getId());
                startActivity(intent);
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(App.getLayerClient(), this);
                return true;
            case android.R.id.home:
                // Menu "Navigate Up" acts like hardware back button
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mComposeBar.onActivityResult(this, requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mComposeBar.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private enum UiState {
        ADDRESS,
        ADDRESS_COMPOSER,
        ADDRESS_CONVERSATION_COMPOSER,
        CONVERSATION_COMPOSER
    }

    private class IdentityChangeListener implements LayerChangeEventListener.Weak {
        @Override
        public void onChangeEvent(LayerChangeEvent layerChangeEvent) {
            // Don't need to update title if there is no conversation
            if (mConversation == null) {
                return;
            }

            for (LayerChange change : layerChangeEvent.getChanges()) {
                if (change.getObjectType().equals(LayerObject.Type.IDENTITY)) {
                    Identity identity = (Identity) change.getObject();
                    if (mConversation.getParticipants().contains(identity)) {
                        setTitle(true);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(mMenuResId, menu);
        return true;
    }

    protected LayerClient getLayerClient() {
        return App.getLayerClient();
    }
}