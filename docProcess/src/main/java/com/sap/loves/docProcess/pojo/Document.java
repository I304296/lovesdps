package com.sap.loves.docProcess.pojo;

public class Document {
	private String documentType;
	//private String documentFormat;
	private String shipFrom;
	private String shipTo;
	private String shipperName;
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
	public String getShipFrom() {
		return shipFrom;
	}
	public void setShipFrom(String shipFrom) {
		this.shipFrom = shipFrom;
	}
	public String getShipTo() {
		return shipTo;
	}
	public void setShipTo(String shipTo) {
		this.shipTo = shipTo;
	}
	public String getShipperName() {
		return shipperName;
	}
	public void setShipperName(String shipperName) {
		this.shipperName = shipperName;
	}
	public String getReceiverName() {
		return receiverName;
	}
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
