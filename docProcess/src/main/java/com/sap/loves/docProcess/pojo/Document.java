package com.sap.loves.docProcess.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Document {
	private String documentType;
	//private String documentFormat;
	@JsonIgnore
	private String shipFrom;
	@JsonIgnore
	private String shipTo;
	@JsonIgnore
	private String shipperName;
	@JsonIgnore
	private String receiverName;
	private Page[] pages;
	
	public Document( String documentType, 
			         //String documentFormat,
			         String shipFrom,
			         String shipTo,
			         String shipperName,
			         String receiverName,
			         Page[] pages) {
		
		this.documentType = documentType;
       // this.documentFormat = documentFormat;
        this.shipFrom = shipFrom;
        this.shipTo = shipTo;
        this.shipperName = shipperName;
        this.receiverName = receiverName;
        this.pages = pages;
	}
	            
	
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
/*	public String getDocumentFormat() {
		return documentFormat;
	}
	public void setDocumentFormat(String documentFormat) {
		this.documentFormat = documentFormat;
	}*/
	@JsonIgnore
	public String getShipFrom() {
		return shipFrom;
	}
	
	@JsonProperty
	public void setShipFrom(String shipFrom) {
		this.shipFrom = shipFrom;
	}
	
	@JsonIgnore
	public String getShipTo() {
		return shipTo;
	}
	
	@JsonProperty
	public void setShipTo(String shipTo) {
		this.shipTo = shipTo;
	}
	
	@JsonIgnore
	public String getShipperName() {
		return shipperName;
	}
	
	@JsonProperty
	public void setShipperName(String shipperName) {
		this.shipperName = shipperName;
	}
	
	@JsonIgnore
	public String getReceiverName() {
		return receiverName;
	}
	
	@JsonProperty
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
	public Page[] getPages() {
		return pages;
	}
	public void setPages(Page[] pages) {
		this.pages = pages;
	}
	
}
