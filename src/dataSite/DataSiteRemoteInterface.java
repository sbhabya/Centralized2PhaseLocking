package dataSite;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DataSiteRemoteInterface extends Remote {
    void sayHello() throws RemoteException;
    Boolean executeTransaction(Transaction t) throws RemoteException;
}
