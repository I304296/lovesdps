package com.sap.loves.docProcess.pojo;

public class RateConfirmation {
	
	private String GUID;        //key
	private String loadNo;      //key
	private String debtorName;  //key
	private String date;        //key
	
	private String documentConfidence;
	private String amount;
	private String amountML;
	private String amountConfidence;
	private String carrierName;
	private String carrierNameML;
	private String carrierNameConfidence;
	private String loadNoML;
	private String loadNoConfidence;
	private String debtorNameML;
	private String debtorNameConfidence;
	private String receiverNameML;
	private String receiverNameConfidence;
	private String shipperNameML;
	private String shipperNameConfidence;
	private String shipToML;
	private String shipToConfidence;
	private String shipFromML;
	private String shipFromConfidence;
	
	public RateConfirmation() {
		//Default Constructor
	}
	
	public RateConfirmation(String gUID, String loadNo, String debtorName, String date, String documentConfidence,
			String amount, String amountML, String amountConfidence, String carrierName, String carrierNameML,
			String carrierNameConfidence, String loadNoML, String loadNoConfidence, String debtorNameML,
			String debtorNameConfidence, String receiverNameML, String receiverNameConfidence, String shipperNameML,
			String shipperNameConfidence, String shipToML, String shipToConfidence, String shipFromML,
			String shipFromConfidence) {
		super();
		GUID = gUID;
		this.loadNo = loadNo;
		this.debtorName = debtorName;
		this.date = date;
		this.documentConfidence = documentConfidence;
		this.amount = amount;
		this.amountML = amountML;
		this.amountConfidence = amountConfidence;
		this.carrierName = carrierName;
		this.carrierNameML = carrierNameML;
		this.carrierNameConfidence = carrierNameConfidence;
		this.loadNoML = loadNoML;
		this.loadNoConfidence = loadNoConfidence;
		this.debtorNameML = debtorNameML;
		this.debtorNameConfidence = debtorNameConfidence;
		this.receiverNameML = receiverNameML;
		this.receiverNameConfidence = receiverNameConfidence;
		this.shipperNameML = shipperNameML;
		this.shipperNameConfidence = shipperNameConfidence;
		this.shipToML = shipToML;
		this.shipToConfidence = shipToConfidence;
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

	public String getDocumentConfidence() {
		return documentConfidence;
	}

	public void setDocumentConfidence(String documentConfidence) {
		this.documentConfidence = documentConfidence;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getAmountML() {
		return amountML;
	}

	public void setAmountML(String amountML) {
		this.amountML = amountML;
	}

	public String getAmountConfidence() {
		return amountConfidence;
	}

	public void setAmountConfidence(String amountConfidence) {
		this.amountConfidence = amountConfidence;
	}

	public String getCarrierName() {
		return carrierName;
	}

	public void setCarrierName(String carrierName) {
		this.carrierName = carrierName;
	}

	public String getCarrierNameML() {
		return carrierNameML;
	}

	public void setCarrierNameML(String carrierNameML) {
		this.carrierNameML = carrierNameML;
	}

	public String getCarrierNameConfidence() {
		return carrierNameConfidence;
	}

	public void setCarrierNameConfidence(String carrierNameConfidence) {
		this.carrierNameConfidence = carrierNameConfidence;
	}

	public String getLoadNoML() {
		return loadNoML;
	}

	public void setLoadNoML(String loadNoML) {
		this.loadNoML = loadNoML;
	}

	public String getLoadNoConfidence() {
		return loadNoConfidence;
	}

	public void setLoadNoConfidence(String loadNoConfidence) {
		this.loadNoConfidence = loadNoConfidence;
	}

	public String getDebtorNameML() {
		return debtorNameML;
	}

	public void setDebtorNameML(String debtorNameML) {
		this.debtorNameML = debtorNameML;
	}

	public String getDebtorNameConfidence() {
		return debtorNameConfidence;
	}

	public void setDebtorNameConfidence(String debtorNameConfidence) {
		this.debtorNameConfidence = debtorNameConfidence;
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
