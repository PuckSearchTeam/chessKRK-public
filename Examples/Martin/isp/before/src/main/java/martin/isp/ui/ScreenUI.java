package martin.isp.ui;

/** The one concrete user interface of the machine. */
public class ScreenUI implements UI {
    @Override public int requestDepositAmount() { return 100; }
    @Override public int requestWithdrawalAmount() { return 50; }
    @Override public int requestTransferAmount() { return 75; }
}
