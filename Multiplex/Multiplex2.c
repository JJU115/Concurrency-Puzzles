/*
	Multiplex2.c - 2nd Implementation of a potential solution to the 'Multiplex' problem.
	
	As with Multiplex1.c each thread has the same attributes but with an additional pointer to another threadStruct.
	This solution attempts to have threads enter the critical section in the order they arrived with the use of a queue.
	The HEAD pointer signifies which thread is next to enter whose struct points to the next thread in line and so on.
	
	As with Multiplex1.c a file containing the number of threads to simulate and each thread's attributes must be provided
	on the command line along with the limit integer signifying the upper bound of threads to allow in the section. See
	Multiplex1.c for file format.
	
	Limit cannot be negative or zero.
	
	Author: Justin Underhay
*/



#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>


//The threadStruct will hold the attributes for each thread and the pointer to the next thread in the queue
struct threadStruct {
	int ID;
	int arrivalTime;
	int duration;
	struct threadStruct *NEXT;
	double idle;
};	

//The front of the queue
struct threadStruct *HEAD;

pthread_mutex_t entryLock;
pthread_mutex_t queueLock;
pthread_cond_t entryCond;

/*
num is the number of threads currently in the critical section.
limit is the upper bound on the number of threads allowed in the critical section at once.
*/
int num;
int limit;



/*
This function is not used in the program but can be inserted at any point to see what the queue looks like.
*/
void printQueue() {
	printf("\n\nHEAD\n");
	struct threadStruct *pass = HEAD;
	
	while (pass != NULL) {
		printf("Thread ID: %d",pass->ID);
		if (pass->NEXT != NULL)
			printf(" --> ");
		pass = pass->NEXT;
	}
	printf("\n--------\n");
}	


/*
Every thread after arriving must join the queue by calling this function.
*/
void enterQueue(struct threadStruct *entry) {
	if (HEAD == NULL)
		HEAD = entry;
	else {
		struct threadStruct *pass = HEAD;
		while (pass->NEXT != NULL)
			pass = pass->NEXT;
		pass->NEXT = entry;	
	}	
}	


/*
Threads begin execution here, sleeping until their 'arrival', entering the queue, and waiting until 
they reach the front and are signaled to enter the critical section. The use of a queue ensures threads
are allowed entry in the order they arrived.
*/
void *thread_entry(void *info) {
	struct threadStruct *newThread = (struct threadStruct*)info;
	
	sleep(newThread->arrivalTime);
	
	clock_t start = clock();
	printf("Thread %d has arrived\n",newThread->ID);
	
	pthread_mutex_lock(&queueLock);
	enterQueue(newThread);
	pthread_mutex_unlock(&queueLock);
	
	pthread_mutex_lock(&entryLock);
	
	while (num >= limit || HEAD != newThread)
		pthread_cond_wait(&entryCond, &entryLock);
	
	newThread->idle = (double) (clock() - start);
	num++;
	
	printf("Thread %d now in the critical section\n",newThread->ID);
	
	pthread_mutex_lock(&queueLock);
	
	HEAD = HEAD->NEXT;
	
	pthread_mutex_unlock(&queueLock);
	pthread_mutex_unlock(&entryLock);

	sleep(newThread->duration);	//Represents duration in the critical section
	
	pthread_mutex_lock(&entryLock);
	num--;
	printf("Thread %d has left the critical section\n",newThread->ID);
	pthread_mutex_unlock(&entryLock);

	pthread_cond_broadcast(&entryCond);	//Must broadcast since a single signal may not wake the thread at the front of the queue
}	


/*
Main function which reads file input, sets thread attributes and starts each thread then waits for each to terminate.
*/
int main(int argc, char *argv[]) {
	
	//Check for proper program invocation
	if (argc != 3) {
		printf("Invalid invocation, usage: Multiplex2 <file-name> <limit integer>");
		exit(EXIT_FAILURE);
	}
	
	//Initialize locks and condition
	if (pthread_mutex_init(&entryLock, NULL) != 0 || pthread_mutex_init(&queueLock, NULL) != 0) {
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
	
	//Following sections set global variables, read the input file, assign thread attributes, create and then wait for threads to terminate
	num = 0;
	HEAD = NULL;
	
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
		threads[x]->NEXT = NULL;
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

	printf("\nAverage idle time: %f",sum/numThreads);	

	return 0;
}