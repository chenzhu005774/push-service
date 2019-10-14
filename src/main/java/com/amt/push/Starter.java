package com.amt.push;

import com.amt.push.handler.IMServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Starter {

	private static Logger logger = LoggerFactory.getLogger(Starter.class);

	private Starter() {}
	/**
	 * 启动器.
	 * @param args
	 */
	public static void main(String[] args){
		IMServer server = IMServer.getInstance();
		try{
			server.start();
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			System.exit(1);
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			System.exit(1);
		}
	}
	
}
