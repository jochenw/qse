package com.github.jochenw.qse.is.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Diagnostics;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.property.ResolvePropertyMap;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;

public class AntRunner implements AntMain {
	private static final Set<String> LAUNCH_COMMANDS = new HashSet<>();
	public static final String DEFAULT_BUILD_FILENAME = "build.xml";
	private int msgOutputLevel = 2;
	private File buildFile;
	private static PrintStream out;
	private static PrintStream err;
	private Vector<String> targets = new Vector<String>();
	private Properties definedProps = new Properties();
	private Vector<String> listeners = new Vector<>(1);
	private Vector<String> propertyFiles = new Vector<>(1);
	private boolean allowInput = true;
	private boolean keepGoingMode = false;
	private String loggerClassname = null;
	private String inputHandlerClassname = null;
	private boolean emacsMode = false;
	private boolean readyToRun = false;
	private boolean projectHelp = false;
	private static boolean isLogFileUsed;
	private Integer threadPriority = null;
	private boolean proxy = false;
	private static String antVersion;

	private static void printMessage(Throwable t) {
		String message = t.getMessage();
		if (message != null) {
			System.err.println(message);
		}

	}

	public void startAnt(String[] args, Properties additionalUserProperties, ClassLoader coreLoader) {
		try {
			this.processArgs(args);
		} catch (RuntimeException re) {
			throw re;
		} catch (Error e) {
			throw e;
		} catch (Throwable t) {
			handleLogfile();
			printMessage(t);
			throw new UndeclaredThrowableException(t);
		}

		if (additionalUserProperties != null) {
			final Enumeration<Object> e = additionalUserProperties.keys();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String property = additionalUserProperties.getProperty(key);
				this.definedProps.put(key, property);
			}
		}

