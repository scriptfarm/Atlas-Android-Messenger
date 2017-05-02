package com.layer.messenger.util;

import com.layer.atlas.util.ConversationFormatter;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class Util {
    private static final ConversationFormatter sConversationFormatter = new ConversationFormatter();

    public static String streamToString(InputStream stream) throws IOException {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        return writer.toString();
    }

    public static String getConversationTitle(LayerClient layerClient, Conversation conversation) {
        return sConversationFormatter.getConversationTitle(layerClient, conversation);
    }

    public static void setConversationMetadataTitle(Conversation conversation, String title) {
        sConversationFormatter.setConversationMetadataTitle(conversation, title);
    }

    public static String getConversationMetadataTitle(Conversation conversation) {
        return sConversationFormatter.getConversationMetadataTitle(conversation);
    }
}
