/*
	SID2.java - A 2nd implementation of a potential solution to the 'Search-Insert-Delete' problem.
	Threads are specified by the ListThread class which contains all the thread's attributes as in SID1.c.
	
	While SID1.c prioritized deleters and allowed them to bypass waiting searchers and inserters this solution
	utilizes a free-for-all approach where deleters must wait for no threads to be operating on the list to activate.
	No condition variables are used, instead threads check if they may enter and sleep if they can't rechecking periodically.
	
	As with SID1.c a file containing the number of threads and their attributes must be provided on the command line. For java
	programs however the attributes must be separated by spaces rather than commas, for example:
	
	3
	1 S 4
	2 I 8
	3 D 2
	
	This specifies 3 threads where the first has ID=1, type=searcher, and value=4.
	
	Author: Justin Underhay
*/



import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Scanner;
import java.util.Arrays;
import java.io.File;
import java.io.FileNotFoundException;


public class SID2 {
	
	private int size;
	private boolean ins_in;
	private boolean del_in;
	private final ReentrantLock qLock = new ReentrantLock(true);
	private final LinkedList list = new LinkedList();
	
	
	/*
	To initialize the problem's global variables
	*/
	public SID2() {
		size = 0;
		ins_in = del_in = false;
	}
	
	
	//Main method calls here where all threads are initialized and have their attributes set
	public void init(int[] IDs, char[] types, int[] vals) {
		ListThread[] threads = new ListThread[IDs.length];
		
		//Create threads
		for (int j=0; j<threads.length; j++)
			threads[j] = new ListThread(IDs[j],types[j],vals[j]);
		
		//Begin running all threads
		for (int k=0; k<threads.length; k++)
			threads[k].start();
		
		try {
			for (int k=0; k<threads.length; k++)
				threads[k].join();
		} catch (InterruptedException e) {}
		
		double sum = 0;
		for (int k=0; k<threads.length; k++) {
			System.out.println("Thread " + threads[k].ID + " idle time: " + threads[k].idle/1000);
			sum += threads[k].idle/1000;
		}	
		
		System.out.println("Average idle time: " + sum/threads.length);
		
	}	
	
	
	
	//The class that models each thread
	class ListThread extends Thread {
		
		private int ID;
		private int val;
		private char type;
		private double idle;
		
		
		public ListThread(int I, char T, int V) {
			this.ID = I;
			this.val = V;
			this.type = T;
		}	

		/*
		Threads enter here when started. Each sequentially checks the conditions that allow it to enter and sleeps if it cannot
		before checking again.
		*/
		public void run() {
			
			long start = System.currentTimeMillis();
			qLock.lock();
			
			try {
				if (type == 'S') {
					while (del_in)
						Thread.sleep(10);
					
					size++;
					qLock.unlock();
					
					if (list.contains(val))
						System.out.println("Searcher " + ID + " found " + val);
					else
						System.out.println("Searcher " + ID + " did not find " + val);
					
					System.out.println(Arrays.toString(list.toArray()));
					
				} else if (type == 'I') {
					while (ins_in || del_in)
						Thread.sleep(10);
					
					size++;
					ins_in = true;
					qLock.unlock();
					
					list.add(val);
					
					System.out.println("Inserter " + ID + " inserted " + val);
					System.out.println(Arrays.toString(list.toArray()));
					
					ins_in = false;
					
				} else {
					while (size != 0 || del_in)
						Thread.sleep(10);
					
					size++;
					del_in = true;
					qLock.unlock();
					
					if (list.removeFirstOccurrence(val)) 
						System.out.println("Deleter " + ID + " removed " + val);
					else
						System.out.println("Deleter " + ID + " could not find " + val);
					System.out.println(Arrays.toString(list.toArray()));
					del_in = false;
				}
			
			} catch (InterruptedException e) {}
			size--;	
			idle = (double) (System.currentTimeMillis() - start);
		}
	}
	
	
	
	
	/*
	Main method reads input file and sends data to create threads.
	*/
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.out.println("Must provide input file on command line: java SID2 <input_file>");
			System.exit(-1);
		}		
		
		Scanner fileReader;
		
		try {
			fileReader = new Scanner(new File(args[0]));
			
			int numThreads = fileReader.nextInt();
			int[] IDs = new int[numThreads]; 
			int[] vals = new int[numThreads];
			char[] types = new char[numThreads];
			
			for (int i=0; i<numThreads; i++) {
				IDs[i] = fileReader.nextInt();
				types[i] = (fileReader.next()).charAt(0);
				vals[i] = fileReader.nextInt();	
			}	
			
			SID2 ex = new SID2();
			ex.init(IDs,types,vals);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error, could not find input file.");
			System.exit(-1);
		}		
	}	
}