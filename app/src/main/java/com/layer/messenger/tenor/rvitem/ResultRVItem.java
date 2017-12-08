package com.layer.messenger.tenor.rvitem;

import android.support.annotation.NonNull;

import com.tenor.android.core.model.impl.Result;
import com.tenor.android.core.widget.adapter.AbstractRVItem;

public class ResultRVItem extends AbstractRVItem {

    private final String mQuery;
    private final Result mResult;

    /**
     * For trending items
     */
    public ResultRVItem(int type, @NonNull Result result) {
        this(type, result, "");
    }

    /**
     * For search items
     */
    public ResultRVItem(int type, @NonNull Result result, @NonNull String query) {
        super(type, result.getId());
        mQuery = query;
        mResult = result;
    }

    @NonNull
    public Result getResult() {
        return mResult;
    }

    @NonNull
    public String getQuery() {
        return mQuery;
    }
}
