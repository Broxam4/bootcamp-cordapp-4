package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import examples.ArtState;
import jdk.nashorn.internal.parser.Token;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.CommandData;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class TokenGenerateFlowInitiator extends FlowLogic<SignedTransaction> {
    private double amount;

    public TokenGenerateFlowInitiator(double amount) {
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
        //  choose notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        //  get own identity.
        Party issuer = getOurIdentity();

        List<PublicKey> signers = new ArrayList<PublicKey>();
        signers.add(issuer.getOwningKey());

        // Id Generation
        List<StateAndRef<TokenState>> tokenStateRefs = getServiceHub().getVaultService().queryBy(TokenState.class).getStates();
        OptionalInt optMax = tokenStateRefs.stream().mapToInt(x -> x.getState().getData().getID()).max();
        Integer tokenID = 0;
        if (optMax.isPresent())
            tokenID = optMax.getAsInt();

        /* ============================================================================
         *         TODO 1 - Create our TokenState to represent on-ledger tokens!
         * ===========================================================================*/
        // We create our new TokenState.
        TokenState token = new TokenState(tokenID + 1, issuer, issuer, amount);


        /* ============================================================================
         *      TODO 3 - Build our token issuance transaction to update the ledger!
         * ===========================================================================*/
        // We build our transaction.
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        txBuilder
                .addOutputState(token, TokenContract.ID)
                .addCommand(new TokenContract.Commands.Generate(), signers);

        /* ============================================================================
         *          TODO 2 - Write our TokenContract to control token issuance!
         * ===========================================================================*/
        // We check our transaction is valid based on its contracts.
        txBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(issuer);

        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(new FinalityFlow(signedTransaction));
    }
}
