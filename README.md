# ssh-remote

## Execution of single command to a particular host
<pre>java -jar ssh-remote-jar-with-dependencies.jar host=127.0.0.1 port=22 user=user password=password privateKey=privateKey command=uname</pre>

### Execution of multiple commands to multiple hosts defined in configuration XML file 
<pre>java -jar ssh-remote-jar-with-dependencies.jar file=ssh-remote.xml</pre>

#### ssh-remote.xml
