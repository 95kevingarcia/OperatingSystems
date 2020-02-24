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
	Sensor.java\
	Door.java \
	finalProjectUI.java \
	Device.java \
	User.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class