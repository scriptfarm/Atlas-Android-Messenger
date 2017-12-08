package com.layer.messenger.tenor.holder;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.tenor.adapter.OnSendGifListener;
import com.layer.atlas.tenor.messagetype.threepartgif.GifSender;
import com.layer.atlas.tenor.model.IMinimalResult;
import com.tenor.android.core.constant.MediaCollectionFormat;
import com.tenor.android.core.loader.GlideTaskParams;
import com.tenor.android.core.loader.gif.GifLoader;
import com.tenor.android.core.model.impl.MediaCollection;
import com.tenor.android.core.model.impl.Result;
import com.tenor.android.core.util.AbstractListUtils;
import com.tenor.android.core.view.IBaseView;
import com.tenor.android.core.widget.viewholder.StaggeredGridLayoutItemViewHolder;

import java.util.List;

public class GifSelectionViewHolder<CTX extends IBaseView> extends StaggeredGridLayoutItemViewHolder<CTX> {

    private final ImageView mImageView;

    private MinimalResult mMinimalResult;
    private GifSender mGifSender;
    private OnSendGifListener mListener;

    public GifSelectionViewHolder(View itemView, CTX context,
                                  @Nullable final GifSender gifSender,
                                  @Nullable final OnSendGifListener listener) {
        super(itemView, context);
        mGifSender = gifSender;
        mListener = listener;
        mImageView = (ImageView) itemView.findViewById(R.id.tgi_iv_item);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGifSender == null || !hasRef()) {
                    return;
                }
                mGifSender.send(mMinimalResult);

                if (mListener != null) {
                    mListener.onGifSent(mMinimalResult);
                }
            }
        });
    }

    public void setImage(@Nullable Result result, @NonNull String query) {
        if (result == null) {
            return;
        }

        final int placeholderColor = Color.parseColor(result.getPlaceholderColorHex());
        mMinimalResult = new MinimalResult(result, query);
        // normal load to display
        List<MediaCollection> mediaCollections = result.getMedias();
        if (AbstractListUtils.isEmpty(mediaCollections)) {
            return;
        }

        final GlideTaskParams<ImageView> payload = new GlideTaskParams<>(mImageView, mMinimalResult.getPreviewUrl());
        payload.setPlaceholder(placeholderColor);
        GifLoader.loadGif(getContext(), payload);
    }

    private static class MinimalResult implements IMinimalResult {

        private final Result mResult;
        private final String mQuery;

        public MinimalResult(@NonNull Result result, @NonNull String query) {
            mResult = result;
            mQuery = query;
        }

        @NonNull
        @Override
        public String getQuery() {
            return mQuery;
        }

        @NonNull
        @Override
        public String getId() {
            return mResult.getId();
        }

        @NonNull
        @Override
        public String getUrl() {
            return mResult.getUrl();
        }

        @NonNull
        @Override
        public String getPreviewUrl() {
            return mResult.getMedias().get(0).get(MediaCollectionFormat.GIF_TINY).getUrl();
        }

        @Override
        public int getWidth() {
            return mResult.getMedias().get(0).get(MediaCollectionFormat.GIF_TINY).getWidth();
        }

        @Override
        public int getHeight() {
            return mResult.getMedias().get(0).get(MediaCollectionFormat.GIF_TINY).getHeight();
        }
    }
}
