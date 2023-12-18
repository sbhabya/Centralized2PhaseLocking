# Centralized 2-Phase Locking 
This project implements a centralized two-phase locking algorithm. The main idea is to ensure that a data item shared by conflicting operations is accessed by one operation at a time. In centralized 2PL algorithm, this is achieved by associating a lock on the data unit. The 2 Phase Locking rule states that no transaction should request a lock after it releases one of its locks. 2PL algorithm executes transaction in two phases. Each transaction has a growing phase where it obtains locks and a shrinking phase, during which it releases locks. One way of implementing the 2PL algorithm for distributed databases is to delegate lock management responsibility to a single site, and this is also known as **Centralized Two-Phase locking** algorithm.
For this project we will have four fully replicated database/data sites and one central site where the central site will manage the lock requests from data sites concurrently and will also detect and handle deadlocks. 

This project is implemented in **Java** and uses **Java RMI** for the communication between central and data sites.

## Design, Methods, and Architecture

* The system has one central site and 4 data sites. All the transactions received by data sites are sent concurrently to the central site to get locks on the data items involved in the transaction.
* Central site has a locks table which records the read and write locks associated to a data item. Central site checks the locks table and gives the locks if available to the data site for the transaction.
* If data sites receive all the locks required to execute the transaction, it executes the transaction and then send the updates to all the other data sites to maintain a consistent view of the system.
* Once the updates on the other data sites are completed, data site releases all its locks that it has acquired for the transaction.
* All the transaction which couldn’t acquire locks goes to the failed transaction list at the Central site.
* Central site keeps checking the failed list and if it gains all the locks, it sends the locks to the data site for the execution of the transaction.
* Wait for graph is used for deadlock detection, after every few intervals we check for a cyclic dependency in the graph, and if it exists, we abort the transactions involved in the cycles and release all the locks. Aborted transactions are then executed on first-come-first-served basis. 
