

# ARG=' thr=8 ' make java

java:
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar ${ARG}

java_process:
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar thr=4 process=0 port=5000 ${ARG} &
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar thr=4 process=1 port=5000 ${ARG} &
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar thr=4 process=2 port=5000 ${ARG} &
	java -ea  -verbose:gc  -Xloggc:/var/js/java.log_benchmark -jar dist/benchmarkSocket.jar thr=4 process=3 port=5000 ${ARG} &


c:
	~/nb/benchmarkSocket/src/benchmarksocket/benchmarksocket ${ARG}
