/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.modules;

// THIS CLASS OUGHT NOT USE NbBundle NOR org.openide CLASSES
// OUTSIDE OF openide-util.jar! UI AND FILESYSTEM/DATASYSTEM
// INTERACTIONS SHOULD GO ELSEWHERE.

import java.util.*;
import org.openide.util.Utilities;

/** A dependency a module can have.
 * @author Jesse Glick
 * @since 1.24
 */
public final class Dependency {
    /** Dependency on another module. */
    public final static int TYPE_MODULE = 1;
    /** Dependency on a package. */
    public final static int TYPE_PACKAGE = 2;
    /** Dependency on Java. */
    public final static int TYPE_JAVA = 3;
    /** Dependency on the IDE. */
    public final static int TYPE_IDE = 4;
    /** Dependency on a token.
     * @see ModuleInfo#getProvides
     * @since 2.3
     */
    public final static int TYPE_REQUIRES = 5;
    /** Comparison by specification version. */
    public final static int COMPARE_SPEC = 1;
    /** Comparison by implementation version. */
    public final static int COMPARE_IMPL = 2;
    /** No comparison, just require the dependency to be present. */
    public final static int COMPARE_ANY = 3;

    /** Name, for purposes of dependencies, of the IDE. */
    public static final String IDE_NAME = System.getProperty("org.openide.major.version", "IDE"); // NOI18N
    /** Specification version of the IDE. */
    public static final SpecificationVersion IDE_SPEC = makeSpec(System.getProperty("org.openide.specification.version")); // NOI18N
    /** Implementation version of the IDE. */
    public static final String IDE_IMPL = System.getProperty("org.openide.version"); // NOI18N
    /** Name, for purposes of dependencies, of the Java platform. */
    public static final String JAVA_NAME = "Java"; // NOI18N
    /** Specification version of the Java platform. */
    public static final SpecificationVersion JAVA_SPEC = ibmHack(makeSpec(System.getProperty("java.specification.version"))); // NOI18N
    /** Implementation version of the Java platform. */
    public static final String JAVA_IMPL = System.getProperty("java.version"); // NOI18N
    /** Name, for purposes of dependencies, of the Java VM. */
    public static final String VM_NAME = "VM"; // NOI18N
    /** Specification version of the Java VM. */
    public static final SpecificationVersion VM_SPEC = makeSpec(System.getProperty("java.vm.specification.version")); // NOI18N
    /** Implementation version of the Java VM. */
    public static final String VM_IMPL = System.getProperty("java.vm.version"); // NOI18N
    

    private final int type, comparison;
    private final String name, version;

    private Dependency(int type, String name, int comparison, String version) {
        this.type = type;
        this.name = name.intern();
        this.comparison = comparison;
        this.version = (version != null) ? version.intern() : null;
    }

    /** Verify the format of a code name.
     * Caller specifies whether a slash plus release version is permitted in this context.
     */
    private static void checkCodeName(String codeName, boolean slashOK) throws IllegalArgumentException {
        String base;
        int slash = codeName.indexOf('/'); // NOI18N
        if (slash == -1) {
            base = codeName;
        } else {
            if (! slashOK) {
                throw new IllegalArgumentException("No slash permitted in: " + codeName); // NOI18N
            }
            base = codeName.substring(0, slash);
            String rest = codeName.substring(slash + 1);
            int dash = rest.indexOf('-'); // NOI18N
            try {
                if (dash == -1) {
                    int release = Integer.parseInt(rest);
                    if (release < 0) throw new IllegalArgumentException("Negative release number: " + codeName); // NOI18N
                } else {
                    int release = Integer.parseInt(rest.substring(0, dash));
                    int releaseMax = Integer.parseInt(rest.substring(dash + 1));
                    if (release < 0) throw new IllegalArgumentException("Negative release number: " + codeName); // NOI18N
                    if (releaseMax <= release) throw new IllegalArgumentException("Release number range must be increasing: " + codeName); // NOI18N
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e.toString());
            }
        }
        // Now check that the rest is a valid package.
        StringTokenizer tok = new StringTokenizer(base, ".", true); // NOI18N
        if (tok.countTokens() % 2 == 0) {
            throw new NumberFormatException("Even number of pieces: " + base); // NOI18N
        }
        boolean expectingPath = true;
        while (tok.hasMoreTokens()) {
            if (expectingPath) {
                expectingPath = false;
                if (! Utilities.isJavaIdentifier(tok.nextToken())) {
                    throw new IllegalArgumentException("Bad package component in " + base); // NOI18N
                }
            } else {
                if (! ".".equals(tok.nextToken())) { // NOI18N
                    throw new NumberFormatException("Expected dot in code name: " + base); // NOI18N
                }
                expectingPath = true;
            }
        }
    }

