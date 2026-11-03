package martin.isp.ui;

import martin.isp.deposit.DepositUI;
import martin.isp.transfer.TransferUI;
import martin.isp.withdrawal.WithdrawalUI;

/** The one concrete user interface, implementing the three segregated interfaces (Martin's fix). */
public class ScreenUI implements DepositUI, WithdrawalUI, TransferUI {
    @Override public int requestDepositAmount() { return 100; }
    @Override public int requestWithdrawalAmount() { return 50; }
    @Override public int requestTransferAmount() { return 75; }
}
