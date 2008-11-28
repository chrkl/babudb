/*
 * Copyright (c) 2008, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist,
 *                     Felix Hupfeld, Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
*/

package org.xtreemfs.babudb.index.reader;

import org.xtreemfs.babudb.index.ByteRange;
import org.xtreemfs.babudb.index.ByteRangeComparator;


public class SearchUtil {
    
    public static int getInclBottomOffset(MiniPage page, byte[] entry, ByteRangeComparator comp) {
        
        int low = 0;
        int high = page.getNumEntries() - 1;
        
        int mid = high;
        int cmp = 0;
        
        // binary search
        while (low <= high) {
            
            mid = (low + high) >>> 1;
            ByteRange currKey = page.getEntry(mid);
            
            cmp = comp.compare(currKey, entry);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        
        return cmp > 0 ? mid - 1 : mid;
    }
    
    public static int getExclBottomOffset(MiniPage page, byte[] entry, ByteRangeComparator comp) {
        
        int low = 0;
        int high = page.getNumEntries() - 1;
        
        int mid = high;
        int cmp = 0;
        
        // binary search
        while (low <= high) {
            
            mid = (low + high) >>> 1;
            ByteRange currKey = page.getEntry(mid);
            
            cmp = comp.compare(currKey, entry);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid - 1;
        }
        
        return cmp > 0 ? mid - 1 : mid;
    }
    
    public static int getInclTopOffset(MiniPage page, byte[] entry, ByteRangeComparator comp) {
        
        int low = 0;
        int high = page.getNumEntries() - 1;
        
        int mid = high;
        int cmp = 0;
        
        // binary search
        while (low <= high) {
            
            mid = (low + high) >>> 1;
            ByteRange currKey = page.getEntry(mid);
            
            cmp = comp.compare(currKey, entry);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        
        return cmp > 0 ? mid : mid + 1;
    }
    
    public static int getOffset(MiniPage page, byte[] entry, ByteRangeComparator comp) {
        
        int low = 0;
        int high = page.getNumEntries() - 1;
        
        int mid = high;
        int cmp = 0;
        
        // binary search
        while (low <= high) {
            
            mid = (low + high) >>> 1;
            ByteRange currKey = page.getEntry(mid);
            
            cmp = comp.compare(currKey, entry);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        
        return -1;
    }
}
