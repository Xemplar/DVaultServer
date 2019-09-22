import java.math.BigDecimal;

public final class Test {
    public static void main(String[] args){
        System.out.println(getValueHex(new BigDecimal("1.0")));
    }

    public static String getValueHexLE(BigDecimal value){
        int width = 16;
        char fill = '0';

        System.out.println("BEFORE DIV: " + value.toPlainString());
        System.out.println("BEFORE DIV: " + value.toPlainString());

        String toPad = value.multiply(new BigDecimal("100000000")).toBigInteger().toString(16);
        String padded = new String(new char[width - toPad.length()]).replace('\0', fill) + toPad;

        return hex2smallEndian(padded);
    }

    public static String getValueHex(BigDecimal value){
        int width = 16;
        char fill = '0';

        System.out.println("BEFORE DIV: " + value.toPlainString());
        System.out.println("BEFORE DIV: " + value.toPlainString());

        String toPad = value.multiply(new BigDecimal("100000000")).toBigInteger().toString(16);
        String padded = new String(new char[width - toPad.length()]).replace('\0', fill) + toPad;

        return padded;
    }

    public static String hex2smallEndian(String hex){
        String[] dat = split(hex, 2);
        String swapped = "";
        for(int i = dat.length - 1; i >= 0; i--){
            swapped += dat[i];
        }

        return swapped;
    }

    public static String[] split(String str, int count){
        String[] dat = new String[str.length() / count];
        for(int i = 0; i < str.length(); i+=count){
            dat[i / count] = str.substring(i, i + count);
        }

        return dat;
    }
}
