package com.template

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction

// *****************
// * Contract Code *
// *****************
class JobContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val ID = "com.template.JobContract"
    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val jobInputs = tx.inputsOfType<JobState>()
        val jobOutputs = tx.outputsOfType<JobState>()
        val jobCommand = tx.commandsOfType<JobContract.Commands>().single()

        when (jobCommand.value) {
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
                "the input status should be UNSTARTED" using (jobInput.status == JobStatus.UNSTARTED)
                "the output status should be STARTED" using (jobOutput.status == JobStatus.STARTED)
                "only the job status should change" using (jobOutput == jobInput.copy(status = JobStatus.STARTED))
                "the developer and contractor are required signers" using
                        (jobCommand.signers.containsAll(listOf(jobOutput.contractor.owningKey, jobOutput.developer.owningKey)))
            }

            is Commands.FinishJob -> requireThat {
                "one input should be consumed" using (jobInputs.size == 1)
                "one output should be produced" using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()

                "the input status must be set as started" using (jobInputs.single().status == JobStatus.STARTED)
                "the output status should be set as finished" using (jobOutputs.single().status == JobStatus.COMPLETED)
                "only the status must change" using (jobInput.copy(status = JobStatus.COMPLETED) == jobOutput)

                "the contractor should be signer" using (jobCommand.signers.contains(jobOutputs.single().contractor.owningKey))
            }

            is Commands.InspectAndReject -> requireThat {
                "one input should be consumed" using (jobInputs.size == 1)
                "one output should be produced" using (jobOutputs.size == 1)

                val jobOutput = jobOutputs.single()
                val jobInput = jobInputs.single()

                "the input status must be set as completed" using (jobInputs.single().status == JobStatus.COMPLETED)
                "the output status should be set as rejected" using (jobOutputs.single().status == JobStatus.REJECTED)
                "only the status must change" using (jobInput.copy(status = JobStatus.REJECTED) == jobOutput)

                "Developer should be a signer" using (jobCommand.signers.contains(jobOutput.developer.owningKey))
            }

            is Commands.InspectAndAccept -> requireThat {
                "one input should be consumed" using (jobInputs.size == 1)
                "one output should be produced" using (jobOutputs.size == 1)

                val jobOutput = jobOutputs.single()
                val jobInput = jobInputs.single()

                "the input status must be set as completed" using (jobInputs.single().status == JobStatus.COMPLETED)
                "the output status should be set as accepted" using (jobOutputs.single().status == JobStatus.ACCEPTED)
                "only the status must change" using (jobInput.copy(status = JobStatus.ACCEPTED) == jobOutput)

                "Developer should be a signer" using (jobCommand.signers.contains(jobOutput.developer.owningKey))
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
        class StartJob : Commands
        class FinishJob : Commands
        class InspectAndReject : Commands
        class InspectAndAccept : Commands
        class Pay : Commands // TODO - Joel

        // TODO - in flow think about consuming other legal documents such as tender etc when proposing a job
    }
}

// *********
// * State *
// *********
// TODO - allow for lists of subjobs
// TODO - allow for percentage completion and payment
// TODO - map descriptions to BIM XML
// TODO - include drawings as an attribute
data class JobState(val description: String,
                    val status: JobStatus,
                    val developer: Party,
                    val contractor: Party,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    override val participants = listOf(developer, contractor)
}

@CordaSerializable
enum class JobStatus {
    UNSTARTED, STARTED, COMPLETED, ACCEPTED, REJECTED
}
