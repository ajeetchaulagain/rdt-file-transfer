public class CheckSum {

    public static byte createChecksum(byte[] frame) {
        frame[102]  = 0;

        long fcs = 0;

        for (int i=0; i<frame.length; i++) {
            fcs = fcs + frame[i];
        }

        byte modFCS = (byte)( fcs % 128);

        return modFCS;
    }


    public static boolean verifyChecksum(byte[] frame) {
        byte fcs = frame[102];

        byte calculatedFCS = createChecksum(frame);

        if (fcs == calculatedFCS)
            return true;
        else
            return false;
    }

}
