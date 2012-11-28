package net.sashag.wams.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class MobileTable<E> {

	private final String serviceUrl;
	private final String apiKey;
	private final String tableName;
	private final Class<E> clazz;
	
	MobileTable(String serviceUrl, String apiKey, Class<E> clazz) {
		this.serviceUrl = serviceUrl;
		this.apiKey = apiKey;
		this.clazz = clazz;
		DataTable dataTableAnnotation = clazz.getAnnotation(DataTable.class);
		if (dataTableAnnotation == null) {
			throw new InvalidParameterException("Only classes annotated with @DataTable can be used with MobileTable");
		}
		this.tableName = dataTableAnnotation.value();
	}
	
	public List<E> all() throws MobileException {
		return new QueryBuilder().select();
	}
	
	public QueryBuilder where() {
		return new QueryBuilder();
	}
	
	public void insert(E item) throws MobileException {
		String insertUrl = getInsertUrl();
		int statusCode;
		JSONObject jsonResult = null;
		try {
			String body = Serializer.toJson(item, /*withId*/false);
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(insertUrl);
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);
				urlConnection.setDoInput(true);
				urlConnection.setRequestMethod("POST");
				urlConnection.addRequestProperty("Content-Type", "application/json");
				urlConnection.addRequestProperty("ACCEPT", "application/json");
				urlConnection.addRequestProperty("X-ZUMO-APPLICATION", apiKey);
				
				DataOutputStream bodyOutput = new DataOutputStream(urlConnection.getOutputStream());
				bodyOutput.writeBytes(body);
				bodyOutput.flush();
				bodyOutput.close();
				
				statusCode = urlConnection.getResponseCode();
				Log.i("MobileTable", "HTTP POST request for insert returned status code: " + statusCode);
				
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				StringBuilder result = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
				jsonResult = new JSONObject(result.toString());
				
				if (statusCode == HttpStatus.SC_CREATED) {
					int id = jsonResult.getInt("id");
					Serializer.setIdOn(item, id);
					return;
				}
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
			}
		} catch (Exception e) {
			throw new MobileException("Error inserting item", e);
		}
		
		throw new MobileException("Error creating new item, status code: " + statusCode);
	}
	
	public void update(E item) throws MobileException {
		int statusCode;
		try {
			int id = Serializer.getIdFrom(item);
			String updateUrl = getUpdateUrlForId(id);
			
			String body = Serializer.toJson(item, /*withId*/true);
			HttpClient httpClient = new DefaultHttpClient();
			HttpPatch httpPatch = new HttpPatch(updateUrl);
			httpPatch.addHeader("Content-Type", "application/json");
			httpPatch.addHeader("ACCEPT", "application/json");
			httpPatch.addHeader("X-ZUMO-APPLICATION", apiKey);
			
			StringEntity postBody = new StringEntity(body);
			postBody.setContentType("application/json");
			httpPatch.setEntity(postBody);
			
			HttpResponse response = httpClient.execute(httpPatch);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			throw new MobileException("Error updating item", e);
		}
		
		if (statusCode == HttpStatus.SC_OK)
			return;
		
		throw new MobileException("Error updating item, status code: " + statusCode);

	}
	
	public void delete(E item) throws MobileException {
		int statusCode;
		try {
			int id = Serializer.getIdFrom(item);
			String deleteUrl = getDeleteUrlForId(id);
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(deleteUrl);
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("DELETE");
				urlConnection.addRequestProperty("X-ZUMO-APPLICATION", apiKey);
				statusCode = urlConnection.getResponseCode();
				Log.i("MobileTable", "HTTP DELETE request returned status code: " + statusCode);
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
			}
		} catch (Exception e) {
			throw new MobileException("Error inserting item", e);
		}
		if (statusCode == HttpStatus.SC_NO_CONTENT)
			return; //Successfully deleted the item
		
		throw new MobileException("Error deleting item, status code: " + statusCode);
	}
	
	private String getDeleteUrlForId(int id) {
		return serviceUrl + "/tables/" + tableName + "/" + id;
	}
	
	private String getInsertUrl() {
		return serviceUrl + "/tables/" + tableName;
	}
	
	private String getSelectUrl() {
		return serviceUrl + "/tables/" + tableName;
	}
	
	private String getUpdateUrlForId(int id) {
		return serviceUrl + "/tables/" + tableName + "/" + id;
	}
	
	public class QueryBuilder {
		
		private static final int EQUAL = 1;
		private static final int GT = 2;
		private static final int LT = 3;
		
		private class QueryOperand {
			public QueryOperand(int operand, String value) {
				this.operand = operand;
				this.value = value;
			}
			
			public int operand;
			public String value;
		}
		
		private Map<String, QueryOperand> operands = new TreeMap<String, QueryOperand>();
		private int top = -1;
		private int skip = -1;
		private String[] orderBy;
		private String[] orderByDesc;
		
		public QueryBuilder equal(String column, Object value) {
			operands.put(column, new QueryOperand(EQUAL, value.toString()));
			return this;
		}
		
		public QueryBuilder gt(String column, Object value) {
			operands.put(column, new QueryOperand(GT, value.toString()));
			return this;
		}
		
		public QueryBuilder lt(String column, Object value) {
			operands.put(column, new QueryOperand(LT, value.toString()));
			return this;
		}
		
		public QueryBuilder top(int items) throws MobileException {
			if (top != -1) {
				throw new MobileException("Value for top has already been set to: " + top);
			}
			top = items;
			return this;
		}
		
		public QueryBuilder skip(int items) throws MobileException {
			if (skip != -1) {
				throw new MobileException("Value for skip has already been set to: " + skip);
			}
			skip = items;
			return this;
		}
		
		public QueryBuilder orderBy(String... properties) throws MobileException {
			if (orderBy != null) {
				throw new MobileException("Value for orderBy has already been set");
			}
			if (orderByDesc != null) {
				throw new MobileException("You cannot use both orderBy and orderByDesc in a single query");
			}
			orderBy = properties;
			return this;
		}
		
		public QueryBuilder orderByDesc(String... properties) throws MobileException {
			if (orderByDesc != null) {
				throw new MobileException("Value for orderByDesc has already been set");
			}
			if (orderBy != null) {
				throw new MobileException("You cannot use both orderBy and orderByDesc in a single query");
			}
			orderByDesc = properties;
			return this;
		}
		
		//TODO - support Boolean algebra (AND, OR ...)
		
		private String quoteValueIfNecessary(String key, String value) {
			Class<?> fieldClass = Serializer.getFieldClassForKey(clazz, key);
			if (fieldClass.equals(String.class)) {
				return "'" + value + "'";
			}
			return value;
		}
		
		private String buildQueryUrl() {
			String selectUrl = getSelectUrl();
			if (operands.size() == 0)
				return selectUrl;
			selectUrl += "?$filter=(";
			//At this time we assume that all operands are to be strung together with 'and'
			ArrayList<String> keys = new ArrayList<String>(operands.keySet());
			for (int i = 0; i < keys.size(); ++i) {
				String key = keys.get(i);
				QueryOperand operand = operands.get(key);
				String quotedValue = quoteValueIfNecessary(key, operand.value);
				switch (operand.operand) {
				case EQUAL:
					selectUrl += "(" + key + "%20eq%20" + quotedValue + ")";
					break;
				case GT:
					selectUrl += "(" + key + "%20gt%20" + quotedValue + ")";
					break;
				case LT:
					selectUrl += "(" + key + "%20lt%20" + quotedValue + ")";
					break;
				}
				if (i != keys.size() - 1) {
					selectUrl += "%20and%20";
				}
			}
			selectUrl += ")";
			if (top != -1) {
				selectUrl += "&$top=" + top;
			}
			if (skip != -1) {
				selectUrl += "&$skip=" + skip;
			}
			if (orderBy != null && orderBy.length > 0) {
				selectUrl += "&$orderby=";
				selectUrl += joinOrderByFields(orderBy, false);
			}
			if (orderByDesc != null && orderByDesc.length > 0) {
				selectUrl += "&$orderby=";
				selectUrl += joinOrderByFields(orderByDesc, true);
			}
			return selectUrl;
		}
		
		private String joinOrderByFields(String[] fields, boolean descending) {
			String result = "";
			for (int i = 0; i < fields.length; ++i) {
				result += fields[i];
				if (descending) {
					result += "%20desc";
				}
				if (i != fields.length - 1) {
					result += ",";
				}
			}
			return result;
		}
		
		public List<E> select() throws MobileException {
			String queryUrl = buildQueryUrl();
			try {
				URL url = new URL(queryUrl);
				Log.d("MobileTable", "Executing select request: " + queryUrl);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.addRequestProperty("Content-Type", "application/json");
				urlConnection.addRequestProperty("ACCEPT", "application/json");
				urlConnection.addRequestProperty("X-ZUMO-APPLICATION", MobileTable.this.apiKey);
				try {
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						response.append(line);
					}
					JSONArray jsonArray = new JSONArray(response.toString());
					List<E> results = new ArrayList<E>(jsonArray.length());
					for (int i = 0; i < jsonArray.length(); i++) {
						results.add(Serializer.toObject(jsonArray.getJSONObject(i), MobileTable.this.clazz));
					}
					return results;
				} finally {
					urlConnection.disconnect();
				}
			} catch (Exception e) {
				throw new MobileException("Error fetching objects", e);
			}
		}
	}
	
}
