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
class InspectionFlow(val linearId: UniqueIdentifier, val approved: Boolean) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        if (inputState.data.developer != ourIdentity) throw IllegalStateException("Developer must start this flow.")

        val jobState = if (approved) inputState.data.copy(status = JobStatus.ACCEPTED) else inputState.data.copy(status = JobStatus.REJECTED)
        val commandType = if (approved) JobContract.Commands.InspectAndAccept() else JobContract.Commands.InspectAndReject()
        val command = Command(commandType, listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(command)

        val signedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        return subFlow(FinalityFlow(signedTransaction))
    }
}