package net.sashag.wams.android;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;


import org.json.JSONException;
import org.json.JSONObject;

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
				//TODO - coercion to types JSON doesn't support
				Object fieldValue = field.get(obj);
				jsonObject.put(dataMemberAnnotation.value(), fieldValue);
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
			}
			DataMember dataMemberAnnotation = field.getAnnotation(DataMember.class);
			if (dataMemberAnnotation != null) {
				field.setAccessible(true);
				//TODO - what if the value does not exist?
				//TODO - support arrays?
				if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
					field.set(obj, jsonObject.getInt(dataMemberAnnotation.value()));
				}
				else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
					field.set(obj, jsonObject.getDouble(dataMemberAnnotation.value()));
				}
				else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
					field.set(obj, jsonObject.getBoolean(dataMemberAnnotation.value()));
				}
				else if (fieldType.equals(String.class)) {
					field.set(obj, jsonObject.getString(dataMemberAnnotation.value()));
				}
				else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
					field.set(obj, jsonObject.getLong(dataMemberAnnotation.value()));
				}
				else {
					//Taking a risk -- this may fail
					field.set(obj, jsonObject.get(dataMemberAnnotation.value()));
				}
			}
		}
		if (!foundKey) {
			throw new InvalidParameterException("The class provided does not have a @Key field");
		}
		return obj;
	}
	
}
