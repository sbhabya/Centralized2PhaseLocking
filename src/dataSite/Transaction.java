package dataSite;

import java.io.Serializable;
import java.util.ArrayList;

public class Transaction implements Serializable {
    int transactionId = -1;
    int dataSiteId;
    ArrayList<Operation> operations;

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getDataSiteId() {
        return dataSiteId;
    }

    public void setDataSiteId(int dataSiteId) {
        this.dataSiteId = dataSiteId;
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }

    public void setOperations(ArrayList<Operation> operations) {
        this.operations = operations;
    }

    public Transaction(int dataSiteId, ArrayList<Operation> operations) {
        this.dataSiteId = dataSiteId;
        this.operations = operations;
    }
}
