/*
  Copyright 2019 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.xdm;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FormattersTests {

	@Test
	public void serializeFromList_singlePropertyList_returnsMapWithSingleProperty() {
		List<TestPropertyA> propertyList = new ArrayList<>();
		propertyList.add(new TestPropertyA("single"));

		List<Map<String, Object>> result = Formatters.serializeFromList(propertyList);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("A", result.get(0).get("id"));
		assertEquals("single", result.get(0).get("key"));
	}

	@Test
	public void serializeFromList_multiplePropertyList_returnsMapWithMultipleProperties() {
		List<Property> propertyList = new ArrayList<>();
		propertyList.add(new TestPropertyA("one"));
		propertyList.add(new TestPropertyB("two"));

		List<Map<String, Object>> result = Formatters.serializeFromList(propertyList);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("one", result.get(0).get("key"));
		assertEquals("A", result.get(0).get("id"));
		assertEquals("two", result.get(1).get("key"));
		assertEquals("B", result.get(1).get("id"));
	}

	@Test
	public void serializeFromList_listWithNullProperties_returnsMapWhichIgnoresNullProperties() {
		List<Property> propertyList = new ArrayList<>();
		propertyList.add(new TestPropertyA("one"));
		propertyList.add(null);
		propertyList.add(new TestPropertyB("two"));

		List<Map<String, Object>> result = Formatters.serializeFromList(propertyList);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("one", result.get(0).get("key"));
		assertEquals("A", result.get(0).get("id"));
		assertEquals("two", result.get(1).get("key"));
		assertEquals("B", result.get(1).get("id"));
	}

	@Test
	public void serializeFromList_onNull_returnsEmptyMap() {
		List<Map<String, Object>> result = Formatters.serializeFromList(null);
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void serializeFromList_onEmpty_returnsEmptyMap() {
		List<Map<String, Object>> result = Formatters.serializeFromList(new ArrayList<>());
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	private static class TestPropertyA implements Property {

		private final String value;

		TestPropertyA(final String value) {
			this.value = value;
		}

		@Override
		public Map<String, Object> serializeToXdm() {
			Map<String, Object> map = new HashMap<>();
			map.put("id", "A");
			map.put("key", this.value);
			return map;
		}
	}

	private static class TestPropertyB implements Property {

		private final String value;

		TestPropertyB(final String value) {
			this.value = value;
		}

		@Override
		public Map<String, Object> serializeToXdm() {
			Map<String, Object> map = new HashMap<>();
			map.put("id", "B");
			map.put("key", this.value);
			return map;
		}
	}
}
