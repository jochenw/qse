# qse
Quick Scanning Engines

QSE is a set of engines for static code analysis, and the like, which are designed from the ground up to be **extremely** quick.

1. The first engine is the *License Inspector* (QSE Lin), basically a faster successor to [Apache Rat](https://creadur.apache.org/rat).
   The License Inspector consists of the two Maven modules **qse-lin-core** (the actual scanning engine), and a Maven plugin (**qse-lin-maven**), which allows to run the engine from within Maven. QSE Lin scans itself (the core project) in less than 500 milliseconds. That's sufficiently fast to have it running within a Git, or Subversion hook.

