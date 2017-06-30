package com.layer.messenger;


import android.content.Context;

import com.layer.sdk.LayerClient;
import com.layer.ui.util.imagecache.ImageCacheWrapper;
import com.layer.ui.util.imagecache.PicassoImageCacheWrapper;
import com.layer.ui.util.imagecache.requesthandlers.MessagePartRequestHandler;

import java.lang.ref.WeakReference;

public class Config {
    private static WeakReference<LayerClient> sLayerClient;

    public static ImageCacheWrapper getImageCacheWrapper(Context context) {
        return new PicassoImageCacheWrapper(getMessagePartHandler(), context);
    }

    private static MessagePartRequestHandler getMessagePartHandler() {
        return new MessagePartRequestHandler(getLayerClient());
    }

    public static void init(LayerClient layerClient) {
        sLayerClient = new WeakReference<>(layerClient);
    }

    public static LayerClient getLayerClient() {
        return sLayerClient != null && sLayerClient.get() != null ? sLayerClient.get() : null;
    }
}