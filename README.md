# Atlas Messenger for Android

Atlas Messenger is a fully-featured messaging app following [Material Design guidelines](https://www.google.com/design/spec/material-design/introduction.html#introduction-goals), built on top of the [Layer SDK](https://layer.com/), using the [Atlas UI toolkit](https://github.com/layerhq/Atlas-Android).

## <a name="just_starting"></a>Just Starting?

Use our new XDK Messenger! The XDK Messenger enables a richer messaging experience and new features will be added there. See the repository at https://github.com/layerhq/Android-XDK-Messenger. Don't worry, Atlas-Android-Messenger will still be supported in the meantime.

## <a name="setup"></a>Setup

Run `git submodule update --init` to initialize and pull in the [Atlas-Android](https://github.com/layerhq/Atlas-Android) submodule.

## <a name="structure"></a>Structure

* **App:** Application class.
* Activities:
  * **BaseActivity:** Base Activity class for handling menu titles and the menu back button and ensuring the `LayerClient` is connected when resuming Activities.
  * **ConversationsListActivity:** List of all Conversations for the authenticated user.
  * **MessagesListActivity:** List of Messages within a particular Conversation.  Also handles message composition and addressing.
  * **SettingsActivity:** Global application settings.
  * **ConversationDetailsActivity:** Settings for a particular Conversation.
* **PushNotificationReceiver:** Handles `com.layer.sdk.PUSH` Intents and displays notifications.
* **AuthenticationProvider:** Interface used by the Messenger app to authenticate users.  Default implementation is provided; see *provider* below.

## <a name="identityproviders"></a>Identity Providers

Atlas Messenger uses the `AuthenticationProvider` interface to authenticate with various backends.  Additional identity providers can integrate with Atlas Messenger by implementing `AuthenticationProvider` and using a custom login Activity.

### <a name="provider"></a> Provider
To use the sample app, you need to register your app on the [Dashboard] (https://developer.layer.com) and follow the guide to deploy the [Sample Identity Provider](https://github.com/layerhq/layer-identity-provider) backend on Heroku. In assets/LayerConfiguration.json, update the "name" and "app_id" to the values displayed on the dashboard; update the "identity_provider_url" to the URL of the sample app you deployed on Heroku.

`[
  {
    "name": null,
    "app_id": null,
    "identity_provider_url": null
  }
]`

Learn more about the authentication flow [here] (https://docs.layer.com/sdk/android/authentication).

### <a name="pushnotifications"></a>Push Notifications

In order to build Atlas-Messenger, you must generate a `google-services.json` file so push notifications will be enabled. If this is not done, the Gradle build will fail with the error: `File google-services.json is missing. The Google Services Plugin cannot function without it.`

To do this, please follow the push notification setup steps [here](https://docs.layer.com/sdk/android/push).

#### To Disable Notification
* Remove the `google-services` plugin in the necessary `build.gradle` files.
* Set `options.useFirebaseCloudMessaging(true)` to false in `App.generateLayerClient()`

## Building and releasing
Ensure that the apk is signed using at least signing `v1` (and optionally `v2`). Not signing with `v1` will result in a certificate not found error on devices below Android 7.0


## <a name="contributing"></a>Contributing
Atlas is an Open Source project maintained by Layer. Feedback and contributions are always welcome and the maintainers try to process patches as quickly as possible. Feel free to open up a Pull Request or Issue on Github.

## <a name="license"></a>License

Atlas is licensed under the terms of the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please see the [LICENSE](LICENSE) file for full details.

## <a name="contact"></a>Contact

Atlas was developed in San Francisco by the Layer team. If you have any technical questions or concerns about this project feel free to reach out to [Layer Support](mailto:support@layer.com).

### <a name="credits"></a>Credits

* [Amar Srinivasan](https://github.com/sriamar)
* [Peter Elliott](https://github.com/smpete)
* [Archit Joshi](https://github.com/thecombatwombat)
* [Akinsanmi Waleola](https://github.com/andela-wakinsanmi)
