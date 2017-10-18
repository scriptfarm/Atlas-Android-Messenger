package com.layer.messenger;

import android.app.AlertDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.layer.sdk.LayerDataObserver;
import com.layer.sdk.LayerDataRequest;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.messaging.Channel;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationType;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.ui.AddressBar;
import com.layer.ui.composebar.ComposeBar;
import com.layer.ui.conversation.ConversationView;
import com.layer.ui.conversation.ConversationViewModel;
import com.layer.ui.message.MessageItemsListViewModel;
import com.layer.ui.message.messagetypes.location.LocationSender;
import com.layer.ui.message.messagetypes.text.TextSender;
import com.layer.ui.message.messagetypes.threepartimage.CameraSender;
import com.layer.ui.message.messagetypes.threepartimage.GallerySender;
import com.layer.ui.util.imagecache.ImageCacheWrapper;
import com.layer.ui.util.views.SwipeableItem;

import java.util.HashSet;
import java.util.List;

public class MessagesListActivity extends AppCompatActivity {
    private UiState mState;
    private Conversation mConversation;

    private MessageItemsListViewModel mMessageItemsListViewModel;
    private AddressBar mAddressBar;
    private ConversationView mConversationView;
    private ComposeBar mComposeBar;
    private IdentityChangeListener mIdentityChangeListener;
    private ConversationViewModel mConversationViewModel;

    private ActivityMessagesListBinding mActivityMessagesListBinding;
    private ActionBar mActionBar;

    private LayerClient mLayerClient;

    private enum UiState {
        ADDRESS,
        ADDRESS_COMPOSER,
        ADDRESS_CONVERSATION_COMPOSER,
        CONVERSATION_COMPOSER
    }

    //=============================================================================================
    // Activity Lifecycle methods
    //=============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = getSupportActionBar();
        if (mActionBar == null) return;
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(R.string.title_select_conversation);

        mLayerClient = ((App) getApplication()).getLayerClient();

        if (((App) getApplication()).routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        mActivityMessagesListBinding = DataBindingUtil.setContentView(this, R.layout.activity_messages_list);

        setupAddressBar();
        setupComposeBar();
        Conversation conversation = getConversationFromIntent();
        setupConversation(conversation);
    }

    @Override
    protected void onResume() {
        // Clear any notifications for this conversation
        PushNotificationReceiver.getNotifications(this).clear(mLayerClient, mConversation);
        super.onResume();

        setTitleFromConversationTitle(mConversation != null);

        // Register for identity changes and update the activity's title as needed
        mIdentityChangeListener = new IdentityChangeListener();
        mLayerClient.registerDataObserver(mIdentityChangeListener);
    }

