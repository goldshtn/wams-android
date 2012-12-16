wams-android
============

This is the Windows Azure Mobile Services unofficial Android SDK. It is provided with no warranty or support, implied or explicit. Use at your own risk. The code is provided under the Creative Commons Attribution License [http://creativecommons.org/licenses/by/3.0/us/].

This SDK covers the following features of Windows Azure Mobile Services:
* Basic CRUD operations on data tables (select, insert, update, delete)
* Simple query operators (gt, lt, equals)
* Simple paging operators (top, skip)

To use the SDK, download the source and add the provided project as a library project to your Eclipse workspace. Next, add a library reference from your app to this project. Note that the project requires an minimum API level of 2.2.

Some examples of what you can do with this SDK, assuming you have a Windows Azure Mobile Service set up at the endpoint http://myservice.azure-mobile.net:

```java
@DataTable("apartments")
public class Apartment {
	//The fields don't have to be public; they are public here for brevity only
	@Key public int id;
	@DataMember public String address;
	@DataMember public int bedrooms;
	@DataMember public boolean published;
}

String apiKey = getResources().getString(R.string.msApiKey);
MobileService ms = new MobileService("http://myservice.azure-mobile.net", apiKey);
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

In the future, I plan to add push support (with GCM) and possibly additional features. Pull requests, enhancements, and any other assistance are very welcome.
