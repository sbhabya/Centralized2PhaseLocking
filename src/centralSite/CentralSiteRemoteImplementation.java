package centralSite;

import dataSite.DataSiteRemoteInterface;
import dataSite.Operation;
import dataSite.Transaction;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class CentralSiteRemoteImplementation implements CentralSiteRemoteInterface{
    static Queue<Transaction> queue = new LinkedList<>();
    static int curTransactionId = 0;
    static Hashtable<Integer,Transaction> transactionOrderTable =  new Hashtable<>();
    static Hashtable<String, Pair> locksTable = new Hashtable<String, Pair>();
    static PriorityQueue<Integer> tOrder = new PriorityQueue<>();
    static ArrayList<Integer> failedTList = new ArrayList<>();

    //graph adjacency list creation
    static ArrayList<Set<Integer>> graph = new ArrayList<>(4);
    public String sayHello() throws RemoteException {
        return "Hello, this is the server!";
    }
    @Override
    public synchronized Transaction receiveTransactionFromDataSite(Transaction t) throws RemoteException {
        transactionOrderTable.put(curTransactionId,t);
        t.setTransactionId(curTransactionId++);
        queue.add(t);
        return t;
    }
    public void addIntoFailedList(Transaction t) {
        failedTList.add(t.getTransactionId());
    }
    public synchronized void releaseLocks(Transaction t) throws RemoteException {
        for(int i = 0; i< t.getOperations().size(); i++) {
            String key = t.getOperations().get(i).getItem();
            if(!key.equals("")){
                Set<Integer> rl = locksTable.get(key).getReadList();
                Set<Integer> wl = locksTable.get(key).getWriteList();
                rl.remove(t.getTransactionId());
                wl.remove(t.getTransactionId());
                Pair p = new Pair(rl,wl);
                locksTable.put(key,p);
            }
        }
        System.out.println("Transaction that released all its locks is " + t.getTransactionId() + "and it was running at datasite" + t.getDataSiteId() );
    }
    @Override
    public boolean locksAvailable(Transaction t) throws RemoteException {
        System.out.println("Transaction that came for locks is " + t.getTransactionId() + " and it is running at datasite" + t.getDataSiteId() );
        boolean check = true;
        int curTId = t.getTransactionId();
            for (int i = 0; i < t.getOperations().size(); i++) {
                Operation op = t.getOperations().get(i);
                if (op.getOperationType().equals("read") || op.getOperationType().equals("write")) {
                    String item = op.getItem();
                    String type = op.getOperationType();
                        if (locksTable.containsKey(item)) {
                            Set<Integer> rl = locksTable.get(item).getReadList();
                            Set<Integer> wl = locksTable.get(item).getWriteList();
                            if (type.equals("read")) {
                                if (wl.size() == 0) {
                                    rl.add(curTId);
                                } else if (wl.size() == 1 && wl.contains(curTId)) {
                                    rl.add(curTId);
                                } else {
                                    check = false;
                                }
                            } else {
                                if (wl.size() == 0 && rl.size() == 0) {
                                    wl.add(curTId);
                                } else if (wl.size() == 0 && rl.size() == 1 && rl.contains(curTId)) {
                                    wl.add(curTId);
                                } else if (wl.size() == 1 && wl.contains(curTId)){
                                    check = true;
                                } else {
                                    check = false;
                                }
                            }
                        } else {
                            if (type.equalsIgnoreCase("read")) {
                                Set<Integer> rl = new HashSet<>();
                                Set<Integer> wl = new HashSet<>();
                                rl.add(curTId);
                                Pair p = new Pair(rl, wl);
                                locksTable.put(item, p);
                            } else {
                                Set<Integer> rl = new HashSet<>();
                                Set<Integer> wl = new HashSet<>();
                                wl.add(curTId);
                                Pair p = new Pair(rl, wl);
                                locksTable.put(item, p);
                            }
                            check = true;
                        }
                    }
                }

        System.out.println("Transaction that got the locks is " + t.getTransactionId() + " and it is running at datasite" + t.getDataSiteId() );
        if(!check){
            tOrder.add(t.getTransactionId());
            failedTList.add(t.getTransactionId());
        }
        return check;
    }
    public synchronized boolean dfsCheck(int node, ArrayList<Set<Integer>> adj, int vis[], int pathVis[],ArrayList<Integer> cycleNodes) {
        vis[node] = 1;
        pathVis[node] = 1;
        ArrayList<Integer> set = new ArrayList<>(adj.get(node));
        for(int it : set) {
            // when the node is not visited
            if(vis[it] == 0) {
                if(dfsCheck(it, adj, vis, pathVis,cycleNodes) == true){
                    cycleNodes.add(it);
                    return true;
                }
            }
            // if the node has been previously visited
            // but it has to be visited on the same path
            else if(pathVis[it] == 1) {
                cycleNodes.add(it);
                return true;
            }
        }

        pathVis[node] = 0;
        return false;
    }

    public synchronized boolean isCyclic(int n, ArrayList<Set<Integer>> adj, ArrayList<Integer> cycleNodes){
        int vis[] = new int[n];
        int pathVis[] = new int[n];

        for(int i = 0;i<n;i++) {
            if(vis[i] == 0) {
                if(dfsCheck(i, adj, vis, pathVis,cycleNodes) == true){
                    cycleNodes.add(i);
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void deadLockDetectionMethod() {
        if(failedTList.size()!=0) {
            System.out.println("There are " + failedTList.size() + " failed transactions");
            for (int i = 0; i < 4; i++) {
                graph.add(new HashSet<>());
            }
            Collections.sort(failedTList);
            Set<String> dataItems = new HashSet<>();
            //code for adjacency list - graph creation using failed sites.
            for (int i = 0; i < failedTList.size(); i++) {
                int tno = failedTList.get(i);
                Transaction t = transactionOrderTable.get(tno);
                for (int o = 0; o < t.getOperations().size(); o++) {
                    String opType = t.getOperations().get(o).getOperationType();
                    String item = t.getOperations().get(o).getItem();
                    if (opType.equals("read")) {
                        if (locksTable.get(item).getReadList().contains(tno)) {
                            continue;
                        } else {
                            dataItems.add(item);
                            //get the write list of item because that can only restrict read access
                            for (int w : locksTable.get(item).getWriteList()) {
                                if (w != tno) {
                                    graph.get(tno).add(w);
                                }
                            }
                        }
                    } else if (opType.equals("write")) {
                        if (locksTable.get(item).getWriteList().contains(tno)) {
                            continue;
                        } else {
                            dataItems.add(item);
                            for (int w : locksTable.get(item).getWriteList()) {
                                if (w != tno) {
                                    graph.get(tno).add(w);
                                }
                            }
                            for (int r : locksTable.get(item).getReadList()) {
                                if (r != tno) {
                                    graph.get(tno).add(r);
                                }
                            }
                        }
                    }
                }
            }

            //check for cycle in the graph
            ArrayList<Integer> cycleNodes = new ArrayList<>();
            boolean checkcycle = isCyclic(4, graph, cycleNodes);
            if (checkcycle) {
                System.out.println("Deadlock Detected");
                for (String d : dataItems) {
                    for (int c = 0; c < cycleNodes.size(); c++) {
                        if (locksTable.get(d).readList.contains(cycleNodes.get(c))) {
                            locksTable.get(d).readList.remove(cycleNodes.get(c));
                        }
                        if (locksTable.get(d).writeList.contains(cycleNodes.get(c))) {
                            locksTable.get(d).writeList.remove(cycleNodes.get(c));
                        }

                    }
                }
            Set<Integer> cycleNodeSet = new HashSet<>(cycleNodes);
            cycleNodes = new ArrayList<>(cycleNodeSet);
            Collections.sort(cycleNodes);
            for (int cn = 0; cn < cycleNodes.size(); cn++) {
                int t = cycleNodes.get(cn);
                int dataServerId = transactionOrderTable.get(t).getDataSiteId();
                boolean checklocks = false;
                try {
                    checklocks = locksAvailable(transactionOrderTable.get(t));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
                if (checklocks) {
                    try {
                        Registry registry = LocateRegistry.getRegistry("localhost", 50 + dataServerId);
                        DataSiteRemoteInterface remoteObj = (DataSiteRemoteInterface) registry.lookup("DataSiteServer" + dataServerId);
                        Boolean exec = remoteObj.executeTransaction(transactionOrderTable.get(t));
                        if (exec) {
                            releaseLocks(transactionOrderTable.get(t));
                            failedTList.remove(0);
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }

                }
            }
        } else {
                System.out.println("Deadlock not detect. Transaction failed because other transaction is currently holding the locks");
                Set<Integer> ss = new HashSet<>(failedTList);
                failedTList = new ArrayList<>(ss);
                for (int i = 0; i < failedTList.size(); i++) {
                    int t = failedTList.get(i);
                    int dataServerId = transactionOrderTable.get(t).getDataSiteId();
                    boolean checklocks = false;
                    while (checklocks!=true){
                        try {
                            checklocks = locksAvailable(transactionOrderTable.get(t));
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    if (checklocks) {
                        try {
                            Registry registry = LocateRegistry.getRegistry("localhost", 50 + dataServerId);
                            DataSiteRemoteInterface remoteObj = (DataSiteRemoteInterface) registry.lookup("DataSiteServer" + dataServerId);
                            Boolean exec = remoteObj.executeTransaction(transactionOrderTable.get(t));
                            if (exec) {
                                releaseLocks(transactionOrderTable.get(i));
                                failedTList.remove(0);
                            }
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }

                    }
                }


        }
            if(failedTList.size() == 0){
                System.out.println("all failed sites got executed");
            }

        } else {
            System.out.println("Currently there are no failed transactions");
        }

    }
}
