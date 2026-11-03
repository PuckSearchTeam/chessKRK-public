package martin.isp.ui;

/**
 * Martin's ATM example (Agile Software Development, chapter 12): one interface for every transaction's dialogue,
 * the "fat" interface. Each transaction uses one method and depends on all three.
 */
public interface UI {
    int requestDepositAmount();
    int requestWithdrawalAmount();
    int requestTransferAmount();
}
