

# ARG=' thr=8 port=6000 verbose' make java
port = 5100

java:
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar ${ARG} 

c:
	~/nb/benchmarkSocket/src/benchmarksocket/benchmarksocket ${ARG}
