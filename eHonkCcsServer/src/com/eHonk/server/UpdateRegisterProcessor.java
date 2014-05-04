package com.eHonk.server;

import java.sql.SQLException;

import com.eHonk.server.Datastore.DriverRecord;

public class UpdateRegisterProcessor implements PayloadProcessor {

  @Override
  public void handleMessage(CcsMessage msg) {
      final String license_plate = msg.getPayload().get(Constants.PROPERTY_LICENSE_PLATE);
      final Datastore ds = Datastore.getDatastore();
  		Datastore.DriverRecord driver_record = 
  				ds.new DriverRecord(msg.getFrom(), license_plate);
  		try {
  			/* registering all */
  			ds.updateRegistration(driver_record);
  		} catch (SQLException e) {
  			e.printStackTrace();
  		}
  }
}
