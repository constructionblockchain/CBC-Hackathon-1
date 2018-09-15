package com.template

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.finance.DOLLARS
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(listOf("com.template", "net.corda.finance.contracts.asset"))
    private val a = network.createNode()
    private val b = network.createNode()

    private val milestoneNames = listOf("Fit some windows.", "Build some walls.", "Add a doorbell.")
    private val milestoneAmounts = listOf(100.DOLLARS, 500.DOLLARS, 50.DOLLARS)

    private val milestones = milestoneNames.zip(milestoneAmounts).map {
        (name, amount) -> Milestone(name, amount)
    }

    private val milestoneIndex = 0

    init {
        b.registerInitiatedFlow(AgreeJobFlowResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    fun agreeJob(): SignedTransaction {
        val flow = AgreeJobFlow(milestones, b.info.chooseIdentity(), notaryToUse = network.defaultNotaryIdentity)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun startJob(linearId: UniqueIdentifier, milestoneIndex: Int): SignedTransaction {
        val flow = StartMilestoneFlow(linearId, milestoneIndex)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun finishJob(linearId: UniqueIdentifier, milestoneIndex: Int): SignedTransaction {
        val flow = FinishJobFlow(linearId, milestoneIndex)

        val resultFuture = b.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun inspectJob(linearId: UniqueIdentifier, isApproved: Boolean, milestoneIndex: Int): SignedTransaction {
        val flow = AcceptOrRejectFlow(linearId, isApproved, milestoneIndex)

        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    fun issueCash() {
        val flow = IssueCashFlow(650.DOLLARS, notaryToUse = network.defaultNotaryIdentity)
        val resultFuture = a.startFlow(flow)
        network.runNetwork()
        resultFuture.get()
    }

    fun payJob(linearId: UniqueIdentifier, milestoneIndex: Int): SignedTransaction {
        val flow = PayFlow(linearId, milestoneIndex)
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
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
                assertEquals(milestones, jobState.milestones)
            }
        }
    }

    @Test
    fun `golden path start job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId, milestoneIndex)

        listOf(a, b).forEach { node ->
            node.transaction {
                val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                assertEquals(1, jobStatesAndRefs.size)

                val jobState = jobStatesAndRefs.single().state.data
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)


                val milestonesState = jobState.milestones
                val milestoneStarted = milestonesState[milestoneIndex]
                assertEquals(milestoneNames, milestonesState.map { it.description })
                assertEquals(milestoneAmounts, milestonesState.map { it.amount })
                assertEquals(MilestoneStatus.STARTED, milestoneStarted.status)
                assertEquals(a.info.chooseIdentity(), jobState.developer)
                assertEquals(b.info.chooseIdentity(), jobState.contractor)
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
                assertEquals(MilestoneStatus.COMPLETED, jobState.status)
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
                assertEquals(milestoneNames, jobState.milestones.toList())
                assertEquals(MilestoneStatus.STARTED, jobState.status)
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
                assertEquals(MilestoneStatus.ACCEPTED, jobState.status)
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
        issueCash()
        payJob(linearId)

        a.transaction {
            val jobStatesAndRefs = a.services.vaultService.queryBy<JobState>().states
            assertEquals(0, jobStatesAndRefs.size)

            val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
            val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
            assertEquals(0, balance)
        }

        b.transaction {
            val jobStatesAndRefs = b.services.vaultService.queryBy<JobState>().states
            assertEquals(0, jobStatesAndRefs.size)

            val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
            val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
            assertEquals(10000, balance)
        }
    }
}