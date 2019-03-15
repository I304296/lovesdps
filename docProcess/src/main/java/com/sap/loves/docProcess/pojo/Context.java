package com.sap.loves.docProcess.pojo;

public class Context {
	public int counter;
	private int index; //Main Loop Index
	private int pageIndex; //Page Index
	private Load load; //Source Payload
	private Status status; //Status Code
	private Status statusDBRecord;
	private RateConfirmation rc;
    private BillOfLading bol;
    private int bolCounter;
    private int miscCounter;
    private boolean exist;

	public Context() {
		//Default constructor		
	}
	
	public Context(int index) {
		this.index = index;
		this.exist = false;
		//Set dummy statusDB record;
		setStatusDBRecord(new Status(
				"",
				"",
				"",
				"", 
				"",
				"",
				0,
				"",
				""
				));
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Load getLoad() {
		return load;
	}

	public void setLoad(Load load) {
		this.load = load;
	}

	public Status getStatusDBRecord() {
		return statusDBRecord;
	}

	public void setStatusDBRecord(Status statusDBRecord) {
		this.statusDBRecord = statusDBRecord;
	}

	public RateConfirmation getRc() {
		return rc;
	}

	public void setRc(RateConfirmation rc) {
		this.rc = rc;
	}

	public BillOfLading getBol() {
		return bol;
	}

	public void setBol(BillOfLading bol) {
		this.bol = bol;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	
	public boolean isExist() {
		return exist;
	}

	public void setExist(boolean exist) {
		this.exist = exist;
	}

	public int getBolCounter() {
		return bolCounter;
	}

	public void setBolCounter(int bolCounter) {
		this.bolCounter = bolCounter;
	}

	public int getMiscCounter() {
		return miscCounter;
	}

	public void setMiscCounter(int miscCounter) {
		this.miscCounter = miscCounter;
	}

}
