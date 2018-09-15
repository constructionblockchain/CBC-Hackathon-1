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
//class InspectAndAcceptCommandTests {
//    private val ledgerServices = MockServices(listOf("com.template"))
//    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
//    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
//    private val thirdParty = TestIdentity(CordaX500Name("John Roe", "Town", "GB"))
//    private val participants = listOf(developer.publicKey, contractor.publicKey)
//    private val completedJobState = JobState(
//        description = "Job description",
//        status = JobStatus.COMPLETED,
//        developer = developer.party,
//        contractor = contractor.party
//    )
//    private val acceptedJobState = completedJobState.copy(status = JobStatus.ACCEPTED)
//
//    @Test
//    fun `InspectAndAccept command should complete successfully`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, acceptedJobState)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `One input should be consumed`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                output(JobContract.ID, acceptedJobState)
//                failsWith("one input should be consumed")
//            }
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, acceptedJobState)
//                failsWith("one input should be consumed")
//            }
//        }
//    }
//
//    @Test
//    fun `One output should be produced`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                failsWith("one output should be produced")
//            }
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, acceptedJobState)
//                output(JobContract.ID, acceptedJobState)
//                failsWith("one output should be produced")
//            }
//        }
//    }
//
//    @Test
//    fun `The input status must be set as COMPLETED`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, acceptedJobState)
//                output(JobContract.ID, acceptedJobState)
//                failsWith("the input status must be set as completed")
//            }
//        }
//    }
//
//    @Test
//    fun `The output status should be set as ACCEPTED`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, completedJobState)
//                failsWith("the output status should be set as accepted")
//            }
//        }
//    }
//
//    @Test
//    fun `Only the status should change`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, acceptedJobState.copy(developer = thirdParty.party))
//                failsWith("only the status must change")
//            }
//        }
//    }
//
//    @Test
//    fun `The developer should be a signer of the transaction`() {
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(contractor.publicKey), JobContract.Commands.InspectAndAccept())
//                input(JobContract.ID, completedJobState)
//                output(JobContract.ID, acceptedJobState)
//                failsWith("Developer should be a signer")
//            }
//        }
//    }
//}
