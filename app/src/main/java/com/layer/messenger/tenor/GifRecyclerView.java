package com.layer.messenger.tenor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.layer.atlas.tenor.AbstractGifRecyclerView;
import com.layer.atlas.tenor.messagetype.threepartgif.GifSender;
import com.layer.atlas.tenor.util.GifSearchQueryClerk;
import com.layer.messenger.tenor.rvitem.ResultRVItem;
import com.layer.messenger.util.Log;
import com.tenor.android.core.constant.StringConstant;
import com.tenor.android.core.model.impl.Result;
import com.tenor.android.core.network.CallStub;
import com.tenor.android.core.response.BaseError;
import com.tenor.android.core.response.impl.GifsResponse;
import com.tenor.android.core.util.AbstractLocaleUtils;
import com.tenor.android.core.weakref.WeakRefRunnable;
import com.tenor.android.core.weakref.WeakRefUiHandler;
import com.tenor.android.core.widget.adapter.AbstractRVItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class GifRecyclerView extends AbstractGifRecyclerView implements IGifRecyclerView {

    private final WeakRefUiHandler<GifRecyclerView> mUiHandler;
    private final IGifRecyclerView.Presenter mPresenter;
    private final GifAdapter<GifRecyclerView> mAdapter;

    private String mNextPageId = "";
    private WeakRefRunnable<GifRecyclerView> mLoadGifsRunnable;
    @NonNull
    private Call mCall = new CallStub();

    public GifRecyclerView(Context context) {
        this(context, null);
    }

    public GifRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GifRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mUiHandler = new WeakRefUiHandler<>(this);
        mPresenter = new GifPresenter(this);
        mAdapter = new GifAdapter<>(this);

        setAdapter(mAdapter);

        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL);
        setLayoutManager(layoutManager);

        addOnScrollListener(new EndlessRecyclerViewOnScrollListener<GifRecyclerView>(this) {
            @Override
            public void onLoadMore(int currentPage) {
                loadGifs(true);
            }
        });
    }

    @Override
    public void postLoadGifs(final boolean append, long delay) {

        final WeakRefRunnable<GifRecyclerView> temp = mLoadGifsRunnable;
        mUiHandler.removeCallbacks(temp);
        mCall.cancel();

        mLoadGifsRunnable = new WeakRefRunnable<GifRecyclerView>(this) {
            @Override
            public void run(@NonNull GifRecyclerView gifRecyclerView) {
                if (!append) {
                    mAdapter.notifyOnGifLoading();
                    mNextPageId = StringConstant.EMPTY;
                }

                if (TextUtils.isEmpty(GifSearchQueryClerk.get().getSearchQuery())) {
                    mCall = mPresenter.getTrending(24, mNextPageId, null, append);
                    return;
                }

                mCall = mPresenter.search(GifSearchQueryClerk.get().getSearchQuery(),
                        AbstractLocaleUtils.getCurrentLocaleName(getContext()),
                        24, mNextPageId, null, append);
            }
        };
        mUiHandler.postDelayed(mLoadGifsRunnable, delay);
    }

    @Override
    public void onReceiveSearchResultsSucceed(@NonNull String query, @NonNull GifsResponse response, boolean isAppend) {
        List<AbstractRVItem> items = new ArrayList<>();

        for (Result result : response.getResults()) {
            items.add(new ResultRVItem(GifAdapter.TYPE_GIF, result, query));
        }
        mAdapter.setQuery(query);
        mAdapter.insert(items, isAppend);
        mNextPageId = response.getNext();
    }

    @Override
    public void onReceiveSearchResultsFailed(@Nullable BaseError error) {
        mAdapter.setQuery(StringConstant.EMPTY);
        Log.e(error != null ? error.getMessage() : "onReceiveSearchResultsFailed...");
    }

    @Override
    public void onReceiveTrendingSucceeded(@NonNull List<Result> list, @NonNull String nextPageId, boolean isAppend) {
        List<AbstractRVItem> items = new ArrayList<>();

        for (Result result : list) {
            items.add(new ResultRVItem(GifAdapter.TYPE_GIF, result));
        }
        mAdapter.setQuery(StringConstant.EMPTY);
        mAdapter.insert(items, isAppend);
        mNextPageId = nextPageId;
    }

    @Override
    public void onReceiveTrendingFailed(@Nullable BaseError error) {
        mAdapter.setQuery(StringConstant.EMPTY);
        Log.e(error != null ? error.getMessage() : "onReceiveTrendingFailed...");
    }

    @Override
    public void setGifSender(GifSender sender) {
        mAdapter.setGifSender(sender);
    }
}
