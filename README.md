Introduction to WAMS-Android
============================

This is the Windows Azure Mobile Services unofficial Android SDK. It is provided with no warranty or support, implied or explicit. Use at your own risk. The code is provided under the [Creative Commons Attribution License](http://creativecommons.org/licenses/by/3.0/us/).

This SDK covers the following features of Windows Azure Mobile Services:
* Basic CRUD operations on data tables (select, insert, update, delete)
* Simple query operators (gt, lt, equals)
* Simple paging operators (top, skip)
* Authentication support with WAMS providers: Microsoft Account, Facebook, Twitter, Google
* Authentication token persistence across application runs
* Push support with GCM and some scripts on the server
* Displaying toasts and notifications when processing a push

To use the SDK, download the source and add the provided project as a library project to your Eclipse workspace. Next, add a library reference from your app to this project. Note that the project requires an minimum API level of 2.2.

Finally, you need to add the following two string resources to your project if you intend to use GCM push. I recommend that you add them in any case, because then you can use a convenient constructor of the MobileService class.

```xml
<string name="mobileServiceUrl">https://YOURSERVICENAME.azure-mobile.net</string>
<string name="mobileServiceApiKey">YOUR_API_KEY</string>
```

Some examples of what you can do with the data SDK:

```java
@DataTable("apartments")
public class Apartment {
	//The fields don't have to be public; they are public here for brevity only
	@Key public int id;
	@DataMember public String address;
	@DataMember public int bedrooms;
	@DataMember public boolean published;
}

//Inside your activity's code:
MobileService ms = new MobileService(this);
MobileTable<Apartment> apartments = ms.getTable(Apartment.class);

Apartment newApartment = new Apartment();
newApartment.address = "One Microsoft Way, Redmond WA";
newApartment.bedrooms = 17;
newApartment.published = true;
apartments.insert(newApartment);

List<Apartment> bigApts = apartments.where().gt("bedrooms", 3).orderByDesc("bedrooms").take(3);
for (Apartment apartment : bigApts) {
	apartment.published = false;
	apartments.update(apartment);
}

//Other operations feature async support as well:
apartments.deleteAsync(newApartment, new MobileServiceCallback() {
	public void completedSuccessfully() {}
	public void errorOccurred(MobileException exception) {}
});
```

Android push setup using GCM
============================

These instructions explain how to set up Google Cloud Messaging (GCM) push for your Windows Azure Mobile Service, while using this unofficial SDK.

Before you begin, make sure you follow the instructions at [GCM Getting Started](http://developer.android.com/google/gcm/gs.html) -- specifically, you need to follow the instructions for:

* Creating a Google API Project
* Enabling the GCM Service
* Obtaining an API Key

You do *not* need to perform the rest of the steps in the GCM Getting Started guide. Instead, follow the instructions below for setting up your application's manifest and implementing an optional service to handle push notifications when the application is not running.

Add a string resource to identify your service
----------------------------------------------

Add the following string resource in addition to the mobileServiceUrl and mobileServiceApiKey resources you added previously:

```xml
<string name="mobileServicePushSenderId">YOUR_GCM_SENDER_ID</string>
```

Modify the manifest
-------------------

In your manifest, insert the following section outside of the *application* element:

```xml
<permission
    android:name="YOUR_PACKAGE_HERE.permission.C2D_MESSAGE"
    android:protectionLevel="signature" />
<uses-permission android:name="YOUR_PACKAGE_HERE.permission.C2D_MESSAGE" />
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Next, inside the *application* element, add the following section:

```xml
<receiver
    android:name="net.sashag.wams.android.WAMSGCMBroadcastReceiver"
    android:permission="com.google.android.c2dm.permission.SEND" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
        <category android:name="YOUR_PACKAGE_HERE" />
    </intent-filter>
</receiver>
<service android:name=".PushHandlerService" />
```

**NOTE**: In both sections, make sure to replace *YOUR_PACKAGE_HERE* with your actual application package (the whole thing, including the dots, e.g. "net.sashag.rentahome").

**NOTE**: If you do not implement the service described in the next step, replace *.PushHandlerService* with *net.sashag.wams.android.WAMSGCMIntentService*.

Implement an optional service to handle push
--------------------------------------------

If you'd like to handle push notifications while your application is not running, add a class that extends WAMSGCMIntentService, as follows:

```java
public class PushHandlerService extends WAMSGCMIntentService {
    @Override
    protected void onPushMessage(Intent intent) {
        //The extras contain the payload you send from the server script:
        String newApartmentAddress = intent.getStringExtra("address");
        ...
    }
}
```

In that case, make sure you use the class name in the appropriate service element in the application manifest, as outlined above.

Register for push on startup
----------------------------

During your application's startup, e.g. in your activity's onCreate method, insert the following code:

```java
mobileService = new MobileService(this);
mobileService.registerPush();
```

If you want to use a transient push callback (a callback that is only valid for the duration of your application's runtime), use the *registerPushWithTransientCallback* method instead:

```java
mobileService.registerPushWithTransientCallback(new MobileServicePushCallback() {
    public void onPushMessageReceived(Intent intent) {
        //Use the intent extras to figure out the payload
    }
});
```

Server side setup for GCM
=========================

Next, you turn to setting up your Windows Azure Mobile Service to support the client.

The client-side code assumes that a "pushChannel" table exists, so you must first create it. Next, you have to set up a script to handle inserts to that table so that you do not insert duplicate registrations. Finally, you have to perform a push request to GCM with an optional payload.

Script to filter for duplicate channels
---------------------------------------

Use this as the insert script for the "pushChannel" table:

```javascript
function insert(item, user, request) {
    var channelTable = tables.getTable('pushChannel');
    channelTable
        .where({ regId: item.regId })
        .read({ success: insertChannelIfNotFound });

    function insertChannelIfNotFound(existingChannels) {
        if (existingChannels.length > 0) {
            request.respond(200, existingChannels[0]);
        } else {
            request.execute();
        }
    }
}
```

Script to push through GCM
--------------------------

Use this function wherever you need to perform a push through GCM. If necessary, modify the code to push the message to only a subset of clients. Also make sure to insert your push API key in the 'Authorization' header below.

```javascript
//Sends a GCM push to all registered subscribers from the pushChannel table,
//with the specified payload body. Note that the payload is limited to about
//4000 bytes after it's serialized to JSON.
function sendGcmPush(payload) {
    var reqModule = require('request');
    var channelTable = tables.getTable('pushChannel');
    channelTable.read({
    	success: function(channels) {
    		channels.forEach(function(channel) {
    			reqModule({
    				url: 'https://android.googleapis.com/gcm/send',
    				method: 'POST',
    				headers: {
    					//TODO: this should be your push API key
    					'Authorization': 'key=PUSH_API_KEY'
    				},
    				json: {
    					//You could pipe up to 1,000 registration ids here
    					registration_ids: [channel.regId],
    					data: payload
    				}
    			}, function (err, resp, body) {
    	            if (err || resp.statusCode !== 200) {
    	                console.error('Error sending GCM push: ', err);
    	            } else {
    	            	console.log('Sent GCM push notification successfully to ' + channel.regId);
    	            	//TODO: handle dead channels
    	            }
                });
    		});
    	}
    });
}
```
To learn more about how to handle dead channels and how to interpret the GCM response in general, consult the [GCM Architecture Overview](http://developer.android.com/google/gcm/gcm.html#response).

Sending toasts and notifications
--------------------------------

The client-side library can automatically process some types of push messages and display a toast or a notification (in the Android notification area) when receiving a push message. If you send a built-in notification, the push intent *will not* be passed to your onPushMessage method. Instead, the client-side library will display a toast or a notification, as appropriate.

To send a toast notification, set the payload field to the following JSON:

```javascript
{
    __builtInType: "toast",
    text: "A new apartment was added in your vicinity."
}
```

To send a notification that appears in the Android notification area, set the payload field to the following JSON:

```javascript
{
    __builtInType: "notification",
    contentTitle: "New apartment added",
    contentText: "3 bedrooms on One Microsoft Way, Redmond WA",
    tickerText: "A new apartment was added on One Microsoft Way",
    number: "1",
    action: "SOME_INTENT_ACTION",
    payload: "SOME_STRING"
}
```

The *number*, *action*, and *payload* properties are optional. If you do not set the *action* property, clicking the notification will open your application's main activity. If you set the *action* property, clicking the notification will open the activity that is registered to handle the specified intent action. If you set the *payload* property, it will be passed as-is in a string extra named "payload" to the activity that opens when the user clicks the notification.

Script to test your push support with curl
------------------------------------------

When testing your client-side push support, you might find the following curl command line useful:

```
curl -X POST -H 'Authorization: key=YOUR_KEY_HERE' -H 'Content-Type: application/json' https://android.googleapis.com/gcm/send --data '{ "registration_ids": ["REG_ID_HERE"], "data": { "__builtInType": "notification", "contentTitle": "This is the title", "contentText": "This is the content text", "tickerText": "This is the ticker text", "number": "42" } }'
```

Authentication
==============

You can use the client-side library to authenticate users through Windows Azure Mobile Services. First, make sure you configured the authentication providers you intend to work with (see the [Developer Guide](https://www.windowsazure.com/en-us/develop/mobile/resources/#header-2) for more information). For example, in the case of Twitter authentication, this requires that you create a Twitter application and provide the application ID and secret in the Windows Azure Management Portal. Alternatively, you can use the command-line 'azure' tool:

```
azure mobile config set YOUR_SERVICE_NAME twitterClientId YOUR_CLIENT_ID
azure mobile config set YOUR_SERVICE_NAME twitterClientSecret YOUR_CLIENT_SECRET
```

Now, you can use the authentication provider as follows:

```java
mobileService.login(MobileServiceAuthenticationProvider.TWITTER, new MobileServiceLoginCallback() {
    public void errorOccurred(MobileException exception) {
        //Invoked if an error occurred in the authentication process
    }
    public void completedSuccessfully(MobileUser user) {
        //Invoked if the login completed successfully -- user.getUserId() provides the user id
        //which you can also access in server-side scripts
    }
    public void cancelled() {
        //Invoked if the user cancelled the login process
    }
});
```

The client-side library persists the authentication token and user information in a file on the device, which means you don't have to invoke the *login* method more than once. To check whether the user is currently logged-in, use the *MobileService.isLoggedIn* method. To log out, use the *MobileService.logout* method (this also clears the information from the device so that subsequent runs will require a login).
