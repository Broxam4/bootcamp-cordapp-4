package bootcamp;

import examples.ArtContract;
import examples.ArtState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;    
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.security.PublicKey;
import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<TokenContract.Commands> cmd = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);
        CommandData cmdType = cmd.getValue();
        List<PublicKey> signers = cmd.getSigners();

        if (cmdType instanceof Commands.Generate) {
            // Shape checks
            if (tx.getOutputStates().size() != 0) throw new IllegalArgumentException("Token.Gen: No input expected");
            if (tx.outputsOfType(TokenState.class).size() != 1) throw new IllegalArgumentException("Token.Gen: Output expected");

            // Grabbing the transaction's contents TODO ggf. check if validation of State is required to avoid mismatching errors
            TokenState tokenIn = tx.inputsOfType(TokenState.class).get(0);
            TokenState tokenOut = tx.outputsOfType(TokenState.class).get(0);

            // Checking the transaction's contents.
            if (tokenOut.getValue() <= 0) throw new IllegalArgumentException("Token.Gen: Output - Illegal value");
            // if (tokenOut.getIssuer() != BankParty) throw new IllegalArgumentException("Token.Gen: Issuer must be a Bank");
            // if (tokenOut.getOwner() != tokenOut.getIssuer()) throw new IllegalArgumentException("Token.Gen: Cannot issue to others");

            // Checking the transaction's required signers.
            if (!signers.contains(tokenOut.getIssuer().getOwningKey()))
                throw new IllegalArgumentException("Token.Gen: Missing issuer signature");
        } else
            throw new IllegalArgumentException("Token: Unknown Command");

    }


    public interface Commands extends CommandData {
        class Generate implements Commands { }
    }
}
