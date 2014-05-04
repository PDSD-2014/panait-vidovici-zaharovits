/*
 * Copyright 2014 Wolfram Rittmeyer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eHonk.server;

import java.sql.SQLException;

/**
 * Handles a user registration.
 */
public class RegisterProcessor implements PayloadProcessor {

	@Override
	public void handleMessage(CcsMessage msg) {
		final String license_plate = msg.getPayload().get(
		    Constants.PROPERTY_LICENSE_PLATE);
		final Datastore ds = Datastore.getDatastore();
		Datastore.DriverRecord driver_record = ds.new DriverRecord(msg.getFrom(),
		    license_plate);
		try {
			/* registering all */
			ds.register(driver_record);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
