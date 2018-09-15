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
//class AgreeJobCommandTests {
//    private val ledgerServices = MockServices(listOf("com.template"))
//    private val developer = TestIdentity(CordaX500Name("John Doe", "City", "GB"))
//    private val contractor = TestIdentity(CordaX500Name("Richard Roe", "Town", "GB"))
//    private val participants = listOf(developer.publicKey, contractor.publicKey)
//    private val unstartedJobState = JobState(
//        description = "Job description",
//        status = JobStatus.UNSTARTED,
//        developer = developer.party,
//        contractor = contractor.party
//    )
//
//    @Test
//    fun `AgreeJob command should complete successfully`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `No inputs should be consumed`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                input(JobContract.ID, unstartedJobState)
//                output(JobContract.ID, unstartedJobState)
//                failsWith("no inputs should be consumed")
//            }
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                input(JobContract.ID, unstartedJobState)
//                input(JobContract.ID, unstartedJobState)
//                output(JobContract.ID, unstartedJobState)
//                failsWith("no inputs should be consumed")
//            }
//        }
//    }
//
//    @Test
//    fun `One output should be produced`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState)
//                output(JobContract.ID, unstartedJobState)
//                failsWith("one output should be produced")
//            }
//        }
//    }
//
//    @Test
//    fun `The developer should be different to the contractor`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState.copy(developer = contractor.party))
//                failsWith("the developer should be different to the contractor")
//            }
//        }
//    }
//
//    @Test
//    fun `Status should be set as UNSTARTED`() {
//        ledgerServices.ledger {
//            transaction {
//                command(participants, JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState.copy(status = JobStatus.COMPLETED))
//                failsWith("the status should be set as unstarted")
//            }
//        }
//    }
//
//    @Test
//    fun `Both developer and contractor should be signers of the transaction`() {
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(developer.publicKey), JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState)
//                failsWith("the developer and contractor are required signer")
//            }
//            transaction {
//                command(listOf(contractor.publicKey), JobContract.Commands.AgreeJob())
//                output(JobContract.ID, unstartedJobState)
//                failsWith("the developer and contractor are required signer")
//            }
//        }
//    }
//}
