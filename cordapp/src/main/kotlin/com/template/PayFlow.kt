package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash

@InitiatingFlow
@StartableByRPC
class PayFlow(private val linearId: UniqueIdentifier, val milestoneIndex: Int) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<JobState>(criteria)
        val inputStateAndRef = results.states.single()
        val inputState = inputStateAndRef.state

        check(inputState.data.developer == ourIdentity) {
            throw FlowException("Payment flow must be initiated by the developer.")
        }

        val milestoneToUpdate = inputState.data.milestones[milestoneIndex]
        val updatedMilestones = inputState.data.milestones.toMutableList()
        updatedMilestones[milestoneIndex] = milestoneToUpdate.copy(status = MilestoneStatus.PAID)

        val jobState = inputState.data.copy(milestones = updatedMilestones)

        val payCommand = Command(
                JobContract.Commands.Pay(),
                ourIdentity.owningKey
        )

        val transactionBuilder = TransactionBuilder(inputState.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(jobState, JobContract.ID)
                .addCommand(payCommand)

        val contractor = inputState.data.contractor
        val (_, cashSigningKeys) = Cash.generateSpend(serviceHub, transactionBuilder, milestoneToUpdate.amount, contractor)

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, cashSigningKeys + ourIdentity.owningKey)

        return subFlow(FinalityFlow(signedTransaction))
    }
}