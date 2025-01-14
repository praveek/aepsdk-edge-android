/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.util;

import com.adobe.marketing.mobile.xdm.Property;
import com.adobe.marketing.mobile.xdm.Schema;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestXDMSchema implements Schema {

	public String testString;
	public Integer testInt;
	public Boolean testBool;
	public Double testDouble;
	public TestXDMObject testXDMObject;
	public Date timestamp;

	@Override
	public String getSchemaVersion() {
		return "1.5";
	}

	@Override
	public String getSchemaIdentifier() {
		return "https://schema.example.com";
	}

	@Override
	public String getDatasetIdentifier() {
		return "abc123def";
	}

	@Override
	public Map<String, Object> serializeToXdm() {
		Map<String, Object> map = new HashMap<>();

		if (this.testString != null) {
			map.put("testString", this.testString);
		}

		if (this.testInt != null) {
			map.put("testInt", this.testInt);
		}

		if (this.testBool != null) {
			map.put("testBool", this.testBool);
		}

		if (this.testDouble != null) {
			map.put("testDouble", this.testDouble);
		}

		if (this.testXDMObject != null) {
			map.put("testXDMObject", this.testXDMObject.serializeToXdm());
		}

		if (this.timestamp != null) {
			map.put("timestamp", TimeUtils.getISO8601UTCDateWithMilliseconds(this.timestamp));
		}

		return map;
	}

	public static class TestXDMObject implements Property {

		public String innerKey;

		@Override
		public Map<String, Object> serializeToXdm() {
			Map<String, Object> map = new HashMap<>();

			if (this.innerKey != null) {
				map.put("innerKey", this.innerKey);
			}

			return map;
		}
	}
}
