package com.layer.sample;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.layer.sample.messagelist.MessagesListActivity;
import com.layer.sample.util.IdentityDisplayNameComparator;
import com.layer.sample.util.IdentityUtils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Identity;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectParticipantsActivity extends BaseActivity {
    private static final String EXTRA_KEY_HAS_PARTICIPANTS = "hasParticipants";

    private boolean mHasCheckedParticipants;
    private ListView mParticipantList;
    private ParticipantAdapter mParticipantAdapter;

    public SelectParticipantsActivity() {
        super(R.layout.activity_new_conversation, R.menu.menu_select_participants, R.string.title_select_participants, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mHasCheckedParticipants = savedInstanceState.getBoolean(EXTRA_KEY_HAS_PARTICIPANTS);
        }

        // Fetch identities from database
        IdentityFetcher identityFetcher = new IdentityFetcher(getLayerClient());
        identityFetcher.fetchIdentities(new IdentitiesFetchedCallback());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem doneButton = menu.findItem(R.id.action_done);
        doneButton.setVisible(mHasCheckedParticipants);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_done) {
            startConversationActivity();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_HAS_PARTICIPANTS, mHasCheckedParticipants);
    }

    private void setUpParticipantAdapter(List<Identity> identities) {
        mParticipantAdapter = new ParticipantAdapter(this);
        mParticipantAdapter.addAll(identities);
    }

    @SuppressWarnings("ConstantConditions")
    private void setUpParticipantList() {
        mParticipantList = (ListView) findViewById(R.id.participant_list);
        mParticipantList.setAdapter(mParticipantAdapter);
        mParticipantList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHasCheckedParticipants = mParticipantList.getCheckedItemCount() != 0;
                invalidateOptionsMenu();
            }
        });
    }

    private void startConversationActivity() {
        Intent intent = new Intent(this, MessagesListActivity.class);
        intent.putExtra(MessagesListActivity.EXTRA_KEY_PARTICIPANT_IDS, getSelectedParticipantIds());
        startActivity(intent);
    }

    private String[] getSelectedParticipantIds() {
        SparseBooleanArray positions = mParticipantList.getCheckedItemPositions();
        List<String> participantIds = new ArrayList<>(positions.size());

        for (int i = 0; i < positions.size(); i++) {
            int checkedPosition = positions.keyAt(i);
            Identity participant = mParticipantAdapter.getItem(checkedPosition);
            if (participant != null) {
                participantIds.add(participant.getUserId());
            }
        }
        String[] participantIdArray = new String[participantIds.size()];
        return participantIds.toArray(participantIdArray);
    }

    private class IdentitiesFetchedCallback implements IdentityFetcher.IdentityFetcherCallback {
        @Override
        public void identitiesFetched(Set<Identity> identities) {
            List<Identity> sortedIdentities = new ArrayList<>(identities);
            Collections.sort(sortedIdentities, new IdentityDisplayNameComparator());
            setUpParticipantAdapter(sortedIdentities);
            setUpParticipantList();
        }
    }

    /**
     * Helper class that handles loading identities from the database via a {@link Query}.
     */
    private static class IdentityFetcher {
        private final LayerClient mLayerClient;

        IdentityFetcher(LayerClient client) {
            mLayerClient = client;
        }

        private void fetchIdentities(final IdentityFetcherCallback callback) {
            Identity currentUser = mLayerClient.getAuthenticatedUser();
            Query.Builder<Identity> builder = Query.builder(Identity.class);
            if (currentUser != null) {
                builder.predicate(new Predicate(Identity.Property.USER_ID, Predicate.Operator.NOT_EQUAL_TO, currentUser.getUserId()));
            }
            final Query<Identity> identitiesQuery = builder.build();

            new AsyncTask<Void, Void, List<Identity>>() {

                @Override
                protected List<Identity> doInBackground(Void... params) {
                    return mLayerClient.executeQuery(identitiesQuery, Query.ResultType.OBJECTS);
                }

                @Override
                protected void onPostExecute(List<Identity> identities) {
                    callback.identitiesFetched(new HashSet<>(identities));
                }
            }.execute();
        }

        interface IdentityFetcherCallback {
            void identitiesFetched(Set<Identity> identities);
        }
    }

    private static class ParticipantAdapter extends ArrayAdapter<Identity> {

        public ParticipantAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_multiple_choice);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            CheckedTextView textView = (CheckedTextView) v;
            textView.setText(IdentityUtils.getDisplayName(getItem(position)));
            return v;
        }
    }
}
