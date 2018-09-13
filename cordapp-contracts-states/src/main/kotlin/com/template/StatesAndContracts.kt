package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *****************
// * Contract Code *
// *****************
class TemplateContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val ID = "com.template.TemplateContract"
    }
    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Action : Commands
    }
}

// *********
// * State *
// *********
data class JobState(val jobs: List<Job>,
                    val developer: Party,
                    val contractor: Party) : ContractState {

    override val participants = listOf(developer, contractor)

    val status = when {
        jobs.all { it.status == JobStatus.COMPLETED } -> JobStatus.COMPLETED
        jobs.all { it.status == JobStatus.UNSTARTED } -> JobStatus.UNSTARTED
        else -> JobStatus.STARTED
    }
}

data class Job(val description: String, val status: JobStatus)

enum class JobStatus {
    UNSTARTED, STARTED, COMPLETED
}