    /** Parse dependencies from tags.
    * @param type like Dependency.type
    * @param body actual text of tag body; if <code>null</code>, returns nothing
    * @return a set of dependencies
    * @throws IllegalArgumentException if they are malformed or inconsistent
    */
    public static Set create(int type, String body) throws IllegalArgumentException {
        if (body == null) {
            return Collections.EMPTY_SET;
        }
        Set deps = new HashSet(5); // Set<Dependency>
        // First split on commas.
        StringTokenizer tok = new StringTokenizer(body, ","); // NOI18N
        if (! tok.hasMoreTokens()) {
            throw new IllegalArgumentException("No deps given: \"" + body + "\""); // NOI18N
        }
        Map depsByKey = new HashMap(10); // Map<DependencyKey,Dependency>
        while (tok.hasMoreTokens()) {
            String onedep = tok.nextToken();
            StringTokenizer tok2 = new StringTokenizer(onedep, " \t\n\r"); // NOI18N
            if (! tok2.hasMoreTokens()) {
                throw new IllegalArgumentException("No name in dependency: " + onedep); // NOI18N
            }
            String name = tok2.nextToken();
            int comparison;
            String version;
            if (tok2.hasMoreTokens()) {
                String compthing = tok2.nextToken();
                if (compthing.equals(">")) { // NOI18N
                    comparison = Dependency.COMPARE_SPEC;
                } else if (compthing.equals("=")) { // NOI18N
                    comparison = Dependency.COMPARE_IMPL;
                } else {
                    throw new IllegalArgumentException("Strange comparison string: " + compthing); // NOI18N
                }
                if (! tok2.hasMoreTokens()) {
                    throw new IllegalArgumentException("Comparison string without version: " + onedep); // NOI18N
                }
                version = tok2.nextToken();
                if (tok2.hasMoreTokens()) {
                    throw new IllegalArgumentException("Trailing garbage in dependency: " + onedep); // NOI18N
                }
                if (comparison == Dependency.COMPARE_SPEC) {
                    try {
                        new SpecificationVersion(version);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(nfe.toString());
                    }
                }
            } else {
                comparison = Dependency.COMPARE_ANY;
                version = null;
            }
            if (type == Dependency.TYPE_MODULE) {
                checkCodeName (name, true);
                if (name.indexOf('-') != -1 && comparison == Dependency.COMPARE_IMPL) {
                    throw new IllegalArgumentException("Cannot have an implementation dependency on a ranged release version: " + onedep); // NOI18N
                }
            } else if (type == Dependency.TYPE_PACKAGE) {
                int idx = name.indexOf('[');
                if (idx != -1) {
                    if (idx > 0) {
                        checkCodeName(name.substring(0, idx), false);
                    }
                    if (name.charAt(name.length() - 1) != ']') {
                        throw new IllegalArgumentException("No close bracket on package dep: " + name); // NOI18N
                    }
                    checkCodeName(name.substring(idx + 1, name.length() - 1), false);
                } else {
                    checkCodeName(name, false);
                }
                if (idx == 0 && comparison != Dependency.COMPARE_ANY) {
                    throw new IllegalArgumentException("Cannot use a version comparison on a package dependency when only a sample class is given"); // NOI18N
                }
                if (idx > 0 && name.substring(idx + 1, name.length() - 1).indexOf('.') != -1) {
                    throw new IllegalArgumentException("Cannot have a sample class with dots when package is specified"); // NOI18N
                }
            } else if (type == Dependency.TYPE_JAVA) {
                if (! (name.equals(JAVA_NAME) || name.equals(VM_NAME))) {// NOI18N
                    throw new IllegalArgumentException("Java dependency must be on \"Java\" or \"VM\": " + name); // NOI18N
                }
                if (comparison == Dependency.COMPARE_ANY) {
                    throw new IllegalArgumentException("Must give a comparison for a Java dep: " + body); // NOI18N
                }
            } else if (type == Dependency.TYPE_IDE) {
                if (! (name.equals ("IDE"))) { // NOI18N
                    int slash = name.indexOf ("/"); // NOI18N
                    boolean ok;
                    if (slash == -1) {
                        ok = false;
                    } else {
                        if (! name.substring(0, slash).equals("IDE")) { // NOI18N
                            ok = false;
                        }
                        try {
                            int v = Integer.parseInt (name.substring (slash + 1));
                            ok = (v >= 0);
                        } catch (NumberFormatException e) {
                            ok = false;
                        }
                    }
                    if (! ok) {
                        throw new IllegalArgumentException("Invalid IDE dependency: " + name); // NOI18N
                    }
                }
                if (comparison == Dependency.COMPARE_ANY) {
                    throw new IllegalArgumentException("Must give a comparison for an IDE dep: " + body); // NOI18N
                }
            } else if (type == Dependency.TYPE_REQUIRES) {
                if (comparison != Dependency.COMPARE_ANY) {
                    throw new IllegalArgumentException("Cannot give a comparison for a token requires dep: " + body); // NOI18N
                }
                checkCodeName(name, false);
            } else {
                throw new IllegalArgumentException("unknown type"); // NOI18N
            }
            Dependency nue = new Dependency(type, name, comparison, version);
            DependencyKey key = new DependencyKey(nue);
            if (depsByKey.containsKey(key)) {
                throw new IllegalArgumentException("Dependency " + nue + " duplicates the similar dependency " + depsByKey.get(key)); // NOI18N
            } else {
                deps.add(nue);
                depsByKey.put(key, nue);
            }
        }
        return deps;
    }

