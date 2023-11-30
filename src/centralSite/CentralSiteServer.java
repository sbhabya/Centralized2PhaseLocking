package centralSite;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CentralSiteServer {
    public static void main(String[] args) {
        try {
            // Create and export the remote object
            CentralSiteRemoteImplementation remoteObj = new CentralSiteRemoteImplementation();
            CentralSiteRemoteInterface stub = (CentralSiteRemoteInterface) UnicastRemoteObject.exportObject(remoteObj, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(45); // Default RMI registry port
            registry.bind("CentralSiteServer", stub);
            System.out.println("Server ready");
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(() -> {
                try {
                    // Call the scheduled method on the remote object
                    remoteObj.deadLockDetectionMethod();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
