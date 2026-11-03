package martin.isp.withdrawal;

import martin.isp.transaction.Transaction;

public class WithdrawalTransaction extends Transaction {
    private final WithdrawalUI ui;
    private int amount;

    public WithdrawalTransaction(WithdrawalUI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestWithdrawalAmount(); }
}
