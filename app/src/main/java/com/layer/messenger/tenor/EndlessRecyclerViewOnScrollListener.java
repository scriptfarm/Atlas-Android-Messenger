package com.layer.messenger.tenor;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.tenor.android.core.util.AbstractLayoutManagerUtils;
import com.tenor.android.core.weakref.WeakRefOnScrollListener;
import com.tenor.android.core.weakref.WeakRefRunnable;

public abstract class EndlessRecyclerViewOnScrollListener<CTX> extends WeakRefOnScrollListener<CTX> {

    private static final int VISIBLE_LIMIT = 10; // The minimum amount of items to have below your current scroll position before loading more.

    private boolean mLoading; // True if we are still waiting for the last set of data to load.
    private int mFirstVisibleItem;
    private int mVisibleItemCount;
    private int mTotalItemCount;
    private int mPreviousTotal; // The total number of items in the dataset after the last load

    private int mCurrentPage;

    private final Handler mHandler;
    private final WeakRefRunnable<EndlessRecyclerViewOnScrollListener> mLoadingRunnable;
    private final int mLimit;

    public EndlessRecyclerViewOnScrollListener(CTX ctx) {
        this(ctx, VISIBLE_LIMIT);
    }

    public EndlessRecyclerViewOnScrollListener(CTX ctx, int limit) {
        super(ctx);
        mLimit = limit;
        mHandler = new Handler();
        mLoadingRunnable = new WeakRefRunnable<EndlessRecyclerViewOnScrollListener>(this) {
            @Override
            public void run(@NonNull EndlessRecyclerViewOnScrollListener endlessRVOnScrollListener) {
                mLoading = false;
            }
        };
        reset();
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        mVisibleItemCount = recyclerView.getChildCount();
        mTotalItemCount = recyclerView.getLayoutManager().getItemCount();

        mFirstVisibleItem = AbstractLayoutManagerUtils.findFirstVisibleItemPosition(recyclerView.getLayoutManager());

        if (mLoading) {
            mHandler.removeCallbacks(mLoadingRunnable);
            if (mTotalItemCount == mPreviousTotal) {

                mHandler.postDelayed(mLoadingRunnable,
                        mTotalItemCount <= mLimit ? 50 : 3000);

            } else if (mTotalItemCount > mPreviousTotal) {
                // more items got loaded, loading completed
                mLoading = false;
                mPreviousTotal = mTotalItemCount;
            } else {
                // data outdated, reset
                reset();
            }
        }

        if (!mLoading
                // not displaying empty or loading view holder,
                && mTotalItemCount > 1
                // End has been reached
                && (mTotalItemCount - mVisibleItemCount) <= (mFirstVisibleItem + mLimit)) {

            // Do something
            mCurrentPage++;

            onLoadMore(mCurrentPage);

            mLoading = true;
        }
    }

    public abstract void onLoadMore(int currentPage);

    public void reset() {
        mLoading = false;
        mCurrentPage = 1;

        mFirstVisibleItem = -1;

        mVisibleItemCount = 0;
        mTotalItemCount = 0;
        mPreviousTotal = 0;
    }
}