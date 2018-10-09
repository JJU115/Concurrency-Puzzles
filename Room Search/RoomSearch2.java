/*
	RoomSearch2.java - A second implementation of a potential solution to the 'Room Search' problem.
	
	The style of solution is identical to RoomSearch1.c but implemented in a different language.
	
	As with RoomSearch1.c an input file containing student attributes must be provided but with all
	attributes separated by spaces. An integer for the interval the dean checks the room must also be provided.
	
	Invoke as: java RoomSearch2 <student_file> <dean_interval_integer>

*/

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;


public class RoomSearch2 {
	
	private final ReentrantLock roomLock = new ReentrantLock(true);
	private final Condition roomCond = roomLock.newCondition();
	private final Condition dCond = roomLock.newCondition();
	private boolean dean_in;
	private int size;
	private boolean end;
	
	
	/*
	To initialize the problem's global variables
	*/
	public RoomSearch2() {
		dean_in = false;
		end = false;
		size = 0;
	}

	
	//Main method calls here where all threads are initialized and have their attributes set
	public void init(int[] i, int[] a, int[] d, int d_i) {
		
		Student[] students = new Student[i.length];
		
		//Create all student threads
		for (int j=0; j<i.length; j++) 
			students[j] = new Student(i[j],a[j],d[j]);
			
		//Create dean thread	
		Dean de = new Dean(d_i);
		de.start();

		//Start all threads
		for (int j=0; j<i.length; j++) 
			students[j].start();
	
		try {
			for (int j=0; j<i.length; j++) 
				students[j].join();
		} catch (InterruptedException e) {}	
		
		end = true;
		
		double sum = 0;
		for (int j=0; j<i.length; j++) {  
			System.out.println("Thread " + students[j].ID + " idle time: " + students[j].idle/1000);
			sum += students[j].idle/1000;
		}

		System.out.println("Average wait time: " + sum/students.length);	
			

	}
	
	
	class Student extends Thread {
		
		private int ID;
		private int arrival;
		private int duration;
		private double idle;
		
		
		public Student(int i, int a, int d) {
			this.ID = i;
			this.arrival = a;
			this.duration = d;
		}

		
		/*
		Student thread entry point. Functions identically to student threads in RoomSearch1.c
		*/
		public void run() {
			
			try {
				Thread.sleep(1000*arrival);
				long start = System.currentTimeMillis();
				
				System.out.println("Student " + ID + " has arrived");
			
				roomLock.lock();
				
				if (dean_in)
					roomCond.await();
				
				idle = (double) (System.currentTimeMillis() - start);
				System.out.println("Student " + ID + " now entering room");
				
				size++;
				roomLock.unlock();
				
				try {
					Thread.sleep(1000*duration);
				} catch (InterruptedException e) {}

				roomLock.lock();
				size--;
				
				System.out.println("Student " + ID + " has left");

				if (size == 0)
					dCond.signal();

				roomLock.unlock();
			} catch (InterruptedException e) {}	
		}		
	}


	class Dean extends Thread {
		
		private int interval;
		
		
		public Dean(int i) {
			this.interval = i;
		}	
		
		
		/*
		Dean thread entry point. Functions identically to dean thread in RoomSearch1.c
		*/
		public void run() {
			
			while (!end) {
				try {
					Thread.sleep(1000*interval);
					
					System.out.println("Dean has arrived");
			
					roomLock.lock();
					
					if (size == 0 || size > 3) {
						System.out.println("Dean entering room");
						dean_in = true;
						
						if (size != 0)
							dCond.await();
						
						dean_in = false;
						System.out.println("Dean leaving room");
						roomCond.signalAll();
					}
					
					System.out.println("Dean is leaving");

					roomLock.unlock();
				} catch (InterruptedException e) {}		
			}	
		}	
	}
	
	
	/*
	Main method reads input file and sends data to create threads.
	*/
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.out.println("Invalid invocation, usage: java RoomSearch2 <input-file> <integer>");
			System.exit(-1);
		}
		
		if (Integer.parseInt(args[1]) < 1) {
			System.out.println("Invalid dean interval specified, must be greater than 0");
			System.exit(-1);
		}	

		Scanner fileReader;
		
		try {
			fileReader = new Scanner(new File(args[0]));
			
			int numThreads = fileReader.nextInt();
			int[] IDs = new int[numThreads]; 
			int[] arrivals = new int[numThreads];
			int[] durations = new int[numThreads];
			
			for (int i=0; i<numThreads; i++) {
				IDs[i] = fileReader.nextInt();
				arrivals[i] = fileReader.nextInt();	
				durations[i] = fileReader.nextInt();		
			}
			
			RoomSearch2 ex = new RoomSearch2();
			ex.init(IDs, arrivals, durations, Integer.parseInt(args[1]));
			
		} catch (FileNotFoundException e) {
			System.out.println("Error, could not find input file(s).");
			System.exit(-1);
		}	
	}	
}