		try {
			try {
				this.runBuild(coreLoader);
				return;
			} catch (ExitStatusException ese) {
				throw ese;
			}
		} catch (BuildException be) {
			if (err != System.err) {
				printMessage(be);
			}
			throw be;
		} catch (Throwable t) {
			t.printStackTrace();
			printMessage(t);
			throw new UndeclaredThrowableException(t);
		} finally {
			handleLogfile();
		}
	}

	protected void exit(int exitCode) {
		System.exit(exitCode);
	}

	private static void handleLogfile() {
		if (isLogFileUsed) {
			FileUtils.close(out);
			FileUtils.close(err);
		}

	}

	@SuppressWarnings("unchecked")
	private void processArgs(String[] args) {
		String searchForThis = null;
		boolean searchForFile = false;
		PrintStream logTo = null;
		boolean justPrintUsage = false;
		boolean justPrintVersion = false;
		boolean justPrintDiagnostics = false;

		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			if (!arg.equals("-help") && !arg.equals("-h")) {
				if (arg.equals("-version")) {
					justPrintVersion = true;
				} else if (arg.equals("-diagnostics")) {
					justPrintDiagnostics = true;
				} else if (!arg.equals("-quiet") && !arg.equals("-q")) {
					if (!arg.equals("-verbose") && !arg.equals("-v")) {
						if (!arg.equals("-debug") && !arg.equals("-d")) {
							if (arg.equals("-noinput")) {
								this.allowInput = false;
							} else if (!arg.equals("-logfile") && !arg.equals("-l")) {
								if (!arg.equals("-buildfile") && !arg.equals("-file") && !arg.equals("-f")) {
									if (arg.equals("-listener")) {
										i = this.handleArgListener(args, i);
									} else if (arg.startsWith("-D")) {
										i = this.handleArgDefine(args, i);
									} else if (arg.equals("-logger")) {
										i = this.handleArgLogger(args, i);
									} else if (arg.equals("-inputhandler")) {
										i = this.handleArgInputHandler(args, i);
									} else if (!arg.equals("-emacs") && !arg.equals("-e")) {
										if (!arg.equals("-projecthelp") && !arg.equals("-p")) {
											if (!arg.equals("-find") && !arg.equals("-s")) {
												if (arg.startsWith("-propertyfile")) {
													i = this.handleArgPropertyFile(args, i);
												} else if (!arg.equals("-k") && !arg.equals("-keep-going")) {
													if (arg.equals("-nice")) {
														i = this.handleArgNice(args, i);
													} else {
														String msg;
														if (LAUNCH_COMMANDS.contains(arg)) {
															msg = "Ant's Main method is being handed an option " + arg
																	+ " that is only for the launcher class."
																	+ "\nThis can be caused by a version mismatch between "
																	+ "the ant script/.bat file and Ant itself.";
															throw new BuildException(msg);
														}

														if (arg.equals("-autoproxy")) {
															this.proxy = true;
														} else {
															if (arg.startsWith("-")) {
																msg = "Unknown argument: " + arg;
																System.err.println(msg);
																printUsage();
																throw new BuildException("");
															}

															this.targets.addElement(arg);
														}
													}
												} else {
													this.keepGoingMode = true;
												}
											} else {
												searchForFile = true;
												if (i < args.length - 1) {
													++i;
													searchForThis = args[i];
												}
											}
										} else {
											this.projectHelp = true;
										}
									} else {
										this.emacsMode = true;
									}
								} else {
									i = this.handleArgBuildFile(args, i);
								}
							} else {
								String msg;
								try {
									File logFile = new File(args[i + 1]);
									++i;
									logTo = new PrintStream(new FileOutputStream(logFile));
									isLogFileUsed = true;
								} catch (IOException var12) {
									msg = "Cannot write on the specified log file. Make sure the path exists and you have write permissions.";
									if (logTo != null) {
										logTo.close();
									}
									throw new BuildException(msg);
								} catch (ArrayIndexOutOfBoundsException var13) {
									msg = "You must specify a log file when using the -log argument";
									throw new BuildException(msg);
								}
							}
						} else {
							this.msgOutputLevel = 4;
						}
					} else {
						this.msgOutputLevel = 3;
					}
				} else {
					this.msgOutputLevel = 1;
				}
			} else {
				justPrintUsage = true;
			}
		}

		if (this.msgOutputLevel >= 3 || justPrintVersion) {
			printVersion(this.msgOutputLevel);
		}

		if (!justPrintUsage && !justPrintVersion && !justPrintDiagnostics) {
			if (this.buildFile == null) {
				ProjectHelper helper;
				Iterator<Object> it;
				if (searchForFile) {
					if (searchForThis != null) {
						this.buildFile = this.findBuildFile(System.getProperty("user.dir"), searchForThis);
						if (this.buildFile == null) {
							throw new BuildException("Could not locate a build file!");
						}
					} else {
						final Iterator<Object> i = (Iterator<Object>) ProjectHelperRepository.getInstance().getHelpers(); 
						it = i;

						do {
							helper = (ProjectHelper) it.next();
							searchForThis = helper.getDefaultBuildFile();
							if (this.msgOutputLevel >= 3) {
								System.out.println("Searching the default build file: " + searchForThis);
							}

							this.buildFile = this.findBuildFile(System.getProperty("user.dir"), searchForThis);
						} while (this.buildFile == null && it.hasNext());

						if (this.buildFile == null) {
							throw new BuildException("Could not locate a build file!");
						}
					}
				} else {
					it = ProjectHelperRepository.getInstance().getHelpers();

					do {
						helper = (ProjectHelper) it.next();
						this.buildFile = new File(helper.getDefaultBuildFile());
						if (this.msgOutputLevel >= 3) {
							System.out.println("Trying the default build file: " + this.buildFile);
						}
					} while (!this.buildFile.exists() && it.hasNext());
				}
			}

			if (!this.buildFile.exists()) {
				System.out.println("Buildfile: " + this.buildFile + " does not exist!");
				throw new BuildException("Build failed");
			} else if (this.buildFile.isDirectory()) {
				System.out.println("What? Buildfile: " + this.buildFile + " is a dir!");
				throw new BuildException("Build failed");
			} else {
				this.buildFile = FileUtils.getFileUtils().normalize(this.buildFile.getAbsolutePath());
				this.loadPropertyFiles();
				if (this.msgOutputLevel >= 2) {
					System.out.println("Buildfile: " + this.buildFile);
				}

				if (logTo != null) {
					out = logTo;
					err = logTo;
					System.setOut(out);
					System.setErr(err);
				}

				this.readyToRun = true;
			}
		} else {
			if (justPrintUsage) {
				printUsage();
			}

			if (justPrintDiagnostics) {
				Diagnostics.doReport(System.out, this.msgOutputLevel);
			}

		}
	}

	private int handleArgBuildFile(String[] args, int pos) {
		try {
			++pos;
			this.buildFile = new File(args[pos].replace('/', File.separatorChar));
			return pos;
		} catch (ArrayIndexOutOfBoundsException var4) {
			throw new BuildException("You must specify a buildfile when using the -buildfile argument");
		}
	}

	private int handleArgListener(String[] args, int pos) {
		try {
			this.listeners.addElement(args[pos + 1]);
			++pos;
			return pos;
		} catch (ArrayIndexOutOfBoundsException var5) {
			String msg = "You must specify a classname when using the -listener argument";
			throw new BuildException(msg);
		}
	}

	private int handleArgDefine(String[] args, int argPos) {
		String arg = args[argPos];
		String name = arg.substring(2, arg.length());
		String value = null;
		int posEq = name.indexOf("=");
		if (posEq > 0) {
			value = name.substring(posEq + 1);
			name = name.substring(0, posEq);
		} else {
			if (argPos >= args.length - 1) {
				throw new BuildException("Missing value for property " + name);
			}

			++argPos;
			value = args[argPos];
		}

		this.definedProps.put(name, value);
		return argPos;
	}

	private int handleArgLogger(String[] args, int pos) {
		if (this.loggerClassname != null) {
			throw new BuildException("Only one logger class may be specified.");
		} else {
			try {
				++pos;
				this.loggerClassname = args[pos];
				return pos;
			} catch (ArrayIndexOutOfBoundsException var4) {
				throw new BuildException("You must specify a classname when using the -logger argument");
			}
		}
	}

	private int handleArgInputHandler(String[] args, int pos) {
		if (this.inputHandlerClassname != null) {
			throw new BuildException("Only one input handler class may be specified.");
		} else {
			try {
				++pos;
				this.inputHandlerClassname = args[pos];
				return pos;
			} catch (ArrayIndexOutOfBoundsException var4) {
				throw new BuildException("You must specify a classname when using the -inputhandler argument");
			}
		}
	}

	private int handleArgPropertyFile(String[] args, int pos) {
		try {
			++pos;
			this.propertyFiles.addElement(args[pos]);
			return pos;
		} catch (ArrayIndexOutOfBoundsException var5) {
			String msg = "You must specify a property filename when using the -propertyfile argument";
			throw new BuildException(msg);
		}
	}

	private int handleArgNice(String[] args, int pos) {
		try {
			++pos;
			this.threadPriority = Integer.decode(args[pos]);
		} catch (ArrayIndexOutOfBoundsException var4) {
			throw new BuildException("You must supply a niceness value (1-10) after the -nice option");
		} catch (NumberFormatException var5) {
			throw new BuildException("Unrecognized niceness value: " + args[pos]);
		}

		if (this.threadPriority >= 1 && this.threadPriority <= 10) {
			return pos;
		} else {
			throw new BuildException("Niceness value is out of the range 1-10");
		}
	}

	private void loadPropertyFiles() {
		for (int propertyFileIndex = 0; propertyFileIndex < this.propertyFiles.size(); ++propertyFileIndex) {
			String filename = (String) this.propertyFiles.elementAt(propertyFileIndex);
			Properties props = new Properties();
			FileInputStream fis = null;

			try {
				fis = new FileInputStream(filename);
				props.load(fis);
			} catch (IOException var9) {
				System.out.println("Could not load property file " + filename + ": " + var9.getMessage());
			} finally {
				FileUtils.close(fis);
			}

			Enumeration<?> propertyNames = props.propertyNames();

			while (propertyNames.hasMoreElements()) {
				String name = (String) propertyNames.nextElement();
				if (this.definedProps.getProperty(name) == null) {
					this.definedProps.put(name, props.getProperty(name));
				}
			}
		}

	}

	private File getParentFile(File file) {
		File parent = file.getParentFile();
		if (parent != null && this.msgOutputLevel >= 3) {
			System.out.println("Searching in " + parent.getAbsolutePath());
		}

		return parent;
	}

	private File findBuildFile(String start, String suffix) {
		if (this.msgOutputLevel >= 2) {
			System.out.println("Searching for " + suffix + " ...");
		}

		File parent = new File((new File(start)).getAbsolutePath());

		File file;
		for (file = new File(parent, suffix); !file.exists(); file = new File(parent, suffix)) {
			parent = this.getParentFile(parent);
			if (parent == null) {
				return null;
			}
		}

		return file;
	}

	private void runBuild(ClassLoader coreLoader) throws BuildException {
		if (this.readyToRun) {
			Project project = new Project();
			project.setCoreLoader(coreLoader);
			Object error = null;

			try {
				this.addBuildListeners(project);
				this.addInputHandler(project);
				PrintStream savedErr = System.err;
				PrintStream savedOut = System.out;
				InputStream savedIn = System.in;
				SecurityManager oldsm = null;
				oldsm = System.getSecurityManager();

				try {
					if (this.allowInput) {
						project.setDefaultInputStream(System.in);
					}

					System.setIn(new DemuxInputStream(project));
					System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
					System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
					if (!this.projectHelp) {
						project.fireBuildStarted();
					}

					if (this.threadPriority != null) {
						try {
							project.log("Setting Ant's thread priority to " + this.threadPriority, 3);
							Thread.currentThread().setPriority(this.threadPriority);
						} catch (SecurityException var33) {
							project.log("A security manager refused to set the -nice value");
						}
					}

					project.init();
					PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
					HashMap<Object,Object> props = new HashMap<>(this.definedProps);
					(new ResolvePropertyMap(project, propertyHelper, propertyHelper.getExpanders()))
							.resolveAllProperties(props, (String) null, false);
					Iterator<?> e = props.entrySet().iterator();

					while (e.hasNext()) {
						Entry<?,?> ent = (Entry<?,?>) e.next();
						String arg = (String) ent.getKey();
						Object value = ent.getValue();
						project.setUserProperty(arg, String.valueOf(value));
					}

					project.setUserProperty("ant.file", this.buildFile.getAbsolutePath());
					project.setUserProperty("ant.file.type", "file");
					project.setKeepGoingMode(this.keepGoingMode);
					if (this.proxy) {
						ProxySetup proxySetup = new ProxySetup(project);
						proxySetup.enableProxies();
					}

					ProjectHelper.configureProject(project, this.buildFile);
					if (!this.projectHelp) {
						if (this.targets.size() == 0 && project.getDefaultTarget() != null) {
							this.targets.addElement(project.getDefaultTarget());
						}

						project.executeTargets(this.targets);
						return;
					}

					printDescription(project);
					printTargets(project, this.msgOutputLevel > 2, this.msgOutputLevel > 3);
				} finally {
					if (oldsm != null) {
						System.setSecurityManager(oldsm);
					}

					System.setOut(savedOut);
					System.setErr(savedErr);
					System.setIn(savedIn);
				}
			} catch (RuntimeException var36) {
				error = var36;
				throw var36;
			} catch (Error var37) {
				error = var37;
				throw var37;
			} finally {
				if (!this.projectHelp) {
					try {
						project.fireBuildFinished((Throwable) error);
					} catch (Throwable var34) {
						System.err.println("Caught an exception while logging the end of the build.  Exception was:");
						var34.printStackTrace();
						if (error != null) {
							System.err.println("There has been an error prior to that:");
							((Throwable) error).printStackTrace();
						}

						throw new BuildException(var34);
					}
				} else if (error != null) {
					project.log(((Throwable) error).toString(), 0);
				}

			}

		}
	}

	protected void addBuildListeners(Project project) {
		project.addBuildListener(this.createLogger());

		for (int i = 0; i < this.listeners.size(); ++i) {
			String className = (String) this.listeners.elementAt(i);
			BuildListener listener = (BuildListener) ClasspathUtils.newInstance(className, Main.class.getClassLoader(),
					BuildListener.class);
			project.setProjectReference(listener);
			project.addBuildListener(listener);
		}

	}

	private void addInputHandler(Project project) throws BuildException {
		InputHandler handler = null;
		if (this.inputHandlerClassname == null) {
			handler = new DefaultInputHandler();
		} else {
			handler = (InputHandler) ClasspathUtils.newInstance(this.inputHandlerClassname, Main.class.getClassLoader(),
					InputHandler.class);
			project.setProjectReference(handler);
		}

		project.setInputHandler((InputHandler) handler);
	}

	private BuildLogger createLogger() {
		BuildLogger logger = null;
		if (this.loggerClassname != null) {
			try {
				logger = (BuildLogger) ClasspathUtils.newInstance(this.loggerClassname, Main.class.getClassLoader(),
						BuildLogger.class);
			} catch (BuildException var3) {
				System.err.println("The specified logger class " + this.loggerClassname + " could not be used because "
						+ var3.getMessage());
				throw new RuntimeException();
			}
		} else {
			logger = new DefaultLogger();
		}

		((BuildLogger) logger).setMessageOutputLevel(this.msgOutputLevel);
		((BuildLogger) logger).setOutputPrintStream(out);
		((BuildLogger) logger).setErrorPrintStream(err);
		((BuildLogger) logger).setEmacsMode(this.emacsMode);
		return (BuildLogger) logger;
	}

	private static void printUsage() {
		String lSep = System.getProperty("line.separator");
		StringBuffer msg = new StringBuffer();
		msg.append("ant [options] [target [target2 [target3] ...]]" + lSep);
		msg.append("Options: " + lSep);
		msg.append("  -help, -h              print this message" + lSep);
		msg.append("  -projecthelp, -p       print project help information" + lSep);
		msg.append("  -version               print the version information and exit" + lSep);
		msg.append("  -diagnostics           print information that might be helpful to" + lSep);
		msg.append("                         diagnose or report problems." + lSep);
		msg.append("  -quiet, -q             be extra quiet" + lSep);
		msg.append("  -verbose, -v           be extra verbose" + lSep);
		msg.append("  -debug, -d             print debugging information" + lSep);
		msg.append("  -emacs, -e             produce logging information without adornments" + lSep);
		msg.append("  -lib <path>            specifies a path to search for jars and classes" + lSep);
		msg.append("  -logfile <file>        use given file for log" + lSep);
		msg.append("    -l     <file>                ''" + lSep);
		msg.append("  -logger <classname>    the class which is to perform logging" + lSep);
		msg.append("  -listener <classname>  add an instance of class as a project listener" + lSep);
		msg.append("  -noinput               do not allow interactive input" + lSep);
		msg.append("  -buildfile <file>      use given buildfile" + lSep);
		msg.append("    -file    <file>              ''" + lSep);
		msg.append("    -f       <file>              ''" + lSep);
		msg.append("  -D<property>=<value>   use value for given property" + lSep);
		msg.append("  -keep-going, -k        execute all targets that do not depend" + lSep);
		msg.append("                         on failed target(s)" + lSep);
		msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
		msg.append("                         properties taking precedence" + lSep);
		msg.append("  -inputhandler <class>  the class which will handle input requests" + lSep);
		msg.append("  -find <file>           (s)earch for buildfile towards the root of" + lSep);
		msg.append("    -s  <file>           the filesystem and use it" + lSep);
		msg.append("  -nice  number          A niceness value for the main thread:" + lSep
				+ "                         1 (lowest) to 10 (highest); 5 is the default" + lSep);
		msg.append("  -nouserlib             Run ant without using the jar files from" + lSep
				+ "                         ${user.home}/.ant/lib" + lSep);
		msg.append("  -noclasspath           Run ant without using CLASSPATH" + lSep);
		msg.append("  -autoproxy             Java1.5+: use the OS proxy settings" + lSep);
		msg.append("  -main <class>          override Ant's normal entry point");
		System.out.println(msg.toString());
	}

	private static void printVersion(int logLevel) throws BuildException {
		System.out.println(getAntVersion());
	}

	public static synchronized String getAntVersion() throws BuildException {
		if (antVersion == null) {
			try {
				Properties props = new Properties();
				InputStream in = Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
				props.load(in);
				in.close();
				StringBuffer msg = new StringBuffer();
				msg.append("Apache Ant(TM) version ");
				msg.append(props.getProperty("VERSION"));
				msg.append(" compiled on ");
				msg.append(props.getProperty("DATE"));
				antVersion = msg.toString();
			} catch (IOException var3) {
				throw new BuildException("Could not load the version information:" + var3.getMessage());
			} catch (NullPointerException var4) {
				throw new BuildException("Could not load the version information.");
			}
		}

		return antVersion;
	}

	private static void printDescription(Project project) {
		if (project.getDescription() != null) {
			project.log(project.getDescription());
		}

	}

	private static Map<?,?> removeDuplicateTargets(Map<?,?> targets) {
		Map<Object,Object> locationMap = new HashMap<>();
		Iterator<?> i = targets.entrySet().iterator();

		while (true) {
			String name;
			Target target;
			Target otherTarget;
			do {
				if (!i.hasNext()) {
					Map<Object,Object> ret = new HashMap<>();
					Iterator<?> it = locationMap.values().iterator();

					while (it.hasNext()) {
						Target trgt = (Target) it.next();
						ret.put(trgt.getName(), trgt);
					}

					return ret;
				}

				@SuppressWarnings("unchecked")
				Entry<Object,Object> entry = (Entry<Object,Object>) i.next();
				name = (String) entry.getKey();
				target = (Target) entry.getValue();
				otherTarget = (Target) locationMap.get(target.getLocation());
			} while (otherTarget != null && otherTarget.getName().length() <= name.length());

			locationMap.put(target.getLocation(), target);
		}
	}

	private static void printTargets(Project project, boolean printSubTargets, boolean printDependencies) {
		int maxLength = 0;
		Map<?,?> ptargets = removeDuplicateTargets(project.getTargets());
		Vector<Object> topNames = new Vector<>();
		Vector<Object> topDescriptions = new Vector<>();
		Vector<Object> topDependencies = new Vector<>();
		Vector<Object> subNames = new Vector<>();
		Vector<Object> subDependencies = new Vector<>();
		Iterator<?> i = ptargets.values().iterator();

		while (i.hasNext()) {
			Target currentTarget = (Target) i.next();
			String targetName = currentTarget.getName();
			if (!targetName.equals("")) {
				String targetDescription = currentTarget.getDescription();
				int pos;
				if (targetDescription == null) {
					pos = findTargetPosition(subNames, targetName);
					subNames.insertElementAt(targetName, pos);
					if (printDependencies) {
						subDependencies.insertElementAt(currentTarget.getDependencies(), pos);
					}
				} else {
					pos = findTargetPosition(topNames, targetName);
					topNames.insertElementAt(targetName, pos);
					topDescriptions.insertElementAt(targetDescription, pos);
					if (targetName.length() > maxLength) {
						maxLength = targetName.length();
					}

					if (printDependencies) {
						topDependencies.insertElementAt(currentTarget.getDependencies(), pos);
					}
				}
			}
		}

		printTargets(project, topNames, topDescriptions, topDependencies, "Main targets:", maxLength);
		if (topNames.size() == 0) {
			printSubTargets = true;
		}

		if (printSubTargets) {
			printTargets(project, subNames, (Vector<?>) null, subDependencies, "Other targets:", 0);
		}

		String defaultTarget = project.getDefaultTarget();
		if (defaultTarget != null && !"".equals(defaultTarget)) {
			project.log("Default target: " + defaultTarget);
		}

	}

	private static int findTargetPosition(Vector<?> names, String name) {
		int res = names.size();

		for (int i = 0; i < names.size() && res == names.size(); ++i) {
			if (name.compareTo((String) names.elementAt(i)) < 0) {
				res = i;
			}
		}

		return res;
	}

	private static void printTargets(Project project, Vector<?> names, Vector<?> descriptions, Vector<?> dependencies,
			String heading, int maxlen) {
		String lSep = System.getProperty("line.separator");

		String spaces;
		for (spaces = "    "; spaces.length() <= maxlen; spaces = spaces + spaces) {
			;
		}

		StringBuffer msg = new StringBuffer();
		msg.append(heading + lSep + lSep);

		for (int i = 0; i < names.size(); ++i) {
			msg.append(" ");
			msg.append(names.elementAt(i));
			if (descriptions != null) {
				msg.append(spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
				msg.append(descriptions.elementAt(i));
			}

			msg.append(lSep);
			if (!dependencies.isEmpty()) {
				Enumeration<?> deps = (Enumeration<?>) dependencies.elementAt(i);
				if (deps.hasMoreElements()) {
					msg.append("   depends on: ");

					while (deps.hasMoreElements()) {
						msg.append(deps.nextElement());
						if (deps.hasMoreElements()) {
							msg.append(", ");
						}
					}

					msg.append(lSep);
				}
			}
		}

		project.log(msg.toString(), 1);
	}

	static {
		LAUNCH_COMMANDS.add("-lib");
		LAUNCH_COMMANDS.add("-cp");
		LAUNCH_COMMANDS.add("-noclasspath");
		LAUNCH_COMMANDS.add("--noclasspath");
		LAUNCH_COMMANDS.add("-nouserlib");
		LAUNCH_COMMANDS.add("-main");
		out = System.out;
		err = System.err;
		isLogFileUsed = false;
		antVersion = null;
	}
}
