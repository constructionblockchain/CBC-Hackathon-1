package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.flows.CashIssueFlow
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCashFlow(val amount: Amount<Currency>,
                   val notaryToUse: Party) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val issueRef = OpaqueBytes.of(0)
        subFlow(CashIssueFlow(amount, issueRef, notaryToUse))
    }
}