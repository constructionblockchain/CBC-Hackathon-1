package com.template

import co.paralleluniverse.fibers.Suspendable
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
class StartMilestoneFlow (val linearId : UniqueIdentifier, val milestoneIndex: Int) : FlowLogic<UniqueIdentifier>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId),
                status = Vault.StateStatus.UNCONSUMED)
        val vaultPage = serviceHub.vaultService.queryBy<JobState>(queryCriteria)
        val inputStateAndRef = vaultPage.states.singleOrNull()
                ?: throw FlowException("there is no Job with linear id $linearId")
        val inputState = inputStateAndRef.state.data

        if (inputState.contractor != ourIdentity) throw IllegalStateException("Contractor must start this flow.")

        val updatedMilestones = inputState.milestones.toMutableList()
        updatedMilestones[milestoneIndex] = updatedMilestones[milestoneIndex].copy(status = MilestoneStatus.STARTED)

        val outputState = inputState.copy(milestones = updatedMilestones)
        val signers = inputState.participants.map { it.owningKey }
        val command = Command(JobContract.Commands.StartMilestone(milestoneIndex), signers)

        val transactionBuilder = TransactionBuilder(notary = inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState, JobContract.ID)
                .addCommand(command)

        transactionBuilder.verify(serviceHub)
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = (outputState.participants - ourIdentity).map {initiateFlow(it)}.toSet()
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, sessions))

        subFlow(FinalityFlow(fullySignedTransaction))

        return outputState.linearId
    }
}

@InitiatedBy(StartMilestoneFlow::class)
class StartJobFlowResponder(val contractorSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        class OurSignTransactionFlow : SignTransactionFlow(contractorSession) {
            override fun checkTransaction(stx: SignedTransaction) {

            }
        }

        subFlow(OurSignTransactionFlow())
    }
}
