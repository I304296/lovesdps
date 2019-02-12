package com.sap.loves.docProcess.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.sap.loves.docProcess.api.ApiController;
import com.sap.loves.docProcess.pojo.Context;

public class RemoteCall extends HystrixCommand<Context>{
	
	private IServer remoteService;
	private Context context;
	
	final static Logger log = LoggerFactory.getLogger(ApiController.class);
	
	public RemoteCall(Setter config, IServer remoteService, Context context) {
		super(config);
		this.remoteService = remoteService;
		this.context = context;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Context run() throws Exception {
		// TODO Auto-generated method stub
		this.context = remoteService.execute();
		return context;
	}
	
	//Fallback implementation
    @Override
    protected Context getFallback() {
    	this.context = remoteService.fallBack();
        return context;
    }	

}
