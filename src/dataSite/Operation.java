package dataSite;

import java.io.Serializable;

public class Operation implements Serializable {
    int operationId;
    String operationType;
    String item;
    String opCmd;

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getOpCmd() {
        return opCmd;
    }

    public void setOpCmd(String opCmd) {
        this.opCmd = opCmd;
    }

    public Operation(int operationId, String operationType, String item, String opCmd) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.item = item;
        this.opCmd = opCmd;
    }
}
