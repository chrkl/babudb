/*
 * Copyright (c) 2008, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist,
 *                     Felix Hupfeld, Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
*/

package org.xtreemfs.babudb.sandbox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.xtreemfs.babudb.index.DefaultByteRangeComparator;
import org.xtreemfs.babudb.index.reader.DiskIndex;
import org.xtreemfs.babudb.index.writer.DiskIndexWriter;
import org.xtreemfs.include.common.logging.Logging;

public class DiskIndexPerformanceTest {
    
    public static void main(String[] args) throws Exception {
   	
        Map<String,CLIParser.CliOption> options = new HashMap();
        options.put("path",new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.FILE,new File("/tmp/babudb_benchmark")));
        options.put("blocksize", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER,16));
        // hitrate in percent
        options.put("hitrate", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER,10));
        options.put("keylength", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER,8));
        options.put("debug", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER,Logging.LEVEL_EMERG));
        options.put("h", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.SWITCH, false));

        List<String> arguments = new ArrayList(1);
        CLIParser.parseCLI(args, options, arguments);

        if (arguments.size() != 3) {
            System.out.println("usage: java " + DiskIndexPerformanceTest.class.getCanonicalName()
                + " [options] <db_file> <num_entries> <num_lookups>");
            System.exit(1);
        }

    	// start logging when executing this without the entire BabuDB stack
    	Logging.start(options.get("debug").numValue.intValue());
    	
        final String path = arguments.get(0);
        final Long entriesPerBlock = options.get("blocksize").numValue;
        final int hitrate = options.get("hitrate").numValue.intValue();
        
        final int minStrLen = 1;
        final int maxStrLen = options.get("keylength").numValue.intValue();
        final char minChar = 48;
        final char maxChar = 122;
        
        final int size = Integer.parseInt(arguments.get(1));
        final int lookups = Integer.parseInt(arguments.get(2));
        final Random generator = new Random();
        
        boolean verbose = false;
        final ArrayList<byte[]> lookupHits = new ArrayList<byte[]>((int) (hitrate*size) + 1);
        
        if (size != 0) {
            // delete old index file
            new File(path).delete();
            
            if(verbose)
            	System.out.println("creating new database with " + size + " random entries ...");
            
            // write the map to a disk index
            DiskIndexWriter index = new DiskIndexWriter(path, entriesPerBlock.intValue());
            index.writeIndex(new Iterator<Entry<byte[], byte[]>>() {
                
                private int    count = 0;
                
                private String next = minChar + "";
                
                @Override
                public boolean hasNext() {
                    return count < size;
                }
                
                @Override
                public Entry<byte[], byte[]> next() {
                    
                    count++;
                    next = createNextString(next, minStrLen, maxStrLen, minChar, maxChar);

                    if(generator.nextInt() % hitrate == 0)
                    	lookupHits.add(next.getBytes());
                    
                    return new Entry<byte[], byte[]>() {
                        
                        final byte[] nextBytes = next.getBytes();
                        
                        @Override
                        public byte[] getKey() {
                            return nextBytes;
                        }
                        
                        @Override
                        public byte[] getValue() {
                            return nextBytes;
                        }
                        
                        @Override
                        public byte[] setValue(byte[] value) {
                            throw new UnsupportedOperationException();
                        }
                        
                    };
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
            });
        }
        
        // read the disk index
        DiskIndex diskIndex = new DiskIndex(path, new DefaultByteRangeComparator());
        
        Iterator<Entry<byte[], byte[]>> it = diskIndex.rangeLookup(null, null, true);
        
        /* iterate over all data in the disk index to measure the prefix lookup throughput */
        long iterStart = System.currentTimeMillis();
        while(it.hasNext()) it.next();
        long iterTime = System.currentTimeMillis() - iterStart;

        // Iterator<Entry<ReusableBuffer, ReusableBuffer>> it =
        // diskIndex.rangeLookup(null, null);
        // while (it.hasNext())
        // System.out.println(new String(it.next().getKey().array()));
        //
        if(verbose)
        	System.out.println("performing " + lookups + " random lookups ...");
        
        // look up each element
        int hits = 0;
        long sumLookups = 0;
        
        Collections.shuffle(lookupHits);
        
        for (int i = 0; i < lookups; i++) {
            byte[] key;
            
            /* pick a random element that is in the index according to the given hitrate */
        	if(generator.nextInt() % hitrate == 0) {
        		key = lookupHits.get(Math.abs(generator.nextInt()) % lookupHits.size());
        	} else {
        		key = createRandomString(minChar, maxChar, maxStrLen+1, maxStrLen*2).getBytes();
        	}
        	
            long t0 = System.currentTimeMillis();
            byte[] result = diskIndex.lookup(key);
            sumLookups += System.currentTimeMillis() - t0;
            if (result != null)
                hits++;
            
            //if (i % 100000 == 0 && verbose)
            //    System.out.println(i);
        }
        
        System.out.print(size + ", ");
        System.out.print(lookups + ", ");
        System.out.print(hits + ", ");
        System.out.print(sumLookups + ", ");
        System.out.print((int) Math.ceil(((double) lookups / (double) sumLookups) * 1000.0) + ", ");
        System.out.println((int) Math.ceil(((double) size / (double) iterTime) * 1000.0));
        
        diskIndex.destroy();
    }
    
    private static String createNextString(String st, int minStrLen, int maxStrLen, char minChar,
        char maxChar) {
        
        char[] chars = st.toCharArray();
        
        for (;;) {
            
            double rnd = Math.random();
            
            if (rnd < .1f + .8f / chars.length && chars.length < maxStrLen) {
                
                // append character
                char[] chars2 = new char[chars.length + 1];
                System.arraycopy(chars, 0, chars2, 0, chars.length);
                chars2[chars2.length - 1] = createRandomChar(minChar, maxChar);
                return new String(chars2);
                
            } else if (rnd > .95f + .05f / chars.length && chars.length > minStrLen) {
                
                int i = chars.length - 2;
                for (; i >= 0; i--)
                    if (chars[i] < maxChar)
                        break;
                
                if (i == -1)
                    continue;
                
                // increment character and truncate
                char[] chars2 = new char[i + 1];
                System.arraycopy(chars, 0, chars2, 0, chars2.length);
                chars2[chars2.length - 1]++;
                return new String(chars2);
                
            } else if (chars[chars.length - 1] < maxChar) {
                
                // increment last character
                chars[chars.length - 1]++;
                return new String(chars);
            }
        }
    }
    
    private static String createRandomString(char minChar, char maxChar, int minLength,
        int maxLength) {
        
        char[] chars = new char[(int) (Math.random() * (maxLength + 1)) + minLength];
        for (int i = 0; i < chars.length; i++)
            chars[i] = createRandomChar(minChar, maxChar);
        
        return new String(chars);
    }
    
    private static char createRandomChar(char minChar, char maxChar) {
        return (char) (Math.random() * (maxChar - minChar + 1) + minChar);
    }
    
}