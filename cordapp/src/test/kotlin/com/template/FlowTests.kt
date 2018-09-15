package com.template

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
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(listOf("com.template", "net.corda.finance.contracts.asset"))
    private val a = network.createNode()
    private val b = network.createNode()

    private val milestoneNames = listOf("Fit some windows.", "Build some walls.", "Add a doorbell.")
    private val milestoneAmounts = listOf(100.DOLLARS, 500.DOLLARS, 50.DOLLARS)

    private val milestones = milestoneNames.zip(milestoneAmounts).map { (name, amount) ->
        Milestone(name, amount)
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

                assertEquals(milestones.subList(1, milestones.size), milestonesState.subList(1, milestones.size))

            }
        }
    }

    @Test
    fun `golden path finish job flow`() {
        val signedTransaction = agreeJob()
        val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
        val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
        startJob(linearId, milestoneIndex)
        finishJob(linearId, milestoneIndex)

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
                assertEquals(MilestoneStatus.COMPLETED, milestoneStarted.status)

                assertEquals(milestones.subList(1, milestones.size), milestonesState.subList(1, milestones.size))

            }
        }

        @Test
        fun `golden path reject job flow`() {
            val signedTransaction = agreeJob()
            val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
            val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
            startJob(linearId, 0)
            finishJob(linearId, 0)
            inspectJob(linearId, false, 0)

            listOf(a, b).forEach { node ->
                node.transaction {
                    val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                    assertEquals(1, jobStatesAndRefs.size)

                    val jobState = jobStatesAndRefs.single().state.data

                    assertEquals(a.info.chooseIdentity(), jobState.developer)
                    assertEquals(b.info.chooseIdentity(), jobState.contractor)
                    assertEquals(MilestoneStatus.STARTED, jobState.milestones[0].status)
                    assertEquals(milestones[0].description, jobState.milestones[0].description)
                    assertEquals(milestones[0].amount, jobState.milestones[0].amount)
                    assertEquals(milestones.subList(1, milestones.size), jobState.milestones.subList(1, milestones.size))
                }
            }
        }

        @Test
        fun `golden path accept first job flow`() {
            val signedTransaction = agreeJob()
            val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
            val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
            startJob(linearId, 0)
            finishJob(linearId, 0)
            inspectJob(linearId, true, 0)

            listOf(a, b).forEach { node ->
                node.transaction {
                    val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                    assertEquals(1, jobStatesAndRefs.size)

                    val jobState = jobStatesAndRefs.single().state.data
                    assertEquals(a.info.chooseIdentity(), jobState.developer)
                    assertEquals(b.info.chooseIdentity(), jobState.contractor)

                    assertEquals(MilestoneStatus.ACCEPTED, jobState.milestones[0].status)
                    assertEquals(milestones[0].description, jobState.milestones[0].description)
                    assertEquals(milestones[0].amount, jobState.milestones[0].amount)

                    assertEquals(milestones.subList(1, milestones.size), jobState.milestones.subList(1, milestones.size))
                }
            }
        }

        @Test
        fun `golden path pay first job flow`() {
            val signedTransaction = agreeJob()
            val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
            val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
            startJob(linearId, 0)
            finishJob(linearId, 0)
            inspectJob(linearId, true, 0)
            issueCash()
            payJob(linearId, 0)

            listOf(a, b).forEach { node ->
                node.transaction {
                    val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                    assertEquals(1, jobStatesAndRefs.size)

                    val jobState = jobStatesAndRefs.single().state.data
                    assertEquals(a.info.chooseIdentity(), jobState.developer)
                    assertEquals(b.info.chooseIdentity(), jobState.contractor)

                    assertEquals(MilestoneStatus.PAID, jobState.milestones[0].status)
                    assertEquals(milestones[0].description, jobState.milestones[0].description)
                    assertEquals(milestones[0].amount, jobState.milestones[0].amount)

                    assertEquals(milestones.subList(1, milestones.size), jobState.milestones.subList(1, milestones.size))
                }
            }

            a.transaction {
                val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
                val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
                assertEquals(55000, balance)
            }

            b.transaction {
                val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
                val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
                assertEquals(10000, balance)
            }
        }

        @Test
        fun `golden path pay all jobs flow`() {
            val signedTransaction = agreeJob()
            val ledgerTransaction = signedTransaction.toLedgerTransaction(a.services)
            val linearId = ledgerTransaction.outputsOfType<JobState>().single().linearId
            (0..2).forEach { index -> startJob(linearId, index) }
            (0..2).forEach { index -> finishJob(linearId, index) }
            (0..2).forEach { index -> inspectJob(linearId, true, index) }
            issueCash()
            (0..2).forEach { index -> payJob(linearId, index) }

            listOf(a, b).forEach { node ->
                node.transaction {
                    val jobStatesAndRefs = node.services.vaultService.queryBy<JobState>().states
                    assertEquals(1, jobStatesAndRefs.size)

                    val jobState = jobStatesAndRefs.single().state.data
                    assertEquals(a.info.chooseIdentity(), jobState.developer)
                    assertEquals(b.info.chooseIdentity(), jobState.contractor)

                    jobState.milestones.forEachIndexed { index, milestone ->
                        assertEquals(MilestoneStatus.PAID, milestone.status)
                        assertEquals(milestones[index].description, milestone.description)
                        assertEquals(milestones[index].amount, milestone.amount)
                    }
                }
            }

            a.transaction {
                val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
                val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
                assertEquals(0, balance)
            }

            b.transaction {
                val cashStatesAndRefs = a.services.vaultService.queryBy<Cash.State>().states
                val balance = cashStatesAndRefs.sumBy { it.state.data.amount.quantity.toInt() }
                assertEquals(65000, balance)
            }
        }
    }
}
