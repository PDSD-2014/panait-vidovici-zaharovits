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

public class ProcessorFactory {

	private static final String PACKAGE = "com.eHonk";
		private static final String ACTION_ECHO = PACKAGE + ".ECHO";
	
	public static PayloadProcessor getProcessor(String action) {
		if (action == null) {
			throw new IllegalStateException("action must not be null");
		}
		if (action.equals(Constants.LABEL_REGISTER_MESSAGE)) {
			return new RegisterProcessor();
		} else if (action.equals(Constants.LABEL_CHANGEDRIVER_MESSAGE)) {
			return new UpdateRegisterProcessor();
		} else if (action.equals(Constants.LABEL_NODRIVER_MESSAGE)) {
			return new NoDriverRegisterProcessor();
		} else if (action.equals(Constants.LABEL_NOTIFY_MESSAGE)) {
			return new NotifyProcessor();
		} else if (action.equals(ACTION_ECHO)) {
			return new EchoProcessor();
		}
		throw new IllegalStateException("Action " + action + " is unknown");
	}
}
