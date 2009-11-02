/*
 * Copyright (c) 2009, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist,
 *                     Felix Hupfeld, Felix Langner, Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */

package org.xtreemfs.babudb.lsmdb;

import java.io.IOException;
import java.util.Map;

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.index.ByteRangeComparator;

public interface DatabaseManager {
    
    /**
     * Returns the database with the given name.
     * 
     * @param dbName
     *            the database name
     * @return the database
     * @throws BabuDBException
     *             if the database does not exist
     */
    public Database getDatabase(String dbName) throws BabuDBException;
    
    /**
     * Returns a map containing all databases.
     * 
     * @return a map containing all databases
     * @throws BabuDBException
     *             if an error occurs
     */
    public Map<String, Database> getDatabases() throws BabuDBException;
    
    /**
     * Creates a new database.
     * 
     * @param databaseName
     *            name, must be unique
     * @param numIndices
     *            the number of indices (cannot be changed afterwards)
     * @return the newly created database
     * @throws BabuDBException
     *             if the database directory cannot be created or the config
     *             cannot be saved
     */
    public Database createDatabase(String databaseName, int numIndices) throws BabuDBException;
    
    /**
     * Creates a new database.
     * 
     * @param databaseName
     *            name, must be unique
     * @param numIndices
     *            the number of indices (cannot be changed afterwards)
     * @param comparators
     *            an array of ByteRangeComparators for each index (use only one
     *            instance)
     * @return the newly created database
     * @throws BabuDBException
     *             if the database directory cannot be created or the config
     *             cannot be saved
     */
    public Database createDatabase(String databaseName, int numIndices, ByteRangeComparator[] comparators)
        throws BabuDBException;
    
    /**
     * Deletes a database.
     * 
     * @param databaseName
     *            name of database to delete
     * @throws BabuDBException
     */
    public void deleteDatabase(String databaseName) throws BabuDBException;
    
    /**
     * Creates a copy of database sourceDB by taking a snapshot, materializing
     * it and loading it as destDB. This does not interrupt operations on
     * sourceDB.
     * 
     * @param sourceDB
     *            the database to copy
     * @param destDB
     *            the new database's name
     * @throws BabuDBException
     * @throws IOException
     */
    public void copyDatabase(String sourceDB, String destDB) throws BabuDBException, IOException,
        InterruptedException;
    
}