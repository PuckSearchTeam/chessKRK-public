package martin.isp.transfer;

import martin.isp.transaction.Transaction;

public class TransferTransaction extends Transaction {
    private final TransferUI ui;
    private int amount;

    public TransferTransaction(TransferUI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestTransferAmount(); }
}
