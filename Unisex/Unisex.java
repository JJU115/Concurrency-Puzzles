/*
	Unisex.java - One implementation of a potential solution to the 'Unisex Bathroom' problem.
	There are two types of threads: Male and Female, which must share access to a unisex bathroom under the following conditions:
	
		• There cannot be men and women in the bathroom at the same time.
		• There should never be more than three employees squandering company time in the bathroom.
		
	The solution allows either Males or Females to 'dominate' the bathroom based on which thread gets to it first.
	If there are no threads in the bathroom then the first thread there 'claims' it allowing threads of the same gender
	to enter freely while observing the limit on size and preventing threads of the opposite gender from entering.
	This will continue until the bathroom is empty allowing the other gender to 'claim' it.

	Every thread has an ID, gender as a single char of 'M' or 'F', arrival time in seconds, and duration in seconds.
	These must be specified in an input file separated by spaces with the number of threads to simulate. For example:
	
	3
	1 M 3 4	
	2 F 3 6	
	3 M 5 7
	
	This specifies 3 threads. The 1st has ID=1, gender=Male, arrival=3, duration=4
	
	Author: Justin Underhay
*/


import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;


public class Unisex {
	
	private char ownedBy;
	private int size;
	private final ReentrantLock dLock = new ReentrantLock(true);
	private final Condition dCond = dLock.newCondition();
	
	
	/*
	To initialize the problem's global variables
	*/
	public Unisex() {
		
		ownedBy = 'F';
		size = 0;
		
	}

	
	//Main method calls here where all threads are initialized and have their attributes set
	public void init(int[] IDs, char[] genders, int[] arrivals, int[] durations) {
		EmpThread[] threads = new EmpThread[IDs.length];
		
		//Create threads 
		for (int j=0; j<threads.length; j++)
			threads[j] = new EmpThread(IDs[j],genders[j],arrivals[j],durations[j]);
		
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

	
	//Each thread uses this class
	class EmpThread	extends Thread {
		
		private int ID;
		private char gender;
		private int arrival;
		private int duration;
		private double idle;
		
		
		public EmpThread(int i, char g, int a, int d) {
			this.ID = i;
			this.gender = g;
			this.arrival = a;
			this.duration = d;
		}	
		

		/*
		Thread entry point. The 'ownedBy' variable determines which gender has control of the bathroom.
		*/
		public void run() {
			
			try {
				Thread.sleep(arrival*1000);
			} catch (InterruptedException e) {}	
			
			long start = System.currentTimeMillis();
			if (gender == 'M') {
				
				dLock.lock();
				while ((size >= 3 || ownedBy == 'F') && size != 0)
					try {
						dCond.await();
					} catch (InterruptedException e) {}	
				
				idle = (double) (System.currentTimeMillis() - start);
				ownedBy = 'M'; 
				size++;
				System.out.println("Male thread " + ID + " entering bathroom");
			} else {

				dLock.lock();
				while ((size >= 3 || ownedBy == 'M') && size != 0)
					try {
						dCond.await();
					} catch (InterruptedException e) {}
				
				idle = (double) (System.currentTimeMillis() - start);
				ownedBy = 'F'; 
				size++;
				System.out.println("Female thread " + ID + " entering bathroom");
			}	
				
				dLock.unlock();
				
				try {
					Thread.sleep(duration*1000);
				} catch (InterruptedException e) {}	
				
				dLock.lock();
				size--;
				System.out.println("Thread " + ID + " has left the bathroom");
				dCond.signalAll();
				dLock.unlock();
			
		}
		
		
		
	}


	/*
	Main method reads input file and sends data to create threads.
	*/
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.out.println("Must provide input file on command line: java Unisex <input_file>");
			System.exit(-1);
		}		
		
		Scanner fileReader;
		
		try {
			fileReader = new Scanner(new File(args[0]));
			
			int numThreads = fileReader.nextInt();
			int[] IDs = new int[numThreads]; 
			char[] genders = new char[numThreads];
			int[] arrivals = new int[numThreads];
			int[] durations = new int[numThreads];
			
			for (int i=0; i<numThreads; i++) {
				IDs[i] = fileReader.nextInt();
				genders[i] = (fileReader.next()).charAt(0);
				arrivals[i] = fileReader.nextInt();
				durations[i] = fileReader.nextInt();				
			}	
			
			Unisex ex = new Unisex();
			ex.init(IDs,genders,arrivals,durations);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error, could not find input file.");
			System.exit(-1);
		}
	}
}