package net.sashag.wams.android;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;


import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class Serializer {

	static int getIdFrom(Object obj) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = obj.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			Key keyAnnotation = field.getAnnotation(Key.class);
			if (keyAnnotation != null) {
				if (field.getType() != int.class) {
					throw new InvalidParameterException("@Key field must be of type int");
				}
				field.setAccessible(true);
				return field.getInt(obj);
			}
		}
		throw new InvalidParameterException("Specified object does not have a @Key field");
	}
	
	static void setIdOn(Object obj, int id) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = obj.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			Key keyAnnotation = field.getAnnotation(Key.class);
			if (keyAnnotation != null) {
				if (field.getType() != int.class) {
					throw new InvalidParameterException("@Key field must be of type int");
				}
				field.setAccessible(true);
				field.setInt(obj, id);
				return;
			}
		}
		throw new InvalidParameterException("Specified object does not have a @Key field");
	}
	
	static Class<?> getFieldClassForKey(Class<?> clazz, String key) {
		for (Field field : clazz.getDeclaredFields()) {
			DataMember dataMemberAnnotation = field.getAnnotation(DataMember.class);
			if (dataMemberAnnotation != null && dataMemberAnnotation.value().equals(key)) {
				return field.getType();
			}
		}
		throw new InvalidParameterException("No field found with @DataMember attribute with key '" + key + "'");
	}
	
	public static String toJson(Object obj, boolean withId) throws IllegalArgumentException, IllegalAccessException, JSONException {
		JSONObject jsonObject = new JSONObject();
		Class<?> clazz = obj.getClass();
		boolean foundKey = false;
		for (Field field : clazz.getDeclaredFields()) {
			if (withId) {
				Key keyAnnotation = field.getAnnotation(Key.class);
				if (keyAnnotation != null) {
					if (foundKey) {
						throw new InvalidParameterException("There can be only one @Key field per class");
					}
					if (field.getType() != int.class) {
						throw new InvalidParameterException("@Key field must be of type int");
					}
					foundKey = true;
					field.setAccessible(true);
					Object fieldValue = field.get(obj);
					jsonObject.put("id", fieldValue);
				}
			}
			DataMember dataMemberAnnotation = field.getAnnotation(DataMember.class);
			if (dataMemberAnnotation != null) {
				field.setAccessible(true);
				Object fieldValue = field.get(obj);
				//Special hack for WAMS -- currently the C# and iOS SDKs convert Booleans to 0/1
				//values, which are then stored in SQL Server as a numeric column and not BIT.
				//We do the same here for cross-platform compatibility with these SDKs.
				if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
					int boolValue = fieldValue.equals(true) ? 1 : 0;
					jsonObject.put(dataMemberAnnotation.value(), boolValue);
				} else {
					jsonObject.put(dataMemberAnnotation.value(), fieldValue);
				}
			}
		}
		if (withId && !foundKey) {
			throw new InvalidParameterException("The class provided does not have a @Key field");
		}
		return jsonObject.toString();
	}
	
	public static <E> E toObject(JSONObject jsonObject, Class<E> clazz)
			throws JSONException, InstantiationException, IllegalAccessException {
		if (!jsonObject.has("id")) {
			throw new InvalidParameterException("The JSON string does not contain an id element");
		}
		E obj = clazz.newInstance();
		boolean foundKey = false;
		for (Field field : clazz.getDeclaredFields()) {
			Key keyAnnotation = field.getAnnotation(Key.class);
			Class<?> fieldType = field.getType();
			if (keyAnnotation != null) {
				if (foundKey) {
					throw new InvalidParameterException("There can be only one @Key field per class");
				}
				if (fieldType != int.class) {
					throw new InvalidParameterException("@Key field must be of type int");
				}
				foundKey = true;
				field.setAccessible(true);
				field.set(obj, jsonObject.getInt("id"));
				continue;
			}
			DataMember dataMemberAnnotation = field.getAnnotation(DataMember.class);
			if (dataMemberAnnotation != null) {
				String propName = dataMemberAnnotation.value();
				if (!jsonObject.has(propName)) {
					//If a value is missing, the server table may have changed and our local class definition
					//was not updated. Ideally, this would be an error, but for compatibility purposes with the
					//other (C#, iOS) SDKs, we simply ignore the field and emit a warning.
					Log.w("Serializer", "Server JSON object does not contain a value for field: " + propName);
					continue;
				}
				field.setAccessible(true);
				if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
					field.set(obj, jsonObject.getInt(propName));
				}
				else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
					field.set(obj, jsonObject.getDouble(propName));
				}
				else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
					//See comment in the toJson method -- we support 0/1 numeric values
					//as the value of a Boolean property, because of cross-platform compat
					//with the C# and iOS SDKs.
					try {
						int boolValue = jsonObject.getInt(propName);
						if (boolValue != 0 && boolValue != 1) {
							throw new InvalidParameterException("Invalid value " + boolValue + " specified for Boolean property: " + propName);
						}
						field.set(obj, boolValue == 1 ? true : false);
					} catch (JSONException e) {
						Log.v("Serializer", "JSON value for Boolean property '" + propName + "' received as Boolean and not a number (0/1). This may fail with the C# and iOS SDKs.");
						field.set(obj, jsonObject.getBoolean(propName));
					}
				}
				else if (fieldType.equals(String.class)) {
					field.set(obj, jsonObject.getString(propName));
				}
				else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
					field.set(obj, jsonObject.getLong(propName));
				}
				else {
					//Taking a risk -- this may fail
					field.set(obj, jsonObject.get(propName));
				}
			}
		}
		if (!foundKey) {
			throw new InvalidParameterException("The class provided does not have a @Key field");
		}
		return obj;
	}
	
}
