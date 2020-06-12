package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

@InitiatedBy(TokenTransferFlowInitiator.class)
public class TokenTransferFlowResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public TokenTransferFlowResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
                super(otherSession, progressTracker);
            }

            /*
             As part of `SignTransactionFlow`, the contracts of the
             transaction's input and output states are run automatically.
             Inside `checkTransaction`, we define our own additional logic
             for checking the received transaction. If `checkTransaction`
             throws an exception, we'll refuse to sign.
            */
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // nothing
            }
        }

        subFlow(new SignTxFlow(otherSide, SignTransactionFlow.tracker()));
        subFlow(new ReceiveFinalityFlow(otherSide));
        /*
         Once the counterparty calls `FinalityFlow`, we will
         automatically record the transaction if we are one of the
         `participants` on one or more of the transaction's states.
        */

        return null;
    }
}