package com.layer.messenger.tenor.network;

import android.content.Context;
import android.support.annotation.NonNull;

import com.tenor.android.core.network.ApiService;
import com.tenor.android.core.network.IApiService;
import com.tenor.android.core.network.constant.Protocols;

/**
 * API Client to make network calls to retrieve contents
 */
public class TenorApiService<T> extends ApiService<T> {

    public TenorApiService(Builder<T> builder) {
        super(builder);
    }

    public static class Builder<T> extends ApiService.Builder<T> {

        private static final long serialVersionUID = -6173559068148648687L;

        public Builder(@NonNull Context context, @NonNull Class<T> cls, boolean debug) {
            super(context, cls);

            // put your tenor API key here
            apiKey("");

            if (debug) {
                protocol(Protocols.HTTP);
                server("qa-api");
            }
        }

        public IApiService<T> build() {
            return new TenorApiService<>(this);
        }
    }
}
