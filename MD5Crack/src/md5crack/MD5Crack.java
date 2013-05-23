package md5crack;

import helpers.CommonHelper;
import helpers.FileHelper;
import helpers.Reductor;
import helpers.UIHelper;
import java.io.DataInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Lauri Kangassalo / lauri.kangassalo@helsinki.fi
 */
public class MD5Crack {

    private String charset;
    private int minPwLength;
    private int maxPwLength;
    private int chainsPerTable;
    private int chainLength;
    private String filename;
    private UIHelper uihelper;;

    public MD5Crack(String charset,int minPwLength, int maxPwLength, int chainsPerTable, int chainLength, String filename) {
        this.charset = charset;
        this.minPwLength = minPwLength;
        this.maxPwLength = maxPwLength;
        this.chainsPerTable = chainsPerTable;
        this.chainLength = chainLength;
        this.filename = filename;
        
        uihelper = new UIHelper();
    }

    public boolean crackHash(String hashString) {
        FileHelper file = new FileHelper();
        CommonHelper helper = new CommonHelper();
        Reductor reductor = new Reductor(charset, minPwLength,maxPwLength);
        MessageDigest md = helper.getMD5digester();
        HashSet<byte[]> foundEndpoints = new HashSet<byte[]>();
        
        uihelper.startFileRead();
        DataInputStream dis = file.openFile(filename);
        HashMap<byte[], byte[]> table = file.readTable(dis, maxPwLength);
        uihelper.done();
        
        

        byte[] hash = hashString.getBytes();

        // reduce the hash until a known endpoint is found
        for (int i = chainLength - 1; i >= 0; i--) {
            byte[] possibleEndpoint = hash;
            for (int j = i; j < chainLength - 1; j++) {
                possibleEndpoint = reductor.reduce(hash, j);
                possibleEndpoint = md.digest(possibleEndpoint);
            }
            possibleEndpoint = reductor.reduce(possibleEndpoint, chainLength - 1);

            // add the endpoint to a hashset for further analysis
            if (table.containsKey(possibleEndpoint)) {
                foundEndpoints.add(possibleEndpoint);
            }
            
        }
        uihelper.printEndpointCount(foundEndpoints.size());
        
        // loop through matching endpoints to eliminate false alarms
            for (byte[] endpoint : foundEndpoints) {
            byte[] currentPlaintext = table.get(endpoint);
            byte[] currentHash;
            for (int i = 0; i < chainLength; i++) {
                currentHash = md.digest(currentPlaintext);
                if(helper.equalBytes(hash, currentHash)) {
                    // found a matching plaintext for hash
                    uihelper.hashCracked(helper.bytesToString(currentPlaintext, charset));
                    return true;
                }
                currentPlaintext = reductor.reduce(currentHash, i);
            }
        }
        return false;       
    }

}