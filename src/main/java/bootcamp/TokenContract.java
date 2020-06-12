package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import java.security.PublicKey;
import java.util.List;


public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<TokenContract.Commands> cmd = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);
        CommandData cmdType = cmd.getValue();
        List<PublicKey> signers = cmd.getSigners();

        if (cmdType instanceof Commands.Generate) {
//          Shape checks
            if (tx.getInputStates().size() != 0) throw new IllegalArgumentException("Token.Gen: No input expected");
            if (tx.outputsOfType(TokenState.class).size() != 1)
                throw new IllegalArgumentException("Token.Gen: Output expected");

//          Tx content check
            TokenState tokenOut = tx.outputsOfType(TokenState.class).get(0);

            if (tokenOut.getValue() <= 0) throw new IllegalArgumentException("Token.Gen: Output - Illegal value");
//          if (!(tokenOut.getIssuer() instanceof BankParty)) throw new IllegalArgumentException("Token.Gen: Issuer must be a Bank");
//          if (tokenOut.getOwner() != tokenOut.getIssuer()) throw new IllegalArgumentException("Token.Gen: Cannot issue to others");

//          Tx signers check
            if (!signers.contains(tokenOut.getIssuer().getOwningKey()) || signers.size() != 1)
                throw new IllegalArgumentException("Token.Gen: Missing issuer signature");

        } else if (cmdType instanceof Commands.Transfer) {
//          Shape checks
            if (tx.inputsOfType(TokenState.class).size() != 1)
                throw new IllegalArgumentException("Token.Trans: Input expected");
            if (tx.outputsOfType(TokenState.class).size() != 1)
                throw new IllegalArgumentException("Token.Trans: Output expected");

//          Tx content check
            TokenState tokenIn = tx.inputsOfType(TokenState.class).get(0);
            TokenState tokenOut = tx.outputsOfType(TokenState.class).get(0);

            if (tokenOut.getValue() != tokenIn.getValue())
                throw new IllegalArgumentException("Token.Trans: Value changed");
            if (tokenOut.getOwner().equals(tokenIn.getOwner()))
                throw new IllegalArgumentException("Token.Trans: No owner change");
            if (tokenOut.getID() != tokenIn.getID())
                throw new IllegalArgumentException("Token.Trans: ID changed");
            if (!tokenOut.getIssuer().equals(tokenIn.getIssuer()))
                throw new IllegalArgumentException("Token.Trans: Issuer changed");

//          Tx signers check
            if (!signers.contains(tokenIn.getIssuer().getOwningKey()))
                throw new IllegalArgumentException("Token.Trans: Missing issuer signature");
            if (!signers.contains(tokenIn.getOwner().getOwningKey()))
                throw new IllegalArgumentException("Token.Trans: Missing owner signature");

        } else
            throw new IllegalArgumentException("Token: Unknown Command");

    }

    public interface Commands extends CommandData {
        class Generate implements Commands { }
        class Transfer implements Commands { }
    }
}
