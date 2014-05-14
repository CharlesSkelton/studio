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

Current Version
----

3.31

Credits
-----------

Studio for kdb+ uses the following open source projects:

* [NetBeans] - text editor component
* [JFreeChart] - charting component

Installation
--------------
Download the latest release from

https://github.com/CharlesSkelton/studio/tree/master/releases

unzip it (retaining the directory structure) to reveal


    releases$unzip studio.zip
    Archive:  studio.zip
    inflating: studio.jar
    creating: lib/
    inflating: lib/images.jar
    inflating: lib/jcommon-1.0.16.jar
    inflating: lib/jfreechart-1.0.13.jar

and this can be executed with the command

    java -jar studio.jar

There is also an osx bundle for studio

unzip it to reveal the bundle

    osx $ unzip studio.app.zip
    Archive:  studio.app.zip
    creating: studio.app/
    creating: studio.app/Contents/
    inflating: studio.app/Contents/Info.plist
    creating: studio.app/Contents/MacOS/
    inflating: studio.app/Contents/MacOS/JavaApplicationStub
    extracting: studio.app/Contents/PkgInfo
    creating: studio.app/Contents/Resources/
    inflating: studio.app/Contents/Resources/GenericJavaApp.icns
    creating: studio.app/Contents/Resources/Java/
    inflating: studio.app/Contents/Resources/Java/images.jar
    inflating: studio.app/Contents/Resources/Java/jcommon-1.0.16.jar
    inflating: studio.app/Contents/Resources/Java/jfreechart-1.0.13.jar
    inflating: studio.app/Contents/Resources/Java/studio.jar

and then from a finder window, drag the "studio.app" folder to the dock for easy launching.

Background
----------
Studio for kdb+ has been developed since October 2002, and the source was released to the kdb+ community in September 2008 as the primary developer wanted to allow the community to develop the application further.

Studio is written 100% in Java. The primary motivation for its development was to be able to comfortably access remote kdb+ processes. In time, it has become clear that it is not an IDE as such, but is better described as a rapid execution environment. One can edit text in the "scratch" window, highlight a selection and execute it against a remote kdb+ process via tcp/ip, with the results displayed as a grid or as in the classic kdb+ console.

License
----
I'll probably change this but currently it is
[license]


[Kx Systems]:http://www.kx.com
[Netbeans]:http:///netbeans.org
[license]:http://creativecommons.org/licenses/by-nc-sa/3.0/
[git-repo-url]:https://github.com/CharlesSkelton/studio
[JFreeChart]:http://www.jfree.org/jfreechart/
