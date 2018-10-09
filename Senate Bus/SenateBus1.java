/*
	SenateBus1.java - One implementation of a potential solution to the 'Senate Bus' Problem described as follows:
	
	Riders come to a bus stop and wait for a bus. When the bus arrives, all the waiting
	riders board the bus, but anyone who arrives while the bus is boarding has
	to wait for the next bus. The capacity of the bus is 50 people; if there are more
	than 50 people waiting, some will have to wait for the next bus.
	When all the waiting riders have boarded, the bus can depart. If the
	bus arrives when there are no riders, it should depart immediately.
	
	Riders and buses are modeled by different threads represented by the Passenger and Bus classes which carry their 
	unique attirbutes. Passengers have an ID and arrival time in seconds, buses have an ID and arrival time in seconds.
	
	Two files must be provided on the command line, the first listing the passenger threads and the second listing the
	bus threads. A passenger file might look like:
	
	2
	1 3 
	2 4
	
	which specifies 2 threads of ID 1 and 2 respectively with arrival time in seconds as 3 and 4 respectively.

	The bus file is identical in format, the number of threads on the first line and on every subsequent line the
	attributes of that thread. Invoke the program as follows: java SenateBus1 <passenger_file> <bus_file>.

	Author: Justin Underhay	
*/


import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;


public class SenateBus1 {
	
	private int ticket;
	boolean boarding;
	private final ReentrantLock R = new ReentrantLock();
	private final Condition entry = R.newCondition();
	private final Condition wait = R.newCondition();
	private final Condition busWait = R.newCondition();
	private final Condition nextBus = R.newCondition();
	
	
	/*
	To initialize the problem's global variables
	*/
	public SenateBus1() {
		ticket = 0;
		boarding = false;
		
	}

	//Main method calls here where all threads are initialized and have their attributes set
	public void init(int[] p1, int[] p2, int[] b1, int[] b2) {
		Passenger[] p = new Passenger[p1.length];
		Bus[] b = new Bus[b1.length];
		
		//Create and start all passenger and bus threads
		for (int j=0; j<p.length; j++) 
			p[j] = new Passenger(p1[j], p2[j]);
		
		for (int j=0; j<b.length; j++) 
			b[j] = new Bus(b1[j], b2[j]);

		for (int k=0; k<p.length; k++) 
			p[k].start();
		
		for (int k=0; k<b.length; k++) 
			b[k].start();
		
		try {
			for (int k=0; k<p.length; k++) 
				p[k].join();
		} catch (InterruptedException e) {}
		
		double sum = 0;
		for (int k=0; k<p.length; k++) { 
			System.out.println("\nPassenger " + p[k].ID + " idle time: " + p[k].idle/1000);
			sum += p[k].idle/1000;
		}

		System.out.println("Average idle time: " + sum/p.length);	
	}
	
	
	//To represent each passenger
	class Passenger extends Thread {
		
		private int ID;
		private int arrival;
		private double idle;
		
		
		public Passenger(int i, int a) {
			this.ID = i;
			this.arrival = a;
		}

		
		/*
		Passenger thread entry point. When a thread arrives it is assigned a 'ticket' number which starts at 0.
		If the current ticket value is above 49, that is 50 passengers have already arrived, it will block
		on a condition until those 50 ahead have left. Passengers waiting to board will wait for the bus to signal them
		then board. The last passenger to board will signal the bus to leave.
		*/
		public void run() {
			try {
				
				Thread.sleep(1000*arrival);
				long start = System.currentTimeMillis();
				
				System.out.println("Passenger " + ID + " has arrived");
			
				R.lock();
			
				if (boarding || ticket > 49)
					entry.await();
				
				ticket++;
				
				while (!boarding)
					wait.await();
				
				idle = (double) (System.currentTimeMillis() - start);
				System.out.println("Passenger " + ID + " now boarding");
				ticket--;
				
				if (ticket == 0) 
					busWait.signal();
					
				R.unlock();
					
			} catch (InterruptedException e) {}
		}		
	}	
	
	
	/*
	Bus thread entry point. An arriving bus will signal to passenger threads to board and wait until the last 
	passenger to board signals it. Of course the bus leaves immediately if no passengers are waiting.	
	*/
	class Bus extends Thread {
	
		private int ID;
		private int arrival;
		
		
		public Bus(int i, int a) {
			this.ID = i;
			this.arrival = a;
		}

		
		public void run() {
			try {
				
				Thread.sleep(1000*arrival);
				System.out.println("Bus " + ID + " has arrived");
			
				R.lock();
			
				if (boarding == true)
					nextBus.await();
				
				if (ticket == 0) {
					R.unlock();
					System.out.println("Bus " + ID + " leaving with no passengers");
					System.exit(1);
				}

				boarding = true;
				System.out.println("Bus " + ID + " now boarding");

				wait.signalAll();
				busWait.await();

				ticket = 0;
				boarding = false;
				System.out.println("Bus " + ID + " now leaving");
				
				nextBus.signal();
				entry.signalAll();		
				
				R.unlock();
			} catch (InterruptedException e) {}	
		}	
	}	
	

	/*
	Main method reads input file and sends data to create threads.
	*/
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.out.println("Must provide input files on command line: java SenateBus1 <passenger_file> <bus_file>");
			System.exit(-1);
		}		
		
		Scanner fileReader;
		
		try {
			fileReader = new Scanner(new File(args[0]));
			
			int numThreads = fileReader.nextInt();
			int[] IDs = new int[numThreads]; 
			int[] arrivals = new int[numThreads];
			
			for (int i=0; i<numThreads; i++) {
				IDs[i] = fileReader.nextInt();
				arrivals[i] = fileReader.nextInt();				
			}

			fileReader = new Scanner(new File(args[1]));			
			
			numThreads = fileReader.nextInt();
			int[] bIDs = new int[numThreads]; 
			int[] bArrivals = new int[numThreads];
			
			for (int i=0; i<numThreads; i++) {
				bIDs[i] = fileReader.nextInt();
				bArrivals[i] = fileReader.nextInt();	
			}
			
			SenateBus1 ex = new SenateBus1();
			ex.init(IDs, arrivals, bIDs, bArrivals);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error, could not find input file(s).");
			System.exit(-1);
		}

	}
}