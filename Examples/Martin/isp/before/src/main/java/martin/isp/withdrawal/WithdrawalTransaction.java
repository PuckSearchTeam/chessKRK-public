package martin.isp.withdrawal;

import martin.isp.transaction.Transaction;
import martin.isp.ui.UI;

public class WithdrawalTransaction extends Transaction {
    private final UI ui;
    private int amount;

    public WithdrawalTransaction(UI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestWithdrawalAmount(); }
}
