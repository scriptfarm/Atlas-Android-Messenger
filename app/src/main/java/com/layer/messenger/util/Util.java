package com.layer.messenger.util;

import android.content.Context;

import com.layer.messenger.App;
import com.layer.sdk.LayerClient;
import com.layer.ui.conversationitem.ConversationItemFormatter;
import com.layer.ui.messagetypes.CellFactory;
import com.layer.ui.messagetypes.location.LocationCellFactory;
import com.layer.ui.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.ui.messagetypes.text.TextCellFactory;
import com.layer.ui.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.ui.util.LayerDateFormatter;
import com.layer.ui.util.imagecache.ImageCacheWrapper;
import com.layer.ui.util.imagecache.PicassoImageCacheWrapper;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Set;

public class Util {
    private static ConversationItemFormatter sConversationItemFormatter;
    private static Set<CellFactory> sCellFactories;
    private static ImageCacheWrapper sImageCacheWrapper;
    private static LayerDateFormatter sLayerDateFormatter;

    public static void init(Context context, LayerClient layerClient, Picasso picasso) {
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        sConversationItemFormatter = new ConversationItemFormatter(context, timeFormat, dateFormat, getCellFactories(layerClient, picasso));
    }

    public static ConversationItemFormatter getConversationItemFormatter() {
        return sConversationItemFormatter;
    }

    public static Set<CellFactory> getCellFactories(LayerClient layerClient, Picasso picasso) {
        if (sCellFactories == null || sCellFactories.isEmpty()) {
            sCellFactories = new HashSet<>();
            sCellFactories.add(new TextCellFactory());
            sCellFactories.add(new ThreePartImageCellFactory(layerClient, picasso));
            sCellFactories.add(new SinglePartImageCellFactory(layerClient, picasso));
            sCellFactories.add(new LocationCellFactory(picasso));

            if (sConversationItemFormatter !=null) {
                sConversationItemFormatter.setCellFactories(sCellFactories);
            }
        }
        return sCellFactories;
    }

    public static String streamToString(InputStream stream) throws IOException {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        return writer.toString();
    }

    /**
     * Provides an instance of {@link ImageCacheWrapper} for caching image
     * This sample app implementation uses Picasso, see {@link PicassoImageCacheWrapper}
     * Replace the implementation with whatever Image Caching library you wish to use.
     */
    public static ImageCacheWrapper getImageCacheWrapper() {
        if (sImageCacheWrapper == null) {
            sImageCacheWrapper = new PicassoImageCacheWrapper(App.getPicasso());
        }
        return sImageCacheWrapper;
    }

    public static LayerDateFormatter getLayerDateFormatter(Context context) {
        if (sLayerDateFormatter == null) {
            sLayerDateFormatter = new LayerDateFormatterImpl(context);
        }
        return sLayerDateFormatter;
    }
}
