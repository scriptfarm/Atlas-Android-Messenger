package com.layer.messenger.util;

import android.content.Context;

import com.layer.messenger.App;
import com.layer.sdk.LayerClient;
import com.layer.ui.conversationitem.ConversationItemFormatter;
import com.layer.ui.identity.IdentityFormatter;
import com.layer.ui.identity.IdentityFormatterImpl;
import com.layer.ui.message.messagetypes.CellFactory;
import com.layer.ui.message.messagetypes.generic.GenericCellFactory;
import com.layer.ui.message.messagetypes.location.LocationCellFactory;
import com.layer.ui.message.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.ui.message.messagetypes.text.TextCellFactory;
import com.layer.ui.message.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.ui.util.DateFormatter;
import com.layer.ui.util.DateFormatterImpl;
import com.layer.ui.util.imagecache.ImageCacheWrapper;
import com.layer.ui.util.imagecache.PicassoImageCacheWrapper;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {
    private static ConversationItemFormatter sConversationItemFormatter;
    private static List<CellFactory<?,?>> sCellFactories;
    private static ImageCacheWrapper sImageCacheWrapper;
    private static IdentityFormatter sIdentityFormatter;
    private static DateFormatter sDateFormatter;

    public static void init(Context context, LayerClient layerClient, Picasso picasso) {
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        sImageCacheWrapper = new PicassoImageCacheWrapper(picasso);
        sConversationItemFormatter = new ConversationItemFormatter(context, timeFormat, dateFormat, getCellFactories(layerClient));
    }

    public static ConversationItemFormatter getConversationItemFormatter() {
        return sConversationItemFormatter;
    }

    public static List<CellFactory<?,?>> getCellFactories(LayerClient layerClient) {
        if (sCellFactories == null || sCellFactories.isEmpty()) {
            sCellFactories = new ArrayList<>();
            sCellFactories.add(new TextCellFactory());
            sCellFactories.add(new ThreePartImageCellFactory(layerClient, sImageCacheWrapper));
            sCellFactories.add(new SinglePartImageCellFactory(layerClient, sImageCacheWrapper));
            sCellFactories.add(new LocationCellFactory(sImageCacheWrapper));
            sCellFactories.add(new GenericCellFactory());

            if (sConversationItemFormatter != null) {
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

    public static DateFormatter getDateFormatter(Context context) {
        if (sDateFormatter == null) {
            sDateFormatter = new DateFormatterImpl(context);
        }
        return sDateFormatter;
    }

    public static IdentityFormatter getIdentityFormatter(Context context) {
        if (sIdentityFormatter == null) {
            sIdentityFormatter = new IdentityFormatterImpl(context);
        }
        return sIdentityFormatter;
    }
}
