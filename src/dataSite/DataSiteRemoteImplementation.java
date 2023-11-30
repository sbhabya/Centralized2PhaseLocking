package dataSite;

import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class DataSiteRemoteImplementation implements  DataSiteRemoteInterface{
    public String sayHello() throws RemoteException {
        return "Hello, this is the server!";
    }
    public  Connection createConnection(String url) throws ClassNotFoundException, SQLException {
        // Establish a connection
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection(url);
        return connection;
    }

    public ArrayList<Integer> getListOfDataSitesId(int curdt) {
        ArrayList<Integer> dtlist = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            if(i!=curdt){
                dtlist.add(i);
            }
        }
        return dtlist;
    }
    public void updateDataSites(Connection con, ArrayList<String> elementNeedsUpdate, Hashtable<String,Integer> lookupTable) throws SQLException {
        String updateQuery = "UPDATE items SET value = ? WHERE name = ?";
        for(int j = 0; j < elementNeedsUpdate.size();j++){
            int val = lookupTable.get(elementNeedsUpdate.get(j));
            PreparedStatement preparedStatement = con.prepareStatement(updateQuery);
            preparedStatement.setInt(1, val);
            preparedStatement.setString(2,elementNeedsUpdate.get(j));
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }
    @Override
    public Boolean executeTransaction(Transaction t) throws RemoteException {
        try {
            String url = "jdbc:sqlite:database"+t.getDataSiteId()+".sqlite";
            Connection con = createConnection(url);
            int i = 0;
            Hashtable<String, Integer> lookupTable = new Hashtable<String, Integer>();
            ArrayList<String> elementNeedsUpdate = new ArrayList<>();
            while (i!=t.getOperations().size()){
                Operation op = t.getOperations().get(i);
                Statement statement = con.createStatement();
                if(op.getOperationType().equals("read")){
                    String query = "SELECT * from items where name = \"" + op.getItem()+"\"";
                    ResultSet resultSet = statement.executeQuery(query);
                    while (resultSet.next()) {
                        // Retrieve column values for each row
                        String item = resultSet.getString("name");
                        int value = resultSet.getInt("value");
                        lookupTable.put(item,value);
                        System.out.println("Name of item read" + item);
                        System.out.println("Name of item read" + value);
                    }
                } else if(op.getOperationType().equalsIgnoreCase("modify")){
                    String[] cmd = op.getOpCmd().split(" ");
                    int op1i, op2i;
                    String op1 = cmd[2];
                    String op2 = cmd[4];
                    String opType = cmd[3];
                    if(lookupTable.containsKey(op1)){
                        op1i = lookupTable.get(op1);
                    } else {
                        op1i = Integer.parseInt(op1);
                    }
                    if(lookupTable.containsKey(op2)) {
                        op2i = lookupTable.get(op2);
                    } else {
                        op2i = Integer.parseInt(op2);
                    }
                    int res;
                    switch (opType) {
                        case "+":
                            res = op1i + op2i;
                            lookupTable.put(cmd[0],res);
                            break;
                        case "-":
                            res = op1i - op2i;
                            lookupTable.put(cmd[0],res);
                            break;
                        case "*":
                            res = op1i * op2i;
                            lookupTable.put(cmd[0],res);
                            break;
                        case "/":
                            res = op1i / op2i;
                            lookupTable.put(cmd[0],res);
                            break;

                        default:
                            System.out.println("Undefined operator");
                    }

                } else if(op.getOperationType().equals("write")){
                    elementNeedsUpdate.add(op.getItem());
                } else if(op.getOperationType().equals("commit")){
                    ArrayList<Integer> listOfDataSites = getListOfDataSitesId(t.getDataSiteId());
                    for(int d = 0; d< listOfDataSites.size(); d++) {
                        String urld = "jdbc:sqlite:database" + listOfDataSites.get(d) + ".sqlite";
                        Connection dcon = createConnection(urld);
                        updateDataSites(dcon,elementNeedsUpdate,lookupTable);
                        dcon.close();
                    }
                }
                i++;
                statement.close();
            }
            con.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return true;
    }

}
