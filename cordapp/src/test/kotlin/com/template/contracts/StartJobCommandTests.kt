package com.template.contracts

import com.template.JobContract
import com.template.JobState
import com.template.JobStatus
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class StartJobCommandTests {
    private val ledgerServices = MockServices(listOf("com.template"))
    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
    private val thirdParty = TestIdentity(CordaX500Name("John Roe", "Town", "GB"))
    private val participants = listOf(developer.publicKey, contractor.publicKey)
    private val unstartedJobState = JobState(
        description = "Job description",
        status = JobStatus.UNSTARTED,
        developer = developer.party,
        contractor = contractor.party
    )

    private val startedJobState = unstartedJobState.copy(status = JobStatus.STARTED)

    @Test
    fun `StartJob command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartJob())
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
                command(participants, JobContract.Commands.StartJob())
                output(JobContract.ID, startedJobState)
                failsWith("one input should be consumed")
            }
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("one input should be consumed")
            }
        }
    }

    @Test
    fun `One output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                failsWith("one output should bbe produced")
            }
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("one output should bbe produced")
            }
        }
    }

    @Test
    fun `Previous status shoud not be STARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, startedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("the input status should be UNSTARTED")
            }
        }
    }

    @Test
    fun `Status should be set to STARTED`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, unstartedJobState)
                failsWith("the output status should be STARTED")
            }
        }
    }

    @Test
    fun `Only the status should change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState.copy(developer = thirdParty.party))
                failsWith("only the job status should change")
            }
        }
    }

    @Test
    fun `Both developer and contractor should sign the transaction`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(developer.publicKey), JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("the developer and contractor are required signers")
            }
            transaction {
                command(listOf(contractor.publicKey), JobContract.Commands.StartJob())
                input(JobContract.ID, unstartedJobState)
                output(JobContract.ID, startedJobState)
                failsWith("the developer and contractor are required signers")
            }
        }
    }
}
