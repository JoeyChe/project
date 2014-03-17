#
# Warning: this is a gmake makefile.  Compile with "gmake". 
#
# This makefile only works under Unix (tested on Linux and Solaris).
#

#
# Configuration
#

JC=javac
JAR=jar
JFLAGS=-g -Xlint:unchecked



MYLIB=./lib/mylib.jar
TARGET=crawler.jar
SRCS=HtmlXPath.java HttpManager.java Test.java

#
# End configuration
#

all:  java  jar

# Building Java classes
java:
	$(JC) $(JFLAGS) -classpath $(MYLIB) $(SRCS)


# Deleting all object files
clean:
	rm -rf $(TARGET) build *.class

# Build package
jar:
	mkdir -p build/webtools && cd build && $(JAR) -xvf ../$(MYLIB) && $(JAR) -xvf ../lib/js.jar
	@cp *.class ./build/webtools/
	cd build && $(JAR) -cfm ../bin/$(TARGET) ../MANIFEST.MF org webtools
	rm build -rf

test: java test_jar


# Build test package
test_jar:
	mkdir -p build/webtools && cd build && $(JAR) -xvf ../$(MYLIB)
	@cp *.class ./build/webtools/
	cd build && $(JAR) -cfm ../bin/$(TARGET) ../test.MF org webtools
	rm build -rf
