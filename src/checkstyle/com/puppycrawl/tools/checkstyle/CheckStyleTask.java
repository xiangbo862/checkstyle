////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.io.FileInputStream;
import org.apache.regexp.RESyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

/**
 * An implementation of a ANT task for calling checkstyle. See the documentation
 * of the task for usage.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 **/
public class CheckStyleTask
    extends Task
{
    /** poor man's enum for an xml formatter **/
    private static final String E_XML = "xml";
    /** poor man's enum for an plain formatter **/
    private static final String E_PLAIN = "plain";

    /** name of file to check **/
    private String mFileName;

    /** whether to fail build on violations **/
    private boolean mFailOnViolation = true;

    /** property to set on violations **/
    private String mFailureProperty = null;

    /** contains the filesets to process **/
    private final List mFileSets = new ArrayList();

    /** contains the formatters to log to **/
    private final List mFormatters = new ArrayList();

    /** the configuration to pass to the checker **/
    private Configuration mConfig = new Configuration();

    /** the properties **/
    private Properties mProperties = null;

    /**
     * holds Runnables that change mConfig just
     * before the Checker is created.
     */
    private final ArrayList mOptionMemory = new ArrayList();

    ////////////////////////////////////////////////////////////////////////////
    // Setters for ANT specific attributes
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Tells this task to set the named property to "true" when there
     * is a violation.
     * @param aPropertyName the name of the property to set
     *                      in the event of an failure.
     */
    public void setFailureProperty(String aPropertyName)
    {
        mFailureProperty = aPropertyName;
    }

    /** @param aFail whether to fail if a violation is found **/
    public void setFailOnViolation(boolean aFail)
    {
        mFailOnViolation = aFail;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     * @param aFS the file set to add
     */
    public void addFileset(FileSet aFS)
    {
        mFileSets.add(aFS);
    }

    /**
     * Add a formatter
     * @param aFormatter the formatter to add for logging.
     */
    public void addFormatter(Formatter aFormatter)
    {
        mFormatters.add(aFormatter);
    }


    /** @param aFile the file to be checked **/
    public void setFile(File aFile)
    {
        mFileName = aFile.getAbsolutePath();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Setters for Checker configuration attributes
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Sets a properties file for use instead
     * of individually setting them.
     * @param aProps the properties File to use
     */
    public void setProperties(File aProps)
    {
        Properties mProperties = new Properties();
        try {
            mProperties.load(new FileInputStream(aProps));
            mConfig = new Configuration(mProperties, System.out);
        }
        catch (Exception e) {
            throw new BuildException(
                "Could not find Properties file '" + aProps + "'", location);
        }
    }

    /** @param aAllowed whether tabs are allowed **/
    public void setAllowTabs(final boolean aAllowed)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setAllowTabs(aAllowed);
                }
            });
    }

    /** @param aTabWidth number of spaces that are represented by one tab **/
    public void setTabWidth(final int aTabWidth)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setTabWidth(aTabWidth);
                }
            });
    }

    /** @param aAllowed whether protected data is allowed **/
    public void setAllowProtected(final boolean aAllowed)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setAllowProtected(aAllowed);
                }
            });
    }

    /** @param aAllowed whether package visible data is allowed **/
    public void setAllowPackage(final boolean aAllowed)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setAllowPackage(aAllowed);
                }
            });
    }

    /** @param aAllowed whether allow having no author **/
    public void setAllowNoAuthor(final boolean aAllowed)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setAllowNoAuthor(aAllowed);
                }
            });
    }

    /** @param aLen max allowed line length **/
    public void setMaxLineLen(final int aLen)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setMaxLineLength(aLen);
                }
            });
    }

    /** @param aLen max allowed method length **/
    public void setMaxMethodLen(final int aLen)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setMaxMethodLength(aLen);
                }
            });
    }

    /** @param aLen max allowed constructor length **/
    public void setMaxConstructorLen(final int aLen)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setMaxConstructorLength(aLen);
                }
            });
    }

    /** @param aLen max allowed file length **/
    public void setMaxFileLen(final int aLen)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setMaxFileLength(aLen);
                }
            });
    }

    /** @param aPat line length check exclusion pattern */
    public void setIgnoreLineLengthPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setIgnoreLineLengthPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse ignoreLineLengthPattern - ", ex);
                    }
                }
            });
    }

    /** @param aIgnore whether max line length should be ignored for
     *                 import statements
     */
    public void setIgnoreImportLen(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreImportLength(aIgnore);
                }
            });
    }

    /** @param aPat pattern for member variables **/
    public void setMemberPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setMemberPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse memberPattern - ", ex);
                    }
                }
            });

    }

    /** @param aPat pattern for public member variables **/
    public void setPublicMemberPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setPublicMemberPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse publicMemberPattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for todo lines **/
    public void setTodoPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setTodoPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse todoPattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for parameters **/
    public void setParamPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setParamPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse paramPattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for constant variables **/
    public void setConstPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setStaticFinalPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse constPattern - " , ex);
                    }
                }
            });
    }

    /** @param aPat pattern for static variables **/
    public void setStaticPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setStaticPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse staticPattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for type names **/
    public void setTypePattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setTypePat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse typePattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for local variables **/
    public void setLocalVarPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setLocalVarPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse localVarPattern - ", ex);
                    }
                }
            });
    }

    /** @param aPat pattern for method names **/
    public void setMethodPattern(final String aPat)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setMethodPat(aPat);
                    }
                    catch (RESyntaxException ex) {
                        throw new BuildException(
                            "Unable to parse methodPattern - ", ex);
                    }
                }
            });
    }

    /** @param aName header file name **/
    public void setHeaderFile(final File aName)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    try {
                        mConfig.setHeaderFile(aName.getAbsolutePath());
                    }
                    catch (IOException ex) {
                        throw new BuildException(
                            "Unable to read headerfile - ", ex);
                    }
                }
            });
    }

    /** @param aIsRegexp whether to interpret header lines as regexp */
    public void setHeaderLinesRegexp(final boolean aIsRegexp)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setHeaderLinesRegexp(aIsRegexp);
                }
            });
    }

    /** @param aList Comma separated list of line numbers **/
    public void setHeaderIgnoreLine(final String aList)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setHeaderIgnoreLines(aList);
                }
            });
    }

    /** @param aJavadocScope visibility scope where Javadoc is checked **/
    public void setJavadocScope(final String aJavadocScope)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setJavadocScope(Scope.getInstance(aJavadocScope));
                }
            });
    }

    /** @param aRequirePackageHtml whether package.html is required **/
    public void setRequirePackageHtml(final boolean aRequirePackageHtml)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setRequirePackageHtml(aRequirePackageHtml);
                }
            });
    }

    /** @param aIgnore whether to ignore import statements **/
    public void setIgnoreImports(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreImports(aIgnore);
                }
            });
    }

    /** @param aPkgPrefixList comma separated list of package prefixes */
    public void setIllegalImports(final String aPkgPrefixList)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIllegalImports(aPkgPrefixList);
                }
            });
    }

    /** @param aClassList comma separated list of fully qualified class names */
    public void setIllegalInstantiations(final String aClassList)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIllegalInstantiations(aClassList);
                }
            });
    }

    /** @param aIgnore whether to ignore whitespace **/
    public void setIgnoreWhitespace(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreWhitespace(aIgnore);
                }
            });
    }

    /** @param aIgnore whether to ignore whitespace after casts **/
    public void setIgnoreCastWhitespace(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreCastWhitespace(aIgnore);
                }
            });
    }

    /** @param aIgnore whether to ignore operator wrapping **/
    public void setIgnoreOpWrap(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreOpWrap(aIgnore);
                }
            });
    }

    /** @param aIgnore whether to ignore braces **/
    public void setIgnoreBraces(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnoreBraces(aIgnore);
                }
            });
    }

    /** @param aIgnore whether to ignore 'public' in interfaces **/
    public void setIgnorePublicInInterface(final boolean aIgnore)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setIgnorePublicInInterface(aIgnore);
                }
            });
    }

    /** @param aCacheFile the file to cache which files have been checked **/
    public void setCacheFile(final File aCacheFile)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setCacheFile(aCacheFile.getAbsolutePath());
                }
            });
    }

    /** @param aTo the left curly placement option for methods **/
    public void setLCurlyMethod(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setLCurlyMethod(extractLeftCurlyOption(aTo));
                }
            });
    }

    /** @param aTo the left curly placement option for types **/
    public void setLCurlyType(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setLCurlyType(extractLeftCurlyOption(aTo));
                }
            });
    }

    /** @param aTo the left curly placement option for others **/
    public void setLCurlyOther(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setLCurlyOther(extractLeftCurlyOption(aTo));
                }
            });
    }

    /** @param aTo the right curly placement option **/
    public void setRCurly(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setRCurly(extractRightCurlyOption(aTo));
                }
            });
    }

    /** @param aTo the catch block option **/
    public void setCatchBlock(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setCatchBlock(extractCatchBlockOption(aTo));
                }
            });
    }

    /** @param aTo the parenthesis padding option **/
    public void setParenPad(final String aTo)
    {
        mOptionMemory.add(new Runnable()
            {
                public void run()
                {
                    mConfig.setParenPadOption(extractPadOption(aTo));
                }
            });
    }

    ////////////////////////////////////////////////////////////////////////////
    // The doers
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Actually checks the files specified. All errors are reported to
     * System.out. Will fail if any errors occurred.
     * @throws BuildException an error occurred
     **/
    public void execute()
        throws BuildException
    {
        // Check for no arguments
        if ((mFileName == null) && (mFileSets.size() == 0)) {
            throw new BuildException(
                "Must specify atleast one of 'file' or nested 'fileset'.",
                location);
        }

        // Create the checker
        Checker c = null;
        try {
            try {
                applyExplicitOptions();
                c = new Checker(mConfig);
                // setup the listeners
                AuditListener[] listeners = getListeners();
                for (int i = 0; i < listeners.length; i++) {
                    c.addListener(listeners[i]);
                }
            }
            catch (Exception e) {
                throw new BuildException("Unable to create a Checker", e);
            }

            // Process the files
            final String[] files = scanFileSets();
            final int numErrs = c.process(files);

            // Handle the return status
            if ((numErrs > 0) && mFailureProperty != null) {
                getProject().setProperty(mFailureProperty, "true");
            }

            if ((numErrs > 0) && mFailOnViolation) {
                throw new BuildException("Got " + numErrs + " errors.",
                                         location);
            }
        }
        finally {
            if (c != null) {
                c.destroy();
            }
        }
    }

    /**
     * Return the list of listeners set in this task.
     * @return the list of listeners.
     * @throws ClassNotFoundException if an error occurs
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws IOException if an error occurs
     */
    protected AuditListener[] getListeners()
        throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, IOException
    {
        if (mFormatters.size() == 0) {
            final Formatter f = new Formatter();
            final FormatterType type = new FormatterType();
            type.setValue(E_PLAIN);
            f.setType(type);
            mFormatters.add(f);
        }

        final AuditListener[] listeners = new AuditListener[mFormatters.size()];
        for (int i = 0; i < listeners.length; i++) {
            final Formatter f = (Formatter) mFormatters.get(i);
            listeners[i] = f.createListener(this);
        }
        return listeners;
    }

    /**
     * returns the list of files (full path name) to process.
     * @return the list of files included via the filesets.
     */
    protected String[] scanFileSets()
    {
        final ArrayList list = new ArrayList();
        if (mFileName != null) {
            // oops we've got an additional one to process, don't
            // forget it. No sweat, it's fully resolved via the setter.
            log("Adding standalone file for audit", Project.MSG_VERBOSE);
            list.add(mFileName);
        }
        for (int i = 0; i < mFileSets.size(); i++) {
            final FileSet fs = (FileSet) mFileSets.get(i);
            final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            ds.scan();

            final String[] names = ds.getIncludedFiles();
            log(i + ") Adding " + names.length + " files from directory "
                + ds.getBasedir(),
                Project.MSG_VERBOSE);

            for (int j = 0; j < names.length; j++) {
                final String pathname =
                    ds.getBasedir() + File.separator + names[j];
                list.add(pathname);
            }
        }

        return (String[]) list.toArray(new String[0]);
    }

    /**
     * Poor mans enumeration for the formatter types.
     * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
     */
    public static class FormatterType
        extends EnumeratedAttribute
    {
        /** my possible values **/
        private static final String[] VALUES = {E_XML, E_PLAIN};

        /** @see EnumeratedAttribute **/
        public String[] getValues()
        {
            return VALUES;
        }
    }

    /**
     * Details about a formatter to be used.
     * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
     */
    public static class Formatter
    {
        /** class name of formatter **/
        private String mClassName = null;
        /** the file to output to **/
        private File mToFile = null;

        /**
         * Set the type of the formatter.
         * @param aType the type
         */
        public void setType(FormatterType aType)
        {
            final String val = aType.getValue();
            if (E_XML.equals(val)) {
                setClassname(XMLLogger.class.getName());
            }
            else if (E_PLAIN.equals(val)) {
                setClassname(DefaultLogger.class.getName());
            }
            else {
                throw new BuildException("Invalid formatter type: " + val);
            }
        }

        /**
         * Set the class name of the formatter.
         * @param aTo the formatter class name
         */
        public void setClassname(String aTo)
        {
            mClassName = aTo;
        }

        /**
         * Set the file to output to.
         * @param aTo the file to output to
         */
        public void setTofile(File aTo)
        {
            mToFile = aTo;
        }

        /**
         * Creates a listener for the formatter.
         * @param aTask the task running
         * @return a listener
         * @throws ClassNotFoundException if an error occurs
         * @throws InstantiationException if an error occurs
         * @throws IllegalAccessException if an error occurs
         * @throws IOException if an error occurs
         */
        public AuditListener createListener(Task aTask)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException
        {
            final Class clazz = Class.forName(mClassName);
            final AuditListener listener = (AuditListener) clazz.newInstance();
            listener.setOutputStream(createOutputStream(aTask));
            return listener;
        }

        /**
         * @return an output stream to log with
         * @param aTask the task to possibly log to
         * @throws IOException if an error occurs
         */
        protected OutputStream createOutputStream(Task aTask)
            throws IOException
        {
            if (mToFile == null) {
                return new LogOutputStream(aTask, Project.MSG_INFO);
            }
            return new FileOutputStream(mToFile);
        }
    }

    /**
     * @param aFrom String to decode the option from
     * @return the LeftCurlyOption represented by aFrom
     * @throws BuildException if unable to decode aFrom
     */
    private LeftCurlyOption extractLeftCurlyOption(String aFrom)
        throws BuildException
    {
        final LeftCurlyOption opt = LeftCurlyOption.decode(aFrom);
        if (opt == null) {
            throw new BuildException("Unable to parse '" + aFrom + "'.",
                                     location);
        }
        return opt;
    }

    /**
     * @param aFrom String to decode the option from
     * @return the RightCurlyOption represented by aFrom
     * @throws BuildException if unable to decode aFrom
     */
    private RightCurlyOption extractRightCurlyOption(String aFrom)
        throws BuildException
    {
        final RightCurlyOption opt = RightCurlyOption.decode(aFrom);
        if (opt == null) {
            throw new BuildException("Unable to parse '" + aFrom + "'.",
                                     location);
        }
        return opt;
    }

    /**
     * @param aFrom String to decode the option from
     * @return the CatchBlockOption represented by aFrom
     * @throws BuildException if unable to decode aFrom
     */
    private CatchBlockOption extractCatchBlockOption(String aFrom)
        throws BuildException
    {
        final CatchBlockOption opt = CatchBlockOption.decode(aFrom);
        if (opt == null) {
            throw new BuildException("Unable to parse '" + aFrom + "'.",
                                     location);
        }
        return opt;
    }

    /**
     * @param aFrom String to decode the option from
     * @return the PadOption represented by aFrom
     * @throws BuildException if unable to decode aFrom
     */
    private PadOption extractPadOption(String aFrom)
        throws BuildException
    {
        final PadOption opt = PadOption.decode(aFrom);
        if (opt == null) {
            throw new BuildException("Unable to parse '" + aFrom + "'.",
                                     location);
        }
        return opt;
    }

    /**
     * Applies the options that have been saved in the mOptionMemory.
     */
    private void applyExplicitOptions()
    {
        final Iterator it = mOptionMemory.iterator();
        while (it.hasNext()) {
            final Runnable runnable = (Runnable) it.next();
            runnable.run();
        }
        mOptionMemory.clear();
    }

}
