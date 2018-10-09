/*
	MultiUser1.java - One implementation of a potential solution to the 'MultiUser' problem described as follows:
	
	In a mock distributed computing environment, multiple users attempt to acquire two resources: memory and processes.
	Some users require more than others, some require very little, some require a lot.
	As long as some resources are available a new user is allowed to enter provided they don't need more than what is available.
	
	Users are represented by threads each requiring some memory and processes. A text file containing the number of threads
	and their specific attributes separated by spaces must be provided on the command line along with the total amount of memory and processes to have
	available to users. For example:
	
	2
	1 7 4 9 5
	2 4 9 4 7
	
	This specifies 2 users. The first has ID=1, arrival time in seconds=7, duration in seconds=4, memory required=9, processes required=5
	and so on for the 2nd thread.
	
	Invoke as follows: java MultiUser1 <User_files> <total_memory> <total_processes>
	
	Author: Justin Underhay
*/

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;


public class MultiUser1 {
	
	private int available_memory;
	private int available_processes;
	private final ReentrantLock resourceLock = new ReentrantLock();
	private final Condition resourceCond = resourceLock.newCondition();

	
	/*
	To initialize the problem's global variables
	*/
	public MultiUser1(int am, int ap) {
		available_memory = am;
		available_processes = ap;
	}
	
	
	//Main method calls here where all threads are initialized and have their attributes set
	public void init(int[][] a) {
		
		//Create and start all threads
		User[] users = new User[a.length];
		
		for (int i=0; i<a.length; i++)
			users[i] = new User(a[i]);
		
		for (int i=0; i<a.length; i++) 
			users[i].start();
		
		try {
			for (int i=0; i<a.length; i++) 
				users[i].join();
		} catch (InterruptedException e) {}	
		
		double sum = 0;
		for (int i=0; i<a.length; i++) { 
			System.out.println("User " + users[i].attributes[0] + " idle time: " + users[i].idle/1000);
			sum += users[i].idle/1000;
		}

		System.out.println("\n\nAverage idle time: " + sum/a.length);	
		
	}	
	
	
	class User extends Thread {
	
		//In order: ID, arrival, duration, memory needed, processes needed
		private int[] attributes = new int[5];
		private double idle;
		
		
		public User(int[] a) {
			for (int i=0; i<5; i++)
				this.attributes[i] = a[i];
		}	
		
		
		/*
		User thread entry point. Users enter and take up resources. If a user arrives and not enough resources are available it blocks.
		Exiting threads release resources and signal all waiting users who check if they can enter. Since all threads are woken it cannot
		be predicted which thread will gain access, only that if a thread does get through there are enough resources for it.	
		*/
		public void run() {
			
			try {
				Thread.sleep(1000*attributes[1]);
				long start = System.currentTimeMillis();
				resourceLock.lock();
				
				System.out.println("User " + attributes[0] + " has arrived");
			
				while (available_memory < attributes[3] || available_processes < attributes[4])
					resourceCond.await();
				
				idle = (double) (System.currentTimeMillis() - start);
				available_memory -= attributes[3];
				available_processes -= attributes[4];
				
				System.out.println("User " + attributes[0] + " has been allocated resources. Remaining memory: " + available_memory + " - Remaining processes: " + available_processes);
				
				resourceLock.unlock();
				
				Thread.sleep(1000*attributes[2]);
				
				resourceLock.lock();
				
				available_memory += attributes[3];
				available_processes += attributes[4];
				
				System.out.println("User " + attributes[0] + " has finished. Remaining memory: " + available_memory + " - Remaining processes: " + available_processes);
				
				resourceCond.signalAll();
				
				resourceLock.unlock();
				
			} catch (InterruptedException e) {}
			
		}	
	}	
	
	
	/*
	Main method reads input file and sends data to create threads.
	*/
	public static void main(String[] args) {
		
		if (args.length != 3) {
			System.out.println("Invalid invocation, usage: java MultiUser1 <input-file> <total_memory> <total_processes>");
			System.exit(-1);
		}

		Scanner fileReader;
		
		try {
			fileReader = new Scanner(new File(args[0]));
			
			int numThreads = fileReader.nextInt();
			int[][] attributes = new int[numThreads][5];
			
			for (int i=0; i<numThreads; i++)
				for (int j=0; j<5; j++) 
					attributes[i][j] = fileReader.nextInt();	
			
			
			MultiUser1 ex = new MultiUser1(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			ex.init(attributes);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error, could not find input file(s).");
			System.exit(-1);
		}	
	}	
}