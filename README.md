# benchmarkSocket_
Speed benchmark of sockets communication between java and c


###abstract

* speed benchmarks for socket communication programs written in Java and c
* This is the only test where Java is slower than c.
* This test is for Linux, but on windows expected to see same results.
* Test outline: One message is circled between a few threads.
* The test runs for specified period (Default 5 seconds) nuber of loops is displayed.
* Please edit makefile according to your environment.


###usage

* java -ea -jar dist/benchmarkSocket.jar help
* java -ea -jar dist/benchmarkSocket.jar delay=5000 threads=16 port=5100 verbose
* ARGS='delay=5000 threads=16 port=5100 verbose'  make java
* 
c:

* make -f Makefile
* ~/nb/benchmarkSocket/src/benchmarksocket/benchmarksocket  delay=5000 port=6130 thr=16 verbose
* ARGS='delay=5000 threads=16 port=5100 verbose'  make c

###arguments

Unique prefix of argument name is enough (No need to type full name).

### test type
* delay - duration of test, default 5000 (5 seconds
* threads - number of threads
* port - socket base port    (thread_1 listens to ${port} + 1  send over ${port} + 2
* verbose - debug prints
* help - show params

###help

* The help is created automatically.
* See Args.java args.c

### files

* Makefile -  compiles c program
* benchmarkSocket.java
* Args.java             Argumet parser for Java (MIT license)</li>
* benchmarkSocket.jar
* benchmarkSocket.c     Source fo c program
* args.c                Argumet parser for c (MIT license)</li>
* args.h


### License

* MIT License, free to change and redistribute, Just keep the credit.
* Any question or requests are welcome

<h3>
<a id="authors-and-contributors" class="anchor" href="#authors-and-contributors" aria-hidden="true"><span aria-hidden="true" class="octicon octicon-link"></span></a>Authors and Contributors</h3>

<p><a href="mailto:eli.shagam@gmail.com">eli.shagam@gmail.com</a></p>

TechoPhil.com
~                                                                                      
~       
