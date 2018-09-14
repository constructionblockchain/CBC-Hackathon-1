package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.JobContract
import com.template.JobState
import com.template.JobStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

@InitiatingFlow
@StartableByRPC
class StartJobFlow ( val linearId : UniqueIdentifier ) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId),
                status = Vault.StateStatus.UNCONSUMED)
        val vaultPage = serviceHub.vaultService.queryBy<JobState>(queryCriteria)
        val inputStateAndRef = vaultPage.states.singleOrNull()
                ?: throw FlowException("there is no Job with linear id $linearId")
        val inputState = inputStateAndRef.state.data
        if (inputState.contractor != ourIdentity) throw IllegalStateException("Contractor must start this flow.")
        val outputState = inputState.copy(status = JobStatus.STARTED)
        val signers = inputState.participants.map { it.owningKey }
        val command = Command(JobContract.Commands.StartJob(), signers)

        val transactionBuilder = TransactionBuilder(notary = inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState, JobContract.ID)
                .addCommand(command)


        transactionBuilder.verify(serviceHub)
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (outputState.participants-ourIdentity).map {initiateFlow(it)}.toSet()
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, sessions))
        return subFlow(FinalityFlow(fullySignedTransaction))

    }

}
