Studio for kdb+
=========

Studio for kdb+ is a rapid development environment for the **ultra-fast** database kdb+ from [Kx Systems]. In the style of commonly used SQL Clients, it allows you to

  - Connect to kdb+ processes
  - Execute selected text from the editor window
  - View results as tables, charts, or classic console style 

The editor component is based on the NetBeans editor component, and includes the following features
  - find/search/replace
  - cut/copy/paste
  - undo/redo
  - syntax highlighting for the q language
  - most recent files menu

Additionally the application features
  - export to Excel
  - drag and drop
  - immediate charting of grid data

Screenshot
---------
![alt tag](https://raw.githubusercontent.com/CharlesSkelton/studio/master/meta/ssthumb.png)

Current Version
----

3.34 build date 2018.02.28

Credits
-----------

Studio for kdb+ uses the following open source projects:

* [NetBeans] - text editor component
* [JFreeChart] - charting component
* [Kx Systems] - kdb+ driver c.java

Installation
--------------
Download the latest release from

https://github.com/CharlesSkelton/studio/tree/master/releases

unzip it to reveal the studio.jar file. This can be then executed with the command

    java -jar studio.jar


Background
----------
Studio for kdb+ has been developed since October 2002, and the source was released to the kdb+ community in September 2008 as the primary developer wanted to allow the community to develop the application further.

Studio is written 100% in Java. The primary motivation for its development was to be able to comfortably access remote kdb+ processes. In time, it has become clear that it is not an IDE as such, but is better described as a rapid execution environment. One can edit text in the "scratch" window, highlight a selection and execute it against a remote kdb+ process via tcp/ip, with the results displayed as a grid or as in the classic kdb+ console.

License
-------
GNU GENERAL PUBLIC LICENSE Version 3 [license]

N.B. Netbeans, JFreeChart and c.java components have their own respective licenses.

Icon Experience Collection

Selected icons within the lib/images directory are part of the Icon Experience
collection (http://www.iconexperience.com) and may be freely used with Studio for kdb+
without charge, but may not be used separately from Studio for kdb+ without a purchase
of a license from Icon Experience.

[Kx Systems]:http://www.kx.com
[Netbeans]:http:///netbeans.org
[license]:https://github.com/CharlesSkelton/studio/blob/master/license.md
[git-repo-url]:https://github.com/CharlesSkelton/studio
[JFreeChart]:http://www.jfree.org/jfreechart/
