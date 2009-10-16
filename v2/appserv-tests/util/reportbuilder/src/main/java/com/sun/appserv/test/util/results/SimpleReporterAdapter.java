package com.sun.appserv.test.util.results;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class SimpleReporterAdapter implements Serializable {
    private boolean debug = true;
    private String testSuiteDescription;
    private HashMap testCaseStatus;
    public static final String PASS = "pass";
    public static final String DID_NOT_RUN = "did_not_run";
    public static final String FAIL = "fail";
    private String testSuiteName = "undefined";
    private String testSuiteID = null;
    private String ws_home = "sqe-pe";
    private Reporter reporter = null;

    public SimpleReporterAdapter() {
        testSuiteName = "undefined";
        ws_home = "appserv-tests";
    }

    public SimpleReporterAdapter(String ws_root) {
        this();
        ws_home = ws_root;
    }

    public void addStatus(String s, String status) {
        if (testSuiteName.compareTo("undefined") == 0) {
            int blankIndex = s.indexOf(" ");
            if (blankIndex == -1) {
                testSuiteName = s;
            } else {
                testSuiteName = s.substring(0, blankIndex);
            }
            testSuiteID = s + "ID";
        }
        if (testCaseStatus == null) {
            testCaseStatus = new HashMap(5);
        }
        int blankIndex = s.indexOf(" ");
        String key = s;
        if (blankIndex != -1) {
            key = s.substring(s.indexOf(" "));
        }
        if (debug) {
            System.out.println("Value of key is:" + key);
        }
        if (!testCaseStatus.containsKey(key)) {
            testCaseStatus.put(key, status.toLowerCase());
        }
    }

    public void addDescription(String s) {
        testSuiteDescription = s;
    }

    public void printStatus() {
        try {
            //Reporter reporter;
            Set keySets;
            Iterator keySetsIT;
            if (testSuiteName.compareTo("undefined") == 0) {
                testSuiteName = getTestSuiteName();
            }
            String rootpath = (new File(".")).getAbsolutePath();
            String ejte_home = rootpath.substring(0, rootpath.indexOf(ws_home));
            final String outputDir = ejte_home + ws_home;
            reporter = Reporter.getInstance(ws_home);
            if (debug) {
                System.out.println("Generating report at \t" + outputDir +
                    File.separatorChar + "test_results.xml");
            }
            reporter.setTestSuite(testSuiteID, testSuiteName, testSuiteDescription);
            reporter.addTest(testSuiteID, testSuiteID, testSuiteName);
            keySets = testCaseStatus.keySet();
            keySetsIT = keySets.iterator();
            String tcName;
            int pass = 0;
            int fail = 0;
            int d_n_r = 0;
            String status;
            System.out.println("\n\n-----------------------------------------");
            while (keySetsIT.hasNext()) {
                tcName = keySetsIT.next().toString();
                status = testCaseStatus.get(tcName).toString();
                if (status.equalsIgnoreCase(PASS)) {
                    pass++;
                } else if (status.equalsIgnoreCase(DID_NOT_RUN)) {
                    d_n_r++;
                } else {
                    fail++;
                }
                System.out.println("-\t " + tcName + ": " + status.toUpperCase() + "\t-");
                reporter.addTestCase(testSuiteID, testSuiteID, tcName + "ID", tcName);
                reporter.setTestCaseStatus(testSuiteID, testSuiteID, tcName + "ID", status);
            }
            System.out.println("-----------------------------------------");
            System.out.println("Total PASS:\t" + pass);
            System.out.println("Total FAIL:\t" + fail);
            System.out.println("Total DID NOT RUN: " + d_n_r);
            System.out.println("-----------------------------------------");
            reporter.flushAll();
            createConfirmationFile();
        }
        catch (Throwable ex) {
            System.out.println("Reporter exception occurred!");
            if (debug) {
                ex.printStackTrace();
            }
        }
    }

    public void createConfirmationFile() {
        try {
            FileOutputStream fout = new FileOutputStream("RepRunConf.txt");
            try {
                fout.write("Test has been reported".getBytes());
            } finally {
                fout.close();
            }
        } catch (Exception e) {
            System.out.println("Exception while creating confirmation file!");
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    public void printSummary(String s) {
        printStatus();
    }

    public void printSummary() {
        printStatus();
    }

    public void run() {
        printSummary();
    }

    private String getTestSuiteName() {
        String s = new File("").getAbsolutePath();
        return s.substring(s.lastIndexOf(File.separator) + 1, s.length());
    }

    public void clearStatus() {
        testCaseStatus.clear();
        testSuiteName = "undefined";
    }
}