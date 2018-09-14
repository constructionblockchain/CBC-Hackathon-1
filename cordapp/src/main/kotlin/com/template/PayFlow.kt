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
class PayFlow(val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        if (inputState.data.developer != ourIdentity) throw IllegalStateException("Developer must start this flow.")

        val jobState = inputState.data.copy(status = JobStatus.COMPLETED)

        val payJobCommand = Command(
                JobContract.Commands.FinishJob(),
                listOf(ourIdentity.owningKey))

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(payJobCommand)

        val signedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        return subFlow(FinalityFlow(signedTransaction))
    }
}

@InitiatedBy(PayFlow::class)
class PayFlowResponder(val developerSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        class OurSignTransactionFlow : SignTransactionFlow(developerSession) {
            override fun checkTransaction(stx: SignedTransaction) {

            }
        }

        subFlow(OurSignTransactionFlow())
    }

}