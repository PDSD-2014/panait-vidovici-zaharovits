package com.eHonk.server;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoDriverRegisterProcessor implements PayloadProcessor {
	
	public static final Logger logger = Logger.getLogger(NoDriverRegisterProcessor.class.getName());
  @Override
  public void handleMessage(CcsMessage msg) {
  		/* just log I am not a driver message */
  	  logger.log(Level.INFO, "I am not a driver: " + msg.getFrom());
  }

}
