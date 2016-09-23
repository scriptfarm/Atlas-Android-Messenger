package com.layer.sample.util;


import com.layer.sdk.messaging.Identity;

import java.util.Comparator;

public class IdentityDisplayNameComparator implements Comparator<Identity> {

    @Override
    public int compare(Identity lhs, Identity rhs) {
        return IdentityUtils.getDisplayName(lhs).compareTo(IdentityUtils.getDisplayName(rhs));
    }
}
