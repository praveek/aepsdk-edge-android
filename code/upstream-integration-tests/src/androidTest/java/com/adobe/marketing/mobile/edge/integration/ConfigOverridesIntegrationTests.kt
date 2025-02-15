/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.integration.util.TestSetupHelper
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.TestableNetworkRequest
import com.adobe.marketing.mobile.util.JSONAsserts
import com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch
import com.adobe.marketing.mobile.util.MonitorExtension
import com.adobe.marketing.mobile.util.RealNetworkService
import com.adobe.marketing.mobile.util.TestConstants
import com.adobe.marketing.mobile.util.TestHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

/**
 * Performs validation on integration with the Edge Network upstream service with configOverrides.
 */
@RunWith(AndroidJUnit4::class)
class ConfigOverridesIntegrationTests {
    private val realNetworkService = RealNetworkService()
    private val edgeLocationHint: String? = TestSetupHelper.defaultLocationHint
    private val tagsMobilePropertyId: String = TestSetupHelper.defaultTagsMobilePropertyId
    private val VALID_DATASTREAM_ID_OVERRIDE = "15d7bce0-3e2c-447b-bbda-129c57c60820"
    private val VALID_DATASET_ID_CONFIGURED_AS_OVERRIDE = "6515e1dbfeb3b128d19bb1e4"
    private val VALID_DATASET_ID_NOT_CONFIGURED_AS_OVERRIDE = "6515e1f6296d1e28d3209b9f"
    private val VALID_RSID_CONFIGURED_AS_OVERRIDE = "mobile5.e2e.rsid2"
    private val VALID_RSID_NOT_CONFIGURED_AS_OVERRIDE = "mobile5e2e.rsid3"

    @JvmField
    @Rule
    var rule: RuleChain = RuleChain.outerRule(TestHelper.LogOnErrorRule())
        .around(TestHelper.SetupCoreRule())

    @Before
    @Throws(Exception::class)
    fun setup() {
        println("Environment var - Edge Network tags mobile property ID: $tagsMobilePropertyId")
        println("Environment var - Edge Network location hint: $edgeLocationHint")
        ServiceProvider.getInstance().networkService = realNetworkService

        // Set the tags mobile property ID for a specific Edge Network environment
        MobileCore.configureWithAppID(tagsMobilePropertyId)

        // Use expectation to guarantee Configuration shared state availability
        // Required primarily by `createInteractURL`
        TestHelper.setExpectationEvent(
            TestConstants.EventType.CONFIGURATION,
            TestConstants.EventSource.RESPONSE_CONTENT,
            1
        )
        val latch = CountDownLatch(1)
        MobileCore.registerExtensions(
            listOf(
                Edge.EXTENSION,
                Identity.EXTENSION,
                MonitorExtension.EXTENSION
            )
        ) {
            latch.countDown()
        }
        latch.await()
        TestHelper.assertExpectedEvents(true)

        // Set Edge location hint if one is set for the test suite
        TestSetupHelper.setInitialLocationHint(edgeLocationHint)

        resetTestExpectations()
    }

    @After
    fun tearDown() {
        realNetworkService.reset()
        TestHelper.resetCoreHelper()
    }

