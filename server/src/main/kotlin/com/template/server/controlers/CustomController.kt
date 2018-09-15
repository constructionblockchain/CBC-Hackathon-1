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
@RequestMapping("/flows") // The paths for GET and POST requests are relative to this base path.
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
            // You pass these lists in the POST body as follows: Description one., Description two., etc.
            // No need for square brackets, enclosing quotes, etc.
            @RequestParam("milestone-descriptions") milestoneDescriptions: List<String>,
            @RequestParam("milestone-amounts") milestoneAmounts: List<String>,
            @RequestParam("milestone-currency") milestoneCurrency: String,
            @RequestParam("contractor") contractorName: String,
            @RequestParam("notary") notaryName: String
    ): ResponseEntity<*> {
        val descriptionsAndAmounts = milestoneDescriptions.zip(milestoneAmounts)

        val milestones = descriptionsAndAmounts.map { (description, amountString) ->
            val amount = Amount(amountString.toLong(), Currency.getInstance(milestoneCurrency))
            Milestone(description, amount)
        }

        val contractor = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(contractorName))
                ?: return ResponseEntity<Any>("Contractor $contractorName not found on network.", HttpStatus.INTERNAL_SERVER_ERROR)
        val notary = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(notaryName))
                ?: return ResponseEntity<Any>("Notary $notaryName not found on network.", HttpStatus.INTERNAL_SERVER_ERROR)

        val linearId = proxy.startFlowDynamic(AgreeJobFlow::class.java, milestones, contractor, notary).returnValue.get()

        return ResponseEntity<Any>("New job created with ID ${linearId.id}.", HttpStatus.OK)
    }

    @PostMapping(value = "/startmilestone")
    private fun startmilestone(

    ): ResponseEntity<*> {
        return ResponseEntity<Any>("", HttpStatus.OK)
    }

    @PostMapping(value = "/finishmilestone")
    private fun finishmilestone(

    ): ResponseEntity<*> {
        return ResponseEntity<Any>("", HttpStatus.OK)
    }

    @PostMapping(value = "/acceptmilestone")
    private fun acceptmilestone(

    ): ResponseEntity<*> {
        return ResponseEntity<Any>("", HttpStatus.OK)
    }

    @PostMapping(value = "/rejectmilestone")
    private fun rejectmilestone(

    ): ResponseEntity<*> {
        return ResponseEntity<Any>("", HttpStatus.OK)
    }

    @PostMapping(value = "/paymilestone")
    private fun paymilestone(

    ): ResponseEntity<*> {
        return ResponseEntity<Any>("", HttpStatus.OK)
    }
}
