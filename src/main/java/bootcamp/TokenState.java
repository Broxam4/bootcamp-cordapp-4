package bootcamp;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
@BelongsToContract(TokenContract.class)
public class TokenState implements ContractState{
    private Party issuer;
    private Party owner;
    private double value;

    public TokenState(Party issuer, Party owner, double value) {
        this.issuer = issuer;
        this.owner = owner;
        this.value = value;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List list = new ArrayList<Party>();
        list.add(issuer);
        list.add(owner);
        return list;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public double getValue() {
        return value;
    }
}