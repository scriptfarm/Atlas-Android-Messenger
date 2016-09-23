package com.layer.sample.util;


import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.layer.sdk.messaging.Identity;

public class IdentityUtils {

    @NonNull
    public static String getDisplayName(Identity identity) {
        if (TextUtils.isEmpty(identity.getDisplayName())) {
            String first = identity.getFirstName();
            String last = identity.getLastName();
            if (!TextUtils.isEmpty(first)) {
                if (!TextUtils.isEmpty(last)) {
                    return String.format("%s %s", first, last);
                }
                return first;
            } else if (!TextUtils.isEmpty(last)) {
                return last;
            } else {
                return identity.getUserId();
            }
        }
        return identity.getDisplayName();
    }
}
