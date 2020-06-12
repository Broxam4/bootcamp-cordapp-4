package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.CommandData;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class TokenGenerateFlowInitiator extends FlowLogic<SignedTransaction> {
    private double amount;

    public TokenGenerateFlowInitiator(int amount) {
        this.amount = amount;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We choose our transaction's notary (the notary prevents double-spends).
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        // We get a reference to our own identity.
        Party issuer = getOurIdentity();
        List<PublicKey> signers = new ArrayList<PublicKey>();
        signers.add(issuer.getOwningKey());

        /* ============================================================================
         *         TODO 1 - Create our TokenState to represent on-ledger tokens!
         * ===========================================================================*/
        // We create our new TokenState.
        TokenState token = new TokenState(issuer, issuer, amount);


        /* ============================================================================
         *      TODO 3 - Build our token issuance transaction to update the ledger!
         * ===========================================================================*/
        // We build our transaction.
        TransactionBuilder txBuilder = new TransactionBuilder();
        txBuilder
                .setNotary(notary);
        txBuilder
                .addOutputState(token, "bootcamp.TokenContract")
                .addCommand(new TokenContract.Commands.Generate(), signers);

        /* ============================================================================
         *          TODO 2 - Write our TokenContract to control token issuance!
         * ===========================================================================*/
        // We check our transaction is valid based on its contracts.
        txBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(issuer);

        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        // The counterparty signs the transaction
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }
}
