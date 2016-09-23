package com.layer.sample.messagelist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.layer.sample.R;
import com.layer.sample.util.IdentityUtils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Identity;

import java.util.ArrayList;
import java.util.List;

public class TypingIndicatorListener implements LayerTypingIndicatorListener {
    private final List<Identity> mActiveTypists = new ArrayList<>();
    private final TextView mIndicatorView;


    public TypingIndicatorListener(TextView indicatorView) {
        mIndicatorView = indicatorView;
    }

    @Override
    public void onTypingIndicator(LayerClient layerClient, Conversation conversation, Identity user, TypingIndicator typingIndicator) {
        if (typingIndicator == TypingIndicator.FINISHED) {
            mActiveTypists.remove(user);
        } else if (!mActiveTypists.contains(user)){
            mActiveTypists.add(user);
        }
        refreshView();
    }

    private void refreshView() {
        String indicatorText = createTypistsString();
        mIndicatorView.setText(indicatorText);
        if (TextUtils.isEmpty(indicatorText)) {
            mIndicatorView.setVisibility(View.GONE);
        } else {
            mIndicatorView.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    private String createTypistsString() {
        StringBuilder sb = new StringBuilder();
        Context context = mIndicatorView.getContext();
        for (Identity typist : mActiveTypists) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(context.getString(R.string.typing_indicator_format, IdentityUtils.getDisplayName(typist)));
        }
        return sb.toString();
    }
}
