/*
	MultiUser2.c - A second implementation of the 'MultiUser' problem as described in MultiUser1.java.
	
	The style of solution used is like MultiUser1.java but with a more structured approach. Users are queued up 
	as they arrive and enter directly if they can. Users that must wait for more resources to become available wait 
	in line until signaled by an exiting thread. When a thread exits a procedure finds the user closest to the front 
	of the queue that meets resource requirements (possibly none) and allows them to enter. Users in front of the selected 
	user consume too many resources and must remain in their original positions in the queue.
	
	As with MultiUser1.java a file specifying the number of users and their attributes (ID, arrival, duration, memory_required, processes_required)
	must be provided on the command line along with the total amount of memory and processes to have available.

	Author: Justin Underhay
*/

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>


//Users are represented by these structs with pointers for use in the program's queue
struct User {
	int ID;
	int arrival;
	int duration;
	int memory_required;
	int processes_required;
	struct User *NEXT;
	struct User *PREV;
	double idle;
};	

//The head of the list HEAD and selected go-ahead user select
struct User *HEAD;
struct User *select;

//Locks and conditions
pthread_mutex_t resourceLock;
pthread_mutex_t queueLock;
pthread_cond_t resourceCond;

int available_memory;
int available_processes;



/*
Not used in this program but can be inserted at any point to see the state of the user queue
*/
void printQueue() {
	printf("\n\nHEAD\n");
	struct User *pass = HEAD;
	
	while (pass != NULL) {
		printf("User ID: %d",pass->ID);
		if (pass->NEXT != NULL)
			printf(" --> ");
		pass = pass->NEXT;
	}
	printf("\n--------\n");
}	


/*
Users call this function after arriving to enter the queue
*/
void enterQueue(struct User *entry) {
	if (HEAD == NULL) { 
		HEAD = entry;
		select = HEAD;
	} else {
		struct User *pass = HEAD;
		while (pass->NEXT != NULL)
			pass = pass->NEXT;
		
		pass->NEXT = entry;	
		entry->PREV = pass;
	}
}


/*
Exiting threads call this to find the next user to admit before signalling all waiting threads.
Since an exiting user may free up enough resources for multiple waiting threads any thread allowed entry
also calls this function and the process repeats until no waiting threads pass the entry barrier. 
*/
void find_next_user() {
	if (HEAD != NULL) {
		select = HEAD;
		while (select != NULL && (select->memory_required > available_memory || select->processes_required > available_processes))
			select = select->NEXT;	
	}	
}	


/*
User thread entry point. As described in top this version this solution attempts to push more users through if resources
permit regardless of their place in the queue. Despite this the program preserves the FIFO property as best as possible
allowing heavy consumers entry in the order they arrived.
*/
void *user_entry(void *info) {
	struct User *user = (struct User*) info;
	
	sleep(user->arrival);
	clock_t start = clock();
	
	pthread_mutex_lock(&resourceLock);
	printf("User %d has arrived\n",user->ID);
		
	enterQueue(user);
	
	while (select != user || (user->memory_required > available_memory || user->processes_required > available_processes))
		pthread_cond_wait(&resourceCond, &resourceLock);	
	
	user->idle = (double) (clock() - start);
	available_memory -= user->memory_required;
	available_processes -= user->processes_required;
	printf("User %d has been allocated resources. Memory remaining: %d, Processes remaining: %d\n",user->ID,available_memory,available_processes);
	
	if (select == HEAD)
		HEAD = HEAD->NEXT;
    else {
		select->PREV->NEXT = select->NEXT;
		if (select->NEXT != NULL)
			select->NEXT->PREV = select->PREV;	
	}	
	
	find_next_user();
	pthread_cond_broadcast(&resourceCond);
		
	pthread_mutex_unlock(&resourceLock);
	
	sleep(user->duration);
	
	pthread_mutex_lock(&resourceLock);
	
	available_memory += user->memory_required;
	available_processes += user->processes_required;
	printf("User %d has finished. Memory remaining: %d, Processes remaining: %d\n",user->ID,available_memory,available_processes);
	
	find_next_user();
	pthread_cond_broadcast(&resourceCond);
	
	pthread_mutex_unlock(&resourceLock);	
}	



/*
Main function which reads file input, sets thread attributes and starts each thread then waits for each to terminate.
*/
int main(int argc, char *argv[]) {
	
	//Check for proper program invocation
	if (argc != 4) {
		printf("Invalid invocation, usage: MultiUser2 <file-name> <total_memory> <total_processes>");
		exit(EXIT_FAILURE);
	}		
	
	available_memory = atoi(argv[2]);
	available_processes = atoi(argv[3]);
	
	if (available_memory < 1|| available_processes < 1) {
		printf("Invalid values specified, must be > 0");
		exit(EXIT_FAILURE);
	}
	
	//Initialize lock and conditions
	if (pthread_mutex_init(&resourceLock, NULL) != 0 || pthread_mutex_init(&queueLock, NULL) != 0) {
		printf("Mutex lock creation failure, exiting...");
		exit(EXIT_FAILURE);
	}	
	
	if (pthread_cond_init(&resourceCond, NULL) != 0) {
		printf("Condition variable creation failure, exiting...");
		exit(EXIT_FAILURE);
	}

	
	//Following sections set global variables, read the input file, assign thread attributes, create and then wait for threads to terminate
	HEAD = NULL;
	select = NULL;
	
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
	
	struct User *users[numThreads];
	
	int x;
	
	for (x=0; x<numThreads; x++) {
		users[x] = (struct User*)malloc(sizeof(struct User));
		
		fgets(buffer, 15, fp);
		
		pos = buffer;
		conv = buffer;
	
		while (*pos != ',')
			pos++;
		*pos = '\0';

		users[x]->ID = atoi(conv);
		
		while (*pos != ',')
			pos++;
		*pos = '\0';
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		users[x]->arrival = atoi(conv);
		
		while (*pos != ',')
			pos++;
		*pos = '\0';
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		users[x]->duration = atoi(conv);
		
		while (*pos != ',')
			pos++;
		*pos = '\0';
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		users[x]->memory_required = atoi(conv);
		
		while (*conv != '\0')
			conv++;
		conv++;
		
		users[x]->processes_required = atoi(conv);
		users[x]->NEXT = NULL;
		users[x]->PREV = NULL;
	}	
	
	fclose(fp);
	
	
	for (x=0; x<numThreads; x++) {
		if (pthread_create(&tID[x],NULL,user_entry,users[x]) != 0) {
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
		printf("\nUser %d idle time: %f\n",users[x]->ID,users[x]->idle/CLOCKS_PER_SEC);
		sum += users[x]->idle/CLOCKS_PER_SEC;
	}

	printf("\nAverage idle time: %f",sum/numThreads);	
	
	return 0;
}