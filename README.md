# ssh-remote

## Execution of single command to a particular host
<pre>java -jar ssh-remote-jar-with-dependencies.jar host=127.0.0.1 port=22 user=user password=password privateKey=privateKey command=uname</pre>

### Execution of multiple commands to multiple hosts defined in configuration XML file 
<pre>java -jar ssh-remote-jar-with-dependencies.jar file=ssh-remote.xml</pre>

#### ssh-remote.xml
<pre>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
&lt;configs&gt;
	&lt;config host=&quot;127.0.0.1&quot; port=&quot;22&quot; user=&quot;user&quot; password=&quot;password&quot; privateKey=&quot;privateKeys&quot;&gt;
		&lt;commands&gt;
			&lt;command&gt;&lt;![CDATA[uname -a]]&gt;&lt;/command&gt;
			&lt;command&gt;&lt;![CDATA[cat /etc/os-release]]&gt;&lt;/command&gt;
		&lt;/commands&gt;
	&lt;/config&gt;
&lt;/configs&gt;</pre>
