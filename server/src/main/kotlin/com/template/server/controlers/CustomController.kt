package com.template.server.controlers

import com.template.AgreeJobFlow
import com.template.Milestone
import com.template.MilestoneStatus
import com.template.server.NodeRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/custom") // The paths for GET and POST requests are relative to this base path.
class CustomController(
        private val rpc: NodeRPCConnection
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/customendpoint", produces = arrayOf("text/plain"))
    private fun status() = "Modify this."

    @PostMapping(value = "/agreejob")
    private fun agreeJob(
        @RequestParam("milestone-description") milestoneDescription: String,
        @RequestParam("milestone-amount") milestoneAmount: Long,
        @RequestParam("milestone-currency") milestoneCurrency: String,
        @RequestParam("milestone-status", required = false) milestoneStatus: String,  // TODO: Remove this
        @RequestParam("contractor") contractorName: String,
        @RequestParam("notary") notaryName: String
        ): ResponseEntity<*> {

        val milestone = Milestone(
            description = milestoneDescription,
            amount = Amount(milestoneAmount, Currency.getInstance(milestoneCurrency)))

        val contractor = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(contractorName)) ?:
                return ResponseEntity<Any>("Contractor $contractorName not found on network.", HttpStatus.INTERNAL_SERVER_ERROR)
        val notary = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(notaryName))  ?:
        return ResponseEntity<Any>("Notary $notaryName not found on network.", HttpStatus.INTERNAL_SERVER_ERROR)

        val result = proxy.startFlowDynamic(
            AgreeJobFlow::class.java,
            listOf(milestone),
            contractor,
            notary).returnValue.get()

        return ResponseEntity<Any>(result.tx.outputs.single().data, HttpStatus.OK)
    }


}
