#include <iostream>
#include <stdio.h>
#include <stdlib.h>

#include <sys/time.h>
#include <pthread.h>
#include <time.h>
#include<unistd.h>
#include "args.h"
#include<string.h>
#include <map>
#include <limits.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <sys/types.h>
#include <netdb.h> 



//* Jewel Store Copyright (C) 2011-2014  TechoPhil Ltd

using namespace std;
const int THREAD_MAX = 100;
const int BASE_PORT = 5100;

long getTimeMili () {
    timeval time;
    gettimeofday(&time, NULL);
    long millis = (time.tv_sec * 1000.0) + (time.tv_usec / 1000.0) + 0.5;
    return millis;
}


static int s_threadCnt;
static int s_verbose;
static int s_delay;
static int s_portNum;
static long s_startMili = getTimeMili();

int *listenfd;
int *sockfd;


    
pthread_t thread_id[THREAD_MAX];

struct sockaddr_in serv_addr_server[THREAD_MAX];
struct sockaddr_in serv_addr_client[THREAD_MAX];
int sockfd_client [THREAD_MAX];

inline long InterlockedIncrement(long* p, int delta)
{
    return __atomic_add_fetch(p, delta, __ATOMIC_SEQ_CST);
}

void error(const char *msg) {
    perror(msg);
    sleep (1);
    exit(1);
}

void *thread ( void *ptr ) {
    size_t tmp = (size_t) ptr;
    int id = (int) tmp;

    struct sockaddr_in serv_addr;
      //time_t ticks;
      char buff[1025];

      memset(buff, '0',sizeof(buff));   

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) 
        error("ERROR opening socket");
    bzero((char *) &serv_addr, sizeof(serv_addr));
    int portno = s_portNum + id;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);
    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) 
        error("ERROR on binding");
    listen(sockfd,5);
 
  
    if (s_verbose)
        fprintf (stderr, "\nthread=%d created", id);

  
    int fd = accept(sockfd, (struct sockaddr*)NULL, NULL);
    if (s_verbose)
        fprintf (stderr, "\nthread=%d accept fd=%d", id, fd);  

  

  for (long loop = 0;  ; loop++) {
    
    int strlenbufff = read(fd, buff, sizeof(buff)-1);
    if(strlenbufff < 0) {
        printf("\n Read error \n");
        exit (1);
    } 
    
    buff[strlenbufff] = 0;
    
    write(sockfd_client [id], buff, strlenbufff);   
      
    // check for exit 
    if ((loop & 0xf) == 0) {
      if (getTimeMili () - s_startMili > s_delay) {
          fprintf (stderr, "\nthread=%d loops=%ld  ", id, loop);
          close (sockfd_client [id]);          
          usleep(200);
          close(fd);          
          close(sockfd); 
          exit (0);
      }
    }      
  }

 
  return NULL;
}

void * serverThread (void *ptr);
void * client (void *ptr);
int clientInit (int id, int portno);

int main(int argc, char * argv[])  {

    s_delay = getInteger ("delay", argc, argv, "duration of test");
    if (s_delay == -1)
        s_delay = 5000;
    
    s_threadCnt = getInteger ("thread", argc, argv, "concurrent thread count");
    if (s_threadCnt == -1)
        s_threadCnt = 16; //getCPUCount();
    if (s_threadCnt < 2 || s_threadCnt >= THREAD_MAX) {
        printf("\ninvaild treadCnt > %d\n", THREAD_MAX);
        exit (1);       
    }
    
    s_verbose  =  getBool("verbose", argc, argv, "printout: thread_create, thread_end");    
    
    s_portNum = getInteger ("port", argc, argv, "ip port base (thread 0 portnum)");
    if (s_portNum == -1) 
        s_portNum = s_portNum;
    args_report();
    const char * err = verify (argc, argv);
    if (err != NULL) {
        fprintf (stderr, "invalid param=%s  \n", err);
        exit (3);
    }    
 
       // create threads
    for (int n = 0; n < s_threadCnt; n++) {
        size_t tmp = n;
        int stat = pthread_create( &thread_id[n], NULL, thread, (void*) tmp);
        if (stat) {
            error ("create thread fail");
        }
    }
    
    // socket client
    sleep (2);
    if (s_verbose)
        fprintf (stderr, "\nbefore client init");
    for (int n = 0; n < s_threadCnt; n++) {
        int portNum = s_portNum + (n+1) % s_threadCnt;      
        clientInit(n, portNum);
        if (s_verbose)
            fprintf (stderr, "\nthread=%d client init", n);          
    }

    // send first buffer
    char buffer[] = {"texttexttexttext texttexttexttext texttexttexttext"};

    int len = write(sockfd_client[0], buffer, strlen(buffer));

    // wait for thread end
    for (int n = 0; n < s_threadCnt; n++) {
        pthread_join (thread_id[n], NULL);
    }
            
    return 0;
}  


int clientInit (int id, int portno) {
    struct hostent *server; 
    const char * host = "localhost";
    sockfd_client[id] = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd_client[id] < 0) 
        error("ERROR opening socket");
    server = gethostbyname(host);
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host\n");
        exit(0);
    }
    bzero((char *) &serv_addr_client[id], sizeof(serv_addr_client[id]));
    serv_addr_client[id].sin_family = AF_INET;
    bcopy((char *)server->h_addr, (char *)&serv_addr_client[id].sin_addr.s_addr, server->h_length);
    serv_addr_client[id].sin_port = htons(portno);
    if (connect(sockfd_client[id],(struct sockaddr *) &serv_addr_client[id],sizeof(serv_addr_client[id])) < 0) 
        error("ERROR connecting");
    
}

