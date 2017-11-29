package com.client

import com.example.flow.ExampleFlow
import com.example.state.IOUState
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

val RPC_USERNAME = "user1"
val RPC_PASSWORD = "test"
val PARTY_B_NAME = "O=PartyB,L=New York,C=US"

fun main(args: Array<String>) {
    RpcClient().main(args)
}

/** An example RPC client that connects to a node and performs various example operations. */
private class RpcClient {
    companion object {
        val logger: Logger = loggerFor<RpcClient>()
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: RpcClient <node address>" }
        val rpcPortString = args[0]

        val rpcConnection = establishRpcConnection(rpcPortString, RPC_USERNAME, RPC_PASSWORD)
        val rpcProxy = rpcConnection.proxy

        logNetworkNodes(rpcProxy)

        val iouValue = 99
        val counterpartyName = CordaX500Name.parse(PARTY_B_NAME)
        issueIOUState(rpcProxy, iouValue, counterpartyName)

        logIOUStates(rpcProxy)

        rpcConnection.close()
    }

    /** Returns a [CordaRPCConnection] to the node listening on [rpcPortString]. */
    private fun establishRpcConnection(rpcPortString: String, username: String, password: String): CordaRPCConnection {
        val nodeAddress = parse(rpcPortString)
        val client = CordaRPCClient(nodeAddress)
        return client.start(username, password)
    }

    /** Logs the names of all the nodes on the network. */
    private fun logNetworkNodes(rpcProxy: CordaRPCOps) {
        logger.info("Logging all network nodes...")
        val nodes = rpcProxy.networkMapSnapshot()
        val identities = nodes.map { it.legalIdentitiesAndCerts.first() }
        identities.forEach {
            logger.info("{}", it)
        }
        logger.info("\n")
    }

    /** Runs a flow to issue a new [IOUState] onto the ledger. */
    private fun issueIOUState(rpcProxy: CordaRPCOps, iouValue: Int, counterpartyName: CordaX500Name) {
        logger.info("Running a flow to create an IOUState...")
        val otherParty = rpcProxy.wellKnownPartyFromX500Name(counterpartyName)!!

        val flowHandle = rpcProxy.startFlow(ExampleFlow::Initiator, iouValue, otherParty)
        val stx = flowHandle.returnValue.get()
        logger.info("Transaction {} created!", stx.id)
        logger.info("\n")
    }

    /** Logs all the [IOUState]s in the node's vault. */
    private fun logIOUStates(rpcProxy: CordaRPCOps) {
        logger.info("Logging all IOUStates...")
        val iouQuery = rpcProxy.vaultQuery(IOUState::class.java)
        val iouStates = iouQuery.states
        iouStates.forEach {
            logger.info("{}", it.state.data)
        }
        logger.info("\n")
    }
}