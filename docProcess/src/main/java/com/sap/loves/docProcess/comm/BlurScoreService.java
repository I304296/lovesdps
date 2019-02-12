package com.sap.loves.docProcess.comm;

import com.sap.loves.docProcess.pojo.Context;

public class BlurScoreService implements IServer {
    private String url;
    private Context context;
    
    public BlurScoreService(Context context, String url) {
    	this.context = context;
    	this.url = url;
    }
	@Override
	public Context execute() {
		// TODO Auto-generated method stub
		String base64content = context.getLoad().getDocuments()[context.getIndex()].getPages()[context.getPageIndex()].getContent();
		// Call Blur service using RestTemplate, get score and update context
		return this.context;
	}
	@Override
	public Context fallBack() {
		// TODO Auto-generated method stub
		// Default implementation
		return context;
	}

}
