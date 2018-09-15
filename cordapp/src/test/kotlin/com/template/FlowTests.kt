package com.template

import com.template.flows.StartJobFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(listOf("com.template"))
    private val a = network.createNode()
    private val b = network.createNode()

    private val description = "Fit some windows."
    private val quantity = 100
    private val currency = "USD"
    private val amount = Amount(quantity.toLong() * 100, Currency.getInstance(currency))

    init {
        b.registerInitiatedFlow(AgreeJobFlowResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    fun agreeJob(): SignedTransaction {
        val flow = AgreeJobFlow(description, b.info.chooseIdentity(), quantity, currency, notaryToUse = network.defaultNotaryIdentity)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun startJob(linearId: UniqueIdentifier): SignedTransaction {
        val flow = StartJobFlow(linearId)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun finishJob(linearId: UniqueIdentifier): SignedTransaction {
        val flow = FinishJobFlow(linearId)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun inspectJob(linearId: UniqueIdentifier, isApproved: Boolean): SignedTransaction {
        val flow = AcceptOrRejectFlow(linearId, isApproved)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun payJob(linearId: UniqueIdentifier): SignedTransaction {
        val flow = PayFlow(linearId)
        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }



    @Test
    fun `golden path agree job flow`() {
        agreeJob()

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(description, jobState.description)
                assertEquals(JobStatus.UNSTARTED, jobState.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(amount, jobState.amount)
            }
        }
    }

    @Test
    fun `golden path start job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(description, jobState.description)
                assertEquals(JobStatus.STARTED, jobState.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(amount, jobState.amount)
            }
        }
    }

    @Test
    fun `golden path finish job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(description, jobState.description)
                assertEquals(JobStatus.COMPLETED, jobState.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(amount, jobState.amount)
            }
        }
    }

    @Test
    fun `golden path reject job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
        inspectJob(linearId, false)

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(description, jobState.description)
                assertEquals(JobStatus.REJECTED, jobState.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(amount, jobState.amount)
            }
        }
    }

    @Test
    fun `golden path accept job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
        inspectJob(linearId, true)

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(description, jobState.description)
                assertEquals(JobStatus.ACCEPTED, jobState.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(amount, jobState.amount)
            }
        }
    }

    @Test
    fun `golden path pay flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId)
        finishJob(linearId)
        inspectJob(linearId, true)
        payJob(linearId)

        // TODO: Add more flow tests elsewhere.
        a.transaction {
            val vaultStates = a.services.vaultService.queryBy<JobState>().states
            assertEquals(1, vaultStates.size)
        }
    }
}