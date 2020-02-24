JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
    GatewayServer.java \
	Outlet.java \
	Motion.java \
	SmartBulb.java \
	Temperature.java \
	Beacon.java \
	DatabaseServer.java\
	Door.java \
	User.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class