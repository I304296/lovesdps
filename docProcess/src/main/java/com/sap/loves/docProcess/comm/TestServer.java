package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class TestServer implements IServer {
	//Test Server
	private String url; 
	private Context context;
	
	final static Logger log = LoggerFactory.getLogger(ApiController.class);
	
	public TestServer(String url) {
		this.url = url;
	}
	@Override
	public Context execute() {
		// TODO Auto-generated method stub
		String ret = "";
		if(this.url.equals("https://test-server.com/")) {
			ret = "Test Success";
		}else {
			ret = "Test failed";
		}
			
		return this.context;
	}
	@Override
	public Context fallBack() {
		// TODO Auto-generated method stub
		return null;
	}

}
