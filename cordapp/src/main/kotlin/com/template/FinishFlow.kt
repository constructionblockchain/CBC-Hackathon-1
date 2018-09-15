package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class FinishJobFlow(val linearId: UniqueIdentifier, val milestoneIndex: Int) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        if (inputState.data.contractor != ourIdentity) throw IllegalStateException("Contractor must start this flow.")

        val updatedMilestones = inputState.data.milestones.toMutableList()
        updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.COMPLETED)

        val jobState = inputState.data.copy(milestones = updatedMilestones)

        val finishJobCommand = Command(
                JobContract.Commands.FinishJob(),
                listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(finishJobCommand)

        val signedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        return subFlow(FinalityFlow(signedTransaction))
    }
}