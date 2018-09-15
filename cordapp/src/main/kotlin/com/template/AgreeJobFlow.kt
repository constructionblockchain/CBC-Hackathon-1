package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.DOLLARS
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AgreeJobFlow(val description: String,
                   val contractor: Party,
                   val quantity: Int,
                   val currency: String,
                   val notaryToUse: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val amount = Amount(quantity.toLong() * 100, Currency.getInstance(currency))
        val jobState = JobState(description, JobStatus.UNSTARTED,
                ourIdentity, contractor, amount)

        val agreeJobCommand = Command(
                JobContract.Commands.AgreeJob(),
                listOf(ourIdentity.owningKey, contractor.owningKey))

        val transactionBuilder = TransactionBuilder(notaryToUse)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(agreeJobCommand)

        val partSignedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        val contractorSession = initiateFlow(contractor)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(
                partSignedTransaction, listOf(contractorSession)))

        return subFlow(FinalityFlow(fullySignedTransaction))
    }
}

@InitiatedBy(AgreeJobFlow::class)
class AgreeJobFlowResponder(val developerSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        class OurSignTransactionFlow : SignTransactionFlow(developerSession) {
            override fun checkTransaction(stx: SignedTransaction) {

            }
        }

        subFlow(OurSignTransactionFlow())
    }
}