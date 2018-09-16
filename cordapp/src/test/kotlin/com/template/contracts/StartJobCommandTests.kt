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

class StartMilestoneCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val milestoneOne = Milestone("Fit windows.", 100.DOLLARS)
    private val milestoneTwo = Milestone("Fit doors.", 200.DOLLARS)
    private val milestoneTwoStarted  = milestoneTwo.copy(status = MilestoneStatus.STARTED)
    private val participants = listOf(developer.publicKey, contractor.publicKey)
    private val unstartedJobState = JobState(
            milestones = listOf(milestoneOne, milestoneTwo),
            developer = developer.party,
            contractor = contractor.party
    )
    private val startedJobState = unstartedJobState.copy(milestones = listOf(milestoneOne, milestoneTwoStarted))
    private val milestoneIndex = 1

    @Test
    fun `StartMilestone command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                verifies()
            }
        }
    }

    @Test
    fun `One input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                output(JobContract.ID, startedJobState)
                failsWith("One JobState input should be consumed.")
            }
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("One JobState input should be consumed.")
            }
        }
    }

    @Test
    fun `One output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                failsWith("One JobState output should be produced.")
            }
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("One JobState output should be produced.")
            }
        }
    }

    @Test
    fun `For the milestone to modify, the input JobState status should be UNSTARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The modified milestone should have an input status of UNSTARTED.")
            }
        }
    }

    @Test
    fun `For the milestone to modify, the output JobState status should be STARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, unstartedJobState)
                failsWith("The modified milestone should have an output status of STARTED.")
            }
        }
    }

    @Test
    fun `For the milestone to modify, fields other than the status should not change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(
                        milestones = listOf(milestoneOne, milestoneTwoStarted.copy(description = "Fit some floors."))))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(
                        milestones = listOf(milestoneOne, milestoneTwoStarted.copy(amount = 300.DOLLARS))))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
        }
    }

    @Test
    fun `The other milestones should not change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(
                        milestones = listOf(milestoneOne.copy(description = "Fit some floors."), milestoneTwoStarted)))
                failsWith("All the other milestones should be unmodified.")
            }
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(
                        milestones = listOf(milestoneOne.copy(amount = 200.DOLLARS), milestoneTwoStarted)))
                failsWith("All the other milestones should be unmodified.")
            }
            transaction {
                command(participants, JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(
                        milestones = listOf(milestoneOne.copy(status = MilestoneStatus.STARTED), milestoneTwoStarted)))
                failsWith("All the other milestones should be unmodified.")
            }
        }
    }

    @Test
    fun `Both developer and contractor should sign the transaction`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(developer.publicKey), JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The developer and contractor should be required signers.")
            }
            transaction {
                command(listOf(contractor.publicKey), JobContract.Commands.StartMilestone(milestoneIndex))
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("The developer and contractor should be required signers.")
            }
        }
    }
}
