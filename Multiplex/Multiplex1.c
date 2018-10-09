/*
	Multiplex1.c - One implementation of a potential solution to the 'Multiplex' problem.
	Using pthreads to simulate concurrent execution there is one 'critical section' which must
	enforce exclusive access to a limited number of threads.

	Each thread has an ID, arrival time in seconds, and duration in seconds. These attributes must be specified in a text file
	provided on the command line along with the limit of the number of threads to be allowed in the critical section.	
	Invoke the program as follows: Multiplex1 <file-name> <limit integer>
	
	<file-name> should have the number of threads to simulate on the first line and on every subsequent line
	specify the attributes of each thread separated by commas, for example:
	
	3
	1,8,15
	2,12,15
	3,12,10
	
	There are 3 threads, the first has ID 1, arrival time 8, and duration 15.
	
	Limit cannot be negative or zero.
	
	Author: Justin Underhay
*/


#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>


//The threadStruct will hold the attributes for each thread
struct threadStruct {
	int ID;
	int arrivalTime;
	int duration;
	double idle;
};	


pthread_mutex_t entryLock;
pthread_cond_t entryCond;

/*
num is the number of threads currently in the critical section.
limit is the upper bound on the number of threads allowed in the critical section at once.
*/
int num;
int limit;	



/*
Once created a thread will begin executing here sleeping until it 'arrives' then attempting to gain acces to the critical section.
If the limit has been reached the thread will block on a condition variable until it is signaled by a thread exiting the critical section. 
Due to unknowns in thread scheduling and wakeup policy, multiple threads blocked on the condition may not be woken in the order they arrived.
*/
void *thread_entry(void *info) {
	struct threadStruct *newThread = (struct threadStruct*)info;
	
	sleep(newThread->arrivalTime);
	clock_t start = clock();
	
	printf("Thread %d has arrived\n",newThread->ID);
	
	pthread_mutex_lock(&entryLock);
	if (num >= limit)
		pthread_cond_wait(&entryCond, &entryLock);
	
	newThread->idle = (double) (clock() - start);
	
	num++;
	printf("Thread %d now in the critical section\n",newThread->ID);
	pthread_mutex_unlock(&entryLock);
	
	sleep(newThread->duration);	//Represents duration in the critical section
	
	pthread_mutex_lock(&entryLock);
	num--;
	printf("Thread %d has left the critical section\n",newThread->ID);
	
	pthread_mutex_unlock(&entryLock);
	pthread_cond_signal(&entryCond);
}	


/*
Main function which reads file input, sets thread attributes and starts each thread then waits for each to terminate.
*/
int main(int argc, char *argv[]) {
	
	//Check for proper program invocation
	if (argc != 3) {
		printf("Invalid invocation, usage: Multiplex1 <file-name> <limit integer>");
		exit(EXIT_FAILURE);
	}
	
	//Initialize lock and condition variable
	if (pthread_mutex_init(&entryLock, NULL) != 0) {
		printf("Mutex lock creation failure, exiting...");
		exit(EXIT_FAILURE);
	}	
	
	if (pthread_cond_init(&entryCond, NULL) != 0) {
		printf("Condition variable creation failure, exiting...");
		exit(EXIT_FAILURE);
	}	
	
	//Get and set limit integer
	limit = atoi(argv[2]);
	
	if (limit < 1) {
		printf("Invalid limit specified, must be > 0");
		exit(EXIT_FAILURE);
	}	
	
	num = 0;
	
	//Following sections read the input file, assign thread attributes, create and then wait for threads to terminate 
	char buffer[15];
	char *pos = buffer;
	char *conv = buffer;
	
	FILE *fp = fopen(argv[1], "r");
	
	fgets(buffer, 5, fp);
	
	int numThreads = atoi(buffer);
	
	if (numThreads < 0) {
		printf("Illegal number of threads specified, must be positive.");
		exit(EXIT_FAILURE);
	}	
	
	pthread_t tID[numThreads];
	
	struct threadStruct *threads[numThreads];
	
	int x;
	
	for (x=0; x<numThreads; x++) {
		threads[x] = (struct threadStruct*)malloc(sizeof(struct threadStruct));
		
		fgets(buffer, 15, fp);
		
		pos = buffer;
		conv = buffer;
	
		while (*pos != ',')
			pos++;
		*pos = '\0';

		threads[x]->ID = atoi(conv);
		
		while (*pos != ',')
			pos++;
		*pos = '\0';
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		threads[x]->arrivalTime = atoi(conv);
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		threads[x]->duration = atoi(conv);
	}	
	
	fclose(fp);
	
	
	for (x=0; x<numThreads; x++) {
		if (pthread_create(&tID[x],NULL,thread_entry,threads[x]) != 0) {
			printf("Thread creation error, quitting...");
			exit(EXIT_FAILURE);
		}	
	}	
	
	for (x=0; x<numThreads; x++) {
		if (pthread_join(tID[x],NULL) != 0) {
				printf("Error in thread wait termination, ending simulation\n");
				exit(EXIT_FAILURE);	
		}
	}
	
	double sum = 0;
	for (x=0; x<numThreads; x++) {
		printf("\nThread %d idle time: %f\n",threads[x]->ID,threads[x]->idle/CLOCKS_PER_SEC);
		sum += threads[x]->idle/CLOCKS_PER_SEC;
	}

	printf("\nAverage Idle time: %f",sum/numThreads);	

	return 0;
}