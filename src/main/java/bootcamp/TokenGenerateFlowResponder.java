package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(TokenGenerateFlowInitiator.class)
public class TokenGenerateFlowResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public TokenGenerateFlowResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(otherSide) {
            @Suspendable
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // nothing
            }
        });
        subFlow(new ReceiveFinalityFlow(otherSide, signedTransaction.getId()));
        return null;
    }
}