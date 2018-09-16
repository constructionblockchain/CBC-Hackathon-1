package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

/**
 * Change the status of a [Milestone] in a [JobState] from [MilestoneStatus.STARTED] to [MilestoneStatus.COMPLETED].
 *
 * Should be run by the contractor.
 *
 * @param linearId the [JobState] to update.
 * @param milestoneIndex the index of the [Milestone] to be updated in the [JobState].
 */
@InitiatingFlow
@StartableByRPC
class CompleteMilestoneFlow(val linearId: UniqueIdentifier, val milestoneIndex: Int) : FlowLogic<UniqueIdentifier>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        if (inputState.data.contractor != ourIdentity) throw IllegalStateException("Contractor must start this flow.")

        val updatedMilestones = inputState.data.milestones.toMutableList()
        updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.COMPLETED)

        val jobState = inputState.data.copy(milestones = updatedMilestones)

        val finishJobCommand = Command(
                JobContract.Commands.FinishMilestone(milestoneIndex),
                listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(finishJobCommand)

        val signedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        subFlow(FinalityFlow(signedTransaction))

        return jobState.linearId
    }
}