    @Override
    protected void onPause() {
        // Update the notification position to the latest seen
        PushNotificationReceiver.getNotifications(this).clear(mLayerClient, mConversation);

        mLayerClient.unregisterDataObserver(mIdentityChangeListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConversationView != null) {
            mConversationView.onDestroy();
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
                LayerClient.sendLogs(mLayerClient, this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_messages_list, menu);
        return true;
    }

    //=============================================================================================
    // private methods
    //=============================================================================================

    // Get or create Conversation from Intent extras
    private Conversation getConversationFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY)) {
                Uri conversationId = intent.getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);

                LiveData<Conversation> conversationLiveData = mLayerClient.getLive(conversationId, Conversation.class);
                conversationLiveData.observe(this, new Observer<Conversation>() {
                    @Override
                    public void onChanged(@Nullable Conversation layerObject) {
                        setupConversation(layerObject);
                        setTitleFromConversationTitle(layerObject != null);
                    }
                });
                return conversationLiveData.getValue();
            }
        }

        return null;
    }

    private void setupComposeBar() {
        mComposeBar = mActivityMessagesListBinding.conversation.getComposeBar();
        mComposeBar.setTextSender(new TextSender(this, mLayerClient));
        mComposeBar.addAttachmentSendersToDefaultAttachmentButton(
                new CameraSender(R.string.attachment_menu_camera,
                        R.drawable.ic_photo_camera_white_24dp, this, mLayerClient,
                        getApplicationContext().getPackageName() + ".file_provider"),
                new GallerySender(R.string.attachment_menu_gallery, R.drawable.ic_photo_white_24dp, this, mLayerClient),
                new LocationSender(R.string.attachment_menu_location, R.drawable.ic_place_white_24dp, this, mLayerClient));

        mComposeBar.setOnMessageEditTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setUiState(UiState.CONVERSATION_COMPOSER);
                    setTitleFromConversationTitle(true);
                }
            }
        });
    }

    private void setupConversation(Conversation conversation) {
        mConversationView = mActivityMessagesListBinding.conversation;
        ImageCacheWrapper imageCacheWrapper = Util.getImageCacheWrapper((App) getApplication());
        mMessageItemsListViewModel = new MessageItemsListViewModel(this, mLayerClient,
                imageCacheWrapper, Util.getDateFormatter(this), Util.getIdentityFormatter(this));

        mConversationViewModel = new ConversationViewModel(getApplicationContext(), mLayerClient,
                Util.getCellFactories(mLayerClient), imageCacheWrapper,
                Util.getDateFormatter(getApplicationContext()), Util.getIdentityFormatter(this),
                new SwipeableItem.OnItemSwipeListener<Message>() {
                    @Override
                    public void onSwipe(final Message message, int direction) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MessagesListActivity.this)
                                .setMessage(R.string.alert_message_delete_message)
                                .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO: simply update this one message
                                        mMessageItemsListViewModel.getAdapter().notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                })

                                .setPositiveButton(R.string.alert_button_delete_all_participants, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        message.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                                    }
                                });
                        // User delete is not available for channels
                        if (!(message.getMessagingPattern() instanceof Channel)) {
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

        mActivityMessagesListBinding.setViewModel(mConversationViewModel);
        setConversation(conversation, conversation != null);
        mActivityMessagesListBinding.executePendingBindings();
    }

    private void setupAddressBar() {
        ImageCacheWrapper imageCacheWrapper = Util.getImageCacheWrapper(((App) getApplication()));
        mAddressBar = mActivityMessagesListBinding.conversationLauncher
                .init(mLayerClient, imageCacheWrapper)
                .setOnConversationClickListener(new AddressBar.OnConversationClickListener() {
                    @Override
                    public void onConversationClick(AddressBar addressBar, Conversation conversation) {
                        setConversation(conversation, true);
                        setTitleFromConversationTitle(true);
                    }
                })
                .setOnParticipantSelectionChangeListener(new AddressBar.OnParticipantSelectionChangeListener() {
                    @Override
                    public void onParticipantSelectionChanged(AddressBar addressBar, final List<Identity> participants) {
                        if (participants.isEmpty()) {
                            setConversation(null, false);
                            return;
                        }
                        setConversation(mLayerClient.newConversation(
                                ConversationType.DIRECT_MESSAGE_CONVERSATION,
                                new HashSet<>(participants)), false);
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
                            setTitleFromConversationTitle(true);
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void setTitleFromConversationTitle(boolean useConversation) {
        if (!useConversation) {
            mActionBar.setTitle(R.string.title_select_conversation);
        } else {
            // TODO Observe authenticated user changes
            mActionBar.setTitle(Util.getConversationItemFormatter().getConversationTitle(mLayerClient.getAuthenticatedUserLive().getValue(), mConversation));
        }
    }

    private void setConversation(Conversation conversation, boolean hideLauncher) {
        mConversation = conversation;
        mConversationViewModel.setConversation(conversation);

        // UI state
        if (conversation == null) {
            setUiState(UiState.ADDRESS);
            return;
        }

        if (hideLauncher) {
            setUiState(UiState.CONVERSATION_COMPOSER);
            return;
        }

        if (conversation.getTotalMessageCount() == 0) {
            // New "temporary" conversation
            setUiState(UiState.ADDRESS_COMPOSER);
        } else {
            setUiState(UiState.ADDRESS_CONVERSATION_COMPOSER);
        }
    }

    private void setUiState(UiState state) {
        if (mState == state) return;
        mState = state;
        switch (state) {
            case ADDRESS:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mComposeBar.setVisibility(View.GONE);
                break;

            case ADDRESS_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;

            case ADDRESS_CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;

            case CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.GONE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mComposeBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    //=============================================================================================
    // Inner classes
    //=============================================================================================

    class IdentityChangeListener implements LayerDataObserver.Weak {

        @Override
        public void onDataChanged(LayerChangeEvent event) {
            // Don't need to update title if there is no conversation
            if (mConversation == null) {
                return;
            }

            for (LayerChange change : event.getChanges()) {
                if (change.getObjectType().equals(LayerObject.Type.IDENTITY)) {
                    Identity identity = (Identity) change.getObject();
                    if (mConversation.getParticipants().contains(identity)) {
                        setTitleFromConversationTitle(true);
                    }
                }
            }
        }

        @Override
        public void onDataRequestCompleted(LayerDataRequest request, LayerObject object) {

        }
    }
}