package com.template.server.controllers

import com.template.*
import com.template.server.NodeRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.finance.contracts.asset.Cash
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/states") // The paths for GET and POST requests are relative to this base path.
class StateController(rpc: NodeRPCConnection) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/jobstates", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    private fun jobStates(): List<Map<String, String>> {
        val jobStatesAndRefs = proxy.vaultQueryBy<JobState>().states
        val jobStates = jobStatesAndRefs.map { it.state.data }
        return jobStates.map { jobState ->
            mapOf(
                    "developer" to jobState.developer.toString(),
                    "contractor" to jobState.contractor.toString(),
                    "milestones" to jobState.milestones.toString(),
                    "id" to jobState.linearId.toString()
            )
        }
    }

    @GetMapping(value = "/cashstates", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    private fun cashStates(): List<Map<String, String>> {
        val cashStatesAndRefs = proxy.vaultQueryBy<Cash.State>().states
        val cashStates = cashStatesAndRefs.map { it.state.data }
        return cashStates.map { cashState ->
            mapOf(
                    "owner" to cashState.owner.toString(),
                    "amount" to cashState.amount.toString()
            )
        }
    }
}