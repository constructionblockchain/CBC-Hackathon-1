package com.template

import com.template.flows.StartJobFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

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

    fun agreeJob(): SignedTransaction {
        val flow = AgreeJobFlow(
                description = "Fit some windows.",
                contractor = b.info.legalIdentities[0],
                notaryToUse = network.defaultNotaryIdentity)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun startJob(linearId: UniqueIdentifier): SignedTransaction {
        val flow = StartJobFlow(linearId = linearId)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun finishJob(linearId: UniqueIdentifier): SignedTransaction {
        val flow = FinishJobFlow(linearId = linearId)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun inspectJob(linearId: UniqueIdentifier, approved: Boolean): SignedTransaction {
        val flow = AcceptOrRejectFlow(linearId = linearId, approved = approved)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    @Test
    fun `golden path agree job flow`() {
        agreeJob()
    }

    @Test
    fun `golden path start job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
    }

    @Test
    fun `golden path finish job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
    }

    @Test
    fun `golden path reject job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
        inspectJob(linearId, false)
    }

    @Test
    fun `golden path accept job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
        inspectJob(linearId, true)

        // TODO: Add more flow tests elsewhere.
        a.transaction {
            val vaultStates = a.services.vaultService.queryBy<JobState>().states
            assertEquals(1, vaultStates.size)
        }
    }
}