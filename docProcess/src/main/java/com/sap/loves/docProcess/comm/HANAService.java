package com.sap.loves.docProcess.comm;

import com.sap.loves.docProcess.pojo.Context;

public class HANAService implements IServer {
	private Context context;
	private String baseUrl;
	private String entity;
	private String operation;
	private String filter;
	public HANAService(Context context, String baseUrl, String entity, String operation) {
		this.context = context;
		this.baseUrl = baseUrl;
		this.entity= entity;
		this.operation = operation;
	}
	@Override
	public Context execute() {
		// TODO Auto-generated method stub
		// Implement OData Call
		return context;
	}
	@Override
	public Context fallBack() {
		// TODO Auto-generated method stub
		// Default implementation
		return context;
	}

}
