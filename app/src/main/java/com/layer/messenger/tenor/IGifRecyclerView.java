package com.layer.messenger.tenor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tenor.android.core.model.impl.Result;
import com.tenor.android.core.presenter.IBasePresenter;
import com.tenor.android.core.response.BaseError;
import com.tenor.android.core.response.impl.GifsResponse;
import com.tenor.android.core.response.impl.TrendingGifResponse;
import com.tenor.android.core.view.IBaseView;

import java.util.List;

import retrofit2.Call;

public interface IGifRecyclerView extends IBaseView {

    interface Presenter extends IBasePresenter<IGifRecyclerView> {
        Call<GifsResponse> search(String query, String locale, int limit, String pos, String type, boolean isAppend);

        Call<TrendingGifResponse> getTrending(int limit, String pos, String type, boolean isAppend);
    }

    void onReceiveSearchResultsSucceed(@NonNull String query, @NonNull GifsResponse response, boolean isAppend);

    void onReceiveSearchResultsFailed(@Nullable BaseError error);

    void onReceiveTrendingSucceeded(@NonNull List<Result> list, @NonNull String nextPageId, boolean isAppend);

    void onReceiveTrendingFailed(@Nullable BaseError error);
}
