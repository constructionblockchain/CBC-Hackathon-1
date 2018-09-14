package com.template

import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(listOf("com.template"))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        b.registerInitiatedFlow(AgreeJobFlowResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `golden path agree job flow`() {
        val flow = AgreeJobFlow(
                description = "Fit some windows.",
                contractor = b.info.legalIdentities[0],
                notaryToUse = network.defaultNotaryIdentity)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        val result = resultFuture.get()
        println(result)
    }
}