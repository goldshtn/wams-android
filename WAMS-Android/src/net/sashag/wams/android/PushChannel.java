package net.sashag.wams.android;

/**
 * Represents a GCM push channel in the mobile service. Stored in a mobile service table
 * that must be created prior to registering for push operations.
 * 
 * @author Sasha Goldshtein
 *
 */
@DataTable("pushChannel")
public class PushChannel {
	
	/**
	 * Initializes this push channel with no data. Used for deserialization purposes only.
	 */
	public PushChannel() {}
	
	/**
	 * Initializes this push channel with the specified registration id.
	 * 
	 * @param regId		the GCM registration id
	 */
	public PushChannel(String regId) {
		this.regId = regId;
	}
	
	/**
	 * Retrieves the database id of this record.
	 * 
	 * @return			the database id of this record
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Retrieves the registration id of this channel.
	 * 
	 * @return			the registration id of this channel
	 */
	public String getRegId() {
		return regId;
	}

	@Key
	private int id;
	
	@DataMember("regId")
	private String regId;
	
	//TODO: Add subscriptions/topics list in the future
	
}
