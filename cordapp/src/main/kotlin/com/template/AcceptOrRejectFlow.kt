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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AcceptOrRejectFlow(val linearId: UniqueIdentifier, val approved: Boolean, val milestoneIndex: Int) : FlowLogic<UniqueIdentifier>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        if (inputState.data.developer != ourIdentity) throw IllegalStateException("Developer must start this flow.")

        val jobState = if (approved) {
            val updatedMilestones = inputState.data.milestones.toMutableList()
            updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.ACCEPTED)
            inputState.data.copy(milestones = updatedMilestones)
        } else {
            val updatedMilestones = inputState.data.milestones.toMutableList()
            updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.STARTED)
            inputState.data.copy(milestones = updatedMilestones)
        }
        val commandType = if (approved) {
            JobContract.Commands.AcceptMilestone(milestoneIndex)
        } else {
            JobContract.Commands.RejectMilestone(milestoneIndex)
        }
        val command = Command(commandType, listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(command)

        val signedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        subFlow(FinalityFlow(signedTransaction))

        return jobState.linearId
    }
}