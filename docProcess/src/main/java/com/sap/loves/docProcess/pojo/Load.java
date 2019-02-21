package com.sap.loves.docProcess.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Load {
	private String GUID;
	private String loadNo;
	private String debtorName;
	private String amount;
	private String carrierName;
	private String date; // YYYMMDD
	private Document[] documents;
	@JsonIgnore
	private List<String> filenames;
	@JsonIgnore
	private String stitchedPdfName;

	public Load(String GUID, String loadNo, String debtorName, String amount, String carrierName, String date,
			Document[] documents) {

		this.GUID = GUID;
		this.loadNo = loadNo;
		this.debtorName = debtorName;
		this.amount = amount;
		this.carrierName = carrierName;
		this.date = date;
		this.documents = documents;
		this.filenames = new ArrayList<String>();
		this.stitchedPdfName = "";
	}

	public String getGUID() {
		return GUID;
	}

	public void setGUID(String gUID) {
		GUID = gUID;
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

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getCarrierName() {
		return carrierName;
	}

	public void setCarrierName(String carrierName) {
		this.carrierName = carrierName;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Document[] getDocuments() {
		return documents;
	}

	public void setDocuments(Document[] documents) {
		this.documents = documents;
	}

	public String getStitchedPdfName() {
		return stitchedPdfName;
	}

	public void setStitchedPdfName(String stitchedPdfName) {
		this.stitchedPdfName = stitchedPdfName;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}

}

