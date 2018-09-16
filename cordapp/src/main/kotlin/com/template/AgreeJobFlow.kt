package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Agree the creation of a [JobState] representing a job organised by a developer and carried out by a [contractor].
 * The job is split into a set of [milestones].
 *
 * Should be run by the developer.
 *
 * @param milestones the milestones involved in the job.
 * @param contractor the contractor carrying out the job.
 * @param notaryToUse the notary to assign the output state to.
 */
@InitiatingFlow
@StartableByRPC
class AgreeJobFlow(val milestones: List<Milestone>,
                   val contractor: Party,
                   val notaryToUse: Party) : FlowLogic<UniqueIdentifier>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        val jobState = JobState(ourIdentity, contractor, milestones)

        val agreeJobCommand = Command(
                JobContract.Commands.AgreeJob(),
                listOf(ourIdentity.owningKey, contractor.owningKey))

        val transactionBuilder = TransactionBuilder(notaryToUse)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(agreeJobCommand)

        transactionBuilder.verify(serviceHub)

        val partSignedTransaction =
                serviceHub.signInitialTransaction(transactionBuilder)

        val contractorSession = initiateFlow(contractor)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(
                partSignedTransaction, listOf(contractorSession)))

        subFlow(FinalityFlow(fullySignedTransaction))

        return jobState.linearId
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



@InitiatingFlow
@StartableByRPC
class InitiatorFlow(val counterparty: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        (0..99).forEach {
            subFlow(SendMessageFlow(counterparty))
        }
    }
}

@InitiatingFlow
class SendMessageFlow(val counterparty: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val counterpartySession = initiateFlow(counterparty)
        counterpartySession.send("My payload.")
    }
}

@InitiatedBy(SendMessageFlow::class)
class ResponderFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        counterpartySession.receive<String>()
    }
}