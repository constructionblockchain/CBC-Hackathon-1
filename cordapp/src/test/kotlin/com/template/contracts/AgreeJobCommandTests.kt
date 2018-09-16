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

class AgreeJobCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val milestone = Milestone("Fit windows.", 100.DOLLARS)
    private val participants = listOf(developer.publicKey, contractor.publicKey)
    private val jobState = JobState(
        milestones = listOf(milestone),
        developer = developer.party,
        contractor = contractor.party
    )

    @Test
    fun `AgreeJob command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState)
                verifies()
            }
        }
    }

    @Test
    fun `No JobState inputs should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                input(JobContract.ID, jobState)
                output(JobContract.ID, jobState)
                failsWith("No JobState inputs should be consumed.")
            }
        }
    }

    @Test
    fun `One JobState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState)
                output(JobContract.ID, jobState)
                failsWith("One JobState output should be produced.")
            }
        }
    }

    @Test
    fun `The developer should be different to the contractor`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState.copy(developer = contractor.party))
                failsWith("The developer and the contractor should be different parties.")
            }
        }
    }

    @Test
    fun `All the milestones should be unstarted`() {
        ledgerServices.ledger {
            // A single started milestone.
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState.copy(
                        milestones = listOf(milestone.copy(status = MilestoneStatus.STARTED))))
                failsWith("All the milestones should be unstarted.")
            }
            // An unstarted milestone first, followed by a started milestone.
            transaction {
                command(participants, JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState.copy(
                        milestones = listOf(milestone, milestone.copy(status = MilestoneStatus.STARTED))))
                failsWith("All the milestones should be unstarted.")
            }
        }
    }

    @Test
    fun `Both the developer and the contractor should be signers of the transaction`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(developer.publicKey), JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState)
                failsWith("The developer and contractor should be required signers.")
            }
            transaction {
                command(listOf(contractor.publicKey), JobContract.Commands.AgreeJob())
                output(JobContract.ID, jobState)
                failsWith("The developer and contractor should be required signers.")
            }
        }
    }
}
