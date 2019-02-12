package com.sap.loves.docProcess.comm;

import com.sap.loves.docProcess.pojo.Context;

public interface IServer {
	Context execute();
	Context fallBack();
}
