package studio.kdb;

public class Sorter {
    static public void sort(boolean a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || ((a[permutation[t_lo]] ? 1 : 0) <= (a[permutation[t_hi]] ? 1 : 0))))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(String a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]].compareTo(a[permutation[t_hi]]) <= 0)))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(char a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(byte a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(short a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(long a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(float a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (Float.isNaN(a[permutation[t_lo]])) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(double a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (Double.isNaN(a[permutation[t_lo]])) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    static public void sort(int a[],
                            int lo,
                            int hi,
                            int permutation[],
                            int scratch[]) {
        if (lo >= hi)
            return;

        int mid = (lo + hi) / 2;
        sort(a,lo,mid,permutation,scratch);
        sort(a,mid + 1,hi,permutation,scratch);

        int k, t_lo = lo, t_hi = mid + 1;

        for (k = lo;k <= hi;k++)
            if ((t_lo <= mid) && ((t_hi > hi) || (a[permutation[t_lo]] <= a[permutation[t_hi]])))
                scratch[k] = permutation[t_lo++];
            else
                scratch[k] = permutation[t_hi++];

        System.arraycopy(scratch,lo,permutation,lo,1 + hi - lo);
    }

    public static int[] gradeUp(Object data,int length) {
        //int length= Array.getLength(data);
        int[] scratch = new int[length];
        int[] permutation = new int[length];
        for (int i = 0;i < permutation.length;i++)
            permutation[i] = i;

        if (data instanceof int[])
            sort((int[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof boolean[])
            sort((boolean[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof double[])
            sort((double[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof float[])
            sort((float[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof long[])
            sort((long[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof short[])
            sort((short[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof char[])
            sort((char[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof byte[])
            sort((byte[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof String[])
            sort((String[]) data,0,length - 1,permutation,scratch);
        else if (data instanceof K.KBase[]) {
            K.KBase[] generalList = (K.KBase[]) data;
            String[] list = new String[generalList.length];
            for (int index = 0; index < generalList.length; index++) {
                list[index] = generalList[index].toString(false);
            }
            sort(list,0, length-1, permutation, scratch);
        }

        return permutation;
    }

    public static int[] reverse(int[] a) {
        int temp;

        for (int i = 0;i < a.length / 2;i++) {
            temp = a[i];
            a[i] = a[a.length - i - 1];
            a[a.length - i - 1] = temp;
        }

        return a;
    }

    public static int[] gradeDown(Object data,int length) {
        return reverse(gradeUp(data,length));
    }
}
