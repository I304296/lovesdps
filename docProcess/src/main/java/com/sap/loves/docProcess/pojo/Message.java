package com.sap.loves.docProcess.pojo;

public class Message {
	private int responseCode;
	private String responseMessage;
	public Message(int responseCode, String responseMessage) {
		this.responseCode = responseCode;
		this.responseMessage = responseMessage; 
	}
	public int getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	

}
