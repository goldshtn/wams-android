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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

/**
 * Represents a data table in a mobile service. Use this class to perform
 * synchronous and asynchronous CRUD operations, including complex queries.
 * 
 * @author Sasha Goldshtein
 *
 * @param <E> the type of elements the table contains (a POJO decorated by {@link DataTable})
 */
public class MobileTable<E> {

	private final String serviceUrl;
	private final String apiKey;
	private final String tableName;
	private final Class<E> clazz;
	private final Executor executor;
	
	MobileTable(String serviceUrl, String apiKey, Class<E> clazz) {
		this.serviceUrl = serviceUrl;
		this.apiKey = apiKey;
		this.clazz = clazz;
		DataTable dataTableAnnotation = clazz.getAnnotation(DataTable.class);
		if (dataTableAnnotation == null) {
			throw new InvalidParameterException("Only classes annotated with @DataTable can be used with MobileTable");
		}
		this.tableName = dataTableAnnotation.value();
		this.executor = Executors.newCachedThreadPool();
	}
	
	/**
	 * Retrieves all elements in the mobile table and posts the specified callback to
	 * the provided {@link Handler} when the results are available or an exception occurs.
	 * 
	 * @param callback	called when the results are available or an exception occurs
	 * @param handler	callbacks are posted to this handler instead of the default thread
	 * 					that executed the operation
	 */
	public void allAsync(MobileServiceCallbackWithResults<E> callback, Handler handler) {
		new QueryBuilder().selectAsync(callback, handler);
	}
	
	/**
	 * Retrieves all elements in the mobile table and calls the specified callback when
	 * the results are available or an exception occurs.
	 * 
	 * @param callback	called when the results are available or an exception occurs
	 */
	public void allAsync(MobileServiceCallbackWithResults<E> callback) {
		new QueryBuilder().selectAsync(callback);
	}
	
	/**
	 * Retrieves all elements in the mobile table and returns them synchronously.
	 * 
	 * @return					all elements in the mobile table, or an empty collection
	 * @throws MobileException	thrown if an error occurred while retrieving the elements
	 */
	public List<E> all() throws MobileException {
		return new QueryBuilder().select();
	}
	
	/**
	 * Begins a query specification for retrieving elements from a mobile table. Call additional
	 * methods to qualify the query, and then finally call {@link MobileTable.QueryBuilder#select select}
	 * to retrieve the results. If all elements are desired, it is simpler to call {@link all}.
	 * <p>
	 * An example of constructing a query:
	 * <pre>
	 * mobileTable.where().equal("published", true).gt("bedrooms", 2).select();
	 * </pre>
	 * 
	 * @return	a {@link QueryBuilder} object that you can use to further customize the query
	 */
	public QueryBuilder where() {
		return new QueryBuilder();
	}
	
	private static class HandlerDecoratorWithResults<T> implements MobileServiceCallbackWithResults<T> {
		private Handler handler;
		private MobileServiceCallbackWithResults<T> callback;
		
		HandlerDecoratorWithResults(Handler handler, MobileServiceCallbackWithResults<T> callback) {
			this.handler = handler;
			this.callback = callback;
		}
		
		public void completedSuccessfully(final List<T> results) {
			handler.post(new Runnable() {
				public void run() {
					callback.completedSuccessfully(results);
				}
			});
		}

		public void errorOccurred(final MobileException exception) {
			handler.post(new Runnable() {
				public void run() {
					callback.errorOccurred(exception);
				}
			});
		}
		
	}
	
	private static class HandlerDecorator implements MobileServiceCallback {
		private Handler handler;
		private MobileServiceCallback callback;
		
		HandlerDecorator(Handler handler, MobileServiceCallback callback) {
			this.handler = handler;
			this.callback = callback;
		}
		
		public void completedSuccessfully() {
			handler.post(new Runnable() {
				public void run() {
					callback.completedSuccessfully();
				}
			});
		}

		public void errorOccurred(final MobileException exception) {
			handler.post(new Runnable() {
				public void run() {
					callback.errorOccurred(exception);
				}
			});
		}
	}
	
	/**
	 * Inserts the specified item into the mobile table, and posts the specified callback
	 * to the provided {@link Handler} when the operation completes. The provided item is
	 * modified to include an id (in the field decorated by the {@link Key} annotation)
	 * returned from the mobile service.
	 * 
	 * @param item		the item to insert
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 * @param handler	callbacks are posted to this handler instead of the thread that
	 * 					performed the operation
	 */
	public void insertAsync(E item, MobileServiceCallback callback, Handler handler) {
		insertAsync(item, new HandlerDecorator(handler, callback));
	}
	
