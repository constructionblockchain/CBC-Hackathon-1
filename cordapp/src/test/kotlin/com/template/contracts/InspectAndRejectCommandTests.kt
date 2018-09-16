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

class InspectAndRejectCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val participants = listOf(developer.publicKey)
    private val completedMilestone = Milestone("Fit windows.", 100.DOLLARS, MilestoneStatus.COMPLETED)
    private val startedMilestone = completedMilestone.copy(status = MilestoneStatus.STARTED)
    private val otherMilestone = Milestone("Fit doors", 50.DOLLARS)
    private val completedJobState = JobState(
        milestones = listOf(completedMilestone, otherMilestone),
        developer = developer.party,
        contractor = contractor.party
    )
    private val startedJobState = completedJobState.copy(
        milestones = listOf(startedMilestone, otherMilestone))

    @Test
    fun `RejectMilestone command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState)
                verifies()
            }
        }
    }

    @Test
    fun `One JobState input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                output(JobContract.ID, startedJobState)
                failsWith("One JobState input should be consumed.")
            }
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("One JobState input should be consumed.")
            }
        }
    }

    @Test
    fun `One JobState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                failsWith("One JobState output should be produced.")
            }
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("One JobState output should be produced.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an input status of COMPLETED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The modified milestone should have an input status of COMPLETED.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an output status of STARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, completedJobState)
                failsWith("The modified milestone should have an output status of STARTED.")
            }
        }
    }

    @Test
    fun `The modified milestone's description and amount shouldn't change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState.copy(
                    milestones = listOf(startedMilestone.copy(
                        description = "Changed milestone description"), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState.copy(
                    milestones = listOf(startedMilestone.copy(amount = 200.DOLLARS), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
        }
    }

    @Test
    fun `All the other milestones should be unmodified`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState.copy(
                    milestones = listOf(startedMilestone, otherMilestone.copy(amount = 200.DOLLARS))))
                failsWith("All the other milestones should be unmodified.")
            }
        }
    }

    @Test
    fun `The developer should be a required signer`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(contractor.publicKey), JobContract.Commands.RejectMilestone(0))
                input(JobContract.ID, completedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The developer should be a required signer.")
            }
        }
    }
}
