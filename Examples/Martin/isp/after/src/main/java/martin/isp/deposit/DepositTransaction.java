package martin.isp.deposit;

import martin.isp.transaction.Transaction;

public class DepositTransaction extends Transaction {
    private final DepositUI ui;
    private int amount;

    public DepositTransaction(DepositUI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestDepositAmount(); }
}
