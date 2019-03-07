# QSE Lin (The QSE License Inspector)

**QSE Lin** is a scanning engine, that can be used to audit a source tree for complying to certain legal requirements. Namely, it can check
whether source files contain a proper license header. Thus, **QSE Lin** can do for you, what [Apache Rat](https://creadur.apache.org/rat)
does. In fact, **QSE Lin** can be considered the successor to Apache Rat, because

1. It is much faster. (Scans itself in less than 500 milliseconds) In fact, it is sufficiently fast to be used in a Git, or Subversion hook.
   These performance improvements are the result of a carefully designed, and event driven architecture. For example, QSE Lin can make use
   of several threads, that operate in parallel. Files are read only once. A text files contents are distributed to the so-called license
   matchers (the components, which actually analyze those contents) as a set of events. The event stream will be aborted as soon as a license
   matcher signals, that it has detected the expected content. (Or, to rephrase that: Events are filtered out, whereever possible.)
2. It is modular, and configurable. Basically, every piece of software (so-called components) can be replaced, and separately
   configured.
3. A plugin architecture allows to extend **QSE Lin** with additional license matchers, archive handlers, or other components. No need to
   bother the developers that "*.mp3" is a binary file, and should be treated as such, and then wait for the next version, which contains
   that "fix". You just reconfigure the respective plugin, or replace it, and that's it.
4. The QSE Lin Core has been carefully designed for reusability. Other components (Maven plugin, Ant task, etc., are sharing are basically
   just **very** thin layers around the core). In Apache Rat, only a comparatively minor part is being shared. Features must be separately
   implemented for the Rat CLI, the Rat Maven plugin, the Rat Ant task, and so on. And, as a result, these latter tools can have
   rather unexpected differences, which arte cumbersome, and hard to explain.

