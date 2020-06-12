package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import examples.ArtState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@InitiatingFlow
@StartableByRPC
public class TokenTransferFlowInitiator extends FlowLogic<SignedTransaction> {
    private int id;
    private String s_issuer;
    private String s_newOwner;

    public TokenTransferFlowInitiator(int id, String s_issuer, String s_newOwner) {
        this.id = id;
        this.s_issuer = s_issuer;
        this.s_newOwner = s_newOwner;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
//        choose notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

//        resolve issuer and newOwner to parties
        Set<Party> issuees = getServiceHub().getIdentityService().partiesFromName(s_issuer, true);
        if (issuees.size() != 1)
            throw new IllegalArgumentException("Token.Transfer: Issuer not identifiable");
        Party issuer = issuees.iterator().next();
        issuees = getServiceHub().getIdentityService().partiesFromName(s_newOwner, true);
        if (issuees.size() != 1)
            throw new IllegalArgumentException("Token.Transfer: newOwner not identifiable");
        Party newOwner = issuees.iterator().next();

//        get list of tokenStates on the network
        List<StateAndRef<TokenState>> tokenStateRefs = getServiceHub().getVaultService().queryBy(TokenState.class).getStates();


//        search for token with given params else --> errormsg
        StateAndRef<TokenState> inputTokenStateRef = tokenStateRefs.stream().filter(tokenStateRef -> {
            TokenState token = tokenStateRef.getState().getData();
            return token.getIssuer().equals(issuer) && token.getID() == id;
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Token.Transfer: No existing token found"));

//        extract actual tokenState
        TokenState inputToken = inputTokenStateRef.getState().getData();

//        check that flowstarter owns the token to be transfered
        if (!getOurIdentity().equals(inputToken.getOwner()))
            throw new IllegalStateException("Token.Transfer: Flow must be started by current Owner");

//        prepare list of signers
        List<PublicKey> signers = new ArrayList<PublicKey>();
        signers.add(issuer.getOwningKey());
        signers.add(newOwner.getOwningKey());

//        prepare new token for ledger
        TokenState token = new TokenState(
                inputToken.getID(),
                inputToken.getIssuer(),
                newOwner,
                inputToken.getValue());

//        build Tx
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        txBuilder
                .addInputState(inputTokenStateRef)
                .addOutputState(token, TokenContract.ID)
                .addCommand(new TokenContract.Commands.Transfer(), signers);


//        verify Tx
        txBuilder.verify(getServiceHub());

//        complete build and sign Tx
        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // We use `CollectSignaturesFlow` to automatically gather a
        // signature from each counterparty. The counterparty will need to
        // call `SignTransactionFlow` to decided whether or not to sign.
//        collect other signatures
        FlowSession ownerSession = initiateFlow(newOwner);

        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(ownerSession)));

        // We use `FinalityFlow` to automatically notarise the transaction
        // and have it recorded by all the `participants` of all the
        // transaction's states.
        return subFlow(new FinalityFlow(fullySignedTx, ownerSession));
    }
}
