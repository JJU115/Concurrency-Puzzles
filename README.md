# Concurrency-Puzzles
Implementations of potential solutions to various synchronization/concurrency puzzles taken from The Little Book of Semaphores by Allen B. Downey

Each problem has 2 potential implemented solutions in either the C language using POSIX pthreads or the Java language using an extension of the Thread class.

Each problem allows the user to specify the number of threads to execute in parallel along with their characteristics which must be provided in an input file. Each problem has 2 different test files to use as demonstrations. Some problems require additional parameters to be specified at program execution. 

<b>IMPORTANT: C programs will require comma separated value files, Java programs will require space separated value files.</b>
Execute programs at the command line.

A description of problems chosen along with input requirements follows:

<h2>Multiplex</h2>
<p>
Given concurrently executing multiple threads and a single 'critical section', allow a certain number of threads to be active in the     defined critical section at once, enforcing an upper limit at all times.

<b>Input:</b> 1st Arg - File containing number of threads to simulate and on each subsequent line the ID, arrival time in seconds, and duration time in critical section in seconds of each thread.<br> 
2nd Arg - A positive integer which will be the max number of threads allowed in the critical section at once.

See provided test files for examples of valid input files.

<b>Invocation:</b> <Multiplex1|Multiplex2> <input_file> <limit_integer>
</p>

<br>

<h2>Search-Insert-Delete</h2>
<p>
This one is from Andrews’s Concurrent Programming.
Three kinds of threads share access to a singly-linked list:
searchers, inserters and deleters. Searchers merely examine the list;
hence they can execute concurrently with each other. Inserters add
new items to the end of the list; insertions must be mutually exclusive
to preclude two inserters from inserting new items at about
the same time. However, one insert can proceed in parallel with
any number of searches. Finally, deleters remove items from anywhere
in the list. At most one deleter process can access the list at
a time, and deletion must also be mutually exclusive with searches
and insertions. 
  
<b>Input:</b> 1st Arg - File containing number of threads to simulate and on each subsequent line the ID, thread type as a single char (search -> 'S', insert -> 'I', delete -> 'D'), and value to search for/insert/delete of each thread.<br>

Java file uses TestThreads.txt and TestThreads4.txt
C file uses TestThreads3.txt and TestThreads2.txt
TestThreads.txt and TestThreads3.txt are equivalent as are TestThreads4.txt and TestThreads2.txt

<b>Invocation:</b> SID1 <input_file> or java SID2 <input_file>
</p>  

<br>

<h2>Unisex Bathroom</h2>
<p>
There are two types of threads: Male and Female, which must share access to a unisex bathroom under the following conditions:
	
		• There cannot be men and women in the bathroom at the same time.
		• There should never be more than three employees squandering company time in the bathroom.
    
<b>Input:</b> 1st Arg - File containing number of threads to simulate and on each subsequent line the ID, thread type as a single char (male -> 'M', female -> 'F'), arrival time in seconds, and duration time in bathroom in seconds of each thread.<br>   

C file uses TestThreads.txt and TestThreads2.txt
Java file uses TestThreads3.txt and TestThreads4.txt
TestThreads.txt and TestThreads3.txt are equivalent as are TestThreads4.txt and TestThreads2.txt

<b>Invocation:</b> Unisex <input_file> or java Unisex <input_file>
</p>
<br>

<h2>Senate Bus</h2>
<p>
This problem was originally based on the Senate bus at Wellesley College. Riders
come to a bus stop and wait for a bus. When the bus arrives, all the waiting
riders invoke boardBus, but anyone who arrives while the bus is boarding has
to wait for the next bus. The capacity of the bus is 50 people; if there are more
than 50 people waiting, some will have to wait for the next bus.
When all the waiting riders have boarded, the bus can invoke depart. If the
bus arrives when there are no riders, it should depart immediately.
  
<b>Input:</b> 1st Arg - File containing number of passenger threads to simulate and on each subsequent line the ID and arrival time in seconds of each thread.<br>
2nd Arg - File containing number of bus threads to simulate and on each subsequent line the ID and arrival time in seconds of each thread.<br>

C file uses TestThreads.txt and TestThreads2.txt
Java file uses TestThreads3.txt and TestThreads4.txt
TestThreads.txt and TestThreads3.txt are equivalent as are TestThreads4.txt and TestThreads2.txt

<b>Invocation:</b> SenateBus1 <passenger_file> <bus_file> or java SenateBus2 <passenger_file> <bus_file>
</p>  
<br>
