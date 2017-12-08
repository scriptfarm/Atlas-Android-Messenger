package com.layer.messenger.tenor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tenor.android.core.constant.AspectRatioRange;
import com.tenor.android.core.constant.MediaFilter;
import com.tenor.android.core.constant.StringConstant;
import com.tenor.android.core.network.ApiClient;
import com.tenor.android.core.presenter.impl.BasePresenter;
import com.tenor.android.core.response.BaseError;
import com.tenor.android.core.response.WeakRefCallback;
import com.tenor.android.core.response.impl.GifsResponse;
import com.tenor.android.core.response.impl.TrendingGifResponse;

import java.lang.ref.WeakReference;

import retrofit2.Call;

public class GifPresenter extends BasePresenter<IGifRecyclerView> implements IGifRecyclerView.Presenter {

    public GifPresenter(IGifRecyclerView view) {
        super(view);
    }

    @Override
    public Call<GifsResponse> search(final String query, String locale, int limit, String pos, String type, final boolean append) {

        final String qry = StringConstant.getOrEmpty(query);

        Call<GifsResponse> call = ApiClient.getInstance(getView().getContext()).search(
                ApiClient.getServiceIds(getView().getContext()),
                qry, limit, StringConstant.getOrEmpty(pos), MediaFilter.BASIC, AspectRatioRange.ALL);

        call.enqueue(new OnSearchCallEnqueuedCallback(getView(), query, append));
        return call;
    }

    @Override
    public Call<TrendingGifResponse> getTrending(int limit, String pos, String type, final boolean isAppend) {
        Call<TrendingGifResponse> call = ApiClient.getInstance(getView().getContext()).getTrending(
                ApiClient.getServiceIds(getView().getContext()),
                limit, StringConstant.getOrEmpty(pos), MediaFilter.BASIC, AspectRatioRange.ALL);

        call.enqueue(new OnTrendingCallEnqueuedCallback(getView(), isAppend));
        return call;
    }

    private class OnSearchCallEnqueuedCallback
            extends WeakRefCallback<IGifRecyclerView, GifsResponse> {

        private final String mQuery;
        private final boolean mAppend;

        public OnSearchCallEnqueuedCallback(@NonNull IGifRecyclerView view, @NonNull String query, boolean append) {
            this(new WeakReference<>(view), query, append);
        }

        public OnSearchCallEnqueuedCallback(@NonNull WeakReference<IGifRecyclerView> weakRef, @NonNull String query, boolean append) {
            super(weakRef);
            mQuery = query;
            mAppend = append;
        }

        @Override
        public void success(@NonNull IGifRecyclerView view, @Nullable GifsResponse response) {
            if (response == null) {
                return;
            }
            view.onReceiveSearchResultsSucceed(mQuery, response, mAppend);
        }

        @Override
        public void failure(@NonNull IGifRecyclerView view, @Nullable BaseError error) {
            view.onReceiveSearchResultsFailed(error);
        }
    }

    private class OnTrendingCallEnqueuedCallback
            extends WeakRefCallback<IGifRecyclerView, TrendingGifResponse> {

        private final boolean mAppend;

        public OnTrendingCallEnqueuedCallback(@NonNull IGifRecyclerView view, boolean append) {
            this(new WeakReference<>(view), append);
        }

        public OnTrendingCallEnqueuedCallback(@NonNull WeakReference<IGifRecyclerView> weakRef, boolean append) {
            super(weakRef);
            mAppend = append;
        }

        @Override
        public void success(@NonNull IGifRecyclerView view, @Nullable TrendingGifResponse response) {
            if (response == null) {
                return;
            }
            view.onReceiveTrendingSucceeded(response.getResults(), response.getNext(), mAppend);
        }

        @Override
        public void failure(@NonNull IGifRecyclerView view, @Nullable BaseError error) {
            view.onReceiveTrendingFailed(error);
        }
    }
}
