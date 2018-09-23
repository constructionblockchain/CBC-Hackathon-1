package com.template.contracts

import com.template.JobContract
import com.template.JobState
import com.template.Milestone
import com.template.MilestoneStatus
import net.corda.core.identity.CordaX500Name
import net.corda.finance.DOLLARS
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class FinishMilestoneCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val participants = listOf(contractor.publicKey)
    private val startedMilestone = Milestone("Fit windows.", 100.DOLLARS, MilestoneStatus.STARTED)
    private val completedMilestone = startedMilestone.copy(status = MilestoneStatus.COMPLETED)
    private val otherMilestone = Milestone("Fit doors", 50.DOLLARS)
    private val startedJobState = JobState(
        milestones = listOf(startedMilestone, otherMilestone),
        developer = developer.party,
        contractor = contractor.party
    )
    private val completedJobState = startedJobState.copy(
        milestones = listOf(completedMilestone, otherMilestone))

    @Test
    fun `FinishMilestone command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState)
                verifies()
            }
        }
    }

    @Test
    fun `One JobState input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                output(JobContract.ID, completedJobState)
                failsWith("One JobState input should be consumed.")
            }
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState)
                failsWith("One JobState input should be consumed.")
            }
        }
    }

    @Test
    fun `One JobState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                failsWith("One JobState output should be produced")
            }
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState)
                output(JobContract.ID, completedJobState)
                failsWith("One JobState output should be produced.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an input status of STARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, completedJobState)
                failsWith("The modified milestone should have an input status of STARTED.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an output status of COMPLETED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The modified milestone should have an output status of COMPLETED.")
            }
        }
    }

    @Test
    fun `The modified milestone's description and amount shouldn't change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState.copy(
                    milestones = listOf(completedMilestone.copy(
                        description = "Changed milestone description"), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState.copy(
                    milestones = listOf(completedMilestone.copy(amount = 200.DOLLARS), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
        }
    }

    @Test
    fun `All the other milestones should be unmodified`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState.copy(
                    milestones = listOf(completedMilestone, otherMilestone.copy(amount = 200.DOLLARS))))
                failsWith("All the other milestones should be unmodified.")
            }
        }
    }
    
    @Test
    fun `The contractor should be a required signer`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(developer.publicKey), JobContract.Commands.FinishMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, completedJobState)
                failsWith("The contractor should be a required signer.")
            }
        }
    }
}
