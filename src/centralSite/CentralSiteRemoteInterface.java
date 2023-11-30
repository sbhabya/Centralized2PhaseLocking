package centralSite;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import dataSite.Operation;
import dataSite.Transaction;
public interface CentralSiteRemoteInterface extends Remote {
    String sayHello() throws RemoteException;
    Transaction receiveTransactionFromDataSite(Transaction t) throws RemoteException;
    boolean locksAvailable(Transaction t) throws RemoteException;
    void deadLockDetectionMethod() throws RemoteException;
    void releaseLocks(Transaction t) throws RemoteException;
}
