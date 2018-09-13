package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *****************
// * Contract Code *
// *****************
class JobContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val ID = "com.template.TemplateContract"
    }
    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val jobInputs = tx.inputsOfType<JobState>()
        val jobOutputs = tx.outputsOfType<JobState>()
        val jobCommand = tx.commandsOfType<JobContract.Commands>().single()

        when(jobCommand.value) {
            is Commands.AgreeJob -> requireThat {
                "no inputs should be consumed" using (jobInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (jobOutputs.size == 1)

                val jobOutput = jobOutputs.single()
                "the developer should be different to the contractor" using (jobOutput.contractor != jobOutput.developer)
                "the status should be set as unstarted" using (jobOutput.status == JobStatus.UNSTARTED)

                "the developer and contractor are required signer" using
                        (jobCommand.signers.containsAll(listOf(jobOutput.contractor.owningKey, jobOutput.developer.owningKey)))
            }

            is Commands.StartJob -> requireThat {
                "one input should be consumed" using (jobInputs.size == 1)
                "one output should bbe produced" using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                "the status should be set to started" using (jobOutput.status == JobStatus.STARTED)
                "the previous status should not be STARTED" using (jobInput.status != JobStatus.STARTED)
                "only the job status should change" using (jobOutput == jobInput.copy(status = JobStatus.STARTED))
                "the developer and contractor are required signers" using
                        (jobCommand.signers.containsAll(listOf(jobOutput.contractor.owningKey, jobOutput.developer.owningKey)))
            }

            is Commands.FinishJob -> requireThat {
                "one input should be produced" using (jobInputs.size == 1)
                "one output should be produced" using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()

                "the input status must be set as started" using (jobInputs.single().status == JobStatus.STARTED)
                "the output status should be set as finished" using (jobOutputs.single().status == JobStatus.COMPLETED)
                "only the status must change" using (jobInput.copy(status = JobStatus.COMPLETED) == jobOutput)
                "the update must be signed by the contractor of the " using (jobOutputs.single().contractor == jobInputs.single().contractor)
                "the contractor should be signer" using (jobCommand.signers.contains(jobOutputs.single().contractor.owningKey))
                
            }

            is Commands.ProposeForInspection -> requireThat {

            }

            is Commands.InspectAndReject -> requireThat {
                // This insures we only have one input and one output
                val jobOutput = jobOutputs.single()
                val jobInput = jobInputs.single()

                "Only status should have changed" using (jobOutput.contractor == jobInput.contractor
                        && jobOutput.developer == jobInput.developer
                        && jobOutput.description == jobInput.description)
                "Status should show rejected" using (jobOutput.status == JobStatus.REJECTED)
                "Job must have been previously started" using (jobInput.status == JobStatus.STARTED)

                "Developer should be a signer" using (jobCommand.signers.contains(jobOutput.developer.owningKey))
            }

            is Commands.InspectAndAccept -> requireThat {

            }

            is Commands.Pay -> requireThat {

            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class AgreeJob : Commands
        // TODO - allow contractor to reject job
        class StartJob : Commands // TODO - Hristo
        class FinishJob : Commands // TODO - Sebastian
        class ProposeForInspection : Commands // TODO - Sven
        class InspectAndReject : Commands // TODO - Cais
        class InspectAndAccept : Commands // TODO - Ayman
        class Pay : Commands

        // TODO - in flow think about consuming other legal documents such as tender etc when proposing a job
    }
}

// *********
// * State *
// *********
// TODO - allow for lists of subjobs
// TODO - allow for percentage completion and payment
// TODO - map descriptions to BIM XML
data class JobState(val description: String,
                    val status: JobStatus,
                    val developer: Party,
                    val contractor: Party) : ContractState {

    override val participants = listOf(developer, contractor)
}

enum class JobStatus {
    UNSTARTED, STARTED, COMPLETED, REJECTED
}
