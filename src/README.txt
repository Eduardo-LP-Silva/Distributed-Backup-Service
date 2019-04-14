The following commands (either to compile or to run the application) must be given from inside
the src folder.

Compilation:

- javac app/*.java
- javac protocol/handler/*.java
- javac protocol/initiator/*.java
- javac protocol/listener/*.java

In addition to this, you must also have an instace of an RMI registry running in the src folder.
You can do this with the command rmiregistry &

Or just use make.sh for all of the above! 

Note: if for some reason there are .class files already present and there are compiler/runner
conflicts with these versions, use cleanup.sh (or delete the .class files) and only then compile
the .java classes.

Running:

The standard way to run any of the java classes is java <package>.<class> [args]
We provide several script files to make it easier. To learn how to use any of them, simply run 
them without any arguments.

Note: The script files also must be executed from the src folder to work.

Note 2: Regarding the enhancements made, since there is no way (without trading extra messages)
for a peer to know which command was given to another at the start of a subprotocol (e.g BACKUP vs
BACKUPENH), the distinction between enhanced protocols and vanilla ones is made purely through
the peer's version. Currently, if the peer's version is 1.0, it will run the vanilla protocols,
else, if the version is 2.0, it will run the enhancements where available.

 