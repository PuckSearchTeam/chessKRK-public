package martin.isp.deposit;

import martin.isp.transaction.Transaction;
import martin.isp.ui.UI;

public class DepositTransaction extends Transaction {
    private final UI ui;
    private int amount;

    public DepositTransaction(UI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestDepositAmount(); }
}
