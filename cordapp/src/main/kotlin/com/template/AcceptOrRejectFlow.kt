package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

/**
 * Change the status of a [Milestone] in a [JobState] from [MilestoneStatus.COMPLETED] to either:
 *  * [MilestoneStatus.STARTED] if the [Milestone] is considered incomplete and requires additional work
 *  * [MilestoneStatus.ACCEPTED] if the [Milestone] is considered complete
 *
 * Should be run by the developer, who performs the inspection.
 *
 * @param linearId the [JobState] to update.
 * @param milestoneIndex the index of the [Milestone] to be updated in the [JobState].
 */
@InitiatingFlow
@StartableByRPC
class AcceptOrRejectFlow(val linearId: UniqueIdentifier, val approved: Boolean, val milestoneIndex: Int) : FlowLogic<UniqueIdentifier>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(queryCriteria)
        val inputStateAndRef = results.states.singleOrNull()
                ?: throw FlowException("There is no JobState with linear ID $linearId")
        val inputState = inputStateAndRef.state.data

        if (inputState.developer != ourIdentity) throw IllegalStateException("The developer must start this flow.")

        val jobState = if (approved) {
            val updatedMilestones = inputState.milestones.toMutableList()
            updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.ACCEPTED)
            inputState.copy(milestones = updatedMilestones)
        } else {
            val updatedMilestones = inputState.milestones.toMutableList()
            updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.STARTED)
            inputState.copy(milestones = updatedMilestones)
        }
        val commandType = if (approved) {
            JobContract.Commands.AcceptMilestone(milestoneIndex)
        } else {
            JobContract.Commands.RejectMilestone(milestoneIndex)
        }
        val command = Command(commandType, listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(command)

        transactionBuilder.verify(serviceHub)

        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        subFlow(FinalityFlow(signedTransaction))

        return jobState.linearId
    }
}