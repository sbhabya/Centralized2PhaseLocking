package dataSite;

import centralSite.CentralSiteRemoteInterface;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataProcessor implements Runnable{
    static ArrayList<Transaction> transactionList;
    Transaction curTransaction;

    public static ArrayList<Transaction> getTransactionList() {
        return transactionList;
    }

    public static void setTransactionList(ArrayList<Transaction> transactionList) {
        DataProcessor.transactionList = transactionList;
    }

    public Transaction getCurTransaction() {
        return curTransaction;
    }

    public void setCurTransaction(Transaction curTransaction) {
        this.curTransaction = curTransaction;
    }

    public DataProcessor(Transaction curTransaction) {
        this.curTransaction = curTransaction;
    }

    public static ArrayList<Transaction> parseInputFile(String fileName) {
        ArrayList<Transaction> tlist = new ArrayList<>();
        File f = new File(fileName);
        ArrayList<String> commands = new ArrayList<>();
        try{
            FileReader fr = new FileReader(f);
            BufferedReader bfr = new BufferedReader(fr);
            String line = bfr.readLine();
            while(line != null) {
                commands.add(line);
                line = bfr.readLine();
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        int curTid = -1;
        int opId = 0;
        for(int i = 0; i < commands.size(); i++)  {
            String cmd = commands.get(i);
            if(cmd.contains("transaction")){
                String[] arr = cmd.split(" ");
                ArrayList<Operation> ops= new ArrayList<>();
                Transaction t = new Transaction(Integer.parseInt(arr[0]),ops);
                tlist.add(t);
                curTid++;
                opId = 1;
            } else {
                String[] arr = cmd.split(" ");
                if(cmd.contains("Read")){
                    Operation op = new Operation(opId++,"read", arr[1],cmd);
                    tlist.get(curTid).getOperations().add(op);
                } else if(cmd.contains("Write")) {
                    Operation op = new Operation(opId++,"write", arr[1],cmd);
                    tlist.get(curTid).getOperations().add(op);
                } else if(cmd.contains("Commit")) {
                    Operation op = new Operation(opId++,"commit", "",cmd);
                    tlist.get(curTid).getOperations().add(op);
                } else if(cmd.contains("Abort")) {
                    Operation op = new Operation(opId++,"abort", "",cmd);
                    tlist.get(curTid).getOperations().add(op);
                } else {
                    Operation op = new Operation(opId++,"modify", arr[0],cmd);
                    tlist.get(curTid).getOperations().add(op);
                }
            }

        }
        return tlist;
    }
    public void centralSiteConnection(Transaction t) {
        try{
        Registry registry = LocateRegistry.getRegistry("localhost", 45);
        CentralSiteRemoteInterface remoteObj = (CentralSiteRemoteInterface) registry.lookup("CentralSiteServer");
        t = sendTransactionToServer(remoteObj,t);
        boolean locksStatus = acquireLocksOnTransaction(remoteObj,t);
        if(locksStatus){
                DataSiteRemoteImplementation d = new DataSiteRemoteImplementation();
                boolean val = d.executeTransaction(t);
                if(val){
                    remoteObj.releaseLocks(t);
                } else {
                    remoteObj.releaseLocks(t);
                    remoteObj.addIntoFailedList(t);
                }
        }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
        }

    }
    public Transaction sendTransactionToServer(CentralSiteRemoteInterface obj, Transaction t) {
        try {
            t = obj.receiveTransactionFromDataSite(t);
            return t;
        } catch (RemoteException e) {
            System.err.println("Sending Transaction to central site failed" + e.getMessage());
        }
        return t;
    }
    public boolean acquireLocksOnTransaction(CentralSiteRemoteInterface obj, Transaction t) {
        boolean val = false;
        try {
            val = obj.locksAvailable(t);
        } catch (RemoteException e) {
            System.err.println("Acquiring locks failed: " + e.getMessage());
        }
        System.out.println("Datasite " + t.getDataSiteId() + " got the locks for transaction " + t.getTransactionId() + " : " + val);
        return val;
    }

    @Override
    public void run() {
        centralSiteConnection(curTransaction);
    }

    public static void main(String[] args) {
        //get the input transaction file transaction.txt
        String fileName = "transaction.txt";
        transactionList = parseInputFile(fileName);
        //after fetching the list of transaction from all datasites,
        // I want the transactions to get assigned with transaction id from central site
        for(int i = 0; i < transactionList.size(); i++) {
            DataProcessor dp = new DataProcessor(transactionList.get(i));
            DataSiteRemoteImplementation remoteObj = new DataSiteRemoteImplementation();
            DataSiteRemoteInterface stub = null;
            Registry registry = null;
            try {
                stub = (DataSiteRemoteInterface) UnicastRemoteObject.exportObject(remoteObj, 0);
                registry = LocateRegistry.createRegistry(i+50);
                registry.bind("DataSiteServer" + i, stub);
                ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                executorService.scheduleAtFixedRate(() -> {
                    try {
                        remoteObj.sayHello();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 0, 100, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            System.out.println(" Data Server"+ i + " ready");
            Thread myThread = new Thread(dp);
            myThread.start();
        }

    }
}