	/**
	 * Inserts the specified item into the mobile table, and calls the specified callback
	 * when the operation completes. The provided item is modified to include an id (in the
	 * field decorated by the {@link Key} annotation) returned from the mobile service.
	 * 
	 * @param item		the item to insert
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 */
	public void insertAsync(final E item, final MobileServiceCallback callback) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					insert(item);
					callback.completedSuccessfully();
				} catch (MobileException e) {
					callback.errorOccurred(e);
				}
			}
		});
	}
	
	/**
	 * Inserts the specified item into the mobile table. The provided item is modified to include
	 * an id (in the field decorated by the {@link Key} annotation) returned from the mobile service.
	 * 
	 * @param item				the item to insert
	 * @throws MobileException	thrown if an error occurred while inserting the item
	 */
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
				Log.d("MobileTable", "HTTP POST request for insert returned status code: " + statusCode);
				
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
			throw new MobileException("Error creating item", e);
		}
		
		throw new MobileException("Error creating new item, status code: " + statusCode);
	}
	
	/**
	 * Updates the mobile table with the new information for the provided item. The id field
	 * (the field decorated with the {@link Key} annotation) must be set to the item's id for 
	 * the update to succeed. When the operation completes, a callback is posted to the provided
	 * {@link Handler}.
	 * 
	 * @param item		the item to update
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 * @param handler	the callback is posted to this handler instead of the thread that performed
	 * 					the operation
	 */
	public void updateAsync(E item, MobileServiceCallback callback, Handler handler) {
		updateAsync(item, new HandlerDecorator(handler, callback));
	}
	
	/**
	 * Updates the mobile table with the new information for the provided item. The id field
	 * (the field decorated with the {@link Key} annotation) must be set to the item's id for 
	 * the update to succeed. When the operation completes, the provided callback is invoked
	 * with exception details if an error occurred.
	 * 
	 * @param item		the item to update
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 */
	public void updateAsync(final E item, final MobileServiceCallback callback) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					update(item);
					callback.completedSuccessfully();
				} catch (MobileException e) {
					callback.errorOccurred(e);
				}
			}
		});
	}
	
	/**
	 * Updates the mobile table with the new information for the provided item. The id field
	 * (the field decorated with the {@link Key} annotation) must be set to the item's id for 
	 * the update to succeed.
	 * 
	 * @param item				the item to update
	 * @throws MobileException	thrown if an error occurred while updating the item
	 */
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
	
	/**
	 * Deletes the specified item from the mobile table. The id field (the field decorated with
	 * the {@link Key} must be set to the item's id for the delete to succeed. When the operation
	 * completes, the provided callback is posted to the specified {@link Handler}.
	 * 
	 * @param item		the item to delete
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 * @param handler	the callback is posted to this handler instead of the thread that performed
	 * 					the operation
	 */
	public void deleteAsync(E item, MobileServiceCallback callback, Handler handler) {
		deleteAsync(item, new HandlerDecorator(handler, callback));
	}
	
	/**
	 * Deletes the specified item from the mobile table. The id field (the field decorated with
	 * the {@link Key} must be set to the item's id for the delete to succeed. When the operation
	 * completes, the provided callback is invoked with exception information, if an error occurred.
	 * 
	 * @param item		the item to delete
	 * @param callback	the callback invoked when the operation completes, specifying an
	 * 					error if one occurred
	 */
	public void deleteAsync(final E item, final MobileServiceCallback callback) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					delete(item);
					callback.completedSuccessfully();
				} catch (MobileException e) {
					callback.errorOccurred(e);
				}
			}
		});
	}
	
	/**
	 * Deletes the specified item from the mobile table. The id field (the field decorated with
	 * the {@link Key} must be set to the item's id for the delete to succeed.
	 * 
	 * @param item				the item to delete
	 * @throws MobileException	thrown if an exception occurs while deleting the item
	 */
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
			throw new MobileException("Error deleting item", e);
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
	
	/**
	 * Helper class for constructing queries using a fluent API. The methods of this class
	 * enable query construction in steps, such as the following:
	 * <p>
	 * <pre>
	 * table.where().equal("bedrooms", 2).gt("footage", 1337).select();
	 * </pre>
	 * <p>
	 * When the query construction is complete, call {@link select} to obtain the list of
	 * items matching the query. Note that the query is evaluated on the server; only objects
	 * matching the query criteria are passed back to the client.
	 * <p>
	 * Currently, this class does not support Boolean algebra operations such as AND/OR.
	 * All filters provided are joined together using an AND clause. In other words, the 
	 * preceding query retrieves objects with a "bedrooms" property equal to 2 AND a "footage"
	 * property whose value is greater than 1337.
	 * 
	 * @author Sasha Goldshtein
	 *
	 */
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
		
		private QueryBuilder() { }
		
		/**
		 * Match objects whose specified property is equal to the specified value.
		 * 
		 * @param column	the property to match
		 * @param value		the value to match
		 * @return			an instance of this class that can be used for further query customization
		 */
		public QueryBuilder equal(String column, Object value) {
			operands.put(column, new QueryOperand(EQUAL, value.toString()));
			return this;
		}
		
		/**
		 * Match objects whose specified numeric property value is greater than the specified value. 
		 * 
		 * @param column	the property to match
		 * @param value		the value to match
		 * @return			an instance of this class that can be used for further query customization
		 */
		public QueryBuilder gt(String column, Object value) {
			operands.put(column, new QueryOperand(GT, value.toString()));
			return this;
		}
		
		/**
		 * Match objects whose specified numeric property value is less than the specified value.
		 * 
		 * @param column	the property to match
		 * @param value		the value to match
		 * @return			an instance of this class that can be used for further query customization
		 */
		public QueryBuilder lt(String column, Object value) {
			operands.put(column, new QueryOperand(LT, value.toString()));
			return this;
		}
		
		/**
		 * Return at most the specified number of items that match all other query criteria.
		 * 
		 * @param items				the maximum number of items to match
		 * @return					an instance of this class that can be used for further query customization
		 * @throws MobileException	thrown if this method has already been called with this
		 * 							{@link QueryBuilder} instance 
		 */
		public QueryBuilder top(int items) throws MobileException {
			if (top != -1) {
				throw new MobileException("Value for top has already been set to: " + top);
			}
			top = items;
			return this;
		}
	
		/**
		 * Skip the specified number of items that match all other query criteria.
		 * 
		 * @param items				the number of items to skip
		 * @return					an instance of this class that can be used for further query customization
		 * @throws MobileException	thrown if this method has already been called with this
		 * 							{@link QueryBuilder} instance 
		 */
		public QueryBuilder skip(int items) throws MobileException {
			if (skip != -1) {
				throw new MobileException("Value for skip has already been set to: " + skip);
			}
			skip = items;
			return this;
		}
		
		/**
		 * Order the results by the following columns, in ascending order.
		 * 
		 * @param properties		the columns to order by
		 * @return					an instance of this class that can be used for further query customization
		 * @throws MobileException	thrown if this method has already been called with this
		 * 							{@link QueryBuilder} instance, or if the {@link orderByDesc} method
		 * 							has already been called with this {@link QueryBuilder} instance 
		 */
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
		
		/**
		 * Order the results by the following columns, in descending order.
		 * 
		 * @param properties		the columns to order by
		 * @return					an instance of this class that can be used for further query customization
		 * @throws MobileException	thrown if this method has already been called with this
		 * 							{@link QueryBuilder} instance, or if the {@link orderBy} method
		 * 							has already been called with this {@link QueryBuilder} instance 
		 */
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
		
		/**
		 * Evaluates the query and returns the matching items. The results are posted via the
		 * specified callback to the provided {@link Handler}.
		 * 
		 * @param callback	the callback invoked when the operation completes, providing the 
		 * 					list of items or an exception if one occurred
		 * @param handler	the callback is posted to this handler instead of the thread that
		 * 					performed the operation
		 */
		public void selectAsync(MobileServiceCallbackWithResults<E> callback, Handler handler) {
			selectAsync(new HandlerDecoratorWithResults<E>(handler, callback));
		}

		/**
		 * Evaluates the query and returns the matching items. The results are posted via the
		 * specified callback.
		 * 
		 * @param callback	the callback invoked when the operation completes, providing the 
		 * 					list of items or an exception if one occurred
		 */
		public void selectAsync(final MobileServiceCallbackWithResults<E> callback) {
			executor.execute(new Runnable() {
				public void run() {
					try {
						List<E> results = select();
						callback.completedSuccessfully(results);
					} catch (MobileException e) {
						callback.errorOccurred(e);
					}
				}
			});
		}
		
		/**
		 * Evaluates the query and returns the matching items.
		 * 
		 * @throws MobileException	thrown if an exception occurred while evaluating the query
		 */
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
