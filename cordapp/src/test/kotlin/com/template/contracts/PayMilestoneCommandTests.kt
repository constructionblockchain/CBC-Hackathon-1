package com.template.contracts

import com.template.JobContract
import com.template.JobState
import com.template.Milestone
import com.template.MilestoneStatus
import net.corda.core.identity.CordaX500Name
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PayMilestoneCommandTests {
    private val fitWindowsCost = 100.DOLLARS
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val participants = listOf(developer.publicKey)
    private val acceptedMilestone = Milestone("Fit windows.", fitWindowsCost, MilestoneStatus.ACCEPTED)
    private val paidMilestone = acceptedMilestone.copy(status = MilestoneStatus.PAID)
    private val otherMilestone = Milestone("Fit doors", 50.DOLLARS)
    private val acceptedJobState = JobState(
        milestones = listOf(acceptedMilestone, otherMilestone),
        developer = developer.party,
        contractor = contractor.party
    )
    private val paidJobState = acceptedJobState.copy(
        milestones = listOf(paidMilestone, otherMilestone))
    private val inputCash = Cash.State(amount = fitWindowsCost.issuedBy(developer.ref(123)),
                                                 owner = developer.party)
    private val outputCash = inputCash.copy(owner = contractor.party)

    @Test
    fun `PayMilestone command should complete successfully`() {
        ledgerServices.ledger {
            unverifiedTransaction {
                attachments(Cash.PROGRAM_ID)
                output(Cash.PROGRAM_ID, inputCash)
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID, inputCash)
                output(Cash.PROGRAM_ID, outputCash)
                verifies()
            }
        }
    }

    @Test
    fun `One JobState input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                output(JobContract.ID, paidJobState)
                failsWith("One JobState input should be consumed.")
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                failsWith("One JobState input should be consumed.")
            }
        }
    }

    @Test
    fun `One JobState output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                failsWith("One JobState output should be produced.")
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                output(JobContract.ID, paidJobState)
                failsWith("One JobState output should be produced.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an input status of ACCEPTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, paidJobState)
                output(JobContract.ID, paidJobState)
                failsWith("The modified milestone should have an input status of ACCEPTED.")
            }
        }
    }

    @Test
    fun `The modified milestone should have an output status of PAID`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, acceptedJobState)
                failsWith("The modified milestone should have an output status of PAID.")
            }
        }
    }

    @Test
    fun `The modified milestone's description and amount shouldn't change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(
                    JobContract.ID, paidJobState.copy(
                        milestones = listOf(paidMilestone.copy(
                            description = "Changed milestone description"), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(
                    JobContract.ID, paidJobState.copy(
                        milestones = listOf(paidMilestone.copy(amount = 200.DOLLARS), otherMilestone)))
                failsWith("The modified milestone's description and amount shouldn't change.")
            }
        }
    }

    @Test
    fun `All the other milestones should be unmodified`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(
                    JobContract.ID, paidJobState.copy(
                        milestones = listOf(paidMilestone, otherMilestone.copy(amount = 200.DOLLARS))))
                failsWith("All the other milestones should be unmodified.")
            }
        }
    }

    @Test
    fun `The Cash command should be Move`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Issue())
                failsWith("The Cash command should be Move")
            }
        }
    }

    @Test
    fun `The cash inputs and outputs should all be in the same currency as the modified milestone`() {
        ledgerServices.ledger {
            unverifiedTransaction {
                attachments(Cash.PROGRAM_ID)
                output(Cash.PROGRAM_ID, inputCash)
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID, inputCash.copy(amount = 100.POUNDS.issuedBy(developer.ref(123))))
                output(Cash.PROGRAM_ID, outputCash)
                failsWith("The cash inputs and outputs should all be in the same currency as the modified milestone")
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID, inputCash)
                output(Cash.PROGRAM_ID, outputCash.copy(amount = 100.POUNDS.issuedBy(developer.ref(123))))
                failsWith("The cash inputs and outputs should all be in the same currency as the modified milestone")
            }
        }
    }

    @Test
    fun `The cash inputs and outputs should have the same value`() {
        ledgerServices.ledger {
            unverifiedTransaction {
                attachments(Cash.PROGRAM_ID)
                output(Cash.PROGRAM_ID, inputCash)
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID,
                      inputCash.copy(amount = (fitWindowsCost.plus(10.DOLLARS)).issuedBy(developer.ref(123))))
                output(Cash.PROGRAM_ID, outputCash)
                failsWith("The cash inputs and outputs should have the same value")
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID, inputCash)
                output(Cash.PROGRAM_ID,
                       outputCash.copy(amount = (fitWindowsCost.plus(10.DOLLARS)).issuedBy(developer.ref(123))))
                failsWith("The cash inputs and outputs should have the same value")
            }
        }
    }

    @Test
    fun `The cash outputs owned by the contractor should have the same value as the modified milestone`() {
        ledgerServices.ledger {
            unverifiedTransaction {
                attachments(Cash.PROGRAM_ID)
                output(Cash.PROGRAM_ID, inputCash)
            }
            transaction {
                command(participants, JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID,
                      inputCash.copy(amount = (fitWindowsCost.plus(10.DOLLARS)).issuedBy(developer.ref(123))))
                output(Cash.PROGRAM_ID,
                       outputCash.copy(amount = (fitWindowsCost.plus(10.DOLLARS)).issuedBy(developer.ref(123))))
                failsWith("The cash outputs owned by the contractor should have the same value as the modified milestone")
            }
        }
    }

    @Test
    fun `The developer should be a required signer`() {
        ledgerServices.ledger {
            unverifiedTransaction {
                attachments(Cash.PROGRAM_ID)
                output(Cash.PROGRAM_ID, inputCash)
            }
            transaction {
                command(listOf(contractor.publicKey), JobContract.Commands.PayMilestone(0))
                input(JobContract.ID, acceptedJobState)
                output(JobContract.ID, paidJobState)
                command(participants, Cash.Commands.Move())
                input(Cash.PROGRAM_ID, inputCash)
                output(Cash.PROGRAM_ID, outputCash)
                failsWith("The developer should be a required signer.")
            }
        }
    }
}
