//package com.template.contracts
//
//import com.template.JobContract
//import com.template.JobState
//import com.template.JobStatus
//import net.corda.core.identity.CordaX500Name
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//
//class FinishJobCommandTests {
//    private val ledgerServices = MockServices(listOf("com.template"))
//    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
//    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
//    private val thirdParty = TestIdentity(CordaX500Name("John Roe", "Town", "GB"))
//    private val participants = listOf(developer.publicKey, contractor.publicKey)
//    private val startedJobState = JobState(
//        description = "Job description",
//        status = JobStatus.STARTED,
//        developer = developer.party,
//        contractor = contractor.party
//    )
//    private val completedJobState = startedJobState.copy(status = JobStatus.COMPLETED)
//
//    @Test
//    fun `FinishJob command should complete successfully`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, completedJobState)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `One input should be consumed`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                output(JobContract.ID, completedJobState)
//                failsWith("one input should be consumed")
//            }
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, completedJobState)
//                failsWith("one input should be consumed")
//            }
//        }
//    }
//
//    @Test
//    fun `One output should be produced`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                failsWith("one output should be produced")
//            }
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, completedJobState)
//                output(JobContract.ID, completedJobState)
//                failsWith("one output should be produced")
//            }
//        }
//    }
//
//    @Test
//    fun `The input status must be set as STARTED`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, completedJobState)
//                failsWith("the input status must be set as started")
//            }
//        }
//    }
//
//    @Test
//    fun `The output status should be set as FINISHED`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, startedJobState)
//                failsWith("the output status should be set as finished")
//            }
//        }
//    }
//
//    @Test
//    fun `Only the status should change`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, completedJobState.copy(developer = thirdParty.party))
//                failsWith("only the status must change")
//            }
//        }
//    }
//
//    @Test
//    fun `The contractor should be a signer of the transaction`() {
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(developer.publicKey), JobContract.Commands.FinishJob())
//                input(JobContract.ID, startedJobState)
//                output(JobContract.ID, completedJobState)
//                failsWith("the contractor should be signer")
//            }
//        }
//    }
//}
