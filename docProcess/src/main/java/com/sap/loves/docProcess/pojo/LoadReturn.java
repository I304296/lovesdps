package com.sap.loves.docProcess.pojo;

public class LoadReturn {
    private Message msg;
	private Load load;
	
	public LoadReturn(Load load, Message msg) {
		this.load = load;
		this.msg = msg;
	}

	public Load getLoad() {
		return load;
	}

	public void setLoad(Load load) {
		this.load = load;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

}
