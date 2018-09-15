package com.template.server.controlers

import com.template.AgreeJobFlow
import com.template.Milestone
import com.template.MilestoneStatus
import com.template.server.NodeRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
        @RequestParam("contractor") contractor: String,
        @RequestParam("notary") notary: String
        ): ResponseEntity<*> {

        val milestone = Milestone(
            description = milestoneDescription,
            amount = Amount(milestoneAmount, Currency.getInstance(milestoneCurrency)),
            status = MilestoneStatus.valueOf(milestoneStatus))

        val contractor = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(contractor))!!
        val notary = proxy.notaryPartyFromX500Name(CordaX500Name.parse(notary))!!
        val milestoneList = mutableListOf(milestone)

        val result = proxy.startFlowDynamic(
            AgreeJobFlow::class.java,
            milestoneList,
            contractor,
            notary).returnValue.get()

        return ResponseEntity<Any>(result.tx.outputs.single().data, HttpStatus.OK)
    }


}
