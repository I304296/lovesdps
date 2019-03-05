package com.sap.loves.docProcess.pojo;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class Status {
	private String GUID;
	private String loadNo;
	private String debtorName;
	private String date; 
	private String documentType;
	private String fileName;
	private int pageCount;
	private String status;
	private String statusDescription;
	
	public Status(
			 String GUID,
			 String loadNo,
			 String debtorName,
			 String date, 
			 String documentType,
			 String fileName,
			 int pageCount,
			 String status,
			 String statusDescription
			) {
		  this.GUID = GUID;
		  this.loadNo = loadNo;
		  this.debtorName = debtorName;
		  this.date = date;
		  this.documentType = documentType;
	      this.fileName = fileName;
		  this.status = status;		
		  this.statusDescription = statusDescription;
	}
	
    @JsonProperty("GUID")
	public String getGUID() {
		return GUID;
	}

	public void setGUID(String GUID) {
		this.GUID = GUID;
	}

	public String getLoadNo() {
		return loadNo;
	}

	public void setLoadNo(String loadNo) {
		this.loadNo = loadNo;
	}

	public String getDebtorName() {
		return debtorName;
	}

	public void setDebtorName(String debtorName) {
		this.debtorName = debtorName;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getStatusDescription() {
		return statusDescription;
	}

	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}

	public int getPageCount() {
		return pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}
	

}
