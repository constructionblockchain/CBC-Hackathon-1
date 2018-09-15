package com.template

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.util.*

// *****************
// * Contract Code *
// *****************
class JobContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        const val ID = "com.template.JobContract"
    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val jobInputs = tx.inputsOfType<JobState>()
        val jobOutputs = tx.outputsOfType<JobState>()
        val jobCommand = tx.commandsOfType<JobContract.Commands>().single()

        when (jobCommand.value) {
            is Commands.AgreeJob -> requireThat {
                "No inputs should be consumed" using (jobInputs.isEmpty())
                "One output should be produced" using (jobOutputs.size == 1)

                val jobOutput = jobOutputs.single()
                "The developer should be different to the contractor" using (jobOutput.contractor != jobOutput.developer)
                "The status of all the milestones should be set to unstarted" using
                        (jobOutput.milestones.all { it.status == MilestoneStatus.UNSTARTED })

                "The developer and contractor are required signer" using
                        (jobCommand.signers.containsAll(listOf(jobOutput.contractor.owningKey, jobOutput.developer.owningKey)))
            }

            is Commands.StartMilestone -> requireThat {
                "One JobState input should be consumed." using (jobInputs.size == 1)
                "One JobState output should be produced." using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                val milestoneIndex = (jobCommand.value as Commands.StartMilestone).milestoneIndex
                val inputModifiedMilestone = jobInput.milestones[milestoneIndex]
                val outputModifiedMilestone = jobOutput.milestones[milestoneIndex]

                "The modified milestone should have an input status of UNSTARTED." using
                        (inputModifiedMilestone.status == MilestoneStatus.UNSTARTED)
                "The modified milestone should have an output status of STARTED." using
                        (outputModifiedMilestone.status == MilestoneStatus.STARTED)
                "The modified milestone's description and amount shouldn't change." using
                        (inputModifiedMilestone.copy(status = MilestoneStatus.STARTED) == outputModifiedMilestone)

                val otherInputMilestones = jobInput.milestones.minusElement(inputModifiedMilestone)
                val otherOutputMilestones = jobOutput.milestones.minusElement(outputModifiedMilestone)

                "All the other milestones should be unmodified." using
                        (otherInputMilestones == otherOutputMilestones)

                "The developer and contractor are required signers." using
                        (jobCommand.signers.containsAll(listOf(jobOutput.contractor.owningKey, jobOutput.developer.owningKey)))
            }

            is Commands.FinishMilestone -> requireThat {
                "One JobState input should be consumed." using (jobInputs.size == 1)
                "One JobState output should be produced." using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                val milestoneIndex = (jobCommand.value as Commands.FinishMilestone).milestoneIndex
                val inputModifiedMilestone = jobInput.milestones[milestoneIndex]
                val outputModifiedMilestone = jobOutput.milestones[milestoneIndex]

                "The modified milestone should have an input status of STARTED." using
                        (inputModifiedMilestone.status == MilestoneStatus.STARTED)
                "The modified milestone should have an output status of COMPLETED." using
                        (outputModifiedMilestone.status == MilestoneStatus.COMPLETED)
                "The modified milestone's description and amount shouldn't change." using
                        (inputModifiedMilestone.copy(status = MilestoneStatus.COMPLETED) == outputModifiedMilestone)

                val otherInputMilestones = jobInput.milestones.minusElement(inputModifiedMilestone)
                val otherOutputMilestones = jobOutput.milestones.minusElement(outputModifiedMilestone)

                "All the other milestones should be unmodified." using
                        (otherInputMilestones == otherOutputMilestones)

                "The contractor should be signer." using (jobCommand.signers.contains(jobOutputs.single().contractor.owningKey))
            }

            is Commands.RejectMilestone -> requireThat {
                "One JobState input should be consumed." using (jobInputs.size == 1)
                "One JobState output should be produced." using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                val milestoneIndex = (jobCommand.value as Commands.RejectMilestone).milestoneIndex
                val inputModifiedMilestone = jobInput.milestones[milestoneIndex]
                val outputModifiedMilestone = jobOutput.milestones[milestoneIndex]

                "The modified milestone should have an input status of COMPLETED." using
                        (inputModifiedMilestone.status == MilestoneStatus.COMPLETED)
                "The modified milestone should have an output status of STARTED." using
                        (outputModifiedMilestone.status == MilestoneStatus.STARTED)
                "The modified milestone's description and amount shouldn't change." using
                        (inputModifiedMilestone.copy(status = MilestoneStatus.STARTED) == outputModifiedMilestone)

                val otherInputMilestones = jobInput.milestones.minusElement(inputModifiedMilestone)
                val otherOutputMilestones = jobOutput.milestones.minusElement(outputModifiedMilestone)

                "All the other milestones should be unmodified." using
                        (otherInputMilestones == otherOutputMilestones)

                "Developer should be a signer" using (jobCommand.signers.contains(jobOutput.developer.owningKey))
            }

            is Commands.AcceptMilestone -> requireThat {
                "One JobState input should be consumed." using (jobInputs.size == 1)
                "One JobState output should be produced." using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                val milestoneIndex = (jobCommand.value as Commands.AcceptMilestone).milestoneIndex
                val inputModifiedMilestone = jobInput.milestones[milestoneIndex]
                val outputModifiedMilestone = jobOutput.milestones[milestoneIndex]

                "The modified milestone should have an input status of COMPLETED." using
                        (inputModifiedMilestone.status == MilestoneStatus.COMPLETED)
                "The modified milestone should have an output status of ACCEPTED." using
                        (outputModifiedMilestone.status == MilestoneStatus.ACCEPTED)
                "The modified milestone's description and amount shouldn't change." using
                        (inputModifiedMilestone.copy(status = MilestoneStatus.ACCEPTED) == outputModifiedMilestone)

                val otherInputMilestones = jobInput.milestones.minusElement(inputModifiedMilestone)
                val otherOutputMilestones = jobOutput.milestones.minusElement(outputModifiedMilestone)

                "All the other milestones should be unmodified." using
                        (otherInputMilestones == otherOutputMilestones)

                "Developer should be a signer." using (jobCommand.signers.contains(jobOutput.developer.owningKey))
            }

            is Commands.PayMilestone -> requireThat {
                "One JobState input should be consumed." using (jobInputs.size == 1)
                "One JobState output should be produced." using (jobOutputs.size == 1)

                val jobInput = jobInputs.single()
                val jobOutput = jobOutputs.single()
                val milestoneIndex = (jobCommand.value as Commands.PayMilestone).milestoneIndex
                val inputModifiedMilestone = jobInput.milestones[milestoneIndex]
                val outputModifiedMilestone = jobOutput.milestones[milestoneIndex]

                "The modified milestone should have an input status of ACCEPTED." using
                        (inputModifiedMilestone.status == MilestoneStatus.ACCEPTED)
                "The modified milestone should have an output status of PAID." using
                        (outputModifiedMilestone.status == MilestoneStatus.PAID)
                "The modified milestone's description and amount shouldn't change." using
                        (inputModifiedMilestone.copy(status = MilestoneStatus.PAID) == outputModifiedMilestone)

                val otherInputMilestones = jobInput.milestones.minusElement(inputModifiedMilestone)
                val otherOutputMilestones = jobOutput.milestones.minusElement(outputModifiedMilestone)

                "All the other milestones should be unmodified." using
                        (otherInputMilestones == otherOutputMilestones)

                "Developer should be a signer." using (jobCommand.signers.contains(jobInput.developer.owningKey))

                // TODO - cash based rules (e.g. has the right amount been paid?)
            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class AgreeJob : Commands
        class StartMilestone(val milestoneIndex: Int) : Commands
        class FinishMilestone(val milestoneIndex: Int) : Commands
        class RejectMilestone(val milestoneIndex: Int) : Commands
        class AcceptMilestone(val milestoneIndex: Int) : Commands
        class PayMilestone(val milestoneIndex: Int) : Commands
    }
}

// *********
// * State *
// *********
data class JobState(
        val developer: Party,
        val contractor: Party,
        val milestones: List<Milestone>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    init {
        if (milestones.map { it.amount.token }.toSet().size != 1) {
            throw IllegalArgumentException("All milestones must be budgeted in the same currency.")
        }
    }

    override val participants = listOf(developer, contractor)
    val amount = Amount(milestones.map { it.amount.quantity }.sum(), milestones[0].amount.token)
}

@CordaSerializable
data class Milestone(
        val description: String,
        val amount: Amount<Currency>,
        val status: MilestoneStatus = MilestoneStatus.UNSTARTED)

@CordaSerializable
enum class MilestoneStatus {
    UNSTARTED, STARTED, COMPLETED, ACCEPTED, PAID
}
