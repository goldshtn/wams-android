package net.sashag.wams.android;

@DataTable("pushChannel")
public class PushChannel {
	
	public PushChannel(String regId) {
		this.regId = regId;
	}
	
	public int getId() {
		return id;
	}
	
	public String getRegId() {
		return regId;
	}

	@Key
	private int id;
	
	@DataMember("regId")
	private String regId;
	
	//TODO: Add subscriptions/topics list in the future
	
}