    @Test
    fun testSendEvent_withValidDatastreamIdOverride_receivesExpectedNetworkResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamIdOverride(VALID_DATASTREAM_ID_OVERRIDE)
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    @Test
    fun testSendEvent_withInvalidDatastreamIdOverride_receivesErrorResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.ERROR_RESPONSE_CONTENT, 1)

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamIdOverride("DummyDatastreamID")
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(400, matchingResponses?.firstOrNull()?.responseCode)

        val expectedErrorJSON = """
        {
            "status": 400,
            "title": "Invalid datastream ID",
            "type": "https://ns.adobe.com/aep/errors/EXEG-0003-400"
        }
        """

        val errorEvents = TestSetupHelper.getEdgeResponseErrors()

        assertEquals(1, errorEvents.size)

        val errorEvent = errorEvents.first()
        assertExactMatch(expectedErrorJSON, errorEvent.eventData)
    }

    @Test
    fun testSendEvent_withValidDatastreamConfigOverride_receivesExpectedNetworkResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)

        val configOverrides = mapOf(
            "com_adobe_experience_platform" to mapOf(
                "datasets" to mapOf(
                    "event" to mapOf(
                        "datasetId" to VALID_DATASET_ID_CONFIGURED_AS_OVERRIDE
                    )
                )
            ),
            "com_adobe_analytics" to mapOf(
                "reportSuites" to listOf(
                    VALID_RSID_CONFIGURED_AS_OVERRIDE
                )
            )
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamConfigOverride(configOverrides)
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    // TODO: Enable after PDCL-11131 issue is fixed
    @Ignore
    @Test
    fun testSendEvent_withInvalidDatastreamConfigOverride_dummyValues_receivesErrorResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.ERROR_RESPONSE_CONTENT, 1)

        val configOverridesWithDummyValues = mapOf(
            "com_adobe_experience_platform" to mapOf(
                "datasets" to mapOf(
                    "event" to mapOf(
                        "datasetId" to "DummyDataset"
                    )
                )
            ),
            "com_adobe_analytics" to mapOf(
                "reportSuites" to listOf(
                    "DummyRSID1",
                    "DummyRSID2"
                )
            )
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamConfigOverride(configOverridesWithDummyValues)
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(400, matchingResponses?.firstOrNull()?.responseCode)

        val expectedErrorJSON = """
        {
            "status": 400,
            "title": "Invalid request",
            "type": "https://ns.adobe.com/aep/errors/EXEG-0113-400"
        }
        """

        val errorEvents = TestSetupHelper.getEdgeResponseErrors()

        assertEquals(1, errorEvents.size)

        val errorEvent = errorEvents.first()
        JSONAsserts.assertEquals(expectedErrorJSON, errorEvent.eventData)
    }

    // TODO: Enable after PDCL-11131 issue is fixed
    @Ignore
    @Test
    fun testSendEvent_withInvalidDatastreamConfigOverride_notConfiguredValues_receivesErrorResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.ERROR_RESPONSE_CONTENT, 1)

        val configOverridesWithUnconfiguredValues = mapOf(
            "com_adobe_experience_platform" to mapOf(
                "datasets" to mapOf(
                    "event" to mapOf(
                        "datasetId" to VALID_DATASET_ID_NOT_CONFIGURED_AS_OVERRIDE
                    )
                )
            ),
            "com_adobe_analytics" to mapOf(
                "reportSuites" to listOf(
                    VALID_RSID_NOT_CONFIGURED_AS_OVERRIDE
                )
            )
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamConfigOverride(configOverridesWithUnconfiguredValues)
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(400, matchingResponses?.firstOrNull()?.responseCode)

        val expectedErrorJSON = """
        {
            "status": 400,
            "title": "Invalid request",
            "type": "https://ns.adobe.com/aep/errors/EXEG-0113-400"
        }
        """

        val errorEvents = TestSetupHelper.getEdgeResponseErrors()

        assertEquals(1, errorEvents.size)

        val errorEvent = errorEvents.first()
        JSONAsserts.assertEquals(expectedErrorJSON, errorEvent.eventData)
    }

    // TODO: Enable after PDCL-11131 issue is fixed
    @Ignore
    @Test
    fun testSendEvent_withInvalidDatastreamConfigOverride_validAndDummyValues_receivesExpectedNetworkResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.ERROR_RESPONSE_CONTENT, 1)

        val configOverrides = mapOf(
            "com_adobe_experience_platform" to mapOf(
                "datasets" to mapOf(
                    "event" to mapOf(
                        "datasetId" to VALID_DATASET_ID_CONFIGURED_AS_OVERRIDE
                    )
                )
            ),
            "com_adobe_analytics" to mapOf(
                "reportSuites" to listOf(
                    VALID_RSID_CONFIGURED_AS_OVERRIDE,
                    "DummyRSID2"
                )
            )
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .setDatastreamConfigOverride(configOverrides)
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(400, matchingResponses?.firstOrNull()?.responseCode)

        val expectedErrorJSON = """
        {
            "status": 400,
            "title": "Invalid request",
            "type": "https://ns.adobe.com/aep/errors/EXEG-0113-400"
        }
        """

        val errorEvents = TestSetupHelper.getEdgeResponseErrors()

        assertEquals(1, errorEvents.size)

        val errorEvent = errorEvents.first()
        JSONAsserts.assertEquals(expectedErrorJSON, errorEvent.eventData)
    }

    /**
     * Resets all test helper expectations and recorded data
     */
    private fun resetTestExpectations() {
        realNetworkService.reset()
        TestHelper.resetTestExpectations()
    }
}
