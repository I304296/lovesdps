package com.sap.loves.docProcess.pojo;

public class BillOfLading {
	private String GUID;        //key
	private String loadNo;      //key
	private String debtorName;  //key
	private String date;        //key
	private String BOLID;       //key
	
	private String receiverName;
	private String receiverNameML;
	private String receiverNameConfidence;
	private String shipperName;
	private String shipperNameML;
	private String shipperNameConfidence;
	private String shipTo;
	private String shipToML;
	private String shipToConfidence;
	private String shipFrom;
	private String shipFromML;
	private String shipFromConfidence;
	
	public BillOfLading() {}

	
	
	public BillOfLading(String gUID, String loadNo, String debtorName, String date, String bOLID, String receiverName,
			String receiverNameML, String receiverNameConfidence, String shipperName, String shipperNameML,
			String shipperNameConfidence, String shipTo, String shipToML, String shipToConfidence, String shipFrom,
			String shipFromML, String shipFromConfidence) {
		super();
		GUID = gUID;
		this.loadNo = loadNo;
		this.debtorName = debtorName;
		this.date = date;
		BOLID = bOLID;
		this.receiverName = receiverName;
		this.receiverNameML = receiverNameML;
		this.receiverNameConfidence = receiverNameConfidence;
		this.shipperName = shipperName;
		this.shipperNameML = shipperNameML;
		this.shipperNameConfidence = shipperNameConfidence;
		this.shipTo = shipTo;
		this.shipToML = shipToML;
		this.shipToConfidence = shipToConfidence;
		this.shipFrom = shipFrom;
		this.shipFromML = shipFromML;
		this.shipFromConfidence = shipFromConfidence;
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

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getBOLID() {
		return BOLID;
	}

	public void setBOLID(String bOLID) {
		BOLID = bOLID;
	}

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}

	public String getReceiverNameML() {
		return receiverNameML;
	}

	public void setReceiverNameML(String receiverNameML) {
		this.receiverNameML = receiverNameML;
	}

	public String getReceiverNameConfidence() {
		return receiverNameConfidence;
	}

	public void setReceiverNameConfidence(String receiverNameConfidence) {
		this.receiverNameConfidence = receiverNameConfidence;
	}

	public String getShipperName() {
		return shipperName;
	}

	public void setShipperName(String shipperName) {
		this.shipperName = shipperName;
	}

	public String getShipperNameML() {
		return shipperNameML;
	}

	public void setShipperNameML(String shipperNameML) {
		this.shipperNameML = shipperNameML;
	}

	public String getShipperNameConfidence() {
		return shipperNameConfidence;
	}

	public void setShipperNameConfidence(String shipperNameConfidence) {
		this.shipperNameConfidence = shipperNameConfidence;
	}

	public String getShipTo() {
		return shipTo;
	}

	public void setShipTo(String shipTo) {
		this.shipTo = shipTo;
	}

	public String getShipToML() {
		return shipToML;
	}

	public void setShipToML(String shipToML) {
		this.shipToML = shipToML;
	}

	public String getShipToConfidence() {
		return shipToConfidence;
	}

	public void setShipToConfidence(String shipToConfidence) {
		this.shipToConfidence = shipToConfidence;
	}

	public String getShipFrom() {
		return shipFrom;
	}

	public void setShipFrom(String shipFrom) {
		this.shipFrom = shipFrom;
	}

	public String getShipFromML() {
		return shipFromML;
	}

	public void setShipFromML(String shipFromML) {
		this.shipFromML = shipFromML;
	}

	public String getShipFromConfidence() {
		return shipFromConfidence;
	}

	public void setShipFromConfidence(String shipFromConfidence) {
		this.shipFromConfidence = shipFromConfidence;
	}
	
	

}