    /** Key for checking for duplicates among dependencies.
     * The unique characteristics of a dependency are:
     * 1. The basic name. No release versions, no sample classes for packages
     * (though if you specify only the class and not the package, this is different).
     * 2. The type of dependency (module, package, etc.).
     * Sample things which ought not be duplicated:
     * 1. Sample classes within a package.
     * 2. The same module with different release versions (use ranged releases as needed).
     * 3. Impl & spec comparisons (the impl comparison is stricter anyway).
     * 4. Different versions of the same thing (makes no sense).
     */
    private static final class DependencyKey {
        private final int type;
        private final String name;
        public DependencyKey(Dependency d) {
            type = d.getType();
            switch (type) {
            case TYPE_MODULE:
            case TYPE_IDE:
                String codeName = d.getName();
                int idx = codeName.lastIndexOf('/');
                if (idx == -1) {
                    name = codeName;
                } else {
                    name = codeName.substring(0, idx);
                }
                break;
            case TYPE_PACKAGE:
                String pkgName = d.getName();
                idx = pkgName.indexOf('[');
                if (idx != -1) {
                    if (idx == 0) {
                        // [org.apache.jasper.Constants]
                        // Keep the [] only to differentiate it from a package name:
                        name = pkgName;
                    } else {
                        // org.apache.jasper[Constants]
                        name = pkgName.substring(0, idx);
                    }
                } else {
                    // org.apache.jasper
                    name = pkgName;
                }
                break;
            default:
                // TYPE_REQUIRES, TYPE_JAVA
                name = d.getName();
                break;
            }
            //System.err.println("Key for " + d + " is " + this);
        }
        public int hashCode() {
            return name.hashCode();
        }
        public boolean equals(Object o) {
            return (o instanceof DependencyKey) &&
                ((DependencyKey)o).name.equals(name) &&
                ((DependencyKey)o).type == type;
        }
        public String toString() {
            return "DependencyKey[" + name + "," + type + "]"; // NOI18N
        }
    }

    /** Get the type. */
    public final int getType() {
        return type;
    }

    /** Get the name of the depended-on object. */
    public final String getName() {
        return name;
    }

    /** Get the comparison type. */
    public final int getComparison() {
        return comparison;
    }

    /** Get the version to compare against (or null). */
    public final String getVersion() {
        return version;
    }

    /** Overridden to compare contents. */
    public boolean equals(Object o) {
        if (o.getClass() != Dependency.class) return false;
        Dependency d = (Dependency) o;
        return type == d.type &&
               comparison == d.comparison &&
               name.equals(d.name) &&
               Utilities.compareObjects(version, d.version);
    }

    /** Overridden to hash by contents. */
    public int hashCode() {
        return 772067 ^ type ^ name.hashCode();
    }
    
    /** Unspecified string representation for debugging. */
    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        if (type == TYPE_MODULE) {
            buf.append("module "); // NOI18N
        } else if (type == TYPE_PACKAGE) {
            buf.append("package "); // NOI18N
        } else if (type == TYPE_REQUIRES) {
            buf.append("token "); // NOI18N
        }
        buf.append(name);
        if (comparison == COMPARE_IMPL) {
            buf.append(" = "); // NOI18N
            buf.append(version);
        } else if (comparison == COMPARE_SPEC) {
            buf.append(" > "); // NOI18N
            buf.append(version);
        }
        return buf.toString();
    }
    
    /** Try to make a specification version from a string.
     * Deal with errors gracefully and try to recover something from it.
     * E.g. "1.4.0beta" is technically erroneous; correct to "1.4.0".
     */
    private static SpecificationVersion makeSpec(String vers) {
        if (vers != null) {
            try {
                return new SpecificationVersion(vers);
            } catch (NumberFormatException nfe) {
                System.err.println("WARNING: invalid specification version: " + vers); // NOI18N
            }
            do {
                vers = vers.substring(0, vers.length() - 1);
                try {
                    return new SpecificationVersion(vers);
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            } while (vers.length() > 0);
        }
        // Nothing decent in it at all; use zero.
        return new SpecificationVersion("0"); // NOI18N
    }
    
    /** Workaround for bug in IBM JDK 1.3.
     * It claims to be 1.2 as far as Java spec version goes.
     * See issue #12647.
     */
    private static SpecificationVersion ibmHack(SpecificationVersion v) {
        if (v.equals(new SpecificationVersion("1.2")) && // NOI18N
                   "IBM Corporation".equals(System.getProperty("java.vendor")) && // NOI18N
                   "1.3.0".equals(System.getProperty("java.version"))) { // NOI18N
            System.err.println("WARNING - this IBM JDK claims java.specification.version=1.2 but is really 1.3"); // NOI18N
            return new SpecificationVersion("1.3"); // NOI18N
        } else {
            return v;
        }
    }

}
