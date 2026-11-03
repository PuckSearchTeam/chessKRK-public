package martin.isp.transfer;

import martin.isp.transaction.Transaction;
import martin.isp.ui.UI;

public class TransferTransaction extends Transaction {
    private final UI ui;
    private int amount;

    public TransferTransaction(UI ui) { this.ui = ui; }

    @Override public void execute() { amount = ui.requestTransferAmount(); }
}
